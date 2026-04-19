package com.cortex.app.data.repository

import com.cortex.app.data.local.dao.LessonProgressDao
import com.cortex.app.data.local.dao.PracticeAttemptDao
import com.cortex.app.data.local.entity.LessonProgressEntity
import com.cortex.app.data.local.entity.PracticeAttemptEntity
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.PracticeAttempt
import com.cortex.app.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepositoryImpl(
    private val progressDao: LessonProgressDao,
    private val attemptDao: PracticeAttemptDao,
) : ProgressRepository {

    override fun observeProgress(lessonId: String): Flow<LessonProgress?> =
        progressDao.getByLessonId(lessonId).map { it?.toDomain() }

    override fun observeAllProgress(): Flow<List<LessonProgress>> =
        progressDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun recordStageAdvance(lessonId: String, newStage: Int, totalStages: Int) {
        val now = System.currentTimeMillis()
        val existing = progressDao.getByLessonIdOnce(lessonId)
        val updated = if (existing == null) {
            LessonProgressEntity(
                lessonId = lessonId,
                currentStage = newStage,
                stagesCompleted = newStage,
                startedAt = now,
                masteredAt = if (newStage >= totalStages) now else null,
                lastOpenedAt = now,
            )
        } else {
            existing.copy(
                currentStage = newStage,
                stagesCompleted = maxOf(existing.stagesCompleted, newStage),
                masteredAt = if (newStage >= totalStages && existing.masteredAt == null) now else existing.masteredAt,
                lastOpenedAt = now,
            )
        }
        progressDao.upsert(updated)
    }

    override suspend fun recordPracticeAttempt(lessonId: String, problemId: String, correct: Boolean) {
        attemptDao.insert(
            PracticeAttemptEntity(
                lessonId = lessonId,
                problemId = problemId,
                correct = correct,
                attemptedAt = System.currentTimeMillis(),
            )
        )
    }

    override fun observePracticeAttempts(lessonId: String): Flow<List<PracticeAttempt>> =
        attemptDao.getAttemptsForLesson(lessonId).map { list -> list.map { it.toDomain() } }

    override suspend fun recordLessonOpened(lessonId: String) {
        val existing = progressDao.getByLessonIdOnce(lessonId) ?: return
        progressDao.upsert(existing.copy(lastOpenedAt = System.currentTimeMillis()))
    }
}

private fun LessonProgressEntity.toDomain() = LessonProgress(
    lessonId = lessonId,
    currentStage = currentStage,
    stagesCompleted = stagesCompleted,
    startedAt = startedAt,
    masteredAt = masteredAt,
    lastOpenedAt = lastOpenedAt,
)

private fun PracticeAttemptEntity.toDomain() = PracticeAttempt(
    id = id,
    lessonId = lessonId,
    problemId = problemId,
    correct = correct,
    attemptedAt = attemptedAt,
)
