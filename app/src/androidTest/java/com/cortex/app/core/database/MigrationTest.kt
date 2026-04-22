package com.cortex.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration_test"

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CortexDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsReviewCardsTableAndPreservesExistingRows() {
        // Create v1 database and seed rows
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO lesson_progress VALUES " +
                    "('two-pointers', 2, 2, 1000, null, 2000)",
            )
            execSQL(
                "INSERT INTO practice_attempt (lessonId, problemId, correct, attemptedAt) VALUES " +
                    "('two-pointers', 'prob-1', 1, 3000)",
            )
            close()
        }

        // Run migration and validate against v2 schema
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 2, true,
            CortexDatabase.MIGRATION_1_2,
        )

        // review_cards table must exist
        val tableCheck = db.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='review_cards'",
        )
        assertTrue("review_cards table must exist after migration", tableCheck.moveToFirst())
        tableCheck.close()

        // Existing lesson_progress row must survive
        val progressCheck = db.query(
            "SELECT lessonId, currentStage FROM lesson_progress WHERE lessonId='two-pointers'",
        )
        assertTrue("lesson_progress row must survive migration", progressCheck.moveToFirst())
        val stageIdx = progressCheck.getColumnIndex("currentStage")
        assertTrue("currentStage column must exist", stageIdx >= 0)
        assertEquals("currentStage must be 2 after migration", 2, progressCheck.getInt(stageIdx))
        progressCheck.close()

        // Existing practice_attempt row must survive
        val attemptCheck = db.query(
            "SELECT * FROM practice_attempt WHERE lessonId='two-pointers'",
        )
        assertTrue("practice_attempt row must survive migration", attemptCheck.moveToFirst())
        attemptCheck.close()

        db.close()
    }
}
