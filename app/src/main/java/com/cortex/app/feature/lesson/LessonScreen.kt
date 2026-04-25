package com.cortex.app.feature.lesson

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cortex.app.core.ui.components.CortexTopBar
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import com.cortex.app.domain.model.LessonStage
import com.cortex.app.domain.model.PracticeItem
import com.cortex.app.domain.model.VisualSpec
import org.koin.androidx.compose.koinViewModel

@Composable
fun LessonScreen(
    onBack: () -> Unit,
    onLessonCompleted: () -> Unit = {},
    viewModel: LessonViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // When the lesson is already complete, the top-bar back must also fire onLessonCompleted
        // so the session queue advances regardless of whether the user uses the CTA or the arrow.
        CortexTopBar(
            title = state.lesson?.title ?: "",
            onBack = if (state.isCompleted) {
                { onLessonCompleted(); onBack() }
            } else {
                onBack
            },
        )

        when {
            state.isLoading -> LoadingContent()
            state.isError -> ErrorContent(onBack)
            state.isCompleted -> CompletedContent(onBack, onLessonCompleted)
            else -> {
                val lesson = state.lesson ?: return
                val stage = lesson.stages.getOrNull(state.currentStageIndex) ?: return
                val stageLabel = stageName(stage)
                val progress = "${state.currentStageIndex + 1} / ${lesson.stages.size}"

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = CortexSpacing.xl),
                ) {
                    Spacer(Modifier.height(CortexSpacing.lg))
                    StageHeader(label = stageLabel, progress = progress)
                    Spacer(Modifier.height(CortexSpacing.xl))

                    when (stage) {
                        is LessonStage.Hook -> HookContent(stage)
                        is LessonStage.Intuition -> IntuitionContent(stage)
                        is LessonStage.WorkedExample -> WorkedExampleContent(stage)
                        is LessonStage.FadedPractice -> FadedPracticeContent(
                            stage = stage,
                            onSubmit = viewModel::onPracticeSubmit,
                            onAllAnswered = viewModel::onStageAdvance,
                        )
                        is LessonStage.TransferProblem -> TransferContent(
                            stage = stage,
                            onComplete = viewModel::onStageAdvance,
                        )
                    }

                    Spacer(Modifier.height(CortexSpacing.xxl))

                    if (stage !is LessonStage.FadedPractice && stage !is LessonStage.TransferProblem) {
                        AdvanceButton(
                            label = if (state.currentStageIndex == lesson.stages.size - 1) "COMPLETE" else "NEXT →",
                            onClick = viewModel::onStageAdvance,
                        )
                        Spacer(Modifier.height(CortexSpacing.xl))
                    }
                }
            }
        }
    }
}

@Composable
private fun StageHeader(label: String, progress: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Accent,
        )
        Text(
            text = progress,
            style = MaterialTheme.typography.labelSmall,
            color = CortexColors.Muted,
        )
    }
    Spacer(Modifier.height(CortexSpacing.sm))
    HorizontalDivider(color = CortexColors.Rule)
    Spacer(Modifier.height(CortexSpacing.xl))
}

@Composable
private fun HookContent(stage: LessonStage.Hook) {
    Text(
        text = stage.problem,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(CortexSpacing.xl))
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = CortexColors.PaperSunken,
        border = BorderStroke(1.dp, CortexColors.Rule),
    ) {
        Column(Modifier.padding(CortexSpacing.lg)) {
            Text(
                text = "WHY IT MATTERS",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Muted,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = stage.stakes,
                style = MaterialTheme.typography.bodyMedium,
                color = CortexColors.Muted,
            )
        }
    }
}

@Composable
private fun IntuitionContent(stage: LessonStage.Intuition) {
    stage.narration.forEachIndexed { index, text ->
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (index < stage.narration.lastIndex) {
            Spacer(Modifier.height(CortexSpacing.lg))
        }
    }
    stage.visual?.let { visual ->
        Spacer(Modifier.height(CortexSpacing.xl))
        VisualFallback(visual)
    }
}

@Composable
private fun VisualFallback(visual: VisualSpec) {
    Surface(
        shape = RoundedCornerShape(2.dp),
        color = CortexColors.PaperSunken,
        border = BorderStroke(1.dp, CortexColors.Rule),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CortexSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            val description = when (visual) {
                is VisualSpec.ArrayPointerVisual ->
                    "[ ${visual.array.joinToString(", ")} ]\n↑ ${visual.leftLabel}           ${visual.rightLabel} ↑\nCanvas visualization in M5."
                is VisualSpec.TextFallback -> visual.description
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CortexColors.Muted,
            )
        }
    }
}

@Composable
private fun WorkedExampleContent(stage: LessonStage.WorkedExample) {
    stage.steps.forEach { step ->
        if (step == "---") {
            Spacer(Modifier.height(CortexSpacing.md))
            HorizontalDivider(color = CortexColors.Rule)
            Spacer(Modifier.height(CortexSpacing.md))
        } else {
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
        }
    }
}

