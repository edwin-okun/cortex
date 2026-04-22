package com.cortex.app.domain.usecase

import com.cortex.app.domain.model.DailySession
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.model.SessionConfig
import com.cortex.app.domain.model.SessionItem
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class BuildDailySessionUseCase(
    private val schedulerRepository: SchedulerRepository,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val clock: Clock = Clock.System,
) {

    suspend operator fun invoke(config: SessionConfig): DailySession {
        val now = clock.now()

        // --- Reviews ---
        val allDue = schedulerRepository.observeDueCards().first()
        val sorted = if (config.prioritizeOverdue) allDue.sortedBy { it.dueAt } else allDue
        val cappedReviews = sorted.take(config.maxReviews)

        // --- New lessons ---
        val allLessons = contentRepository.getAllLessons()
        val allProgress = progressRepository.observeAllProgress().first()
        val newLessons = selectAvailableNewLessons(allLessons, allProgress, config.maxNewLessons)

        // --- Assemble: new lessons inserted between first and second third of reviews ---
        val reviewItems = cappedReviews.map { SessionItem.ReviewCard(it) }
        val newLessonItems = newLessons.map { SessionItem.NewLesson(it) }
        val insertAt = reviewItems.size / 3
        val combined = buildList {
            addAll(reviewItems.subList(0, insertAt))
            addAll(newLessonItems)
            addAll(reviewItems.subList(insertAt, reviewItems.size))
        }

        val lessonMap = allLessons.associateBy { it.id }
        val interleaved = interleave(combined, config, lessonMap)

        return DailySession(
            items = interleaved,
            totalReviews = cappedReviews.size,
            totalNewLessons = newLessons.size,
            generatedAt = now,
        )
    }

    // -------------------------------------------------------------------------
    // Interleaving
    // -------------------------------------------------------------------------

    private fun interleave(
        items: List<SessionItem>,
        config: SessionConfig,
        lessonMap: Map<String, Lesson>,
    ): List<SessionItem> {
        if (items.size <= 1) return items
        val result = items.toMutableList()
        var i = 0
        while (i < result.size) {
            val trackRun = consecutiveRun(result, i) { track(lessonMap) }
            if (trackRun > config.maxConsecutiveSameTrack) {
                val currentTrack = result[i].track(lessonMap)
                val swapIdx = (i + 1 until result.size)
                    .firstOrNull { result[it].track(lessonMap) != currentTrack }
                if (swapIdx != null) {
                    swap(result, i, swapIdx)
                    continue
                }
            }

            val tierRun = consecutiveRun(result, i) { tier(lessonMap) }
            if (tierRun > config.maxConsecutiveSameTier) {
                val currentTier = result[i].tier(lessonMap)
                val swapIdx = (i + 1 until result.size)
                    .firstOrNull { result[it].tier(lessonMap) != currentTier }
                if (swapIdx != null) {
                    swap(result, i, swapIdx)
                    continue
                }
            }

            i++
        }
        return result
    }

    /** How many items in a row (ending at index i, inclusive) share the same value of [key]. */
    private fun <T> consecutiveRun(
        list: List<SessionItem>,
        i: Int,
        key: SessionItem.() -> T,
    ): Int {
        val value = list[i].key()
        var count = 1
        var j = i - 1
        while (j >= 0 && list[j].key() == value) {
            count++
            j--
        }
        return count
    }

    private fun swap(list: MutableList<SessionItem>, a: Int, b: Int) {
        val tmp = list[a]; list[a] = list[b]; list[b] = tmp
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun SessionItem.track(lessonMap: Map<String, Lesson>) = when (this) {
        is SessionItem.ReviewCard -> lessonMap[card.lessonId]?.track
        is SessionItem.NewLesson -> lesson.track
    }

    private fun SessionItem.tier(lessonMap: Map<String, Lesson>) = when (this) {
        is SessionItem.ReviewCard -> lessonMap[card.lessonId]?.tier
        is SessionItem.NewLesson -> lesson.tier
    }
}
