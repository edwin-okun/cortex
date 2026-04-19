package com.cortex.app.domain.model

data class LessonProgress(
    val lessonId: String,
    val currentStage: Int,
    val stagesCompleted: Int,
    val startedAt: Long?,
    val masteredAt: Long?,
    val lastOpenedAt: Long,
)