@Composable
private fun FadedPracticeContent(
    stage: LessonStage.FadedPractice,
    onSubmit: (problemId: String, correct: Boolean) -> Unit,
    onAllAnswered: () -> Unit,
) {
    val answered = remember { mutableStateOf(setOf<String>()) }

    stage.problems.forEachIndexed { index, problem ->
        PracticeCard(
            index = index + 1,
            total = stage.problems.size,
            problem = problem,
            isAnswered = problem.id in answered.value,
            onAnswer = { correct ->
                onSubmit(problem.id, correct)
                answered.value = answered.value + problem.id
            },
        )
        Spacer(Modifier.height(CortexSpacing.lg))
    }

    if (answered.value.size == stage.problems.size) {
        Spacer(Modifier.height(CortexSpacing.sm))
        AdvanceButton(label = "NEXT →", onClick = onAllAnswered)
        Spacer(Modifier.height(CortexSpacing.xl))
    }
}

@Composable
private fun PracticeCard(
    index: Int,
    total: Int,
    problem: PracticeItem,
    isAnswered: Boolean,
    onAnswer: (correct: Boolean) -> Unit,
) {
    var showHints by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(2.dp),
        color = CortexColors.PaperRaised,
        border = BorderStroke(1.dp, if (isAnswered) CortexColors.Rule else CortexColors.Accent),
    ) {
        Column(Modifier.padding(CortexSpacing.lg)) {
            Text(
                text = "PROBLEM $index / $total",
                style = MaterialTheme.typography.labelSmall,
                color = CortexColors.Muted,
            )
            Spacer(Modifier.height(CortexSpacing.sm))
            Text(
                text = problem.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (problem.hints.isNotEmpty() && problem.scaffold > 0 && !isAnswered) {
                Spacer(Modifier.height(CortexSpacing.md))
                if (!showHints) {
                    OutlinedButton(
                        onClick = { showHints = true },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CortexColors.Rule),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CortexColors.Muted,
                        ),
                    ) {
                        Text("SHOW HINT", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    problem.hints.forEach { hint ->
                        Text(
                            text = "→ $hint",
                            style = MaterialTheme.typography.bodySmall,
                            color = CortexColors.Muted,
                        )
                    }
                }
            }

            if (!isAnswered) {
                Spacer(Modifier.height(CortexSpacing.md))
                if (!showAnswer) {
                    OutlinedButton(
                        onClick = { showAnswer = true },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CortexColors.Rule),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CortexColors.Muted),
                    ) {
                        Text("REVEAL ANSWER", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text(
                        text = problem.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = CortexColors.Muted,
                    )
                    Spacer(Modifier.height(CortexSpacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(CortexSpacing.sm)) {
                        Button(
                            onClick = { onAnswer(true) },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CortexColors.Success,
                                contentColor = CortexColors.Paper,
                            ),
                        ) {
                            Text("GOT IT", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onAnswer(false) },
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CortexColors.Danger),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CortexColors.Danger),
                        ) {
                            Text("MISSED", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(CortexSpacing.sm))
                Text(
                    text = "✓ answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = CortexColors.Success,
                )
            }
        }
    }
}

@Composable
private fun TransferContent(
    stage: LessonStage.TransferProblem,
    onComplete: () -> Unit,
) {
    var revealed by remember { mutableStateOf(false) }

    Text(
        text = stage.prompt,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(Modifier.height(CortexSpacing.xl))

    if (!revealed) {
        AdvanceButton(label = "REVEAL SOLUTION", onClick = { revealed = true })
    } else {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = CortexColors.PaperSunken,
            border = BorderStroke(1.dp, CortexColors.Rule),
        ) {
            Column(Modifier.padding(CortexSpacing.lg)) {
                Text(
                    text = "SOLUTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = CortexColors.Accent,
                )
                Spacer(Modifier.height(CortexSpacing.sm))
                Text(
                    text = stage.solution,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        Spacer(Modifier.height(CortexSpacing.xl))
        AdvanceButton(label = "COMPLETE LESSON", onClick = onComplete)
        Spacer(Modifier.height(CortexSpacing.xl))
    }
}

@Composable
private fun AdvanceButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CortexColors.Accent,
            contentColor = CortexColors.Paper,
        ),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading…", style = MaterialTheme.typography.bodyMedium, color = CortexColors.Muted)
    }
}

@Composable
private fun ErrorContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CortexSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Lesson not found.", style = MaterialTheme.typography.bodyLarge, color = CortexColors.Danger)
        Spacer(Modifier.height(CortexSpacing.lg))
        AdvanceButton(label = "← BACK", onClick = onBack)
    }
}

@Composable
private fun CompletedContent(onBack: () -> Unit, onLessonCompleted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(CortexSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lesson complete.",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(CortexSpacing.sm))
        Text(
            text = "Review cards have entered your queue.",
            style = MaterialTheme.typography.bodyMedium,
            color = CortexColors.Muted,
        )
        Spacer(Modifier.height(CortexSpacing.xxl))
        AdvanceButton(
            label = "← BACK",
            onClick = {
                onLessonCompleted()
                onBack()
            },
        )
    }
}

private fun stageName(stage: LessonStage) = when (stage) {
    is LessonStage.Hook -> "HOOK"
    is LessonStage.Intuition -> "INTUITION"
    is LessonStage.WorkedExample -> "WORKED EXAMPLE"
    is LessonStage.FadedPractice -> "PRACTICE"
    is LessonStage.TransferProblem -> "TRANSFER"
}
