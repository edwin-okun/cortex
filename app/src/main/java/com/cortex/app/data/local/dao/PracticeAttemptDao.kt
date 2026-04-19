package com.cortex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cortex.app.data.local.entity.PracticeAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeAttemptDao {

    @Insert
    suspend fun insert(attempt: PracticeAttemptEntity)

    @Query("SELECT * FROM practice_attempt WHERE lessonId = :lessonId ORDER BY attemptedAt ASC")
    fun getAttemptsForLesson(lessonId: String): Flow<List<PracticeAttemptEntity>>

    @Query("SELECT COUNT(*) FROM practice_attempt WHERE lessonId = :lessonId AND problemId = :problemId AND correct = 1")
    fun getCorrectCountForProblem(lessonId: String, problemId: String): Flow<Int>
}
