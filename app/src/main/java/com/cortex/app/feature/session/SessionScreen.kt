package com.cortex.app.feature.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.components.GradeButtons
import com.cortex.app.core.ui.components.ReviewCardContent
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import com.cortex.app.domain.model.SessionItem
import com.cortex.app.domain.scheduler.Rating
import org.koin.androidx.compose.koinViewModel

@Composable
fun SessionScreen(
    onNavigateToLesson: (lessonId: String) -> Unit,
    onSessionDone: () -> Unit,
    viewModel: SessionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is SessionNavEvent.NavigateToLesson -> onNavigateToLesson(event.lessonId)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val s = state) {
            SessionUiState.Loading -> Unit

            is SessionUiState.Active -> ActiveState(
                state = s,
                onReveal = viewModel::onReveal,
                onGrade = viewModel::onGrade,
                onBeginLesson = viewModel::onBeginLesson,
            )

            is SessionUiState.Done -> DoneState(
                summary = s.summary,
                onFinish = onSessionDone,
            )
        }
    }
}

@Composable
private fun ActiveState(
    state: SessionUiState.Active,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
    onBeginLesson: () -> Unit,
) {
    val progress = if (state.totalItems > 0) state.progress.toFloat() / state.totalItems else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CortexSpacing.xl),
    ) {
        Spacer(Modifier.height(CortexSpacing.lg))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = CortexColors.Accent,
            trackColor = CortexColors.Rule,
        )

        Spacer(Modifier.height(CortexSpacing.sm))

        Text(
            text = "${state.progress} / ${state.totalItems}",
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )

        Spacer(Modifier.weight(1f))

        when (val item = state.currentItem) {
            is SessionItem.ReviewCard -> ReviewCardContent(
                prompt = item.card.prompt,
                answer = item.card.answer,
                isAnswerVisible = state.isAnswerVisible,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )

            is SessionItem.NewLesson -> NewLessonCard(
                title = item.lesson.title,
                track = item.lesson.track.name,
            )
        }

        Spacer(Modifier.weight(1f))

        when (val item = state.currentItem) {
            is SessionItem.ReviewCard -> ReviewActions(
                isAnswerVisible = state.isAnswerVisible,
                buttonLabels = state.buttonLabels,
                onReveal = onReveal,
                onGrade = onGrade,
            )

            is SessionItem.NewLesson -> Button(
                onClick = onBeginLesson,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CortexColors.Accent,
                    contentColor = CortexColors.Paper,
                ),
            ) {
                Text("BEGIN", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(CortexSpacing.xl))
    }
}

@Composable
private fun ReviewActions(
    isAnswerVisible: Boolean,
    buttonLabels: com.cortex.app.core.ui.components.ButtonLabels?,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
) {
    if (!isAnswerVisible) {
        Button(
            onClick = onReveal,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CortexColors.Accent,
                contentColor = CortexColors.Paper,
            ),
        ) {
            Text("REVEAL", style = MaterialTheme.typography.labelMedium)
        }
    } else if (buttonLabels != null) {
        GradeButtons(labels = buttonLabels, onGrade = onGrade)
    }
}

@Composable
private fun NewLessonCard(title: String, track: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = CortexColors.PaperRaised,
        border = BorderStroke(1.dp, CortexColors.Accent),
    ) {
        Column(modifier = Modifier.padding(CortexSpacing.xl)) {
            Text(
                text = "NEW LESSON",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Accent,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CortexSpacing.xs))
            Text(
                text = track,
                style = MaterialTheme.typography.bodyMedium,
                color = CortexColors.Muted,
            )
        }
    }
}

@Composable
private fun DoneState(summary: SessionSummary, onFinish: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CortexSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val parts = buildList {
                if (summary.reviewsCompleted > 0) {
                    add("${summary.reviewsCompleted} ${if (summary.reviewsCompleted == 1) "review" else "reviews"}")
                }
                if (summary.newLessonsStarted > 0) {
                    add("${summary.newLessonsStarted} new ${if (summary.newLessonsStarted == 1) "lesson" else "lessons"}")
                }
                val goodCount = (summary.ratingCounts[Rating.Good] ?: 0) +
                    (summary.ratingCounts[Rating.Easy] ?: 0)
                val hardCount = summary.ratingCounts[Rating.Hard] ?: 0
                val againCount = summary.ratingCounts[Rating.Again] ?: 0
                if (goodCount + hardCount + againCount > 0) {
                    add("$goodCount good · $hardCount hard · $againCount again")
                }
            }

            Text(
                text = "Session complete.",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            parts.forEach { part ->
                Text(
                    text = part,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CortexColors.Muted,
                )
            }
            Spacer(Modifier.height(CortexSpacing.xxl))
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CortexColors.Accent,
                    contentColor = CortexColors.Paper,
                ),
            ) {
                Text("← HOME", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
