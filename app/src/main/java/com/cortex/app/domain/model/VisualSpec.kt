package com.cortex.app.domain.model

// M5: Canvas visualizations. All variants render as TextFallback until then.
sealed class VisualSpec {
    data class ArrayPointerVisual(
        val array: List<Int>,
        val leftLabel: String = "left",
        val rightLabel: String = "right",
    ) : VisualSpec()

    data class TextFallback(val description: String) : VisualSpec()
}