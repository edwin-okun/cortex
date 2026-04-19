package com.cortex.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cortex.app.data.local.dao.LessonProgressDao
import com.cortex.app.data.local.dao.PracticeAttemptDao
import com.cortex.app.data.local.entity.LessonProgressEntity
import com.cortex.app.data.local.entity.PracticeAttemptEntity

@Database(
    entities = [LessonProgressEntity::class, PracticeAttemptEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CortexDatabase : RoomDatabase() {
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun practiceAttemptDao(): PracticeAttemptDao
}
