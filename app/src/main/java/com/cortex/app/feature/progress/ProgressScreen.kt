package com.cortex.app.feature.progress

import androidx.compose.runtime.Composable
import com.cortex.app.core.ui.components.ComingSoonScreen

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    ComingSoonScreen(
        title = "Progress",
        milestone = "MILESTONE 6",
        description = "Streak, retention curve, lessons mastered. Evidence of compounding.",
        onBack = onBack,
    )
}
