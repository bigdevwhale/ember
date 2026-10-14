package app.ember.ui.fast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.data.CheckIn
import app.ember.data.Fast
import app.ember.data.WaterDay
import app.ember.domain.Plans
import app.ember.domain.Stages
import app.ember.ui.EmberData
import app.ember.ui.Go
import app.ember.ui.Tab
import app.ember.ui.challengeText
import app.ember.ui.challengeTitle
import app.ember.ui.clock
import app.ember.ui.clockTime
import app.ember.ui.components.Bar
import app.ember.ui.components.Confetti
import app.ember.ui.components.DateTimeDialog
import app.ember.ui.components.FastRing
import app.ember.ui.components.IconDisc
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.ScalePicker
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.SoftButton
import app.ember.ui.components.UCard
import app.ember.ui.components.UTextField
import app.ember.ui.components.rememberNow
import app.ember.ui.content
import app.ember.ui.duration
import app.ember.ui.greeting
import app.ember.ui.moodEmoji
import app.ember.ui.planName
import app.ember.ui.planRatio
import app.ember.ui.stageColor
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.whenLabel
import java.time.LocalTime

@Composable
fun FastTab(data: EmberData, go: Go, onTab: (Tab) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val haptic = LocalHapticFeedback.current
    val s = data.settings
    val plan = Plans.of(s)
    val active = data.active
    val now = rememberNow(1000)
    val text = content()
    var pickStart by remember { mutableStateOf(false) }
    var checkIn by remember { mutableStateOf(false) }

    fun startAt(at: Long) {
        if (s.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        container.write { dao().insertFast(Fast(start = at, goalMinutes = plan.fastHours * 60, plan = plan.id)) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val hello = greeting(LocalTime.now().hour)
                    Text(if (s.name.isBlank()) hello else "$hello, ${s.name}", style = UType.Title.copy(fontSize = 24.sp))
                }
                StreakPill(data.stats.streak) { onTab(Tab.PROGRESS) }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.clip(U.RSm).background(U.c.surface).border(1.dp, U.c.line, U.RSm).clickable { go.plans() }.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shown = active?.let { Plans.byId(it.plan, it.goalMinutes / 60) } ?: plan
                Text(planRatio(shown), style = UType.Strong.copy(color = U.c.accent))
                Spacer(Modifier.width(8.dp))
                Text(planName(shown), style = UType.Muted)
                Icon(Icons.Rounded.ExpandMore, null, tint = U.c.faint)
            }

            Spacer(Modifier.height(8.dp))
            if (active != null) ActiveFast(active, now, data, go, text.note(active.hours(now)))
            else IdleFast(data, now, onStart = { startAt(System.currentTimeMillis()) }, onEarlier = { pickStart = true })

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Rounded.NoFood, stringResource(R.string.hunger_sos), U.c.rose, U.c.roseSoft, Modifier.weight(1f)) { go.hunger() }
                QuickAction(Icons.Rounded.Spa, stringResource(R.string.check_in), U.c.sage, U.c.sageSoft, Modifier.weight(1f)) { checkIn = true }
            }

            WaterCard(data)

            SectionTitle(stringResource(R.string.this_week))
            val ch = data.challenge
            UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.EmojiEvents, 42.dp, U.c.gold, U.c.goldSoft)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(challengeTitle(ch), style = UType.Strong)
                        Text(challengeText(ch), style = UType.Muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Bar(ch.progress.toFloat() / ch.target, Modifier.weight(1f), color = U.c.gold, track = U.c.goldSoft)
                    Spacer(Modifier.width(10.dp))
                    Text(if (ch.done) "✓" else "${ch.progress}/${ch.target}", style = UType.Strong.copy(fontSize = 14.sp, color = U.c.gold))
                }
            }

            SectionTitle(stringResource(R.string.tip_of_day))
            UCard {
                Row {
                    Icon(Icons.Rounded.Lightbulb, null, tint = U.c.gold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(text.tip(data.stats.today.toEpochDay()), style = UType.Quote.copy(fontSize = 17.sp, lineHeight = 25.sp))
                }
            }

            val last = data.stats.done.lastOrNull()
            if (last != null) {
                SectionTitle(stringResource(R.string.last_fast))
                UCard(onClick = { go.fast(last.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(duration(last.minutes()), style = UType.Number)
                            Text(whenLabel(last.start) + " → " + whenLabel(last.end!!), style = UType.Muted)
                        }
                        if (last.mood != null) Text(moodEmoji(last.mood), fontSize = 26.sp)
                        Spacer(Modifier.width(8.dp))
                        if (last.goalReached) IconDisc(Icons.Rounded.LocalFireDepartment, 36.dp, U.c.accent, U.c.accentSoft)
                    }
                }
            }
        }
        // Celebrate the moment the goal is reached while watching.
        if (active != null) {
            val goalAt = active.start + active.goalMinutes * 60_000L
            if (now >= goalAt && now - goalAt < 4000) Confetti(active.id, Modifier.fillMaxSize())
        }
    }

    if (pickStart) {
        DateTimeDialog(System.currentTimeMillis() - 60 * 60_000L, onDismiss = { pickStart = false }) { at ->
            startAt(minOf(at, System.currentTimeMillis()))
        }
    }
    if (checkIn) CheckInDialog(onDismiss = { checkIn = false })
}

