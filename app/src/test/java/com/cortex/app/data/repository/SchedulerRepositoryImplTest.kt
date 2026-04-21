package com.cortex.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.cortex.app.core.database.CortexDatabase
import com.cortex.app.domain.model.LessonReviewCard
import com.cortex.app.domain.scheduler.CardState
import com.cortex.app.domain.scheduler.FsrsParameters
import com.cortex.app.domain.scheduler.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SchedulerRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: CortexDatabase
    private lateinit var repo: SchedulerRepositoryImpl

    private val fixedNow = Instant.parse("2024-01-01T09:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = fixedNow
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, CortexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SchedulerRepositoryImpl(db.reviewCardDao(), fixedClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        stopKoin()
    }

    // -------------------------------------------------------------------------
    // Seed tests
    // -------------------------------------------------------------------------

    @Test
    fun `seedCardsForLesson inserts new cards with dueAt == now`() = runTest(testDispatcher) {
        val authored = listOf(
            LessonReviewCard("inv", "What is the loop invariant?", "Left < right at all times."),
        )
        repo.seedCardsForLesson("two-pointers", authored)

        val card = db.reviewCardDao().getById("two-pointers:inv")
        assertNotNull(card)
        assertEquals(fixedNow.toEpochMilliseconds(), card!!.dueAt)
        assertEquals(CardState.Learning.ordinal, card.state)
        assertEquals(0, card.reps)
    }

    @Test
    fun `seedCardsForLesson is idempotent - existing cards are not reset`() = runTest(testDispatcher) {
        val authored = listOf(
            LessonReviewCard("inv", "What is the loop invariant?", "Left < right."),
        )
        // Seed once
        repo.seedCardsForLesson("two-pointers", authored)
        // Grade it (this changes state)
        repo.grade("two-pointers:inv", Rating.Good)

        val afterGrade = db.reviewCardDao().getById("two-pointers:inv")!!
        assertEquals(1, afterGrade.reps)

        // Seed again — should not reset
        repo.seedCardsForLesson("two-pointers", authored)

        val afterReseed = db.reviewCardDao().getById("two-pointers:inv")!!
        assertEquals("reps must not be reset by re-seeding", 1, afterReseed.reps)
    }

    // -------------------------------------------------------------------------
    // Grade tests
    // -------------------------------------------------------------------------

    @Test
    fun `grading Good on a new card increments reps and sets dueAt in the future`() = runTest(testDispatcher) {
        repo.seedCardsForLesson("lesson", listOf(LessonReviewCard("q1", "Prompt", "Answer")))
        repo.grade("lesson:q1", Rating.Good)

        val card = db.reviewCardDao().getById("lesson:q1")!!
        assertEquals(1, card.reps)
        assertTrue("dueAt must be after fixedNow", card.dueAt > fixedNow.toEpochMilliseconds())
    }

    @Test
    fun `grading Again on Review card moves to Relearning`() = runTest(testDispatcher) {
        val cardId = "lesson:q1"
        repo.seedCardsForLesson("lesson", listOf(LessonReviewCard("q1", "Prompt", "Answer")))

        // Graduate the card to Review state (Good → Good)
        repo.grade(cardId, Rating.Good)
        repo.grade(cardId, Rating.Good)
        val afterTwo = db.reviewCardDao().getById(cardId)!!
        assertEquals("Should be Review after graduating", CardState.Review.ordinal, afterTwo.state)

        // Now fail it
        repo.grade(cardId, Rating.Again)
        val afterAgain = db.reviewCardDao().getById(cardId)!!
        assertEquals("Again on Review -> Relearning", CardState.Relearning.ordinal, afterAgain.state)
        assertEquals("lapses incremented", 1, afterAgain.lapses)
    }

    // -------------------------------------------------------------------------
    // observeDueCount reacts to card state changes
    // -------------------------------------------------------------------------

    @Test
    fun `observeDueCount emits 1 immediately after seeding (dueAt == now)`() = runTest(testDispatcher) {
        repo.seedCardsForLesson("lesson", listOf(LessonReviewCard("q1", "Prompt", "Answer")))

        repo.observeDueCount().test {
            assertEquals("One card is due immediately", 1, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `grading Good reduces due count to 0 (card rescheduled in future)`() = runTest(testDispatcher) {
        repo.seedCardsForLesson("lesson", listOf(LessonReviewCard("q1", "Prompt", "Answer")))
        // Graduate to Review so the next interval is days-based
        repo.grade("lesson:q1", Rating.Good)  // step 0 -> step 1
        repo.grade("lesson:q1", Rating.Good)  // graduates

        // After graduation the card is due in N days; far future relative to fixedNow
        val count = repo.observeDueCount().first()
        assertEquals("Card due in future after graduating", 0, count)
    }

    // -------------------------------------------------------------------------
    // observeDueCards ordering
    // -------------------------------------------------------------------------

    @Test
    fun `observeDueCards returns cards ordered by dueAt ascending`() = runTest(testDispatcher) {
        repo.seedCardsForLesson(
            "lesson",
            listOf(
                LessonReviewCard("a", "Prompt A", "Answer A"),
                LessonReviewCard("b", "Prompt B", "Answer B"),
            ),
        )

        repo.observeDueCards().test {
            val cards = awaitItem()
            assertEquals(2, cards.size)
            assertTrue(
                "Cards ordered by dueAt",
                cards[0].dueAt <= cards[1].dueAt,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
