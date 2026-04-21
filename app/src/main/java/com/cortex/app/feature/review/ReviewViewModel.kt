package com.cortex.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.Fsrs
import com.cortex.app.domain.scheduler.FsrsCard
import com.cortex.app.domain.scheduler.FsrsParameters
import com.cortex.app.domain.scheduler.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ReviewViewModel(
    private val schedulerRepository: SchedulerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    // In-memory queue — loaded once on init; not re-queried between grades
    // so the current card never disappears mid-session.
    private val queue = ArrayDeque<ReviewCard>()
    private var reviewedCount = 0

    init {
        viewModelScope.launch {
            val due = schedulerRepository.observeDueCards().first()
            if (due.isEmpty()) {
                _state.update { ReviewUiState.Empty }
            } else {
                queue.addAll(due)
                showCurrentCard()
            }
        }
    }

    fun onReveal() {
        val current = _state.value as? ReviewUiState.Reviewing ?: return
        _state.update { current.copy(isAnswerVisible = true) }
    }

    fun onGrade(rating: Rating) {
        val current = _state.value as? ReviewUiState.Reviewing ?: return
        viewModelScope.launch {
            schedulerRepository.grade(current.card.cardId, rating)
            reviewedCount++
            queue.removeFirst()
            if (queue.isEmpty()) {
                _state.update { ReviewUiState.Done(reviewedCount) }
            } else {
                showCurrentCard()
            }
        }
    }

    private fun showCurrentCard() {
        val card = queue.first()
        val now = Clock.System.now()
        val params = FsrsParameters.Default
        val fsrs = card.toFsrs()

        _state.update {
            ReviewUiState.Reviewing(
                card = card,
                queueSize = queue.size,
                reviewedCount = reviewedCount,
                isAnswerVisible = false,
                buttonLabels = ButtonLabels(
                    again = previewInterval(fsrs, Rating.Again, now, params),
                    hard = previewInterval(fsrs, Rating.Hard, now, params),
                    good = previewInterval(fsrs, Rating.Good, now, params),
                    easy = previewInterval(fsrs, Rating.Easy, now, params),
                ),
            )
        }
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

    private fun ReviewCard.toFsrs() = FsrsCard(
        stability = stability,
        difficulty = difficulty,
        elapsedDays = elapsedDays,
        scheduledDays = scheduledDays,
        reps = reps,
        lapses = lapses,
        state = state,
        lastReview = lastReview,
        step = step,
    )
}

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data object Empty : ReviewUiState
    data class Done(val reviewedCount: Int) : ReviewUiState
    data class Reviewing(
        val card: ReviewCard,
        val queueSize: Int,
        val reviewedCount: Int,
        val isAnswerVisible: Boolean,
        val buttonLabels: ButtonLabels,
    ) : ReviewUiState
}

data class ButtonLabels(
    val again: String,
    val hard: String,
    val good: String,
    val easy: String,
)
