package com.cortex.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onBeginSession: () -> Unit,
    onContinueLesson: (lessonId: String) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenProgress: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = CortexSpacing.xl),
    ) {
        Spacer(Modifier.height(CortexSpacing.xxxl))
        HeaderMark()
        Spacer(Modifier.height(CortexSpacing.xxl))
        Greeting(state.greeting)
        Spacer(Modifier.height(CortexSpacing.xxl))

        // CONTINUE card surfaces above BEGIN SESSION when lesson is in-progress
        state.resumeLesson?.let { resume ->
            ContinueCard(
                info = resume,
                onClick = { onContinueLesson(resume.lessonId) },
            )
            Spacer(Modifier.height(CortexSpacing.lg))
        }

        TodayCard(
            dueReviewCount = state.dueReviewCount,
            newLessonTitle = state.newLessonTitle,
            onBegin = onBeginSession,
        )
        Spacer(Modifier.height(CortexSpacing.lg))
        MetaRow(
            streakDays = state.streakDays,
            onOpenLibrary = onOpenLibrary,
            onOpenProgress = onOpenProgress,
        )
        Spacer(Modifier.weight(1f))
        FooterMark()
        Spacer(Modifier.height(CortexSpacing.xl))
    }
}

@Composable
private fun ContinueCard(info: ResumeLessonInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = CortexColors.PaperRaised,
        border = BorderStroke(1.dp, CortexColors.Accent),
    ) {
        Column(modifier = Modifier.padding(CortexSpacing.xl)) {
            Text(
                text = "CONTINUE",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Accent,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = info.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(CortexSpacing.xs))
            Text(
                text = "stage ${info.currentStage + 1} of ${info.totalStages}",
                style = MaterialTheme.typography.bodyMedium,
                color = CortexColors.Muted,
            )
            Spacer(Modifier.height(CortexSpacing.lg))
            HorizontalDivider(color = CortexColors.Rule)
            Spacer(Modifier.height(CortexSpacing.md))
            Text(
                text = "TAP TO RESUME →",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Accent,
            )
        }
    }
}

@Composable
private fun HeaderMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(CortexColors.Accent),
        )
        Spacer(Modifier.width(CortexSpacing.sm))
        Text(
            text = "CORTEX",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "v0.1 · M2",
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )
    }
}

@Composable
private fun Greeting(greeting: String) {
    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(CortexSpacing.xs))
        Text(
            text = "Today — fifteen minutes of deep focus.",
            style = MaterialTheme.typography.bodyLarge,
            color = CortexColors.Muted,
        )
    }
}

@Composable
private fun TodayCard(
    dueReviewCount: Int,
    newLessonTitle: String?,
    onBegin: () -> Unit,
) {
    // Enabled only when the session will have actual content: reviews or a new lesson.
    // A resume-only lesson doesn't count — the CONTINUE card handles that separately.
    val hasContent = dueReviewCount > 0 || newLessonTitle != null
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = CortexColors.PaperRaised,
        border = BorderStroke(1.dp, CortexColors.Rule),
    ) {
        Column(modifier = Modifier.padding(CortexSpacing.xl)) {
            Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Accent,
            )
            Spacer(Modifier.height(CortexSpacing.md))

            if (!hasContent) {
                Text(
                    text = "All caught up.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(CortexSpacing.sm))
                Text(
                    text = "No reviews due. Continue your lesson above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CortexColors.Muted,
                )
            } else {
                if (dueReviewCount > 0) {
                    StatLine(value = dueReviewCount.toString(), label = "cards due for review")
                    Spacer(Modifier.height(CortexSpacing.sm))
                }
                if (newLessonTitle != null) {
                    StatLine(value = "→", label = "Begin: $newLessonTitle")
                }
            }

            Spacer(Modifier.height(CortexSpacing.xl))
            Button(
                onClick = onBegin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CortexColors.Accent,
                    contentColor = CortexColors.Paper,
                    disabledContainerColor = CortexColors.Rule,
                    disabledContentColor = CortexColors.Muted,
                ),
                enabled = hasContent,
            ) {
                Text(
                    text = "BEGIN SESSION",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun StatLine(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(CortexSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CortexColors.Muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun MetaRow(
    streakDays: Int,
    onOpenLibrary: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CortexSpacing.sm),
    ) {
        MetaTile(
            label = "STREAK",
            value = if (streakDays == 0) "—" else "$streakDays",
            sub = if (streakDays == 0) "start today" else "days",
            onClick = onOpenProgress,
            modifier = Modifier.weight(1f),
        )
        MetaTile(
            label = "LIBRARY",
            value = "25",
            sub = "lessons",
            onClick = onOpenLibrary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetaTile(
    label: String,
    value: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, CortexColors.Rule),
    ) {
        Column(modifier = Modifier.padding(CortexSpacing.lg)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Muted,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = CortexColors.Muted,
            )
        }
    }
}

@Composable
private fun FooterMark() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "ALGORITHMS",
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(CortexColors.Rule),
        )
        Text(
            text = "WEALTH",
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )
    }
}
