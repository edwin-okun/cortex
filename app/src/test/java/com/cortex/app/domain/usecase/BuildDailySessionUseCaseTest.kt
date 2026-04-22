package com.cortex.app.domain.usecase

import com.cortex.app.domain.model.Lesson
import com.cortex.app.domain.model.LessonProgress
import com.cortex.app.domain.model.ReviewCard
import com.cortex.app.domain.model.SessionConfig
import com.cortex.app.domain.model.SessionItem
import com.cortex.app.domain.model.Tier
import com.cortex.app.domain.model.Track
import com.cortex.app.domain.repository.ContentRepository
import com.cortex.app.domain.repository.ProgressRepository
import com.cortex.app.domain.repository.SchedulerRepository
import com.cortex.app.domain.scheduler.CardState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.minus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildDailySessionUseCaseTest {

    private val scheduler = mockk<SchedulerRepository>()
    private val content = mockk<ContentRepository>()
    private val progress = mockk<ProgressRepository>()
    private val fixedNow: Instant = Instant.parse("2024-06-01T09:00:00Z")
    private val clock = object : Clock { override fun now() = fixedNow }

    private lateinit var useCase: BuildDailySessionUseCase

    @Before
    fun setUp() {
        useCase = BuildDailySessionUseCase(scheduler, content, progress, clock)
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun algLesson(id: String = "alg-1", tier: Tier = Tier.FOUNDATIONS) = Lesson(
        id = id, track = Track.ALGORITHMS, tier = tier,
        title = "Alg $id", stages = emptyList(), reviewCards = emptyList(),
    )

    private fun wealthLesson(id: String = "wealth-1", tier: Tier = Tier.FOUNDATIONS) = Lesson(
        id = id, track = Track.WEALTH, tier = tier,
        title = "Wealth $id", stages = emptyList(), reviewCards = emptyList(),
    )

    private fun card(lessonId: String, overdueHours: Int = 0) = ReviewCard(
        cardId = "$lessonId:${overdueHours}h", lessonId = lessonId,
        prompt = "Q", answer = "A",
        stability = 0.0, difficulty = 0.0,
        elapsedDays = 0, scheduledDays = 0,
        reps = 0, lapses = 0,
        state = CardState.Learning,
        lastReview = null,
        dueAt = fixedNow.minus(overdueHours.toLong(), DateTimeUnit.HOUR),
        step = 0,
    )

    private fun progressEntry(lessonId: String, stage: Int = 1) = LessonProgress(
        lessonId = lessonId, currentStage = stage, stagesCompleted = stage,
        startedAt = 1000L, masteredAt = null, lastOpenedAt = 2000L,
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `empty case - no cards no lessons returns empty session`() = runTest {
        every { scheduler.observeDueCards() } returns flowOf(emptyList())
        every { content.getAllLessons() } returns emptyList()
        every { progress.observeAllProgress() } returns flowOf(emptyList())

        val session = useCase(SessionConfig())

        assertTrue(session.items.isEmpty())
        assertEquals(0, session.totalReviews)
        assertEquals(0, session.totalNewLessons)
    }

    @Test
    fun `only reviews - all items are ReviewCard type`() = runTest {
        val cards = (1..5).map { i -> card("alg-1", overdueHours = i) }
        every { scheduler.observeDueCards() } returns flowOf(cards)
        every { content.getAllLessons() } returns listOf(algLesson())
        every { progress.observeAllProgress() } returns flowOf(listOf(progressEntry("alg-1")))

        val session = useCase(SessionConfig(maxReviews = 5, maxNewLessons = 0))

        assertEquals(5, session.items.size)
        assertTrue(session.items.all { it is SessionItem.ReviewCard })
    }

    @Test
    fun `cap enforcement - maxReviews=5 with 10 due returns exactly 5`() = runTest {
        val cards = (1..10).map { i -> card("alg-1", overdueHours = i) }
        every { scheduler.observeDueCards() } returns flowOf(cards)
        every { content.getAllLessons() } returns listOf(algLesson())
        every { progress.observeAllProgress() } returns flowOf(listOf(progressEntry("alg-1")))

        val session = useCase(SessionConfig(maxReviews = 5, maxNewLessons = 0))

        assertEquals(5, session.items.filterIsInstance<SessionItem.ReviewCard>().size)
        assertEquals(5, session.totalReviews)
    }

    @Test
    fun `only new lessons - first time user gets one new lesson`() = runTest {
        every { scheduler.observeDueCards() } returns flowOf(emptyList())
        every { content.getAllLessons() } returns listOf(algLesson("two-pointers"))
        every { progress.observeAllProgress() } returns flowOf(emptyList())

        val session = useCase(SessionConfig())

        assertEquals(1, session.items.size)
        val newLesson = session.items[0] as SessionItem.NewLesson
        assertEquals("two-pointers", newLesson.lesson.id)
        assertEquals(0, session.totalReviews)
        assertEquals(1, session.totalNewLessons)
    }

    @Test
    fun `interleaving - 3 alg cards in a row triggers a swap with nearest different-track card`() = runTest {
        // Input order: alg, alg, alg, wealth — with maxConsecutiveSameTrack=2
        val alg1 = card("alg-1")
        val alg2 = card("alg-2")
        val alg3 = card("alg-3")
        val w1 = card("wealth-1")
        every { scheduler.observeDueCards() } returns flowOf(listOf(alg1, alg2, alg3, w1))
        every { content.getAllLessons() } returns listOf(
            algLesson("alg-1"), algLesson("alg-2"), algLesson("alg-3"), wealthLesson("wealth-1"),
        )
        every { progress.observeAllProgress() } returns flowOf(emptyList())

        val session = useCase(
            SessionConfig(maxReviews = 4, maxNewLessons = 0, maxConsecutiveSameTrack = 2),
        )

        // Verify: no run of more than 2 cards from the same track
        val tracks = session.items.map { item ->
            val id = (item as SessionItem.ReviewCard).card.lessonId
            if (id.startsWith("alg")) Track.ALGORITHMS else Track.WEALTH
        }
        for (i in 2 until tracks.size) {
            val runLength = (i downTo maxOf(0, i - 2)).count { tracks[it] == tracks[i] }
            assertTrue("Run of $runLength at index $i exceeds max of 2", runLength <= 2)
        }
    }

    @Test
    fun `prerequisite ordering - intermediate lesson does not surface until foundations started`() = runTest {
        val foundations = algLesson("foundations", Tier.FOUNDATIONS)
        val intermediate = algLesson("intermediate", Tier.INTERMEDIATE)
        every { scheduler.observeDueCards() } returns flowOf(emptyList())
        every { content.getAllLessons() } returns listOf(foundations, intermediate)
        every { progress.observeAllProgress() } returns flowOf(emptyList())

        val session = useCase(SessionConfig(maxNewLessons = 2))

        val newLessons = session.items.filterIsInstance<SessionItem.NewLesson>()
        assertEquals("Only FOUNDATIONS should surface when nothing is started", 1, newLessons.size)
        assertEquals("foundations", newLessons[0].lesson.id)
    }

    @Test
    fun `prerequisite ordering - intermediate surfaces once foundations lesson started`() = runTest {
        val foundations = algLesson("foundations", Tier.FOUNDATIONS)
        val intermediate = algLesson("intermediate", Tier.INTERMEDIATE)
        every { scheduler.observeDueCards() } returns flowOf(emptyList())
        every { content.getAllLessons() } returns listOf(foundations, intermediate)
        // foundations already started — only intermediate is unseen
        every { progress.observeAllProgress() } returns flowOf(listOf(progressEntry("foundations")))

        val session = useCase(SessionConfig(maxNewLessons = 2))

        val newLessons = session.items.filterIsInstance<SessionItem.NewLesson>()
        assertEquals(1, newLessons.size)
        assertEquals("intermediate", newLessons[0].lesson.id)
    }

    @Test
    fun `new lesson is inserted between first and second third of reviews`() = runTest {
        // 6 reviews; new lesson should land at index 6/3 = 2
        val cards = (1..6).map { i -> card("alg-1", overdueHours = i) }
        val newLessonLesson = wealthLesson("wealth-new")
        every { scheduler.observeDueCards() } returns flowOf(cards)
        every { content.getAllLessons() } returns listOf(algLesson("alg-1"), newLessonLesson)
        every { progress.observeAllProgress() } returns flowOf(listOf(progressEntry("alg-1")))

        val session = useCase(SessionConfig(maxReviews = 6, maxNewLessons = 1))

        assertEquals(7, session.items.size)
        val newIdx = session.items.indexOfFirst { it is SessionItem.NewLesson }
        // Inserted at index 2 (6/3); interleaving may move it slightly but it should stay in the first half
        assertTrue("New lesson at $newIdx should be in first half", newIdx < 4)
    }
}
