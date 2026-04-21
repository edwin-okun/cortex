package com.cortex.app.domain.scheduler

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * FSRS-6 model parameters.
 * Default weights are from the official py-fsrs implementation, optimised on ~100M reviews.
 * Source: https://github.com/open-spaced-repetition/py-fsrs
 */
data class FsrsParameters(
    /** 21 FSRS-6 model weights (w[0]–w[20]). */
    val w: DoubleArray,
    val desiredRetention: Double = 0.9,
    val maximumInterval: Int = 36500,
    /** Intra-day steps for new (Learning) cards before they graduate to Review. */
    val learningSteps: List<Duration> = listOf(1.minutes, 10.minutes),
    /** Intra-day steps for lapsed (Relearning) cards before they return to Review. */
    val relearningSteps: List<Duration> = listOf(10.minutes),
) {
    /** Exponent of the power forgetting curve. Always negative. */
    val decay: Double = -w[20]

    /** Pre-computed constant so that R(S, S) == desiredRetention. */
    val factor: Double = 0.9.pow(1.0 / decay) - 1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FsrsParameters) return false
        return w.contentEquals(other.w) &&
            desiredRetention == other.desiredRetention &&
            maximumInterval == other.maximumInterval &&
            learningSteps == other.learningSteps &&
            relearningSteps == other.relearningSteps
    }

    override fun hashCode(): Int {
        var result = w.contentHashCode()
        result = 31 * result + desiredRetention.hashCode()
        result = 31 * result + maximumInterval
        result = 31 * result + learningSteps.hashCode()
        result = 31 * result + relearningSteps.hashCode()
        return result
    }

    companion object {
        val Default = FsrsParameters(
            w = doubleArrayOf(
                0.212,  1.2931, 2.3065, 8.2956,   // w[0-3]:  initial stability (Again/Hard/Good/Easy)
                6.4133, 0.8334, 3.0194, 0.001,     // w[4-7]:  difficulty curve
                1.8722, 0.1666, 0.796,              // w[8-10]: recall stability
                1.4835, 0.0614, 0.2629, 1.6483,    // w[11-14]: forget stability
                0.6014, 1.8729,                     // w[15-16]: hard penalty / easy bonus
                0.5425, 0.0912, 0.0658,             // w[17-19]: short-term stability
                0.1542,                             // w[20]:   decay (trainable)
            ),
        )
    }
}
