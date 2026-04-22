package com.cortex.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.usecase.selectAvailableNewLessons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val schedulerRepository: SchedulerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                progressRepository.observeAllProgress(),
                schedulerRepository.observeDueCount(),
            ) { allProgress, dueCount ->
                val lessons = contentRepository.getAllLessons()

                val resumeProgress = allProgress
                    .filter { it.masteredAt == null }
                    .maxByOrNull { it.lastOpenedAt }

                val resumeLesson = resumeProgress?.let { p ->
                    contentRepository.getLesson(p.lessonId)?.let { lesson ->
                        ResumeLessonInfo(
                            lessonId = p.lessonId,
                            title = lesson.title,
                            currentStage = p.currentStage,
                            totalStages = lesson.stages.size,
                        )
                    }
                }

                val newLesson = selectAvailableNewLessons(
                    allLessons = lessons,
                    allProgress = allProgress,
                    maxNewLessons = 1,
                ).firstOrNull()

                HomeUiState(
                    greeting = "Good morning.",
                    dueReviewCount = dueCount,
                    newLessonTitle = newLesson?.title,
                    streakDays = 0,
                    isLoading = false,
                    resumeLesson = resumeLesson,
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }
}

data class HomeUiState(
    val greeting: String = "Good morning.",
    val dueReviewCount: Int = 0,
    val newLessonTitle: String? = null,
    val streakDays: Int = 0,
    val isLoading: Boolean = true,
    val resumeLesson: ResumeLessonInfo? = null,
)

data class ResumeLessonInfo(
    val lessonId: String,
    val title: String,
    val currentStage: Int,
    val totalStages: Int,
)
