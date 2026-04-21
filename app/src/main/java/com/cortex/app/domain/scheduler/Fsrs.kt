package com.cortex.app.domain.scheduler

import kotlinx.datetime.Instant
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.time.Duration

/**
 * FSRS-6 scheduler — pure functions, no side effects, no Android dependencies.
 *
 * Formulas ported from the official py-fsrs implementation:
 *   https://github.com/open-spaced-repetition/py-fsrs/blob/main/fsrs/scheduler.py
 *
 * Key invariants:
 *   - stability > 0 always (clamped to STABILITY_MIN)
 *   - difficulty in [1.0, 10.0] always
 *   - interval >= 1 day for Review cards
 */
object Fsrs {

    private const val STABILITY_MIN = 0.001
    private const val MIN_DIFFICULTY = 1.0
    private const val MAX_DIFFICULTY = 10.0

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Advance a card's FSRS state after a review.
     *
     * @param card   Current card state (use [FsrsCard.new] for a brand-new card).
     * @param rating User's rating for this review.
     * @param now    Wall-clock time of the review.
     * @param params FSRS-6 model parameters (defaults to published optimised weights).
     * @return Updated card state. The caller uses [nextDue] to compute the due timestamp.
     */
    fun schedule(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
        params: FsrsParameters = FsrsParameters.Default,
    ): FsrsCard {
        val daysSinceLastReview = card.lastReview?.let { last ->
            ((now - last).inWholeSeconds / 86400.0).toInt()
        }

        return when (card.state) {
            CardState.Learning -> scheduleLearning(card, rating, now, daysSinceLastReview, params)
            CardState.Review -> scheduleReview(card, rating, now, daysSinceLastReview, params)
            CardState.Relearning -> scheduleRelearning(card, rating, now, daysSinceLastReview, params)
        }
    }

    /**
     * The Duration from [now] until this card is next due.
     * Learning/Relearning cards are due in minutes; Review cards are due in days.
     */
    fun nextDue(card: FsrsCard, params: FsrsParameters = FsrsParameters.Default): Duration {
        return when (card.state) {
            CardState.Learning -> {
                val step = card.step ?: 0
                if (params.learningSteps.isEmpty()) kotlin.time.Duration.ZERO
                else params.learningSteps[step.coerceIn(0, params.learningSteps.lastIndex)]
            }
            CardState.Relearning -> {
                val step = card.step ?: 0
                if (params.relearningSteps.isEmpty()) kotlin.time.Duration.ZERO
                else params.relearningSteps[step.coerceIn(0, params.relearningSteps.lastIndex)]
            }
            CardState.Review -> kotlin.time.Duration.parse("${card.scheduledDays}d")
        }
    }

    /**
     * Predicted probability of recall at [now] given the card's last review and stability.
     * Returns 0 if the card has never been reviewed.
     */
    fun retrievability(
        card: FsrsCard,
        now: Instant,
        params: FsrsParameters = FsrsParameters.Default,
    ): Double {
        val lastReview = card.lastReview ?: return 0.0
        if (card.stability <= 0.0) return 0.0
        val elapsedDays = max(0, ((now - lastReview).inWholeSeconds / 86400).toInt())
        return forgettingCurve(elapsedDays.toDouble(), card.stability, params)
    }

    // ---------------------------------------------------------------------------
    // State machine
    // ---------------------------------------------------------------------------

