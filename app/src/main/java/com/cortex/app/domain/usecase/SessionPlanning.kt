package com.cortex.app.domain.usecase

import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.Tier

fun selectAvailableNewLessons(
    allLessons: List<Lesson>,
    allProgress: List<LessonProgress>,
    maxNewLessons: Int,
): List<Lesson> {
    val startedIds = allProgress.map { it.lessonId }.toSet()
    val startedTiers = allProgress
        .filter { it.currentStage > 0 }
        .mapNotNull { progress -> allLessons.find { it.id == progress.lessonId }?.tier }
        .toSet()

    return allLessons
        .filter { it.id !in startedIds }
        .sortedBy { it.tier.ordinal }
        .filter { it.tier.isAvailable(startedTiers, allLessons) }
        .take(maxNewLessons)
}

private fun Tier.isAvailable(startedTiers: Set<Tier>, allLessons: List<Lesson>): Boolean {
    if (ordinal == 0) return true
    val prereq = Tier.entries[ordinal - 1]
    val prereqExists = allLessons.any { it.tier == prereq }
    return !prereqExists || startedTiers.contains(prereq)
}
