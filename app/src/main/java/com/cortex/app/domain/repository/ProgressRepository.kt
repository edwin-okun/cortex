package com.cortex.app.domain.repository

import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.PracticeAttempt
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeProgress(lessonId: String): Flow<LessonProgress?>
    fun observeAllProgress(): Flow<List<LessonProgress>>
    suspend fun recordStageAdvance(lessonId: String, newStage: Int, totalStages: Int)
    suspend fun recordPracticeAttempt(lessonId: String, problemId: String, correct: Boolean)
    fun observePracticeAttempts(lessonId: String): Flow<List<PracticeAttempt>>
    suspend fun recordLessonOpened(lessonId: String)
}
