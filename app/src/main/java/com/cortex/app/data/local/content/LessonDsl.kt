package com.cortex.app.data.local.content

import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonStage
import com.cortex.app.domain.model.PracticeItem
import com.cortex.app.domain.model.LessonReviewCard
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.model.VisualSpec

fun lesson(id: String, block: LessonBuilder.() -> Unit): Lesson =
    LessonBuilder(id).apply(block).build()

class LessonBuilder(val id: String) {
    var track: Track = Track.ALGORITHMS
    var tier: Tier = Tier.FOUNDATIONS
    var title: String = ""

    private val stages = mutableListOf<LessonStage>()
    private val cards = mutableListOf<LessonReviewCard>()

    fun hook(block: HookBuilder.() -> Unit) { stages += HookBuilder().apply(block).build() }
    fun intuition(block: IntuitionBuilder.() -> Unit) { stages += IntuitionBuilder().apply(block).build() }
    fun workedExample(block: WorkedExampleBuilder.() -> Unit) { stages += WorkedExampleBuilder().apply(block).build() }
    fun fadedPractice(block: FadedPracticeBuilder.() -> Unit) { stages += FadedPracticeBuilder().apply(block).build() }
    fun transferProblem(block: TransferBuilder.() -> Unit) { stages += TransferBuilder().apply(block).build() }
    fun reviewCards(block: ReviewCardsBuilder.() -> Unit) { cards += ReviewCardsBuilder().apply(block).build() }

    fun build() = Lesson(id, track, tier, title, stages.toList(), cards.toList())
}

class HookBuilder {
    var problem: String = ""
    var stakes: String = ""
    fun build() = LessonStage.Hook(problem, stakes)
}

class IntuitionBuilder {
    private val parts = mutableListOf<String>()
    var visual: VisualSpec? = null
    fun narration(vararg text: String) { parts += text }
    fun build() = LessonStage.Intuition(parts.toList(), visual)
}

class WorkedExampleBuilder {
    private val steps = mutableListOf<String>()
    fun step(text: String) { steps += text }
    fun build() = LessonStage.WorkedExample(steps.toList())
}

class FadedPracticeBuilder {
    private val problems = mutableListOf<PracticeItem>()
    fun problem(id: String, scaffold: Int, block: PracticeItemBuilder.() -> Unit) {
        problems += PracticeItemBuilder(id, scaffold).apply(block).build()
    }
    fun build() = LessonStage.FadedPractice(problems.toList())
}

class PracticeItemBuilder(val id: String, val scaffold: Int) {
    var prompt: String = ""
    var answer: String = ""
    private val hints = mutableListOf<String>()
    fun hint(text: String) { hints += text }
    fun build() = PracticeItem(id, prompt, scaffold, hints.toList(), answer)
}

class TransferBuilder {
    var prompt: String = ""
    var solution: String = ""
    fun build() = LessonStage.TransferProblem(prompt, solution)
}

class ReviewCardsBuilder {
    private val cards = mutableListOf<LessonReviewCard>()
    fun card(id: String, front: String, back: String) { cards += LessonReviewCard(id, front, back) }
    fun build(): List<LessonReviewCard> = cards.toList()
}
