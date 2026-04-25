package com.cortex.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonStage
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.usecase.selectAvailableNewLessons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(progressRepository.observeAllProgress()) { flows ->
                val allProgress = flows.first()
                val lessons = contentRepository.getAllLessons()
                LibraryUiState(
                    isLoading = false,
                    lessons = lessons.map { lesson ->
                        val progress = allProgress.firstOrNull { it.lessonId == lesson.id }
                        val isMastered = progress?.masteredAt != null
                        val isInProgress = progress != null && !isMastered
                        val isAvailable = isInProgress ||
                            isMastered ||
                            lesson in selectAvailableNewLessons(
                                allLessons = lessons,
                                allProgress = allProgress,
                                maxNewLessons = Int.MAX_VALUE,
                            )
                        LibraryLessonItem(
                            lessonId = lesson.id,
                            title = lesson.title,
                            track = lesson.track.name.replace('_', ' '),
                            tier = lesson.tier.name.replace('_', ' '),
                            stageCount = lesson.stages.size,
                            reviewCount = lesson.reviewCards.size,
                            preview = lesson.previewText(),
                            status = when {
                                isMastered -> "MASTERED"
                                isInProgress -> "IN PROGRESS"
                                else -> "NOT STARTED"
                            },
                            ctaLabel = when {
                                isMastered -> "REVISIT"
                                isInProgress -> "RESUME"
                                !isAvailable -> "LOCKED"
                                else -> "START"
                            },
                            isActionEnabled = isAvailable,
                            restartOnOpen = isMastered,
                            progressLabel = when {
                                isMastered -> "${lesson.stages.size}/${lesson.stages.size} stages"
                                isInProgress -> "${(progress?.currentStage ?: 0) + 1}/${lesson.stages.size} stages"
                                else -> null
                            },
                        )
                    },
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    private fun Lesson.previewText(): String {
        val hook = stages.filterIsInstance<LessonStage.Hook>().firstOrNull()
        return hook?.problem?.lineSequence()?.firstOrNull()?.trim().orEmpty()
    }
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val lessons: List<LibraryLessonItem> = emptyList(),
)

data class LibraryLessonItem(
    val lessonId: String,
    val title: String,
    val track: String,
    val tier: String,
    val stageCount: Int,
    val reviewCount: Int,
    val preview: String,
    val status: String,
    val ctaLabel: String,
    val isActionEnabled: Boolean,
    val restartOnOpen: Boolean,
    val progressLabel: String?,
)
