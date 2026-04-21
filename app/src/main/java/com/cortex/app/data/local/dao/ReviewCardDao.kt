package com.cortex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cortex.app.data.local.entity.ReviewCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewCardDao {

    @Query("SELECT * FROM review_cards WHERE dueAt <= :now ORDER BY dueAt ASC")
    fun observeDue(now: Long): Flow<List<ReviewCardEntity>>

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAt <= :now")
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM review_cards WHERE lessonId = :lessonId")
    fun observeAllForLesson(lessonId: String): Flow<List<ReviewCardEntity>>

    @Upsert
    suspend fun upsert(card: ReviewCardEntity)

    @Upsert
    suspend fun upsertAll(cards: List<ReviewCardEntity>)

    @Query("SELECT * FROM review_cards WHERE cardId = :cardId")
    suspend fun getById(cardId: String): ReviewCardEntity?

    @Query("SELECT COUNT(*) FROM review_cards WHERE dueAt <= :now")
    suspend fun countDue(now: Long): Int
}
