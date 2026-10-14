package app.ember.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalDining
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Woman
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.ember.R
import app.ember.content.Content
import app.ember.data.Reason
import app.ember.data.Settings
import app.ember.data.Units
import app.ember.domain.Badge
import app.ember.domain.Challenge
import app.ember.domain.ChallengeKind
import app.ember.domain.Metric
import app.ember.domain.Plan
import app.ember.domain.Plans
import app.ember.ui.theme.StageColors
import app.ember.ui.theme.U
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Texts in the current app language; switches along with it. */
@Composable
fun content(): Content {
    val l = locale()
    return remember(l.language) { Content.of(l) }
}

fun stageColor(index: Int): Color = StageColors[index.coerceIn(0, StageColors.size - 1)]

/** "16:8", "OMAD", "36 h". */
@Composable
fun planRatio(p: Plan): String = when {
    p.id == "omad" -> "OMAD"
    !p.daily -> stringResource(R.string.hours_short, p.fastHours)
    else -> "${p.fastHours}:${p.eatHours}"
}

@Composable
fun planName(p: Plan): String = stringResource(
    when (p.id) {
        "12_12" -> R.string.plan_12_12
        "14_10" -> R.string.plan_14_10
        "16_8" -> R.string.plan_16_8
        "18_6" -> R.string.plan_18_6
        "20_4" -> R.string.plan_20_4
        "omad" -> R.string.plan_omad
        "24h" -> R.string.plan_24h
        "36h" -> R.string.plan_36h
        "48h" -> R.string.plan_48h
        "72h" -> R.string.plan_72h
        else -> R.string.plan_custom
    }
)

@Composable
fun planText(p: Plan): String = stringResource(
    when (p.id) {
        "12_12" -> R.string.plan_12_12_text
        "14_10" -> R.string.plan_14_10_text
        "16_8" -> R.string.plan_16_8_text
        "18_6" -> R.string.plan_18_6_text
        "20_4" -> R.string.plan_20_4_text
        "omad" -> R.string.plan_omad_text
        "24h" -> R.string.plan_24h_text
        "36h" -> R.string.plan_36h_text
        "48h" -> R.string.plan_48h_text
        "72h" -> R.string.plan_72h_text
        else -> R.string.plan_custom_text
    }
)

@Composable
fun levelLabel(level: Int): String = stringResource(
    when (level) {
        1 -> R.string.difficulty_1
        2 -> R.string.difficulty_2
        3 -> R.string.difficulty_3
        4 -> R.string.difficulty_4
        else -> R.string.difficulty_5
    }
)

/** "14:05:09" for the running timer. */
fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}

/** "14 h 32 min", "2 d 3 h". */
@Composable
fun duration(minutes: Long): String {
    val m = minutes.coerceAtLeast(0)
    return when {
        m < 60 -> stringResource(R.string.dur_m, m)
        m < 48 * 60 -> if (m % 60 == 0L) stringResource(R.string.dur_h, m / 60) else stringResource(R.string.dur_hm, m / 60, m % 60)
        else -> stringResource(R.string.dur_dh, m / 1440, m / 60 % 24)
    }
}

/** "16.5 h" for averages and totals. */
@Composable
fun hoursLabel(h: Double): String =
    if (h >= 100) stringResource(R.string.hours_short, h.roundToInt())
    else stringResource(R.string.hours_short_f, "%.1f".format(locale(), h).removeSuffix(",0").removeSuffix(".0"))

@Composable
fun daysLabel(n: Int): String = pluralStringResource(R.plurals.days, n, n)

@Composable
fun timeLabel(ms: Long): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale())
        .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
fun clockTime(minutes: Int): String {
    val m = Math.floorMod(minutes, 24 * 60)
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()).format(LocalTime.of(m / 60, m % 60))
}

/** "Today, 20:15", "Yesterday, 08:00", "Mon 12 Oct, 19:30". */
@Composable
fun whenLabel(ms: Long): String {
    val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    return dayLabel(d, short = true) + ", " + timeLabel(ms)
}

@Composable
fun dayLabel(d: LocalDate, short: Boolean = false): String {
    val today = LocalDate.now()
    return when (d) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> DateTimeFormatter.ofPattern(
            when {
                short -> "EEE, d MMM"
                d.year == today.year -> "EEEE, d MMMM"
                else -> "d MMMM yyyy"
            },
            locale(),
        ).format(d).replaceFirstChar { it.titlecase(locale()) }
    }
}

