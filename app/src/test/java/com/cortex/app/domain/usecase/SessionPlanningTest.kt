package com.cortex.app.domain.usecase

import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPlanningTest {

    @Test
    fun `selectAvailableNewLessons skips tier locked lesson previews`() {
        val intermediate = lesson("intermediate", Tier.INTERMEDIATE)
        val foundations = lesson("foundations", Tier.FOUNDATIONS)

        val selected = selectAvailableNewLessons(
            allLessons = listOf(intermediate, foundations),
            allProgress = emptyList(),
            maxNewLessons = 1,
        )

        assertEquals(listOf(foundations), selected)
    }

    @Test
    fun `selectAvailableNewLessons excludes lesson once it has any progress row`() {
        val foundations = lesson("foundations", Tier.FOUNDATIONS)
        val next = lesson("next-up", Tier.FOUNDATIONS)

        val selected = selectAvailableNewLessons(
            allLessons = listOf(foundations, next),
            allProgress = listOf(progress("foundations", stage = 0)),
            maxNewLessons = 1,
        )

        assertEquals(listOf(next), selected)
    }

    private fun lesson(id: String, tier: Tier) = Lesson(
        id = id,
        track = Track.ALGORITHMS,
        tier = tier,
        title = id,
        stages = emptyList(),
        reviewCards = emptyList(),
    )

    private fun progress(lessonId: String, stage: Int) = LessonProgress(
        lessonId = lessonId,
        currentStage = stage,
        stagesCompleted = stage,
        startedAt = 1_000L,
        masteredAt = null,
        lastOpenedAt = 2_000L,
    )
}
