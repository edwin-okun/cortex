package com.cortex.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cortex.app.core.ui.theme.CortexColors
import com.cortex.app.core.ui.theme.CortexSpacing
import com.cortex.app.domain.scheduler.Rating

data class ButtonLabels(
    val again: String,
    val hard: String,
    val good: String,
    val easy: String,
)

@Composable
fun ReviewCardContent(
    prompt: String,
    answer: String,
    isAnswerVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        if (isAnswerVisible) {
            Spacer(Modifier.height(CortexSpacing.xl))
            HorizontalDivider(color = CortexColors.Rule)
            Spacer(Modifier.height(CortexSpacing.xl))
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyLarge,
                color = CortexColors.Muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun GradeButtons(labels: ButtonLabels, onGrade: (Rating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CortexSpacing.sm),
    ) {
        GradeButton(Modifier.weight(1f), "Again", labels.again, CortexColors.Danger) { onGrade(Rating.Again) }
        GradeButton(Modifier.weight(1f), "Hard", labels.hard, CortexColors.Warning) { onGrade(Rating.Hard) }
        GradeButton(Modifier.weight(1f), "Good", labels.good, CortexColors.Accent) { onGrade(Rating.Good) }
        GradeButton(Modifier.weight(1f), "Easy", labels.easy, CortexColors.Success) { onGrade(Rating.Easy) }
    }
}

@Composable
private fun GradeButton(
    modifier: Modifier,
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
