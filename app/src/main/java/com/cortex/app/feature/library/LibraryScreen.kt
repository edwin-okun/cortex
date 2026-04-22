package com.cortex.app.feature.library

import androidx.compose.runtime.Composable
import com.cortex.app.core.ui.components.ComingSoonScreen

@Composable
fun LibraryScreen(onBack: () -> Unit) {
    ComingSoonScreen(
        title = "Library",
        milestone = "MILESTONE 6",
        description = "Browse all lessons by track and tier. See your mastery state for each.",
        onBack = onBack,
    )
}