@Composable
private fun StreakPill(streak: Int, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (streak > 0) U.c.accentSoft else U.c.surface2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = if (streak > 0) U.c.accent else U.c.faint)
        Spacer(Modifier.width(4.dp))
        Text("$streak", style = UType.Strong.copy(fontSize = 15.sp, color = if (streak > 0) U.c.accent else U.c.faint))
    }
}

@Composable
private fun ColumnScope.ActiveFast(f: Fast, now: Long, data: EmberData, go: Go, note: String) {
    val hours = f.hours(now)
    val goalMs = f.goalMinutes * 60_000L
    val elapsed = now - f.start
    val reached = elapsed >= goalMs
    val stage = Stages.at(hours)
    val text = content()
    var editStart by remember { mutableStateOf(false) }
    val container = LocalContext.current.container

    FastRing(hours, f.goalMinutes / 60.0, Modifier.fillMaxWidth().widthIn(max = 340.dp).aspectRatio(1f).align(Alignment.CenterHorizontally)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (reached) R.string.goal_reached else R.string.fasting).uppercase(), style = UType.Eyebrow.copy(color = if (reached) U.c.gold else U.c.faint))
            Spacer(Modifier.height(4.dp))
            Text(clock(elapsed), style = UType.Timer)
            Spacer(Modifier.height(2.dp))
            Text(
                if (reached) stringResource(R.string.over_goal, clock(elapsed - goalMs)) else stringResource(R.string.left, clock(goalMs - elapsed)),
                style = UType.Muted,
            )
            Spacer(Modifier.height(8.dp))
            Text("${(elapsed * 100 / goalMs.coerceAtLeast(1))}%", style = UType.Strong.copy(color = stageColor(stage.index)))
        }
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        TimeCell(stringResource(R.string.started), whenLabel(f.start), Modifier.weight(1f), onEdit = { editStart = true })
        TimeCell(stringResource(R.string.goal), whenLabel(f.start + goalMs), Modifier.weight(1f), end = true)
    }
    PrimaryButton(
        stringResource(R.string.end_fast), { go.end() }, Modifier.fillMaxWidth(),
        color = if (reached) U.c.accent else U.c.ink.copy(alpha = 0.85f), icon = Icons.Rounded.Stop,
    )

    Spacer(Modifier.height(16.dp))
    val st = text.stage(stage.id)
    val next = Stages.next(hours)
    UCard(onClick = { go.stages(stage.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(stageColor(stage.index)))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.stage_n, stage.index + 1).uppercase(), style = UType.Eyebrow)
        }
        Spacer(Modifier.height(6.dp))
        Text(st.title, style = UType.Title.copy(fontSize = 21.sp))
        Spacer(Modifier.height(2.dp))
        Text(st.short, style = UType.Sub)
        if (next != null) {
            Spacer(Modifier.height(12.dp))
            Bar(Stages.within(hours), color = stageColor(stage.index), track = stageColor(stage.index).copy(alpha = 0.15f))
            Spacer(Modifier.height(6.dp))
            val toNext = (f.start + next.hour * 3_600_000L - now) / 60_000 + 1
            Text(stringResource(R.string.next_stage_in, text.stage(next.id).title, duration(toNext)), style = UType.Muted)
        }
        Spacer(Modifier.height(12.dp))
        StageDots(stage.index)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        note, style = UType.Quote.copy(fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 25.sp, color = U.c.muted, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    )

    if (editStart) {
        DateTimeDialog(f.start, onDismiss = { editStart = false }) { at ->
            container.write { dao().updateFast(f.copy(start = minOf(at, System.currentTimeMillis()))) }
        }
    }
}

@Composable
private fun StageDots(current: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Stages.all.forEach { st ->
            Box(
                Modifier.weight(1f).height(6.dp).clip(CircleShape)
                    .background(if (st.index <= current) stageColor(st.index) else stageColor(st.index).copy(alpha = 0.18f)),
            )
        }
    }
}

