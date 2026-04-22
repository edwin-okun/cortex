package com.cortex.app.domain.model

import kotlinx.datetime.Instant

sealed class SessionItem {
    data class ReviewCard(val card: com.cortex.app.domain.model.ReviewCard) : SessionItem()
    data class NewLesson(val lesson: Lesson) : SessionItem()
}

data class DailySession(
    val items: List<SessionItem>,
    val totalReviews: Int,
    val totalNewLessons: Int,
    val generatedAt: Instant,
)
