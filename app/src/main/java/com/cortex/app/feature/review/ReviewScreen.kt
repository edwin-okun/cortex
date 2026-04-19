package com.cortex.app.feature.review

import androidx.compose.runtime.Composable
import com.cortex.app.core.ui.components.ComingSoonScreen

@Composable
fun ReviewScreen(onBack: () -> Unit) {
    ComingSoonScreen(
        title = "Review",
        onBack = onBack,
        milestone = "MILESTONE 3",
        description = "Retrieval practice on scheduled cards. Again · Hard · Good · Easy.",
    )
}
