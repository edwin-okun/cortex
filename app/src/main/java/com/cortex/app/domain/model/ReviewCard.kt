package com.cortex.app.domain.model

import com.cortex.app.domain.scheduler.CardState
import kotlinx.datetime.Instant

/** Runtime representation of a review card including its FSRS scheduling state. */
data class ReviewCard(
    val cardId: String,        // "${lessonId}:${conceptKey}"
    val lessonId: String,
    val prompt: String,
    val answer: String,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reps: Int,
    val lapses: Int,
    val state: CardState,
    val lastReview: Instant?,
    val dueAt: Instant,
    val step: Int?,
)
