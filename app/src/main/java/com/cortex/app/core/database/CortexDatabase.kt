package com.cortex.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cortex.app.data.local.dao.LessonProgressDao
import com.cortex.app.data.local.dao.PracticeAttemptDao
import com.cortex.app.data.local.dao.ReviewCardDao
import com.cortex.app.data.local.entity.LessonProgressEntity
import com.cortex.app.data.local.entity.PracticeAttemptEntity
import com.cortex.app.data.local.entity.ReviewCardEntity

@Database(
    entities = [
        LessonProgressEntity::class,
        PracticeAttemptEntity::class,
        ReviewCardEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class CortexDatabase : RoomDatabase() {
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun practiceAttemptDao(): PracticeAttemptDao
    abstract fun reviewCardDao(): ReviewCardDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS review_cards (
                        cardId       TEXT NOT NULL PRIMARY KEY,
                        lessonId     TEXT NOT NULL,
                        prompt       TEXT NOT NULL,
                        answer       TEXT NOT NULL,
                        stability    REAL NOT NULL,
                        difficulty   REAL NOT NULL,
                        elapsedDays  INTEGER NOT NULL,
                        scheduledDays INTEGER NOT NULL,
                        reps         INTEGER NOT NULL,
                        lapses       INTEGER NOT NULL,
                        state        INTEGER NOT NULL,
                        step         INTEGER NOT NULL,
                        lastReview   INTEGER,
                        dueAt        INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_cards_lessonId ON review_cards (lessonId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_cards_dueAt ON review_cards (dueAt)")
            }
        }
    }
}