@Composable
fun greeting(hour: Int): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.greet_morning
        in 12..17 -> R.string.greet_day
        in 18..22 -> R.string.greet_evening
        else -> R.string.greet_night
    }
)

@Composable
fun moodLabel(m: Int): String = stringResource(
    when (m) {
        1 -> R.string.mood_1
        2 -> R.string.mood_2
        3 -> R.string.mood_3
        4 -> R.string.mood_4
        else -> R.string.mood_5
    }
)

fun moodEmoji(m: Int): String = when (m) {
    1 -> "😣"
    2 -> "😕"
    3 -> "😐"
    4 -> "🙂"
    else -> "😄"
}

@Composable
fun reasonLabel(r: Reason): String = stringResource(
    when (r) {
        Reason.WEIGHT -> R.string.reason_weight
        Reason.HEALTH -> R.string.reason_health
        Reason.ENERGY -> R.string.reason_energy
        Reason.DISCIPLINE -> R.string.reason_discipline
    }
)

fun reasonIcon(r: Reason): ImageVector = when (r) {
    Reason.WEIGHT -> Icons.Rounded.MonitorWeight
    Reason.HEALTH -> Icons.Rounded.Favorite
    Reason.ENERGY -> Icons.Rounded.Bolt
    Reason.DISCIPLINE -> Icons.Rounded.SelfImprovement
}

/** Weight in the user's units, without the unit. */
fun weightValue(kg: Double, units: Units): String =
    if (units == Units.IMPERIAL) "%.1f".format(kg * 2.20462) else "%.1f".format(kg)

@Composable
fun weightLabel(kg: Double, units: Units): String =
    stringResource(if (units == Units.IMPERIAL) R.string.unit_lb else R.string.unit_kg, weightValue(kg, units))

@Composable
fun unitLabel(units: Units): String = stringResource(if (units == Units.IMPERIAL) R.string.lb else R.string.kg)

fun toKg(value: Double, units: Units): Double = if (units == Units.IMPERIAL) value / 2.20462 else value

@Composable
fun levelTitle(index: Int): String = stringArrayResourceSafe(R.array.level_titles, index)

@Composable
private fun stringArrayResourceSafe(id: Int, index: Int): String {
    val arr = androidx.compose.ui.res.stringArrayResource(id)
    return arr[index.coerceIn(0, arr.size - 1)]
}

@Composable
fun badgeTitle(b: Badge): String = stringResource(
    when (b.id) {
        "first" -> R.string.badge_first
        "h16" -> R.string.badge_h16
        "h18" -> R.string.badge_h18
        "h20" -> R.string.badge_h20
        "h24" -> R.string.badge_h24
        "h36" -> R.string.badge_h36
        "h48" -> R.string.badge_h48
        "h72" -> R.string.badge_h72
        "s3" -> R.string.badge_s3
        "s7" -> R.string.badge_s7
        "s14" -> R.string.badge_s14
        "s30" -> R.string.badge_s30
        "s60" -> R.string.badge_s60
        "s100" -> R.string.badge_s100
        "f10" -> R.string.badge_f10
        "f25" -> R.string.badge_f25
        "f50" -> R.string.badge_f50
        "f100" -> R.string.badge_f100
        "f365" -> R.string.badge_f365
        "t100" -> R.string.badge_t100
        "t500" -> R.string.badge_t500
        "t1000" -> R.string.badge_t1000
        "t2500" -> R.string.badge_t2500
        "w1" -> R.string.badge_w1
        "w30" -> R.string.badge_w30
        "l1" -> R.string.badge_l1
        "l5" -> R.string.badge_l5
        "l10" -> R.string.badge_l10
        "water7" -> R.string.badge_water7
        "water30" -> R.string.badge_water30
        "c10" -> R.string.badge_c10
        "hunger5" -> R.string.badge_hunger5
        else -> R.string.badge_hunger25
    }
)

@Composable
fun badgeText(b: Badge): String = when (b.metric) {
    Metric.LONGEST -> pluralStringResource(R.plurals.badge_longest, b.target, b.target)
    Metric.STREAK -> pluralStringResource(R.plurals.badge_streak, b.target, b.target)
    Metric.FASTS -> if (b.target == 1) stringResource(R.string.badge_first_text) else pluralStringResource(R.plurals.badge_fasts, b.target, b.target)
    Metric.HOURS -> pluralStringResource(R.plurals.badge_hours, b.target, b.target)
    Metric.WEIGH_INS -> if (b.target == 1) stringResource(R.string.badge_weigh_first) else pluralStringResource(R.plurals.badge_weigh, b.target, b.target)
    Metric.LOST -> pluralStringResource(R.plurals.badge_lost, b.target, b.target)
    Metric.WATER_DAYS -> pluralStringResource(R.plurals.badge_water, b.target, b.target)
    Metric.CHECKINS -> pluralStringResource(R.plurals.badge_checkins, b.target, b.target)
    Metric.HUNGER -> pluralStringResource(R.plurals.badge_hunger, b.target, b.target)
}

