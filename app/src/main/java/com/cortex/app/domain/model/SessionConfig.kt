package com.cortex.app.domain.model

data class SessionConfig(
    val maxReviews: Int = 20,
    val maxNewLessons: Int = 1,
    val maxConsecutiveSameTrack: Int = 2,
    val maxConsecutiveSameTier: Int = 3,
    val prioritizeOverdue: Boolean = true,
)
