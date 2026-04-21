package com.cortex.app.feature.home

import app.cash.turbine.test
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(): HomeViewModel {
        val contentRepo = mockk<ContentRepository> {
            every { getAllLessons() } returns emptyList()
            every { getLesson(any()) } returns null
        }
        val progressRepo = mockk<ProgressRepository> {
            every { observeAllProgress() } returns flowOf(emptyList())
        }
        val schedulerRepo = mockk<SchedulerRepository> {
            every { observeDueCount() } returns flowOf(0)
        }
        return HomeViewModel(contentRepo, progressRepo, schedulerRepo)
    }

    @Test
    fun `emits initial loading then stable state on subscribe`() = runTest(testDispatcher) {
        val vm = buildVm()
        vm.state.test {
            val loading = awaitItem()
            assertEquals(true, loading.isLoading)

            advanceUntilIdle()

            val ready = awaitItem()
            assertEquals(false, ready.isLoading)
            assertEquals(0, ready.dueReviewCount)
            assertNull(ready.newLessonTitle)
            assertEquals(0, ready.streakDays)
            assertNull(ready.resumeLesson)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
