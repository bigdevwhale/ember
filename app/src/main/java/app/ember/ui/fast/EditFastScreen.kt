package app.ember.ui.fast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ember.R
import app.ember.container
import app.ember.data.Fast
import app.ember.data.Settings
import app.ember.ui.components.Chip
import app.ember.ui.components.ConfirmDialog
import app.ember.ui.components.DateTimeDialog
import app.ember.ui.components.InfoRow
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.ScalePicker
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.components.UCard
import app.ember.ui.components.UTextField
import app.ember.ui.duration
import app.ember.ui.moodEmoji
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.whenLabel

/** Fix the times of a past fast, its goal, mood and note, or delete it. [id] 0 adds a forgotten fast. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditFastScreen(id: Long, settings: Settings, onDone: () -> Unit) {
    val container = LocalContext.current.container
    var fast by remember { mutableStateOf<Fast?>(null) }
    LaunchedEffect(id) {
        fast = if (id == 0L) {
            val end = System.currentTimeMillis() - 60 * 60_000L
            val p = app.ember.domain.Plans.of(settings)
            Fast(start = end - p.fastHours * 3_600_000L, end = end, goalMinutes = p.fastHours * 60, plan = p.id)
        } else container.db.dao().fast(id)
    }
    val f = fast ?: return
    var start by remember(f) { mutableStateOf(f.start) }
    var end by remember(f) { mutableStateOf(f.end ?: System.currentTimeMillis()) }
    var goal by remember(f) { mutableIntStateOf(f.goalMinutes / 60) }
    var mood by remember(f) { mutableIntStateOf(f.mood ?: 0) }
    var note by remember(f) { mutableStateOf(f.note) }
    var picking by remember { mutableIntStateOf(0) }
    var delete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(if (id == 0L) R.string.add_fast else R.string.edit_fast), onDone) {
            if (id != 0L) IconButton(onClick = { delete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.danger) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(duration((end - start) / 60_000), style = UType.Display, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            Spacer(Modifier.height(12.dp))
            UCard {
                InfoRow(Icons.Rounded.PlayArrow, stringResource(R.string.started), whenLabel(start), onClick = { picking = 1 })
                HorizontalDivider(color = U.c.line)
                InfoRow(Icons.Rounded.Stop, stringResource(R.string.ended), whenLabel(end), U.c.sage, U.c.sageSoft, onClick = { picking = 2 })
            }
            SectionTitle(stringResource(R.string.goal))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (listOf(12, 13, 14, 15, 16, 18, 20, 23, 24, 36, 48, 72) + goal).distinct().sorted().forEach { h ->
                    Chip(stringResource(R.string.hours_short, h), goal == h, { goal = h })
                }
            }
            SectionTitle(stringResource(R.string.how_was_it))
            ScalePicker(mood, { mood = it }, { moodEmoji(it) })
            Spacer(Modifier.height(12.dp))
            UTextField(note, { note = it.take(500) }, placeholder = stringResource(R.string.note_hint), minLines = 2)
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.save), {
                val plan = app.ember.domain.Plans.all.firstOrNull { it.fastHours == goal }?.id ?: app.ember.domain.Plans.CUSTOM
                val saved = f.copy(start = start, end = end, goalMinutes = goal * 60, plan = plan, mood = mood.takeIf { it > 0 }, note = note.trim())
                container.write { if (id == 0L) dao().insertFast(saved) else dao().updateFast(saved) }
                onDone()
            },
            Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
            enabled = end > start, icon = Icons.Rounded.Flag,
        )
    }

    when (picking) {
        1 -> DateTimeDialog(start, onDismiss = { picking = 0 }) { at -> start = minOf(at, end - 60_000) }
        2 -> DateTimeDialog(end, onDismiss = { picking = 0 }) { at -> end = at.coerceIn(start + 60_000, System.currentTimeMillis()) }
    }
    if (delete) {
        ConfirmDialog(
            stringResource(R.string.delete_fast), stringResource(R.string.delete_fast_text), stringResource(R.string.delete),
            onConfirm = {
                container.write { dao().deleteFast(f) }
                onDone()
            },
            onDismiss = { delete = false }, danger = true,
        )
    }
}
