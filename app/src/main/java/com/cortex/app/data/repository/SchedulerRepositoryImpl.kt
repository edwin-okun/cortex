package com.cortex.app.data.repository

import com.cortex.app.data.local.dao.ReviewCardDao
import com.cortex.app.data.local.entity.ReviewCardEntity
import com.cortex.app.domain.model.LessonReviewCard
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.CardState
import com.cortex.app.domain.scheduler.Fsrs
import com.cortex.app.domain.scheduler.FsrsCard
import com.cortex.app.domain.scheduler.FsrsParameters
import com.cortex.app.domain.scheduler.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SchedulerRepositoryImpl(
    private val dao: ReviewCardDao,
    private val clock: Clock = Clock.System,
    private val params: FsrsParameters = FsrsParameters.Default,
) : SchedulerRepository {

    override fun observeDueCards(): Flow<List<ReviewCard>> =
        dao.observeDue(clock.now().toEpochMilliseconds()).map { list ->
            list.map { it.toDomain() }
        }

    override fun observeDueCount(): Flow<Int> =
        dao.observeDueCount(clock.now().toEpochMilliseconds())

    override suspend fun seedCardsForLesson(lessonId: String, cards: List<LessonReviewCard>) {
        val now = clock.now()
        val entities = cards.map { authored ->
            val cardId = "$lessonId:${authored.id}"
            // Only insert if not already present — never overwrite existing FSRS state
            dao.getById(cardId) ?: ReviewCardEntity(
                cardId = cardId,
                lessonId = lessonId,
                prompt = authored.front,
                answer = authored.back,
                stability = 0.0,
                difficulty = 0.0,
                elapsedDays = 0,
                scheduledDays = 0,
                reps = 0,
                lapses = 0,
                state = CardState.Learning.ordinal,
                step = 0,
                lastReview = null,
                dueAt = now.toEpochMilliseconds(),
            )
        }
        dao.upsertAll(entities)
    }

    override suspend fun grade(cardId: String, rating: Rating) {
        val entity = dao.getById(cardId) ?: return
        val now = clock.now()

        val fsrsCard = entity.toFsrs()
        val updated = Fsrs.schedule(fsrsCard, rating, now, params)
        val due = now + Fsrs.nextDue(updated, params)

        dao.upsert(updated.toEntity(entity, due))
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------

    private fun ReviewCardEntity.toFsrs(): FsrsCard = FsrsCard(
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = CardState.entries[state],
        lastReview = lastReview?.let { Instant.fromEpochMilliseconds(it) },
        step = if (step == -1) null else step,
    )

    private fun FsrsCard.toEntity(original: ReviewCardEntity, dueAt: Instant): ReviewCardEntity =
        original.copy(
            stability = stability,
            difficulty = difficulty,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            reps = reps,
            lapses = lapses,
            state = state.ordinal,
            step = step ?: -1,
            lastReview = lastReview?.toEpochMilliseconds(),
            dueAt = dueAt.toEpochMilliseconds(),
        )

    private fun ReviewCardEntity.toDomain(): ReviewCard = ReviewCard(
        cardId = cardId,
        lessonId = lessonId,
        prompt = prompt,
        answer = answer,
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = CardState.entries[state],
        lastReview = lastReview?.let { Instant.fromEpochMilliseconds(it) },
        dueAt = Instant.fromEpochMilliseconds(dueAt),
        step = if (step == -1) null else step,
    )
}
