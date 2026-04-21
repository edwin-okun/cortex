package com.cortex.app.domain.scheduler

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

/**
 * Reference test vectors sourced from the official py-fsrs test suite:
 *   https://github.com/open-spaced-repetition/py-fsrs/blob/main/tests/test_basic.py
 *
 * Vector 1 (test_review_card): 13-rating sequence → known interval history
 * Vector 2 (test_memo_state):  6-rating sequence at fixed offsets → final S and D
 */
class FsrsTest {

    private val params = FsrsParameters.Default
    // Base epoch for deterministic tests — 2022-11-29T12:30:00Z (matches py-fsrs fixture)
    private val epoch = Instant.parse("2022-11-29T12:30:00Z")

    // -------------------------------------------------------------------------
    // Helper: replay a rating sequence, advance time by the scheduled interval
    // -------------------------------------------------------------------------

    private data class Step(val rating: Rating, val offsetSeconds: Long = 0)

    /**
     * Simulates the py-fsrs Scheduler.review_card loop with enable_fuzzing=False.
     * Returns the interval (in whole days) after each review, matching py-fsrs's
     *   ivl = (card.due - card.last_review).days
     */
    private fun replayIntervals(ratings: List<Rating>): List<Int> {
        var card = FsrsCard.new()
        var now = epoch
        val ivls = mutableListOf<Int>()

        for (rating in ratings) {
            card = Fsrs.schedule(card, rating, now, params)
            val due = now + Fsrs.nextDue(card, params)
            val ivl = ((due - now).inWholeSeconds / 86400).toInt()
            ivls.add(ivl)
            now = due
        }
        return ivls
    }

    /**
     * Simulates the py-fsrs test_memo_state test:
     * Ratings and explicit elapsed-day offsets. Advances clock by the given days
     * rather than by the computed interval, to stress-test the formula with
     * specific elapsed times.
     */
    private fun replayWithOffsets(steps: List<Step>): FsrsCard {
        var card = FsrsCard.new()
        var now = epoch
        for ((rating, offsetSec) in steps) {
            now += offsetSec.seconds
            card = Fsrs.schedule(card, rating, now, params)
        }
        return card
    }

    // -------------------------------------------------------------------------
    // Reference vector 1 — interval sequence
    // py-fsrs: ivl_history == [0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21]
    // -------------------------------------------------------------------------

    @Test
    fun `reference vector 1 - interval sequence matches py-fsrs`() {
        val ratings = listOf(
            Rating.Good, Rating.Good, Rating.Good, Rating.Good, Rating.Good, Rating.Good,
            Rating.Again, Rating.Again,
            Rating.Good, Rating.Good, Rating.Good, Rating.Good, Rating.Good,
        )
        val expected = listOf(0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21)
        val actual = replayIntervals(ratings)
        assertEquals("Interval sequence must match py-fsrs reference", expected, actual)
    }

    // -------------------------------------------------------------------------
    // Reference vector 2 — final stability and difficulty
    // py-fsrs: stability ≈ 53.62691, difficulty ≈ 6.3574867
    // -------------------------------------------------------------------------

    @Test
    fun `reference vector 2 - final stability and difficulty match py-fsrs`() {
        // elapsed days between reviews: [0, 0, 1, 3, 8, 21]
        val steps = listOf(
            Step(Rating.Again, offsetSeconds = 0),
            Step(Rating.Good,  offsetSeconds = 0),
            Step(Rating.Good,  offsetSeconds = 1 * 86400L),
            Step(Rating.Good,  offsetSeconds = 3 * 86400L),
            Step(Rating.Good,  offsetSeconds = 8 * 86400L),
            Step(Rating.Good,  offsetSeconds = 21 * 86400L),
        )
        val card = replayWithOffsets(steps)

        assertEquals(
            "Stability must match py-fsrs (±1e-4)",
            53.62691,
            card.stability,
            1e-4,
        )
        assertEquals(
            "Difficulty must match py-fsrs (±1e-4)",
            6.3574867,
            card.difficulty,
            1e-4,
        )
    }

    // -------------------------------------------------------------------------
    // Property tests
    // -------------------------------------------------------------------------

    @Test
    fun `new card rated Good has positive stability and reps == 1`() {
        val card = Fsrs.schedule(FsrsCard.new(), Rating.Good, epoch, params)
        assertTrue("stability > 0", card.stability > 0.0)
        assertEquals("reps == 1", 1, card.reps)
    }

    @Test
    fun `Easy interval is at least as long as Good interval for Review card`() {
        // Get a card into Review state first
        var card = FsrsCard.new()
        val t0 = epoch
        card = Fsrs.schedule(card, Rating.Good, t0, params)
        card = Fsrs.schedule(card, Rating.Good, t0 + Fsrs.nextDue(card, params), params)

        // Now both ratings from Review state
        val t1 = t0 + card.scheduledDays.days
        val goodCard = Fsrs.schedule(card, Rating.Good, t1, params)
        val easyCard = Fsrs.schedule(card, Rating.Easy, t1, params)

        assertTrue(
            "Easy scheduledDays (${easyCard.scheduledDays}) >= Good (${goodCard.scheduledDays})",
            easyCard.scheduledDays >= goodCard.scheduledDays,
        )
    }

    @Test
    fun `Again on Review card moves state to Relearning`() {
        var card = FsrsCard.new()
        val t0 = epoch
        // Graduate to Review
        card = Fsrs.schedule(card, Rating.Good, t0, params)
        card = Fsrs.schedule(card, Rating.Good, t0 + Fsrs.nextDue(card, params), params)
        assertTrue("Should be Review after two Goods", card.state == CardState.Review)

        val t1 = t0 + card.scheduledDays.days
        val lapsed = Fsrs.schedule(card, Rating.Again, t1, params)
        assertEquals("Again on Review -> Relearning", CardState.Relearning, lapsed.state)
        assertEquals("lapses incremented", 1, lapsed.lapses)
    }

    @Test
    fun `stability is monotone non-decreasing across successive Good reviews on mature card`() {
        var card = FsrsCard.new()
        var now = epoch
        // Warm up: graduate and build stability
        repeat(3) {
            card = Fsrs.schedule(card, Rating.Good, now, params)
            now += Fsrs.nextDue(card, params)
        }
        assertTrue("Card should be in Review", card.state == CardState.Review)

        var prevStability = card.stability
        repeat(5) {
            now += card.scheduledDays.days
            card = Fsrs.schedule(card, Rating.Good, now, params)
            assertTrue(
                "Stability (${card.stability}) >= prev ($prevStability)",
                card.stability >= prevStability,
            )
            prevStability = card.stability
        }
    }

    @Test
    fun `difficulty stays within bounds after extreme rating sequences`() {
        val extremeRatings = List(20) { Rating.Again } + List(20) { Rating.Easy }
        var card = FsrsCard.new()
        var now = epoch
        for (rating in extremeRatings) {
            card = Fsrs.schedule(card, rating, now, params)
            now += Fsrs.nextDue(card, params)
            assertTrue("difficulty >= 1.0", card.difficulty >= 1.0)
            assertTrue("difficulty <= 10.0", card.difficulty <= 10.0)
        }
    }

    @Test
    fun `stability is always positive after any rating sequence`() {
        val ratings = listOf(Rating.Again, Rating.Again, Rating.Hard, Rating.Good, Rating.Easy)
        var card = FsrsCard.new()
        var now = epoch
        for (rating in ratings) {
            card = Fsrs.schedule(card, rating, now, params)
            now += Fsrs.nextDue(card, params)
            assertTrue("stability > 0 (got ${card.stability})", card.stability > 0.0)
        }
    }
}
