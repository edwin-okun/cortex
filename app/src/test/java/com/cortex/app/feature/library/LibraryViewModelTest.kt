package com.cortex.app.feature.library

import app.cash.turbine.test
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.LessonStage
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

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
    fun `locks tiered lessons that are not available yet`() = runTest(testDispatcher) {
        val foundations = lesson("foundations", Tier.FOUNDATIONS)
        val intermediate = lesson("intermediate", Tier.INTERMEDIATE)
        val vm = buildVm(
            lessons = listOf(foundations, intermediate),
            progress = emptyList(),
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            val ready = awaitItem()

            val foundationsItem = ready.lessons.first { it.lessonId == "foundations" }
            val intermediateItem = ready.lessons.first { it.lessonId == "intermediate" }
            assertTrue(foundationsItem.isActionEnabled)
            assertEquals("START", foundationsItem.ctaLabel)
            assertFalse(intermediateItem.isActionEnabled)
            assertEquals("LOCKED", intermediateItem.ctaLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mastered lessons are enabled and open in restart mode`() = runTest(testDispatcher) {
        val mastered = lesson("mastered", Tier.FOUNDATIONS)
        val vm = buildVm(
            lessons = listOf(mastered),
            progress = listOf(masteredProgress(stage = mastered.stages.size)),
        )

        vm.state.test {
            awaitItem()
            advanceUntilIdle()
            val ready = awaitItem()
            val item = ready.lessons.single()

            assertEquals("REVISIT", item.ctaLabel)
            assertTrue(item.isActionEnabled)
            assertTrue(item.restartOnOpen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildVm(
        lessons: List<Lesson>,
        progress: List<LessonProgress>,
    ): LibraryViewModel {
        val contentRepository = mockk<ContentRepository> {
            every { getAllLessons() } returns lessons
            every { getLesson(any()) } answers {
                lessons.firstOrNull { it.id == firstArg<String>() }
            }
        }
        val progressRepository = mockk<ProgressRepository> {
            every { observeAllProgress() } returns flowOf(progress)
        }
        return LibraryViewModel(contentRepository, progressRepository)
    }

    private fun lesson(id: String, tier: Tier) = Lesson(
        id = id,
        track = Track.ALGORITHMS,
        tier = tier,
        title = id,
        stages = listOf(
            LessonStage.Hook("problem", "stakes"),
            LessonStage.Intuition(listOf("narration"), null),
        ),
        reviewCards = emptyList(),
    )

    private fun masteredProgress(stage: Int) = LessonProgress(
        lessonId = "mastered",
        currentStage = stage,
        stagesCompleted = stage,
        startedAt = 1_000L,
        masteredAt = 2_000L,
        lastOpenedAt = 3_000L,
    )
}
