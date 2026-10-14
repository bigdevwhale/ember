package app.ember.ui.fast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.domain.Stages
import app.ember.ui.EmberData
import app.ember.ui.components.ConfirmDialog
import app.ember.ui.components.Confetti
import app.ember.ui.components.DateTimeDialog
import app.ember.ui.components.Ember
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.ScalePicker
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.components.UCard
import app.ember.ui.components.UTextField
import app.ember.ui.content
import app.ember.ui.duration
import app.ember.ui.moodEmoji
import app.ember.ui.moodLabel
import app.ember.ui.stageColor
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.whenLabel

/** Closing a fast: what it achieved, how it felt. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EndFastScreen(data: EmberData, onDone: () -> Unit) {
    val container = LocalContext.current.container
    val f = remember { data.active }
    LaunchedEffect(f) { if (f == null) onDone() }
    if (f == null) return
    var end by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var mood by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var pickEnd by remember { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    val text = content()
    val minutes = (end - f.start) / 60_000
    val reached = minutes >= f.goalMinutes
    val hours = minutes / 60.0

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(stringResource(R.string.end_fast), onDone)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Ember(if (reached) 1f else 0.4f, Modifier.size(110.dp), color = if (reached) U.c.accent else U.c.faint)
                    Spacer(Modifier.height(4.dp))
                    Text(duration(minutes), style = UType.Display)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (reached) text.cheer(f.start)
                        else stringResource(R.string.end_short, duration(f.goalMinutes.toLong())),
                        style = UType.Sub.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                SectionTitle(stringResource(R.string.stages_reached))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stages.all.filter { it.hour <= hours }.forEach { st ->
                        Row(
                            Modifier.clip(U.RSm).background(stageColor(st.index).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(stageColor(st.index)))
                            Spacer(Modifier.width(6.dp))
                            Text(text.stage(st.id).title, style = UType.Muted.copy(color = U.c.ink))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                UCard(onClick = { pickEnd = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.started) + ": " + whenLabel(f.start), style = UType.Muted)
                            Spacer(Modifier.height(2.dp))
                            Text(stringResource(R.string.ended) + ": " + whenLabel(end), style = UType.Strong)
                        }
                        Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = U.c.accent)
                    }
                }
                SectionTitle(stringResource(R.string.how_was_it))
                ScalePicker(mood, { mood = it }, { moodEmoji(it) })
                if (mood > 0) Text(moodLabel(mood), style = UType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
                Spacer(Modifier.height(12.dp))
                UTextField(note, { note = it.take(500) }, placeholder = stringResource(R.string.note_hint), minLines = 2)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { discard = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.discard_fast), color = U.c.danger, style = UType.Button.copy(fontSize = 14.sp))
                }
                Spacer(Modifier.height(16.dp))
            }
            Column(Modifier.padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp)) {
                PrimaryButton(stringResource(R.string.save_fast), {
                    val ended = f.copy(end = end, mood = mood.takeIf { it > 0 }, note = note.trim())
                    container.write { dao().updateFast(ended) }
                    onDone()
                }, Modifier.fillMaxWidth())
                TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.keep_fasting), color = U.c.muted, style = UType.Button)
                }
            }
        }
        if (reached) Confetti(f.id, Modifier.fillMaxSize())
    }

    if (pickEnd) {
        DateTimeDialog(end, onDismiss = { pickEnd = false }) { at -> end = at.coerceIn(f.start + 60_000, System.currentTimeMillis()) }
    }
    if (discard) {
        ConfirmDialog(
            stringResource(R.string.discard_fast), stringResource(R.string.discard_fast_text), stringResource(R.string.delete),
            onConfirm = {
                container.write { dao().deleteFast(f) }
                onDone()
            },
            onDismiss = { discard = false }, danger = true,
        )
    }
}
