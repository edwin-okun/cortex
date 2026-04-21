package com.cortex.app.domain.repository

import com.cortex.app.domain.model.LessonReviewCard
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.scheduler.Rating
import kotlinx.coroutines.flow.Flow

interface SchedulerRepository {
    /** Cards whose dueAt is on or before now, ordered by dueAt ascending. */
    fun observeDueCards(): Flow<List<ReviewCard>>

    /** Live count of due cards — drives the Home screen badge. */
    fun observeDueCount(): Flow<Int>

    /**
     * Seed review cards for a lesson. Idempotent — existing cards keep their FSRS state;
     * only cards with new IDs are inserted. Re-authoring a lesson never resets progress.
     */
    suspend fun seedCardsForLesson(lessonId: String, cards: List<LessonReviewCard>)

    /**
     * Record a user rating and persist the updated FSRS state + next due date.
     */
    suspend fun grade(cardId: String, rating: Rating)
}
