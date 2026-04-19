package com.cortex.app.domain.model

data class Lesson(
    val id: String,
    val track: Track,
    val tier: Tier,
    val title: String,
    val stages: List<LessonStage>,
    val reviewCards: List<ReviewCard>,
)

sealed class LessonStage {
    data class Hook(val problem: String, val stakes: String) : LessonStage()
    data class Intuition(val narration: List<String>, val visual: VisualSpec?) : LessonStage()
    data class WorkedExample(val steps: List<String>) : LessonStage()
    data class FadedPractice(val problems: List<PracticeItem>) : LessonStage()
    data class TransferProblem(val prompt: String, val solution: String) : LessonStage()
}

data class PracticeItem(
    val id: String,
    val prompt: String,
    val scaffold: Int,       // 2 = lots of hints, 0 = cold
    val hints: List<String>,
    val answer: String,
)

data class ReviewCard(
    val id: String,
    val front: String,
    val back: String,
)