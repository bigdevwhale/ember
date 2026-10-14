package app.ember.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Units { METRIC, IMPERIAL }

/** Why the user fasts; shapes the plan suggested in onboarding and a line on the home screen. */
enum class Reason { WEIGHT, HEALTH, ENERGY, DISCIPLINE }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    /** Used in the greeting; empty is fine. */
    val name: String = "",
    val reason: Reason = Reason.HEALTH,
    /** Plan id from [app.ember.domain.Plans], or "custom". */
    val plan: String = "16_8",
    /** Fasting hours of the custom plan. */
    val customHours: Int = 15,
    /** When the daily fast usually begins, minutes after midnight; -1 when there is no schedule. */
    val startAt: Int = 20 * 60,
    /** A nudge when it is time to begin the fast. */
    val remindStart: Boolean = true,
    /** "Last meal soon" half an hour before the eating window closes. */
    val remindWindow: Boolean = false,
    /** A notification when the goal is reached. */
    val remindGoal: Boolean = true,
    /** A quiet notification at each new stage. */
    val remindStages: Boolean = true,
    /** The ongoing notification with the timer while fasting. */
    val ongoing: Boolean = true,
    val units: Units = Units.METRIC,
    /** 0 when unknown. */
    val heightCm: Int = 0,
    /** 0 when not set. */
    val goalKg: Double = 0.0,
    val waterGoal: Int = 8,
    val haptics: Boolean = true,
    /** Badge ids already celebrated, so each unlock is shown once. */
    val seenBadges: Set<String> = emptySet(),
    /** Hunger waves ridden out with the SOS screen. */
    val hungerWins: Int = 0,
    /** Week key ("2026-W42") whose challenge win was already celebrated. */
    val challengeSeen: String = "",
    /** Levels already celebrated. */
    val levelSeen: Int = 1,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        reason = this[REASON]?.let { r -> runCatching { Reason.valueOf(r) }.getOrNull() } ?: Reason.HEALTH,
        plan = this[PLAN] ?: "16_8",
        customHours = this[CUSTOM_HOURS] ?: 15,
        startAt = this[START_AT] ?: (20 * 60),
        remindStart = this[REMIND_START] ?: true,
        remindWindow = this[REMIND_WINDOW] ?: false,
        remindGoal = this[REMIND_GOAL] ?: true,
        remindStages = this[REMIND_STAGES] ?: true,
        ongoing = this[ONGOING] ?: true,
        units = this[UNITS]?.let { u -> runCatching { Units.valueOf(u) }.getOrNull() } ?: Units.METRIC,
        heightCm = this[HEIGHT] ?: 0,
        goalKg = this[GOAL_KG] ?: 0.0,
        waterGoal = this[WATER_GOAL] ?: 8,
        haptics = this[HAPTICS] ?: true,
        seenBadges = this[SEEN_BADGES] ?: emptySet(),
        hungerWins = this[HUNGER_WINS] ?: 0,
        challengeSeen = this[CHALLENGE_SEEN] ?: "",
        levelSeen = this[LEVEL_SEEN] ?: 1,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[REASON] = s.reason.name
            it[PLAN] = s.plan
            it[CUSTOM_HOURS] = s.customHours
            it[START_AT] = s.startAt
            it[REMIND_START] = s.remindStart
            it[REMIND_WINDOW] = s.remindWindow
            it[REMIND_GOAL] = s.remindGoal
            it[REMIND_STAGES] = s.remindStages
            it[ONGOING] = s.ongoing
            it[UNITS] = s.units.name
            it[HEIGHT] = s.heightCm
            it[GOAL_KG] = s.goalKg
            it[WATER_GOAL] = s.waterGoal
            it[HAPTICS] = s.haptics
            it[SEEN_BADGES] = s.seenBadges
            it[HUNGER_WINS] = s.hungerWins
            it[CHALLENGE_SEEN] = s.challengeSeen
            it[LEVEL_SEEN] = s.levelSeen
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val REASON = stringPreferencesKey("reason")
        val PLAN = stringPreferencesKey("plan")
        val CUSTOM_HOURS = intPreferencesKey("custom_hours")
        val START_AT = intPreferencesKey("start_at")
        val REMIND_START = booleanPreferencesKey("remind_start")
        val REMIND_WINDOW = booleanPreferencesKey("remind_window")
        val REMIND_GOAL = booleanPreferencesKey("remind_goal")
        val REMIND_STAGES = booleanPreferencesKey("remind_stages")
        val ONGOING = booleanPreferencesKey("ongoing")
        val UNITS = stringPreferencesKey("units")
        val HEIGHT = intPreferencesKey("height_cm")
        val GOAL_KG = doublePreferencesKey("goal_kg")
        val WATER_GOAL = intPreferencesKey("water_goal")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SEEN_BADGES = stringSetPreferencesKey("seen_badges")
        val HUNGER_WINS = intPreferencesKey("hunger_wins")
        val CHALLENGE_SEEN = stringPreferencesKey("challenge_seen")
        val LEVEL_SEEN = intPreferencesKey("level_seen")
    }
}
