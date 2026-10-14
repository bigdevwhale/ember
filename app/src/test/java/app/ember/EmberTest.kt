package app.ember

import app.ember.content.ContentEn
import app.ember.content.ContentRu
import app.ember.data.Fast
import app.ember.domain.Badges
import app.ember.domain.ChallengeKind
import app.ember.domain.Challenges
import app.ember.domain.Facts
import app.ember.domain.Levels
import app.ember.domain.Plans
import app.ember.domain.Stages
import app.ember.domain.Stats
import app.ember.notify.Alarms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class EmberTest {
    private val zone = ZoneOffset.UTC
    private val hour = 3_600_000L

    private fun at(d: LocalDate, h: Int, m: Int = 0) = d.atTime(h, m).toInstant(zone).toEpochMilli()

    /** A 16 h fast that starts at 20:00 on [d]. */
    private fun fast16(d: LocalDate, hours: Int = 16) = Fast(start = at(d, 20), end = at(d, 20) + hours * hour, goalMinutes = 16 * 60, plan = "16_8")

    private val today = LocalDate.of(2026, 10, 14)
    private val noon = at(today, 12, 30)

    @Test
    fun stagesFollowTheHours() {
        assertEquals("digest", Stages.at(0.0).id)
        assertEquals("switch", Stages.at(11.9).id)
        assertEquals("burn", Stages.at(12.0).id)
        assertEquals("ketosis", Stages.at(16.5).id)
        assertEquals("renewal", Stages.at(100.0).id)
        assertEquals("autophagy", Stages.next(17.0)?.id)
        assertNull(Stages.next(80.0))
        assertEquals(0.5f, Stages.within(14.0), 0.001f)
        assertEquals(listOf("digest", "settle", "switch", "burn"), Stages.along(16.0).map { it.id })
    }

    @Test
    fun plansAndCustomHours() {
        assertEquals(8, Plans.byId("16_8").eatHours)
        assertFalse(Plans.byId("36h").daily)
        assertEquals(Plans.MAX_CUSTOM, Plans.byId("custom", 500).fastHours)
        assertEquals(19, Plans.byId("nope", 19).fastHours)
        assertEquals("14_10", Plans.suggest(0))
    }

    @Test
    fun streakCountsDaysWithAReachedGoal() {
        val fasts = (1..5).map { fast16(today.minusDays(it.toLong())) }
        val s = Stats(fasts, noon, zone)
        // Fasts end on the mornings of the 10th..14th.
        assertEquals(5, s.streak)
        assertEquals(5, s.bestStreak)
        assertEquals(5, s.successes)
    }

    @Test
    fun todayNotYetDoneDoesNotBreakTheStreak() {
        val fasts = (2..4).map { fast16(today.minusDays(it.toLong())) } + Fast(start = at(today.minusDays(1), 21), goalMinutes = 960, plan = "16_8")
        val s = Stats(fasts, at(today, 9), zone)
        assertEquals(3, s.streak)
        assertTrue(s.active != null)
    }

    @Test
    fun aShortFastBreaksTheStreak() {
        val fasts = listOf(fast16(today.minusDays(3)), fast16(today.minusDays(2), hours = 12), fast16(today.minusDays(1)))
        val s = Stats(fasts, noon, zone)
        assertEquals(1, s.streak)
        assertEquals(1, s.bestStreak)
        assertEquals(2, s.successes)
    }

    @Test
    fun aLongFastCoversTheDaysInside() {
        val start = at(today.minusDays(3), 19)
        val f = Fast(start = start, end = start + 48 * hour, goalMinutes = 48 * 60, plan = "48h")
        val s = Stats(listOf(f), noon, zone)
        assertEquals(setOf(today.minusDays(2), today.minusDays(1)), s.days)
        assertEquals(2, s.streak)
    }

    @Test
    fun hoursAreSplitAcrossMidnight() {
        val s = Stats(listOf(fast16(today.minusDays(1))), noon, zone)
        val h = s.hoursByDay(today.minusDays(1), today)
        assertEquals(4.0, h[today.minusDays(1)]!!, 0.001)
        assertEquals(12.0, h[today]!!, 0.001)
    }

    @Test
    fun levelsGrow() {
        assertEquals(1, Levels.level(0))
        assertEquals(1, Levels.level(19))
        assertEquals(2, Levels.level(20))
        assertEquals(3, Levels.level(60))
        assertEquals(0.5f, Levels.progress(40), 0.001f)
        assertEquals(11, Levels.title(40))
    }

    @Test
    fun badgesUnlockFromFacts() {
        val got = Badges.unlocked(Facts(longest = 24.5, bestStreak = 7, fasts = 10, hours = 120.0, hunger = 5)).map { it.id }.toSet()
        assertTrue(listOf("first", "h16", "h24", "s7", "f10", "t100", "hunger5").all { it in got })
        assertFalse("h36" in got || "s14" in got || "w1" in got)
        assertEquals(Badges.all.size, Badges.all.map { it.id }.toSet().size)
    }

    @Test
    fun weeklyChallengeRotatesAndCounts() {
        val kinds = (0 until 6).map { Challenges.kindFor(Stats.weekStart(today).plusWeeks(it.toLong())) }.toSet()
        assertEquals(ChallengeKind.entries.toSet(), kinds)
        val fasts = (1..3).map { fast16(today.minusDays(it.toLong())) }
        val c = Challenges.of(Stats(fasts, noon, zone), Plans.byId("16_8"), emptyList(), 8, emptyList(), zone)
        assertEquals(Challenges.weekKey(Stats.weekStart(today)), c.week)
        assertTrue(c.progress <= c.target)
    }

    @Test
    fun alarmsPickTheNextClockTime() {
        val now = LocalDateTime.of(2026, 10, 14, 21, 0)
        assertEquals(LocalDateTime.of(2026, 10, 15, 20, 0), Alarms.nextAt(20 * 60, now))
        assertEquals(LocalDateTime.of(2026, 10, 14, 23, 30), Alarms.nextAt(23 * 60 + 30, now))
        assertEquals(LocalDateTime.of(2026, 10, 15, 19, 30), Alarms.nextAt(-30 + 20 * 60, now))
        val f = Fast(start = 0, goalMinutes = 960, plan = "16_8")
        assertEquals(12 * hour, Alarms.nextStageAt(f, 9 * hour))
    }

    @Test
    fun contentIsComplete() {
        for (c in listOf(ContentEn, ContentRu)) {
            assertEquals(Stages.all.map { it.id }.toSet(), c.stages.keys)
            assertEquals(18, c.articles.size)
            assertEquals(ContentEn.articles.map { it.id }, c.articles.map { it.id })
            assertEquals(0, c.hours.first().hour)
            assertEquals(c.hours.sortedBy { it.hour }, c.hours)
            assertTrue(c.tips.size >= 30 && c.hunger.isNotEmpty() && c.cheers.isNotEmpty())
            c.stages.values.forEach { assertTrue(it.tips.isNotEmpty()) }
        }
    }
}