fun badgeIcon(b: Badge): ImageVector = when (b.metric) {
    Metric.LONGEST -> Icons.Rounded.HourglassBottom
    Metric.STREAK -> Icons.Rounded.LocalFireDepartment
    Metric.FASTS -> Icons.Rounded.EmojiEvents
    Metric.HOURS -> Icons.Rounded.Bolt
    Metric.WEIGH_INS -> Icons.Rounded.MonitorWeight
    Metric.LOST -> Icons.AutoMirrored.Rounded.TrendingDown
    Metric.WATER_DAYS -> Icons.Rounded.WaterDrop
    Metric.CHECKINS -> Icons.Rounded.Spa
    Metric.HUNGER -> Icons.Rounded.NoFood
}

@Composable
fun badgeColor(b: Badge): Pair<Color, Color> = when (b.metric) {
    Metric.LONGEST -> U.c.violet to U.c.violetSoft
    Metric.STREAK -> U.c.accent to U.c.accentSoft
    Metric.FASTS -> U.c.gold to U.c.goldSoft
    Metric.HOURS -> U.c.gold to U.c.goldSoft
    Metric.WEIGH_INS, Metric.LOST -> U.c.sage to U.c.sageSoft
    Metric.WATER_DAYS -> U.c.sky to U.c.skySoft
    Metric.CHECKINS -> U.c.rose to U.c.roseSoft
    Metric.HUNGER -> U.c.rose to U.c.roseSoft
}

@Composable
fun challengeTitle(c: Challenge): String = when (c.kind) {
    ChallengeKind.DAYS -> stringResource(R.string.ch_days, c.target)
    ChallengeKind.HOURS -> stringResource(R.string.ch_hours, c.target)
    ChallengeKind.LONG -> stringResource(R.string.ch_long, c.param)
    ChallengeKind.WATER -> stringResource(R.string.ch_water, c.target)
    ChallengeKind.CHECKINS -> stringResource(R.string.ch_checkins, c.target)
    ChallengeKind.EARLY -> stringResource(R.string.ch_early, c.target, clockTime(c.param * 60))
}

@Composable
fun challengeText(c: Challenge): String = stringResource(
    when (c.kind) {
        ChallengeKind.DAYS -> R.string.ch_days_text
        ChallengeKind.HOURS -> R.string.ch_hours_text
        ChallengeKind.LONG -> R.string.ch_long_text
        ChallengeKind.WATER -> R.string.ch_water_text
        ChallengeKind.CHECKINS -> R.string.ch_checkins_text
        ChallengeKind.EARLY -> R.string.ch_early_text
    }
)

fun articleIcon(id: String): ImageVector = when (id) {
    "basics" -> Icons.AutoMirrored.Rounded.MenuBook
    "choose_plan" -> Icons.Rounded.Tune
    "first_week" -> Icons.Rounded.WbSunny
    "what_breaks" -> Icons.Rounded.NoFood
    "hunger" -> Icons.Rounded.Restaurant
    "electrolytes" -> Icons.Rounded.WaterDrop
    "breaking_fast" -> Icons.Rounded.LocalDining
    "eating_window" -> Icons.Rounded.Restaurant
    "exercise" -> Icons.Rounded.FitnessCenter
    "sleep" -> Icons.Rounded.Bedtime
    "autophagy" -> Icons.Rounded.Recycling
    "ketosis" -> Icons.Rounded.LocalFireDepartment
    "plateaus" -> Icons.AutoMirrored.Rounded.TrendingFlat
    "women" -> Icons.Rounded.Woman
    "safety" -> Icons.Rounded.HealthAndSafety
    "long_fasts" -> Icons.Rounded.HourglassBottom
    "coffee_tea" -> Icons.Rounded.Coffee
    "social" -> Icons.Rounded.Groups
    else -> Icons.AutoMirrored.Rounded.DirectionsRun
}

/** Goal in minutes for a new fast under the current plan. */
fun goalMinutes(s: Settings): Int = Plans.of(s).fastHours * 60
