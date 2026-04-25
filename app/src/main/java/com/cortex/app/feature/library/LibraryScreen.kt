package com.cortex.app.feature.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.components.CortexTopBar
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onOpenLesson: (lessonId: String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CortexTopBar(title = "Library", onBack = onBack)

        when {
            state.isLoading -> {
                Text(
                    text = "Loading lessons…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CortexColors.Muted,
                    modifier = Modifier.padding(CortexSpacing.xl),
                )
            }

            state.lessons.isEmpty() -> {
                Text(
                    text = "No lessons available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CortexColors.Muted,
                    modifier = Modifier.padding(CortexSpacing.xl),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(CortexSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(CortexSpacing.lg),
                ) {
                    item {
                        Text(
                            text = "Browse first. Start only when the lesson looks worth your time.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CortexColors.Muted,
                        )
                    }
                    items(state.lessons, key = { it.lessonId }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            onOpenLesson = { onOpenLesson(lesson.lessonId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: LibraryLessonItem,
    onOpenLesson: () -> Unit,
) {
    var expanded by rememberSaveable(lesson.lessonId) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = CortexColors.PaperRaised,
        border = BorderStroke(1.dp, CortexColors.Rule),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(CortexSpacing.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = CortexColors.Accent,
                    )
                    Spacer(Modifier.height(CortexSpacing.sm))
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(CortexSpacing.xs))
                    Text(
                        text = "${lesson.track} · ${lesson.tier}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CortexColors.Muted,
                    )
                }
                Text(
                    text = if (expanded) "HIDE" else "VIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = CortexColors.Accent,
                )
            }

            Spacer(Modifier.height(CortexSpacing.lg))
            Text(
                text = lesson.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (expanded) {
                Spacer(Modifier.height(CortexSpacing.lg))
                HorizontalDivider(color = CortexColors.Rule)
                Spacer(Modifier.height(CortexSpacing.lg))
                MetaRow(label = "Structure", value = "${lesson.stageCount} stages")
                Spacer(Modifier.height(CortexSpacing.sm))
                MetaRow(label = "Reviews", value = "${lesson.reviewCount} cards")
                lesson.progressLabel?.let {
                    Spacer(Modifier.height(CortexSpacing.sm))
                    MetaRow(label = "Progress", value = it)
                }
                Spacer(Modifier.height(CortexSpacing.xl))
            } else {
                Spacer(Modifier.height(CortexSpacing.xl))
            }

            Button(
                onClick = onOpenLesson,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CortexColors.Accent,
                    contentColor = CortexColors.Paper,
                ),
            ) {
                Text(
                    text = lesson.ctaLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
