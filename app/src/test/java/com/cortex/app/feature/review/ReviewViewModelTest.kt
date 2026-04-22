package com.cortex.app.feature.review

import app.cash.turbine.test
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.CardState
import io.mockk.every
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

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
    fun `transitions from empty to reviewing when due cards later appear`() = runTest(testDispatcher) {
        val dueCards = MutableStateFlow<List<ReviewCard>>(emptyList())
        val repo = makeSchedulerRepo(dueCards)
        val vm = ReviewViewModel(repo)

        vm.state.test {
            assertEquals(ReviewUiState.Loading, awaitItem())

            advanceUntilIdle()
            assertEquals(ReviewUiState.Empty, awaitItem())

            dueCards.value = listOf(makeReviewCard())
            advanceUntilIdle()

            val reviewing = awaitItem() as ReviewUiState.Reviewing
            assertEquals("lesson:q1", reviewing.card.cardId)
            assertEquals(1, reviewing.queueSize)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeSchedulerRepo(dueCards: Flow<List<ReviewCard>>): SchedulerRepository = mockk {
        coEvery { seedCardsForLesson(any(), any()) } just runs
        coEvery { grade(any(), any()) } just runs
        every { observeDueCards() } returns dueCards
        every { observeDueCount() } returns MutableStateFlow(0)
    }

    private fun makeReviewCard() = ReviewCard(
        cardId = "lesson:q1",
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
