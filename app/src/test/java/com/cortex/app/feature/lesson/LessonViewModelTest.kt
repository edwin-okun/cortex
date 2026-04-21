package com.cortex.app.feature.lesson

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.LessonStage
import com.cortex.app.domain.model.PracticeAttempt
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.usecase.GetLessonUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeLesson(id: String = "test-lesson") = Lesson(
        id = id,
        track = Track.ALGORITHMS,
        tier = Tier.FOUNDATIONS,
        title = "Test Lesson",
        stages = listOf(
            LessonStage.Hook("problem", "stakes"),
            LessonStage.Intuition(listOf("narration"), null),
            LessonStage.WorkedExample(listOf("step 1")),
            LessonStage.FadedPractice(emptyList()),
            LessonStage.TransferProblem("prompt", "solution"),
        ),
        reviewCards = emptyList(),
    )

    private fun makeProgressRepo(
        progress: LessonProgress? = null,
    ): ProgressRepository = mockk {
        every { observeProgress(any()) } returns flowOf(progress)
        every { observeAllProgress() } returns flowOf(emptyList())
        every { observePracticeAttempts(any()) } returns flowOf(emptyList())
        coEvery { recordLessonOpened(any()) } just runs
        coEvery { recordStageAdvance(any(), any(), any()) } just runs
        coEvery { recordPracticeAttempt(any(), any(), any()) } just runs
    }

    private fun makeUseCase(lesson: Lesson?): GetLessonUseCase = mockk<GetLessonUseCase>().also {
        every { it.invoke(any()) } returns lesson
    }

    private fun makeSchedulerRepo(): SchedulerRepository = mockk {
        coEvery { seedCardsForLesson(any(), any()) } just runs
    }

    private fun buildVm(
        lessonId: String = "test-lesson",
        lesson: Lesson? = makeLesson(lessonId),
        progress: LessonProgress? = null,
    ) = LessonViewModel(
        savedStateHandle = SavedStateHandle(mapOf("lessonId" to lessonId)),
        getLessonUseCase = makeUseCase(lesson),
        progressRepository = makeProgressRepo(progress),
        schedulerRepository = makeSchedulerRepo(),
    )

    @Test
    fun `with no saved progress starts at stage 0`() = runTest(testDispatcher) {
        val vm = buildVm(progress = null)

        vm.state.test {
            awaitItem() // initial loading state
            advanceUntilIdle()
            val ready = awaitItem()
            assertEquals(0, ready.currentStageIndex)
            assertEquals(false, ready.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `with saved progress at stage 2 starts at stage 2`() = runTest(testDispatcher) {
        val savedProgress = LessonProgress(
            lessonId = "test-lesson",
            currentStage = 2,
            stagesCompleted = 2,
            startedAt = 1000L,
            masteredAt = null,
            lastOpenedAt = 2000L,
        )
        val vm = buildVm(progress = savedProgress)

        vm.state.test {
            awaitItem() // loading
            advanceUntilIdle()
            val ready = awaitItem()
            assertEquals(2, ready.currentStageIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onStageAdvance invokes repository with correct args`() = runTest(testDispatcher) {
        val lesson = makeLesson()
        val progressRepo = makeProgressRepo()
        val vm = LessonViewModel(
            savedStateHandle = SavedStateHandle(mapOf("lessonId" to "test-lesson")),
            getLessonUseCase = makeUseCase(lesson),
            progressRepository = progressRepo,
            schedulerRepository = makeSchedulerRepo(),
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            awaitItem() // ready at stage 0

            vm.onStageAdvance()
            advanceUntilIdle()
            awaitItem() // stage 1

            coVerify { progressRepo.recordStageAdvance("test-lesson", 1, lesson.stages.size) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
