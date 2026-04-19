package com.cortex.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_attempt",
    indices = [Index(value = ["lessonId", "problemId"])],
)
data class PracticeAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: String,
    val problemId: String,
    val correct: Boolean,
    val attemptedAt: Long,
)
