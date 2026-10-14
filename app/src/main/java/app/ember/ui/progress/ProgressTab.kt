package app.ember.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.data.Fast
import app.ember.domain.Plans
import app.ember.ui.EmberData
import app.ember.ui.Go
import app.ember.ui.components.DayBars
import app.ember.ui.components.Ember
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.Segmented
import app.ember.ui.components.UCard
import app.ember.ui.components.WeightChart
import app.ember.ui.daysLabel
import app.ember.ui.duration
import app.ember.ui.hoursLabel
import app.ember.ui.locale
import app.ember.ui.moodEmoji
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.weightLabel
import app.ember.ui.whenLabel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.roundToInt

@Composable
fun ProgressTab(data: EmberData, go: Go) {
    val st = data.stats
    val goal = Plans.of(data.settings).fastHours.toDouble().coerceAtMost(24.0)
    var range by rememberSaveable { mutableIntStateOf(7) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.tab_progress), style = UType.Title)
        Spacer(Modifier.height(14.dp))

        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ember((st.streak / 14f).coerceIn(0.2f, 1f), Modifier.size(64.dp), color = if (st.streak > 0) U.c.accent else U.c.faint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.streak).uppercase(), style = UType.Eyebrow)
                    Text(daysLabel(st.streak), style = UType.Title.copy(fontSize = 24.sp))
                    Text(stringResource(R.string.best_streak, daysLabel(st.bestStreak)), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(if (st.streak > 0) R.string.streak_keep else R.string.streak_start), style = UType.Muted)
        }

        SectionTitle(stringResource(R.string.hours_fasted))
        UCard {
            Segmented(listOf(7, 30), range, { stringResource(if (it == 7) R.string.week else R.string.month) }, { range = it })
            Spacer(Modifier.height(14.dp))
            val days = remember(st, range) { st.hoursByDay(st.today.minusDays(range - 1L), st.today) }
            DayBars(days.values.toList(), goal, Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.height(6.dp))
            if (range == 7) {
                Row(Modifier.fillMaxWidth()) {
                    days.keys.forEach { d ->
                        Text(
                            d.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, locale()), style = UType.Small,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.range_total, hoursLabel(days.values.sum())), style = UType.Muted)
        }

        SectionTitle(stringResource(R.string.stats))
        val cells = listOf(
            stringResource(R.string.stat_fasts) to "${st.count}",
            stringResource(R.string.stat_total) to hoursLabel(st.totalHours),
            stringResource(R.string.stat_longest) to hoursLabel(st.longestHours),
            stringResource(R.string.stat_average) to hoursLabel(st.averageHours),
            stringResource(R.string.stat_success) to "${(st.successRate * 100).roundToInt()}%",
            stringResource(R.string.stat_hunger) to "${data.settings.hungerWins}",
        )
        cells.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) ->
                    UCard(Modifier.weight(1f)) {
                        Text(value, style = UType.Number)
                        Text(label, style = UType.Muted)
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.calendar))
        MonthCalendar(data)

        SectionTitle(stringResource(R.string.weight))
        WeightSummary(data, go)

        SectionTitle(stringResource(R.string.recent_fasts))
        val recent = st.done.takeLast(5).reversed()
        if (recent.isEmpty()) {
            Text(stringResource(R.string.no_fasts), style = UType.Muted, modifier = Modifier.padding(start = 4.dp))
        }
        recent.forEach { f -> FastRow(f) { go.fast(f.id) } }
        TextButton(onClick = { go.history() }) { Text(stringResource(R.string.all_fasts), color = U.c.accent, style = UType.Button) }
    }
}

@Composable
fun FastRow(f: Fast, onClick: () -> Unit) {
    UCard(Modifier.padding(bottom = 8.dp), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(if (f.goalReached) U.c.accentSoft else U.c.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(20.dp), tint = if (f.goalReached) U.c.accent else U.c.faint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(duration(f.minutes()), style = UType.Strong)
                Text(whenLabel(f.start), style = UType.Muted)
            }
            if (f.mood != null) Text(moodEmoji(f.mood), fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.of_goal, f.goalMinutes / 60), style = UType.Small)
        }
    }
}

@Composable
private fun MonthCalendar(data: EmberData) {
    val st = data.stats
    var month by remember { mutableStateOf(YearMonth.from(st.today)) }
    val hours = remember(st, month) { st.hoursByDay(month.atDay(1), month.atEndOfMonth()) }
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous), tint = U.c.muted) }
            Text(
                DateTimeFormatter.ofPattern("LLLL yyyy", locale()).format(month).replaceFirstChar { it.titlecase(locale()) },
                style = UType.Strong, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(st.today)) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next), tint = U.c.muted)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            java.time.DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.NARROW_STANDALONE, locale()), style = UType.Small, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(6.dp))
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - 1
        val cells = offset + month.lengthOfMonth()
        (0 until (cells + 6) / 7).forEach { w ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { c ->
                    val i = w * 7 + c - offset
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (i in 0 until month.lengthOfMonth()) {
                            val d = first.plusDays(i.toLong())
                            val success = d in st.days
                            val fasted = (hours[d] ?: 0.0) >= 1
                            val today = d == st.today
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(
                                        when {
                                            success -> U.c.accent
                                            fasted -> U.c.accentSoft
                                            else -> Color.Transparent
                                        }
                                    )
                                    .then(if (today) Modifier.border(1.5.dp, U.c.accent, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${d.dayOfMonth}",
                                    style = UType.Muted.copy(fontSize = 13.sp, color = if (success) U.c.onAccent else if (d > st.today) U.c.faint else U.c.ink),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(U.c.accent))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.cal_goal), style = UType.Small)
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(U.c.accentSoft))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.cal_fasted), style = UType.Small)
        }
    }
}

@Composable
private fun WeightSummary(data: EmberData, go: Go) {
    val w = data.weights
    val units = data.settings.units
    UCard(onClick = { go.weight() }) {
        if (w.isEmpty()) {
            Text(stringResource(R.string.weight_empty), style = UType.Strong)
            Text(stringResource(R.string.weight_empty_text), style = UType.Muted)
            return@UCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(weightLabel(w.last().kg, units), style = UType.Number, modifier = Modifier.weight(1f))
            if (w.size > 1) {
                val diff = w.last().kg - w.first().kg
                Text(
                    (if (diff > 0) "+" else if (diff < 0) "−" else "") + weightLabel(kotlin.math.abs(diff), units),
                    style = UType.Strong.copy(color = if (diff <= 0) U.c.sage else U.c.rose, fontSize = 15.sp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        WeightChart(w.takeLast(30).map { it.at to it.kg }, data.settings.goalKg.takeIf { it > 0 }, Modifier.fillMaxWidth().height(110.dp))
        Spacer(Modifier.height(6.dp))
        Text(whenLabel(w.last().at), style = UType.Small)
    }
}
