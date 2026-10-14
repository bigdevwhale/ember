package app.ember.ui.fast

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.ui.EmberData
import app.ember.ui.clock
import app.ember.ui.components.Confetti
import app.ember.ui.components.IconDisc
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.components.UCard
import app.ember.ui.components.rememberNow
import app.ember.ui.content
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import kotlinx.coroutines.launch

private const val WAVE_MS = 15 * 60_000L

/** Hunger rises and falls like a wave: breathe with the orb for 15 minutes and let it pass. */
@Composable
fun HungerScreen(data: EmberData, onClose: () -> Unit) {
    val container = LocalContext.current.container
    var started by rememberSaveable { mutableLongStateOf(0L) }
    var won by rememberSaveable { mutableStateOf(false) }
    val now = rememberNow(1000)
    val running = started > 0 && !won
    val left = if (running) (started + WAVE_MS - now).coerceAtLeast(0) else WAVE_MS
    LaunchedEffect(running, left == 0L) {
        if (running && left == 0L) {
            won = true
            container.scope.launch { container.settings.update { it.copy(hungerWins = it.hungerWins + 1) } }
        }
    }
    val text = content()

    Box(Modifier.fillMaxSize().background(U.c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(stringResource(R.string.hunger_sos), onClose)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
                Text(stringResource(R.string.hunger_title), style = UType.Display.copy(fontSize = 26.sp), modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.hunger_text), style = UType.Sub, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(16.dp))
                UCard {
                    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                        BreathOrb(running, 1f - left.toFloat() / WAVE_MS)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (won) "✓" else clock(left).removePrefix("00:"), style = UType.Timer.copy(color = U.c.ink))
                            if (running) Text(stringResource(R.string.breathe_with), style = UType.Muted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        won -> Text(stringResource(R.string.wave_passed), style = UType.Strong.copy(color = U.c.sage, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
                        running -> TextButton(onClick = { started = 0 }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.stop), color = U.c.muted, style = UType.Button)
                        }
                        else -> PrimaryButton(stringResource(R.string.ride_wave), { started = System.currentTimeMillis() }, Modifier.fillMaxWidth(), color = U.c.rose, icon = Icons.Rounded.Waves)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.hunger_wins, data.settings.hungerWins),
                        style = UType.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
                SectionTitle(stringResource(R.string.hunger_helps))
                text.hunger.forEachIndexed { i, t ->
                    UCard(Modifier.padding(bottom = 10.dp)) {
                        Row {
                            IconDisc(Icons.Rounded.Waves, 34.dp, U.c.rose, U.c.roseSoft)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("${i + 1}. ${t.title}", style = UType.Strong)
                                Spacer(Modifier.height(2.dp))
                                Text(t.text, style = UType.Muted)
                            }
                        }
                    }
                }
            }
        }
        if (won) Confetti(started, Modifier.fillMaxSize())
    }
}

/** In for four, out for six; the outer ring shows how much of the wave has passed. */
@Composable
private fun BreathOrb(running: Boolean, done: Float) {
    val t = rememberInfiniteTransition(label = "breath")
    val scale by t.animateFloat(
        0.62f, 0.62f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 10_000
                0.62f at 0 using FastOutSlowInEasing
                1f at 4_000 using FastOutSlowInEasing
                0.62f at 10_000
            },
        ),
        label = "scale",
    )
    val rose = U.c.rose
    val track = U.c.roseSoft
    Canvas(Modifier.size(220.dp)) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        val s = if (running) scale else 0.8f
        drawCircle(Brush.radialGradient(listOf(rose.copy(alpha = 0.32f), rose.copy(alpha = 0.06f), Color.Transparent), c, r * s), r * s, c)
        val w = 6.dp.toPx()
        drawCircle(track, r - w, c, style = Stroke(w))
        if (done > 0) drawArc(rose, -90f, 360f * done, false, Offset(w, w), androidx.compose.ui.geometry.Size(size.width - 2 * w, size.height - 2 * w), style = Stroke(w, cap = StrokeCap.Round))
    }
}
