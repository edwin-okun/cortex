package com.cortex.app.feature.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.usecase.GetLessonUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    private val getLessonUseCase: GetLessonUseCase,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    val lessonId: String = checkNotNull(savedStateHandle["lessonId"])

    private val _state = MutableStateFlow(LessonUiState())
    val state: StateFlow<LessonUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val lesson = getLessonUseCase(lessonId)
            if (lesson == null) {
                _state.update { it.copy(isLoading = false, isError = true) }
                return@launch
            }

            val savedProgress = progressRepository.observeProgress(lessonId).first()
            val startStage = savedProgress?.currentStage ?: 0

            progressRepository.recordLessonOpened(lessonId)

            _state.update {
                LessonUiState(
                    lesson = lesson,
                    currentStageIndex = startStage,
                    isLoading = false,
                )
            }
        }
    }

    fun onStageAdvance() {
        val lesson = _state.value.lesson ?: return
        val nextStage = _state.value.currentStageIndex + 1
        _state.update { it.copy(currentStageIndex = nextStage) }
        viewModelScope.launch {
            progressRepository.recordStageAdvance(lessonId, nextStage, lesson.stages.size)
        }
    }

    fun onPracticeSubmit(problemId: String, correct: Boolean) {
        viewModelScope.launch {
            progressRepository.recordPracticeAttempt(lessonId, problemId, correct)
        }
    }
}

data class LessonUiState(
    val lesson: Lesson? = null,
    val currentStageIndex: Int = 0,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
) {
    val isCompleted: Boolean
        get() = lesson != null && currentStageIndex >= lesson.stages.size
}
