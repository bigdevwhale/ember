package app.ember.domain

import app.ember.data.Fast
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything worth counting in a fasting history. */
class Stats(fasts: List<Fast>, val now: Long, private val zone: ZoneId) {
    val done: List<Fast> = fasts.filter { it.end != null }.sortedBy { it.start }
    val active: Fast? = fasts.lastOrNull { it.end == null }

    val count: Int get() = done.size
    val successes: Int = done.count { it.goalReached }
    val totalHours: Double = done.sumOf { it.hours() }
    val longestHours: Double = done.maxOfOrNull { it.hours() } ?: 0.0

    /** Average of the last 30 fasts. */
    val averageHours: Double = done.takeLast(30).takeIf { it.isNotEmpty() }?.map { it.hours() }?.average() ?: 0.0

    val successRate: Float = if (done.isEmpty()) 0f else successes.toFloat() / done.size

    /** Days a successful fast "covers": the days it ended on and the full days inside it. */
    val days: Set<LocalDate> = buildSet {
        for (f in done) {
            if (!f.goalReached) continue
            val s = day(f.start)
            val e = day(f.end!!)
            var d = if (s < e) s.plusDays(1) else e
            while (d <= e) {
                add(d); d = d.plusDays(1)
            }
        }
    }

    private fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    val today: LocalDate = day(now)

    /** Consecutive days up to today; today not counting yet doesn't break it. */
    val streak: Int = run {
        var d = if (today in days) today else today.minusDays(1)
        var n = 0
        while (d in days) {
            n++; d = d.minusDays(1)
        }
        n
    }

    val bestStreak: Int = run {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        best
    }

    /** Hours fasted on each calendar day (a fast across midnight is split), the active fast up to now. */
    fun hoursByDay(from: LocalDate, to: LocalDate): Map<LocalDate, Double> {
        val out = LinkedHashMap<LocalDate, Double>()
        var d = from
        while (d <= to) {
            out[d] = 0.0; d = d.plusDays(1)
        }
        val startMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        for (f in done + listOfNotNull(active)) {
            val a = maxOf(f.start, startMs)
            val b = minOf(f.end ?: now, endMs)
            if (b <= a) continue
            var cur = a
            while (cur < b) {
                val day = day(cur)
                val next = minOf(b, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
                out[day] = (out[day] ?: 0.0) + (next - cur) / 3_600_000.0
                cur = next
            }
        }
        return out
    }

    /** Monday of the week [date] is in. */
    companion object {
        fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
    }
}
