package com.cortex.app.domain.scheduler

import kotlinx.datetime.Instant

data class FsrsCard(
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: CardState = CardState.Learning,
    val lastReview: Instant? = null,
    // Tracks position within learning/relearning step sequence.
    // null when state == Review (graduated, day-scale intervals).
    val step: Int? = 0,
) {
    companion object {
        fun new(): FsrsCard = FsrsCard()
    }
}
