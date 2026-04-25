package com.cortex.app.feature.session

import app.cash.turbine.test
import com.cortex.app.data.store.SessionConfigStore
import com.cortex.app.domain.model.DailySession
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.model.SessionConfig
import com.cortex.app.domain.model.SessionItem
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.CardState
import com.cortex.app.domain.scheduler.Rating
import com.cortex.app.domain.usecase.BuildDailySessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `duplicate grade taps only grade and advance one card`() = runTest(testDispatcher) {
        val first = card("lesson:first")
        val second = card("lesson:second")
        val gradeStarted = CompletableDeferred<Unit>()
        val allowGradeToFinish = CompletableDeferred<Unit>()
        val schedulerRepository = schedulerRepo {
            gradeStarted.complete(Unit)
            allowGradeToFinish.await()
        }
        val vm = buildVm(
            items = listOf(SessionItem.ReviewCard(first), SessionItem.ReviewCard(second)),
            schedulerRepository = schedulerRepository,
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            vm.onGrade(Rating.Good)
            advanceUntilIdle()
            gradeStarted.await()

            vm.onGrade(Rating.Easy)
            allowGradeToFinish.complete(Unit)
            advanceUntilIdle()

            val active = awaitItem() as SessionUiState.Active
            val current = active.currentItem as SessionItem.ReviewCard
            assertEquals("lesson:second", current.card.cardId)
            assertEquals(1, active.progress)
            coVerify(exactly = 1) { schedulerRepository.grade("lesson:first", any()) }
            coVerify(exactly = 0) { schedulerRepository.grade("lesson:second", any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stale lesson returned signal is ignored when active item is a review`() = runTest(testDispatcher) {
        val vm = buildVm(items = listOf(SessionItem.ReviewCard(card("lesson:first"))))

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            vm.onLessonReturned()

            val active = vm.state.value as SessionUiState.Active
            assertTrue(active.currentItem is SessionItem.ReviewCard)
            assertEquals(0, active.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lesson returned only advances when current item is a new lesson`() = runTest(testDispatcher) {
        val vm = buildVm(
            items = listOf(
                SessionItem.NewLesson(newLesson()),
                SessionItem.ReviewCard(card("lesson:first")),
            ),
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem()

            vm.onLessonReturned()

            val active = awaitItem() as SessionUiState.Active
            assertTrue(active.currentItem is SessionItem.ReviewCard)
            assertEquals(1, active.progress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildVm(
        items: List<SessionItem>,
        schedulerRepository: SchedulerRepository = schedulerRepo(),
    ): SessionViewModel {
        val useCase = mockk<BuildDailySessionUseCase> {
            coEvery { this@mockk.invoke(any()) } returns DailySession(
                items = items,
                totalReviews = items.filterIsInstance<SessionItem.ReviewCard>().size,
                totalNewLessons = items.filterIsInstance<SessionItem.NewLesson>().size,
                generatedAt = Instant.parse("2024-01-01T09:00:00Z"),
            )
        }
        val configStore = mockk<SessionConfigStore> {
            every { observe() } returns flowOf(SessionConfig())
        }
        return SessionViewModel(useCase, schedulerRepository, configStore)
    }

    private fun schedulerRepo(
        onGrade: suspend () -> Unit = {},
    ): SchedulerRepository = mockk {
        coEvery { seedCardsForLesson(any(), any()) } just runs
        coEvery { grade(any(), any()) } coAnswers { onGrade() }
        every { observeDueCards() } returns flowOf(emptyList())
        every { observeDueCount() } returns flowOf(0)
    }

    private fun newLesson() = Lesson(
        id = "new-lesson",
        track = Track.ALGORITHMS,
        tier = Tier.FOUNDATIONS,
        title = "new-lesson",
        stages = emptyList(),
        reviewCards = emptyList(),
    )

    private fun card(id: String) = ReviewCard(
        cardId = id,
        lessonId = "lesson",
        prompt = "Prompt",
        answer = "Answer",
        stability = 0.0,
        difficulty = 0.0,
        elapsedDays = 0,
        scheduledDays = 0,
        reps = 0,
        lapses = 0,
        state = CardState.Learning,
        lastReview = null,
        dueAt = Instant.parse("2024-01-01T09:00:00Z"),
        step = 0,
    )
}
