package com.cortex.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val currentStage: Int,
    val stagesCompleted: Int,
    val startedAt: Long?,
    val masteredAt: Long?,
    val lastOpenedAt: Long,
)