@Composable
private fun ColumnScope.IdleFast(data: EmberData, now: Long, onStart: () -> Unit, onEarlier: () -> Unit) {
    val s = data.settings
    val plan = Plans.of(s)
    val last = data.stats.done.lastOrNull()
    FastRing(null, plan.fastHours.toDouble(), Modifier.fillMaxWidth().widthIn(max = 340.dp).aspectRatio(1f).align(Alignment.CenterHorizontally)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            if (last != null) {
                Text(stringResource(R.string.eating_for).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(4.dp))
                Text(clock(now - last.end!!), style = UType.Timer)
            } else {
                Text(stringResource(R.string.ready).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(6.dp))
                Text(planRatio(plan), style = UType.Timer)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.startAt >= 0 && plan.daily) stringResource(R.string.next_fast_at, clockTime(s.startAt)) else stringResource(R.string.goal_hours, plan.fastHours),
                style = UType.Muted, textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    PrimaryButton(stringResource(R.string.start_fast), onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow)
    TextButton(onClick = onEarlier, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(R.string.started_earlier), color = U.c.accent, style = UType.Button)
    }
}

@Composable
private fun TimeCell(label: String, value: String, modifier: Modifier, end: Boolean = false, onEdit: (() -> Unit)? = null) {
    Column(
        modifier.clip(U.RSm).then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier).padding(8.dp),
        horizontalAlignment = if (end) Alignment.End else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = UType.Eyebrow)
            if (onEdit != null) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Edit, null, Modifier.size(13.dp), tint = U.c.accent)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(value, style = UType.Strong.copy(fontSize = 15.sp))
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, color: Color, soft: Color, modifier: Modifier, onClick: () -> Unit) {
    SoftButton(label, onClick, modifier, color = color, soft = soft, icon = icon)
}

@Composable
private fun WaterCard(data: EmberData) {
    val container = LocalContext.current.container
    val goal = data.settings.waterGoal
    val glasses = data.glassesToday()
    fun set(n: Int) = container.write { setWater(n.coerceIn(0, 30)) }
    SectionTitle(stringResource(R.string.water))
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.water_n, glasses, goal), style = UType.Strong)
                Text(stringResource(if (glasses >= goal) R.string.water_done else R.string.water_text), style = UType.Muted)
            }
            IconButton(onClick = { set(glasses - 1) }, enabled = glasses > 0) { Icon(Icons.Rounded.Remove, stringResource(R.string.less), tint = U.c.muted) }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(U.c.sky).clickable { set(glasses + 1) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Add, stringResource(R.string.add_glass), tint = Color.White) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(maxOf(goal, glasses)) { i ->
                Icon(
                    Icons.Rounded.WaterDrop, null,
                    Modifier.weight(1f).height(22.dp).clickable { set(if (i < glasses) i else i + 1) },
                    tint = if (i < glasses) U.c.sky else U.c.skySoft,
                )
            }
        }
    }
}

private suspend fun app.ember.data.EmberDatabase.setWater(n: Int) {
    val day = java.time.LocalDate.now().toEpochDay()
    dao().setWater(WaterDay(day, n))
}

@Composable
fun CheckInDialog(onDismiss: () -> Unit) {
    val container = LocalContext.current.container
    var hunger by remember { mutableIntStateOf(2) }
    var energy by remember { mutableIntStateOf(4) }
    var mood by remember { mutableIntStateOf(4) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.bg,
        title = { Text(stringResource(R.string.check_in_title), style = UType.Strong.copy(fontSize = 19.sp)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.ci_hunger), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                ScalePicker(hunger, { hunger = it }, { listOf("😌", "🙂", "😐", "😋", "😫")[it - 1] }, color = U.c.rose)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.ci_energy), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                ScalePicker(energy, { energy = it }, { listOf("🪫", "😴", "😐", "🙂", "⚡")[it - 1] }, color = U.c.gold)
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.ci_mood), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                ScalePicker(mood, { mood = it }, { moodEmoji(it) }, color = U.c.sage)
                Spacer(Modifier.height(14.dp))
                UTextField(note, { note = it.take(300) }, placeholder = stringResource(R.string.ci_note))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                container.write { dao().insertCheckIn(CheckIn(at = System.currentTimeMillis(), hunger = hunger, energy = energy, mood = mood, note = note.trim())) }
                onDismiss()
            }) { Text(stringResource(R.string.save), color = U.c.accent, style = UType.Button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}
