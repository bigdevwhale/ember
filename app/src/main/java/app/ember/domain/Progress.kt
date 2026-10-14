package app.ember.domain

import app.ember.data.CheckIn
import app.ember.data.Fast
import app.ember.data.WaterDay
import app.ember.data.WeightEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields

/** Levels grow with fasted hours: the fire gets bigger. */
object Levels {
    /** One point per fasted hour, two more for every fast that reached its goal. */
    fun xp(stats: Stats): Int = stats.totalHours.toInt() + stats.successes * 2

    /** Points needed to reach [level]. */
    fun need(level: Int): Int = 10 * level * (level - 1)

    fun level(xp: Int): Int {
        var l = 1
        while (xp >= need(l + 1)) l++
        return l
    }

    /** Progress toward the next level, 0..1. */
    fun progress(xp: Int): Float {
        val l = level(xp)
        return (xp - need(l)).toFloat() / (need(l + 1) - need(l))
    }

    const val TITLES = 12

    /** Index into the level titles: spark, ember, flicker… sun. */
    fun title(level: Int): Int = (level - 1).coerceIn(0, TITLES - 1)
}

enum class Metric { LONGEST, STREAK, FASTS, HOURS, WEIGH_INS, LOST, WATER_DAYS, CHECKINS, HUNGER }

data class Badge(val id: String, val metric: Metric, val target: Int)

/** The numbers badges are measured against. */
data class Facts(
    val longest: Double = 0.0,
    val bestStreak: Int = 0,
    val fasts: Int = 0,
    val hours: Double = 0.0,
    val weighIns: Int = 0,
    val lostKg: Double = 0.0,
    val waterDays: Int = 0,
    val checkIns: Int = 0,
    val hunger: Int = 0,
) {
    fun value(m: Metric): Double = when (m) {
        Metric.LONGEST -> longest
        Metric.STREAK -> bestStreak.toDouble()
        Metric.FASTS -> fasts.toDouble()
        Metric.HOURS -> hours
        Metric.WEIGH_INS -> weighIns.toDouble()
        Metric.LOST -> lostKg
        Metric.WATER_DAYS -> waterDays.toDouble()
        Metric.CHECKINS -> checkIns.toDouble()
        Metric.HUNGER -> hunger.toDouble()
    }

    companion object {
        fun of(stats: Stats, weights: List<WeightEntry>, water: List<WaterDay>, waterGoal: Int, checkIns: List<CheckIn>, hunger: Int): Facts {
            val w = weights.sortedBy { it.at }
            return Facts(
                longest = stats.longestHours,
                bestStreak = stats.bestStreak,
                fasts = stats.successes,
                hours = stats.totalHours,
                weighIns = w.size,
                lostKg = if (w.size < 2) 0.0 else (w.first().kg - w.last().kg).coerceAtLeast(0.0),
                waterDays = water.count { it.glasses >= waterGoal },
                checkIns = checkIns.size,
                hunger = hunger,
            )
        }
    }
}

object Badges {
    val all = listOf(
        Badge("first", Metric.FASTS, 1),
        Badge("h16", Metric.LONGEST, 16),
        Badge("h18", Metric.LONGEST, 18),
        Badge("h20", Metric.LONGEST, 20),
        Badge("h24", Metric.LONGEST, 24),
        Badge("h36", Metric.LONGEST, 36),
        Badge("h48", Metric.LONGEST, 48),
        Badge("h72", Metric.LONGEST, 72),
        Badge("s3", Metric.STREAK, 3),
        Badge("s7", Metric.STREAK, 7),
        Badge("s14", Metric.STREAK, 14),
        Badge("s30", Metric.STREAK, 30),
        Badge("s60", Metric.STREAK, 60),
        Badge("s100", Metric.STREAK, 100),
        Badge("f10", Metric.FASTS, 10),
        Badge("f25", Metric.FASTS, 25),
        Badge("f50", Metric.FASTS, 50),
        Badge("f100", Metric.FASTS, 100),
        Badge("f365", Metric.FASTS, 365),
        Badge("t100", Metric.HOURS, 100),
        Badge("t500", Metric.HOURS, 500),
        Badge("t1000", Metric.HOURS, 1000),
        Badge("t2500", Metric.HOURS, 2500),
        Badge("w1", Metric.WEIGH_INS, 1),
        Badge("w30", Metric.WEIGH_INS, 30),
        Badge("l1", Metric.LOST, 1),
        Badge("l5", Metric.LOST, 5),
        Badge("l10", Metric.LOST, 10),
        Badge("water7", Metric.WATER_DAYS, 7),
        Badge("water30", Metric.WATER_DAYS, 30),
        Badge("c10", Metric.CHECKINS, 10),
        Badge("hunger5", Metric.HUNGER, 5),
        Badge("hunger25", Metric.HUNGER, 25),
    )

    fun unlocked(f: Facts): List<Badge> = all.filter { f.value(it.metric) >= it.target }

    fun progress(b: Badge, f: Facts): Float = (f.value(b.metric) / b.target).toFloat().coerceIn(0f, 1f)
}

enum class ChallengeKind { DAYS, HOURS, LONG, WATER, CHECKINS, EARLY }

/** This week's small quest. [param] is the hours a LONG fast needs, or the hour EARLY fasts start before. */
data class Challenge(val kind: ChallengeKind, val target: Int, val progress: Int, val param: Int, val week: String) {
    val done: Boolean get() = progress >= target
}

object Challenges {
    const val EARLY_HOUR = 19

    fun weekKey(monday: LocalDate): String = "%d-W%02d".format(monday.get(IsoFields.WEEK_BASED_YEAR), monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

    fun kindFor(monday: LocalDate): ChallengeKind = ChallengeKind.entries[Math.floorMod(monday.toEpochDay() / 7, ChallengeKind.entries.size.toLong()).toInt()]

    fun of(stats: Stats, plan: Plan, water: List<WaterDay>, waterGoal: Int, checkIns: List<CheckIn>, zone: ZoneId): Challenge {
        val monday = Stats.weekStart(stats.today)
        val sunday = monday.plusDays(6)
        val kind = kindFor(monday)
        fun inWeek(ms: Long) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().let { it >= monday && it <= sunday }
        val dailyHours = plan.fastHours.coerceAtMost(20)
        val longHours = maxOf(18, minOf(plan.fastHours + 2, 24))
        val (target, progress, param) = when (kind) {
            ChallengeKind.DAYS -> Triple(5, stats.days.count { it >= monday && it <= sunday }, 0)
            ChallengeKind.HOURS -> {
                val t = ((dailyHours * 5).coerceIn(50, 100) / 10) * 10
                Triple(t, stats.hoursByDay(monday, sunday).values.sum().toInt(), 0)
            }
            ChallengeKind.LONG -> Triple(1, stats.done.count { inWeek(it.end!!) && it.hours() >= longHours }, longHours)
            ChallengeKind.WATER -> Triple(5, water.count { LocalDate.ofEpochDay(it.day).let { d -> d >= monday && d <= sunday } && it.glasses >= waterGoal }, 0)
            ChallengeKind.CHECKINS -> Triple(3, checkIns.count { inWeek(it.at) }, 0)
            ChallengeKind.EARLY -> Triple(
                3,
                (stats.done + listOfNotNull(stats.active)).count { inWeek(it.start) && Instant.ofEpochMilli(it.start).atZone(zone).hour < EARLY_HOUR && it.hours(stats.now) >= 12 },
                EARLY_HOUR,
            )
        }
        return Challenge(kind, target, progress.coerceAtMost(target), param, weekKey(monday))
    }
}