    private fun scheduleLearning(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
        daysSinceLastReview: Int?,
        params: FsrsParameters,
    ): FsrsCard {
        val step = card.step ?: 0

        // Update memory state
        val (newS, newD) = when {
            card.stability == 0.0 || card.difficulty == 0.0 -> {
                // First-ever review: initialise from weights
                initialStability(rating, params) to initialDifficulty(rating, params)
            }
            daysSinceLastReview != null && daysSinceLastReview < 1 -> {
                // Same-day review: use short-term formula
                shortTermStability(card.stability, rating, params) to
                    nextDifficulty(card.difficulty, rating, params)
            }
            else -> {
                // Came back on a later day
                val r = forgettingCurve(
                    (daysSinceLastReview ?: card.elapsedDays).toDouble(),
                    card.stability,
                    params,
                )
                nextStability(card.difficulty, card.stability, r, rating, params) to
                    nextDifficulty(card.difficulty, rating, params)
            }
        }

        // State transition
        if (params.learningSteps.isEmpty() ||
            (step >= params.learningSteps.size && rating.grade >= Rating.Hard.grade)
        ) {
            // Graduate to Review immediately
            return graduate(card, newS, newD, now, params)
        }

        return when (rating) {
            Rating.Again -> card.copy(
                stability = newS, difficulty = newD,
                step = 0, state = CardState.Learning,
                lastReview = now,
                elapsedDays = daysSinceLastReview ?: 0,
                scheduledDays = 0,
                reps = card.reps + 1,
            )
            Rating.Hard -> {
                // Stay at current step
                card.copy(
                    stability = newS, difficulty = newD,
                    step = step, state = CardState.Learning,
                    lastReview = now,
                    elapsedDays = daysSinceLastReview ?: 0,
                    scheduledDays = 0,
                    reps = card.reps + 1,
                )
            }
            Rating.Good -> {
                if (step + 1 >= params.learningSteps.size) {
                    graduate(card, newS, newD, now, params)
                } else {
                    card.copy(
                        stability = newS, difficulty = newD,
                        step = step + 1, state = CardState.Learning,
                        lastReview = now,
                        elapsedDays = daysSinceLastReview ?: 0,
                        scheduledDays = 0,
                        reps = card.reps + 1,
                    )
                }
            }
            Rating.Easy -> graduate(card, newS, newD, now, params)
        }
    }

    private fun scheduleReview(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
        daysSinceLastReview: Int?,
        params: FsrsParameters,
    ): FsrsCard {
        val r = forgettingCurve(
            (daysSinceLastReview ?: card.elapsedDays).toDouble(),
            card.stability,
            params,
        )

        val (newS, newD) = if (daysSinceLastReview != null && daysSinceLastReview < 1) {
            shortTermStability(card.stability, rating, params) to
                nextDifficulty(card.difficulty, rating, params)
        } else {
            nextStability(card.difficulty, card.stability, r, rating, params) to
                nextDifficulty(card.difficulty, rating, params)
        }

        return when (rating) {
            Rating.Again -> {
                if (params.relearningSteps.isEmpty()) {
                    // No relearning steps — stay in Review with new interval
                    card.copy(
                        stability = newS, difficulty = newD,
                        step = null, state = CardState.Review,
                        lastReview = now,
                        elapsedDays = daysSinceLastReview ?: card.elapsedDays,
                        scheduledDays = nextInterval(newS, params),
                        reps = card.reps + 1,
                        lapses = card.lapses + 1,
                    )
                } else {
                    card.copy(
                        stability = newS, difficulty = newD,
                        step = 0, state = CardState.Relearning,
                        lastReview = now,
                        elapsedDays = daysSinceLastReview ?: card.elapsedDays,
                        scheduledDays = 0,
                        reps = card.reps + 1,
                        lapses = card.lapses + 1,
                    )
                }
            }
            Rating.Hard, Rating.Good, Rating.Easy -> card.copy(
                stability = newS, difficulty = newD,
                step = null, state = CardState.Review,
                lastReview = now,
                elapsedDays = daysSinceLastReview ?: card.elapsedDays,
                scheduledDays = nextInterval(newS, params),
                reps = card.reps + 1,
            )
        }
    }

    private fun scheduleRelearning(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
        daysSinceLastReview: Int?,
        params: FsrsParameters,
    ): FsrsCard {
        val step = card.step ?: 0

        val (newS, newD) = if (daysSinceLastReview != null && daysSinceLastReview < 1) {
            shortTermStability(card.stability, rating, params) to
                nextDifficulty(card.difficulty, rating, params)
        } else {
            val r = forgettingCurve(
                (daysSinceLastReview ?: card.elapsedDays).toDouble(),
                card.stability,
                params,
            )
            nextStability(card.difficulty, card.stability, r, rating, params) to
                nextDifficulty(card.difficulty, rating, params)
        }

        if (params.relearningSteps.isEmpty() ||
            (step >= params.relearningSteps.size && rating.grade >= Rating.Hard.grade)
        ) {
            return graduate(card, newS, newD, now, params)
        }

        return when (rating) {
            Rating.Again -> card.copy(
                stability = newS, difficulty = newD,
                step = 0, state = CardState.Relearning,
                lastReview = now,
                elapsedDays = daysSinceLastReview ?: 0,
                scheduledDays = 0,
                reps = card.reps + 1,
            )
            Rating.Hard -> card.copy(
                stability = newS, difficulty = newD,
                step = step, state = CardState.Relearning,
                lastReview = now,
                elapsedDays = daysSinceLastReview ?: 0,
                scheduledDays = 0,
                reps = card.reps + 1,
            )
            Rating.Good -> {
                if (step + 1 >= params.relearningSteps.size) {
                    graduate(card, newS, newD, now, params)
                } else {
                    card.copy(
                        stability = newS, difficulty = newD,
                        step = step + 1, state = CardState.Relearning,
                        lastReview = now,
                        elapsedDays = daysSinceLastReview ?: 0,
                        scheduledDays = 0,
                        reps = card.reps + 1,
                    )
                }
            }
            Rating.Easy -> graduate(card, newS, newD, now, params)
        }
    }

