package app.ember.domain

import app.ember.data.Settings

/** A fasting rhythm: [fastHours] without food, then an eating window of the rest of the day. */
data class Plan(val id: String, val fastHours: Int, val level: Int) {
    /** Daily plans repeat every day; longer fasts are done now and then. */
    val daily: Boolean get() = fastHours < 24
    val eatHours: Int get() = if (daily) 24 - fastHours else 0
}

object Plans {
    const val CUSTOM = "custom"

    val all = listOf(
        Plan("12_12", 12, 1),
        Plan("14_10", 14, 1),
        Plan("16_8", 16, 2),
        Plan("18_6", 18, 3),
        Plan("20_4", 20, 3),
        Plan("omad", 23, 4),
        Plan("24h", 24, 4),
        Plan("36h", 36, 5),
        Plan("48h", 48, 5),
        Plan("72h", 72, 5),
    )

    fun byId(id: String, customHours: Int = 15): Plan =
        all.firstOrNull { it.id == id } ?: Plan(CUSTOM, customHours.coerceIn(MIN_CUSTOM, MAX_CUSTOM), level(customHours))

    fun of(s: Settings): Plan = byId(s.plan, s.customHours)

    const val MIN_CUSTOM = 10
    const val MAX_CUSTOM = 96

    private fun level(hours: Int) = when {
        hours < 15 -> 1
        hours < 17 -> 2
        hours < 21 -> 3
        hours < 30 -> 4
        else -> 5
    }

    /** First plans that fit how experienced someone is: 0 new, 1 tried it, 2 regular. */
    fun suggest(experience: Int): String = when (experience) {
        0 -> "14_10"
        1 -> "16_8"
        else -> "18_6"
    }
}

/** A stretch of a fast that starts [hour] hours in. Texts are in [app.ember.content.Content.stages]. */
data class Stage(val id: String, val hour: Int, val index: Int)

object Stages {
    val all = listOf("digest" to 0, "settle" to 4, "switch" to 8, "burn" to 12, "ketosis" to 16, "autophagy" to 24, "growth" to 48, "renewal" to 72)
        .mapIndexed { i, (id, h) -> Stage(id, h, i) }

    fun at(hours: Double): Stage = all.last { it.hour <= hours }

    fun next(hours: Double): Stage? = all.firstOrNull { it.hour > hours }

    /** Stages a fast with this goal passes through, plus the one right after it. */
    fun along(goalHours: Double): List<Stage> = all.filter { it.hour < goalHours }

    /** Progress through the current stage, 0..1. */
    fun within(hours: Double): Float {
        val cur = at(hours)
        val nxt = next(hours) ?: return 1f
        return ((hours - cur.hour) / (nxt.hour - cur.hour)).toFloat().coerceIn(0f, 1f)
    }
}
