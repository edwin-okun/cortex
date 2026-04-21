package com.cortex.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review_cards",
    indices = [
        Index("lessonId"),
        Index("dueAt"),
    ],
)
data class ReviewCardEntity(
    @PrimaryKey val cardId: String,   // "${lessonId}:${conceptKey}"
    val lessonId: String,
    val prompt: String,
    val answer: String,
    val stability: Double,
    val difficulty: Double,
    val elapsedDays: Int,
    val scheduledDays: Int,
    val reps: Int,
    val lapses: Int,
    val state: Int,                   // CardState ordinal
    val step: Int,                    // -1 when state == Review (graduated)
    val lastReview: Long?,            // epoch millis, null until first review
    val dueAt: Long,                  // epoch millis
)