    private fun graduate(
        card: FsrsCard,
        stability: Double,
        difficulty: Double,
        now: Instant,
        params: FsrsParameters,
    ): FsrsCard = card.copy(
        stability = stability,
        difficulty = difficulty,
        step = null,
        state = CardState.Review,
        lastReview = now,
        scheduledDays = nextInterval(stability, params),
        reps = card.reps + 1,
    )

    // ---------------------------------------------------------------------------
    // FSRS-6 core formulas (ported 1:1 from py-fsrs)
    // ---------------------------------------------------------------------------

    private fun forgettingCurve(elapsedDays: Double, stability: Double, params: FsrsParameters): Double {
        return (1.0 + params.factor * elapsedDays / stability).pow(params.decay)
    }

    private fun initialStability(rating: Rating, params: FsrsParameters): Double =
        max(params.w[rating.grade - 1], STABILITY_MIN)

    private fun initialDifficulty(rating: Rating, params: FsrsParameters): Double {
        val raw = params.w[4] - exp(params.w[5] * (rating.grade - 1)) + 1
        return raw.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun nextInterval(stability: Double, params: FsrsParameters): Int {
        val raw = (stability / params.factor) * (params.desiredRetention.pow(1.0 / params.decay) - 1)
        return round(raw).toInt().coerceIn(1, params.maximumInterval)
    }

    /** Same-day review (card reviewed twice on the same calendar day). */
    private fun shortTermStability(stability: Double, rating: Rating, params: FsrsParameters): Double {
        val increase = exp(params.w[17] * (rating.grade - 3 + params.w[18])) *
            stability.pow(-params.w[19])
        val clamped = if (rating == Rating.Good || rating == Rating.Easy) max(increase, 1.0) else increase
        return max(stability * clamped, STABILITY_MIN)
    }

    private fun nextDifficulty(difficulty: Double, rating: Rating, params: FsrsParameters): Double {
        val d0Easy = params.w[4] - exp(params.w[5] * (Rating.Easy.grade - 1)) + 1  // unclamped
        val delta = -(params.w[6] * (rating.grade - 3))
        val damped = (10.0 - difficulty) * delta / 9.0
        val raw = difficulty + damped
        val reverted = params.w[7] * d0Easy + (1 - params.w[7]) * raw
        return reverted.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun nextStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
        params: FsrsParameters,
    ): Double = if (rating == Rating.Again) {
        nextForgetStability(difficulty, stability, retrievability, params)
    } else {
        nextRecallStability(difficulty, stability, retrievability, rating, params)
    }

    private fun nextRecallStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        rating: Rating,
        params: FsrsParameters,
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) params.w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) params.w[16] else 1.0
        val result = stability * (
            1.0 + exp(params.w[8]) *
                (11.0 - difficulty) *
                stability.pow(-params.w[9]) *
                (exp((1.0 - retrievability) * params.w[10]) - 1.0) *
                hardPenalty *
                easyBonus
        )
        return max(result, STABILITY_MIN)
    }

    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double,
        params: FsrsParameters,
    ): Double {
        val longTerm = params.w[11] *
            difficulty.pow(-params.w[12]) *
            ((stability + 1.0).pow(params.w[13]) - 1.0) *
            exp((1.0 - retrievability) * params.w[14])
        val shortTermBound = stability / exp(params.w[17] * params.w[18])
        return max(min(longTerm, shortTermBound), STABILITY_MIN)
    }
}
