package com.cortex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cortex.app.data.local.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {

    @Query("SELECT * FROM lesson_progress")
    fun getAll(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    fun getByLessonId(lessonId: String): Flow<LessonProgressEntity?>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getByLessonIdOnce(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsert(entity: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress")
    suspend fun deleteAll()
}
