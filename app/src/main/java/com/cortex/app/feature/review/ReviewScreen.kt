package com.cortex.app.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.components.CortexTopBar
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import com.cortex.app.domain.scheduler.Rating
import org.koin.androidx.compose.koinViewModel

@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CortexTopBar(title = "Review", onBack = onBack)

        when (val s = state) {
            ReviewUiState.Loading -> Unit
            ReviewUiState.Empty -> EmptyState()
            is ReviewUiState.Done -> DoneState(s.reviewedCount, onBack)
            is ReviewUiState.Reviewing -> ReviewingState(
                state = s,
                onReveal = viewModel::onReveal,
                onGrade = viewModel::onGrade,
            )
        }
    }
}

@Composable
private fun ReviewingState(
    state: ReviewUiState.Reviewing,
    onReveal: () -> Unit,
    onGrade: (Rating) -> Unit,
) {
    val total = state.reviewedCount + state.queueSize
    val progress = if (total > 0) state.reviewedCount.toFloat() / total else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CortexSpacing.xl),
    ) {
        Spacer(Modifier.height(CortexSpacing.lg))

        // Progress header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.reviewedCount} / $total",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Muted,
            )
        }
        Spacer(Modifier.height(CortexSpacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = CortexColors.Accent,
            trackColor = CortexColors.Rule,
        )

        Spacer(Modifier.weight(1f))

        // Card prompt — the hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.card.prompt,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            if (state.isAnswerVisible) {
                Spacer(Modifier.height(CortexSpacing.xl))
                HorizontalDivider(color = CortexColors.Rule)
                Spacer(Modifier.height(CortexSpacing.xl))
                Text(
                    text = state.card.answer,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CortexColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Action area
        if (!state.isAnswerVisible) {
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
        } else {
            GradeButtons(labels = state.buttonLabels, onGrade = onGrade)
        }

        Spacer(Modifier.height(CortexSpacing.xl))
    }
}

@Composable
private fun GradeButtons(labels: ButtonLabels, onGrade: (Rating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CortexSpacing.sm),
    ) {
        GradeButton(
            modifier = Modifier.weight(1f),
            label = "Again",
            interval = labels.again,
            containerColor = CortexColors.Danger,
            onClick = { onGrade(Rating.Again) },
        )
        GradeButton(
            modifier = Modifier.weight(1f),
            label = "Hard",
            interval = labels.hard,
            containerColor = CortexColors.Warning,
            onClick = { onGrade(Rating.Hard) },
        )
        GradeButton(
            modifier = Modifier.weight(1f),
            label = "Good",
            interval = labels.good,
            containerColor = CortexColors.Accent,
            onClick = { onGrade(Rating.Good) },
        )
        GradeButton(
            modifier = Modifier.weight(1f),
            label = "Easy",
            interval = labels.easy,
            containerColor = CortexColors.Success,
            onClick = { onGrade(Rating.Easy) },
        )
    }
}

@Composable
private fun GradeButton(
    modifier: Modifier = Modifier,
    label: String,
    interval: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = CortexColors.Paper,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = CortexSpacing.sm,
            vertical = CortexSpacing.sm,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(interval, style = MaterialTheme.typography.labelSmall, color = CortexColors.Paper.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Nothing due.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = "Come back later.",
                style = MaterialTheme.typography.bodyMedium,
                color = CortexColors.Muted,
            )
        }
    }
}

@Composable
private fun DoneState(reviewedCount: Int, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CortexSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$reviewedCount",
                style = MaterialTheme.typography.displayLarge,
                color = CortexColors.Accent,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = if (reviewedCount == 1) "card reviewed." else "cards reviewed.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CortexSpacing.xxl))
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CortexColors.Rule),
            ) {
                Text(
                    "Return home",
                    style = MaterialTheme.typography.labelMedium,
                    color = CortexColors.Muted,
                )
            }
        }
    }
}
