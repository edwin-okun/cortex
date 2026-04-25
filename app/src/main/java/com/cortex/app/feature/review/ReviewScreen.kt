package com.cortex.app.feature.review

import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.components.CortexTopBar
import com.cortex.app.core.ui.components.GradeButtons
import com.cortex.app.core.ui.components.ReviewCardContent
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

        Row(modifier = Modifier.fillMaxWidth()) {
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

        ReviewCardContent(
            prompt = state.card.prompt,
            answer = state.card.answer,
            isAnswerVisible = state.isAnswerVisible,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )

        Spacer(Modifier.weight(1f))

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
