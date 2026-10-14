package app.ember.data

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Made-up fasting history for README screenshots; only takes effect with `-Pdemo` (app.ember.demo). */
object DemoSeed {
    private const val HOUR = 3_600_000L
    private const val MIN = 60_000L

    suspend fun seedIfEmpty(context: Context, db: EmberDatabase, settings: SettingsStore) {
        val dao = db.dao()
        if (dao.fasts().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        settings.update {
            it.copy(
                onboarded = true, name = if (ru) "Аня" else "Anna", plan = "16_8", startAt = 20 * 60, reason = Reason.WEIGHT,
                heightCm = 168, goalKg = 64.0, hungerWins = 7,
                // Everything already earned counts as celebrated, so screenshots open without a dialog.
                seenBadges = app.ember.domain.Badges.all.map { b -> b.id }.toSet(),
                levelSeen = 99, challengeSeen = "",
            )
        }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        // Seven weeks of mostly 16:8, a few longer fasts, two short ones, and a current streak of 19.
        for (d in 49 downTo 2) {
            if (d == 20 || d == 33) continue
            val evening = today.minusDays(d.toLong()).atTime(19, 30 + (d * 7) % 60 / 2).atZone(zone).toInstant().toEpochMilli() - (if (d == 6 || d == 27) 90 * MIN else 0)
            val (goal, length) = when {
                d == 6 -> 24 to 24 * 60 + 25
                d == 27 -> 24 to 24 * 60 + 40
                d % 9 == 0 -> 18 to 18 * 60 + 20
                d == 41 || d == 45 -> 16 to 13 * 60 + 15
                else -> 16 to 16 * 60 + (d * 37) % 90
            }
            val mood = listOf(4, 5, 4, 3, 4, 5, 4)[d % 7]
            val note = if (d == 6) (if (ru) "Первые сутки! Голод накатывал волнами, вечером было легко." else "First full day! Hunger came in waves, the evening felt easy.") else ""
            dao.insertFast(Fast(start = evening, end = evening + length * MIN, goalMinutes = goal * 60, plan = if (goal == 24) "24h" else if (goal == 18) "18_6" else "16_8", mood = mood, note = note))
            dao.setWater(WaterDay(today.minusDays(d.toLong()).toEpochDay(), if (d % 4 == 0) 6 else 8 + d % 3))
        }
        // The fast in progress: 14 h 32 min into a 16:8, deep in the fat-burning stage.
        dao.insertFast(Fast(start = now - 14 * HOUR - 32 * MIN, goalMinutes = 16 * 60, plan = "16_8"))
        dao.setWater(WaterDay(today.toEpochDay(), 5))

        val weights = listOf(72.4, 72.1, 71.9, 71.6, 71.8, 71.2, 70.9, 70.6, 70.7, 70.2, 69.9, 69.6, 69.4, 69.5, 69.0)
        weights.forEachIndexed { i, kg ->
            dao.insertWeight(WeightEntry(at = today.minusDays((weights.size - 1 - i) * 3L + 1).atTime(8, 10).atZone(zone).toInstant().toEpochMilli(), kg = kg))
        }
        listOf(Triple(3, 4, 4), Triple(2, 4, 5), Triple(4, 3, 3), Triple(2, 5, 5), Triple(3, 4, 4), Triple(2, 4, 4), Triple(1, 5, 5), Triple(2, 4, 4), Triple(3, 4, 5), Triple(2, 5, 4), Triple(2, 4, 5)).forEachIndexed { i, (h, e, m) ->
            dao.insertCheckIn(CheckIn(at = now - i * 2 * 24 * HOUR - 3 * HOUR, hunger = h, energy = e, mood = m))
        }
    }
}
