package com.cortex.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.cortex.app.core.database.CortexDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ProgressRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: CortexDatabase
    private lateinit var repo: ProgressRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, CortexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ProgressRepositoryImpl(db.lessonProgressDao(), db.practiceAttemptDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        stopKoin()
    }

    @Test
    fun `first recordStageAdvance creates row with startedAt set`() = runTest(testDispatcher) {
        repo.recordStageAdvance("lesson-1", 1, 5)
        val progress = repo.observeProgress("lesson-1").first()
        assertNotNull(progress)
        assertNotNull(progress!!.startedAt)
        assertEquals(1, progress.currentStage)
    }

    @Test
    fun `subsequent advances update currentStage and preserve startedAt`() = runTest(testDispatcher) {
        repo.recordStageAdvance("lesson-1", 1, 5)
        val startedAt = repo.observeProgress("lesson-1").first()!!.startedAt

        repo.recordStageAdvance("lesson-1", 2, 5)
        repo.recordStageAdvance("lesson-1", 3, 5)

        val progress = repo.observeProgress("lesson-1").first()!!
        assertEquals(3, progress.currentStage)
        assertEquals(startedAt, progress.startedAt)
    }

    @Test
    fun `reaching newStage equal to totalStages sets masteredAt`() = runTest(testDispatcher) {
        repo.recordStageAdvance("lesson-1", 1, 5)
        assertNull(repo.observeProgress("lesson-1").first()!!.masteredAt)

        repo.recordStageAdvance("lesson-1", 5, 5)
        assertNotNull(repo.observeProgress("lesson-1").first()!!.masteredAt)
    }

    @Test
    fun `masteredAt is not overwritten once set`() = runTest(testDispatcher) {
        repo.recordStageAdvance("lesson-1", 5, 5)
        val masteredAt = repo.observeProgress("lesson-1").first()!!.masteredAt!!

        // Extra advance shouldn't overwrite masteredAt
        repo.recordStageAdvance("lesson-1", 5, 5)
        assertEquals(masteredAt, repo.observeProgress("lesson-1").first()!!.masteredAt)
    }

    @Test
    fun `recordPracticeAttempt persists and flows back out`() = runTest(testDispatcher) {
        repo.recordPracticeAttempt("lesson-1", "problem-1", correct = true)
        repo.recordPracticeAttempt("lesson-1", "problem-1", correct = false)

        repo.observePracticeAttempts("lesson-1").test {
            val attempts = awaitItem()
            assertEquals(2, attempts.size)
            assertEquals(true, attempts[0].correct)
            assertEquals(false, attempts[1].correct)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeProgress emits null for unknown lessonId`() = runTest(testDispatcher) {
        val result = repo.observeProgress("unknown-lesson").first()
        assertNull(result)
    }
}
