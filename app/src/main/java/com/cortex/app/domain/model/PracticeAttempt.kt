package com.cortex.app.domain.model

data class PracticeAttempt(
    val id: Long,
    val lessonId: String,
    val problemId: String,
    val correct: Boolean,
    val attemptedAt: Long,
)