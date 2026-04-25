package com.cortex.app.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.core.ui.components.ButtonLabels
import com.cortex.app.data.store.SessionConfigStore
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.model.SessionItem
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.Fsrs
import com.cortex.app.domain.scheduler.FsrsCard
import com.cortex.app.domain.scheduler.FsrsParameters
import com.cortex.app.domain.scheduler.Rating
import com.cortex.app.domain.usecase.BuildDailySessionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SessionViewModel(
    private val buildDailySessionUseCase: BuildDailySessionUseCase,
    private val schedulerRepository: SchedulerRepository,
    private val configStore: SessionConfigStore,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    private val _navEvents = MutableSharedFlow<SessionNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<SessionNavEvent> = _navEvents.asSharedFlow()

    // In-memory queue loaded once on init — same pattern as ReviewViewModel.
    private val queue = ArrayDeque<SessionItem>()
    private var completedReviews = 0
    private var completedLessons = 0
    private val ratingCounts = mutableMapOf<Rating, Int>()
    private val startedAt: Instant = clock.now()
    private var isAdvancing = false

    init {
        viewModelScope.launch {
            val config = configStore.observe().first()
            val session = buildDailySessionUseCase(config)
            queue.addAll(session.items)
            if (queue.isEmpty()) {
                _state.update { SessionUiState.Done(buildSummary()) }
            } else {
                showCurrentItem()
            }
        }
    }

    fun onReveal() {
        val active = _state.value as? SessionUiState.Active ?: return
        if (active.currentItem is SessionItem.ReviewCard) {
            _state.update { active.copy(isAnswerVisible = true) }
        }
    }

    fun onGrade(rating: Rating) {
        if (isAdvancing) return
        val active = _state.value as? SessionUiState.Active ?: return
        val item = active.currentItem as? SessionItem.ReviewCard ?: return
        isAdvancing = true
        viewModelScope.launch {
            try {
                schedulerRepository.grade(item.card.cardId, rating)
                ratingCounts[rating] = (ratingCounts[rating] ?: 0) + 1
                completedReviews++
                queue.removeFirst()
                if (queue.isEmpty()) {
                    _state.update { SessionUiState.Done(buildSummary()) }
                } else {
                    showCurrentItem()
                }
            } finally {
                isAdvancing = false
            }
        }
    }

    fun onBeginLesson() {
        val active = _state.value as? SessionUiState.Active ?: return
        val lesson = (active.currentItem as? SessionItem.NewLesson)?.lesson ?: return
        viewModelScope.launch {
            _navEvents.emit(SessionNavEvent.NavigateToLesson(lesson.id))
        }
    }

    /** Called when the user returns from LessonScreen (regardless of completion). */
    fun onLessonReturned() {
        val active = _state.value as? SessionUiState.Active ?: return
        if (active.currentItem !is SessionItem.NewLesson) return
        completedLessons++
        queue.removeFirstOrNull()
        if (queue.isEmpty()) {
            _state.update { SessionUiState.Done(buildSummary()) }
        } else {
            showCurrentItem()
        }
        isAdvancing = false
    }

    private fun showCurrentItem() {
        val item = queue.firstOrNull() ?: return
        val progress = completedReviews + completedLessons
        val total = progress + queue.size

        _state.update {
            SessionUiState.Active(
                currentItem = item,
                progress = progress,
                totalItems = total,
                isAnswerVisible = false,
                buttonLabels = if (item is SessionItem.ReviewCard) {
                    computeLabels(item.card)
                } else null,
            )
        }
    }

    private fun computeLabels(card: ReviewCard): ButtonLabels {
        val now = clock.now()
        val params = FsrsParameters.Default
        val fsrs = card.toFsrs()
        return ButtonLabels(
            again = previewInterval(fsrs, Rating.Again, now, params),
            hard = previewInterval(fsrs, Rating.Hard, now, params),
            good = previewInterval(fsrs, Rating.Good, now, params),
            easy = previewInterval(fsrs, Rating.Easy, now, params),
        )
    }

    private fun previewInterval(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
        params: FsrsParameters,
    ): String {
        val next = Fsrs.schedule(card, rating, now, params)
        val due = Fsrs.nextDue(next, params)
        val totalSeconds = due.inWholeSeconds
        return when {
            totalSeconds < 60 -> "< 1m"
            totalSeconds < 3600 -> "${totalSeconds / 60}m"
            totalSeconds < 86400 -> "${totalSeconds / 3600}h"
            else -> "${due.inWholeDays}d"
        }
    }

    private fun buildSummary() = SessionSummary(
        reviewsCompleted = completedReviews,
        newLessonsStarted = completedLessons,
        ratingCounts = ratingCounts.toMap(),
        durationSeconds = (clock.now() - startedAt).inWholeSeconds,
    )

    private fun ReviewCard.toFsrs() = FsrsCard(
        stability = stability, difficulty = difficulty,
        elapsedDays = elapsedDays, scheduledDays = scheduledDays,
        reps = reps, lapses = lapses, state = state,
        lastReview = lastReview, step = step,
    )
}

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Active(
        val currentItem: SessionItem,
        val progress: Int,
        val totalItems: Int,
        val isAnswerVisible: Boolean,
        val buttonLabels: ButtonLabels?,
    ) : SessionUiState

    data class Done(val summary: SessionSummary) : SessionUiState
}

data class SessionSummary(
    val reviewsCompleted: Int,
    val newLessonsStarted: Int,
    val ratingCounts: Map<Rating, Int>,
    val durationSeconds: Long,
)

sealed interface SessionNavEvent {
    data class NavigateToLesson(val lessonId: String) : SessionNavEvent
}
