package app.ember.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ember.domain.Stages
import app.ember.ui.stageColor
import app.ember.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The fasting clock: a full ring split into stage-coloured arcs up to the goal, filled as the
 * hours pass, with a glowing ember at the current moment. With no [hours] it is an empty ring.
 */
@Composable
fun FastRing(hours: Double?, goalHours: Double, modifier: Modifier = Modifier, stroke: Dp = 18.dp, content: @Composable () -> Unit = {}) {
    val track = U.c.line
    val bg = U.c.bg
    val glow = rememberInfiniteTransition(label = "glow")
    val pulse by glow.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2 + 10.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val r = arc.width / 2
            val center = Offset(size.width / 2, size.height / 2)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(w))
            val stages = Stages.along(goalHours)
            val gap = if (stages.size > 1) 1.6f else 0f
            fun angle(h: Double) = (h / goalHours * 360).toFloat()
            stages.forEachIndexed { i, st ->
                val from = st.hour.toDouble()
                val to = stages.getOrNull(i + 1)?.hour?.toDouble() ?: goalHours
                val color = stageColor(st.index)
                val a0 = angle(from) + gap / 2
                val sweep = angle(to) - angle(from) - gap
                drawArc(color.copy(alpha = 0.18f), -90f + a0, sweep, false, topLeft, arc, style = Stroke(w))
                if (hours != null && hours > from) {
                    val filled = (minOf(hours, to) - from) / (to - from) * sweep
                    drawArc(color, -90f + a0, filled.toFloat(), false, topLeft, arc, style = Stroke(w, cap = if (hours < to) StrokeCap.Round else StrokeCap.Butt))
                }
            }
            // Stage boundaries as small notches.
            stages.drop(1).forEach { st ->
                val a = (angle(st.hour.toDouble()) - 90) * PI / 180
                drawCircle(bg, w * 0.22f, Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat()))
            }
            if (hours != null && hours > 0) {
                val a = (angle(minOf(hours, goalHours)) - 90) * PI / 180
                val p = Offset(center.x + r * cos(a).toFloat(), center.y + r * sin(a).toFloat())
                val color = stageColor(Stages.at(hours).index)
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * pulse), Color.Transparent), p, w * 1.6f), w * 1.6f, p)
                drawCircle(Color.White, w * 0.42f, p)
                drawCircle(color, w * 0.28f, p)
            }
            if (hours != null && hours > goalHours) {
                // Past the goal: a thin outer orbit fills for every extra goal's worth of time.
                val extra = ((hours - goalHours) / goalHours).coerceAtMost(1.0)
                val o = inset - w * 0.95f
                drawArc(
                    stageColor(Stages.at(hours).index).copy(alpha = 0.7f), -90f, (extra * 360).toFloat(), false,
                    Offset(o, o), Size(size.width - o * 2, size.height - o * 2), style = Stroke(w * 0.22f, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Bars of hours per day with a dashed goal line; the last bar is today. */
@Composable
fun DayBars(values: List<Double>, goal: Double, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val accent = U.c.accent
    val soft = U.c.accentSoft
    val gold = U.c.gold
    val line = U.c.faint
    Canvas(modifier) {
        val max = maxOf(goal * 1.25, values.maxOrNull() ?: 0.0, 1.0)
        val n = values.size
        val slot = size.width / n
        val bw = slot * 0.56f
        values.forEachIndexed { i, v ->
            val h = (v / max * size.height).toFloat()
            val x = i * slot + (slot - bw) / 2
            drawRoundRect(soft, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 2))
            if (h > 0) {
                val c = if (v >= goal) (if (highlightLast && i == n - 1) gold else accent) else accent.copy(alpha = 0.5f)
                drawRoundRect(c, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 2))
            }
        }
        val gy = (size.height - goal / max * size.height).toFloat()
        drawLine(line, Offset(0f, gy), Offset(size.width, gy), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
    }
}

/** A smooth line through weights over time, with an optional dashed goal. */
@Composable
fun WeightChart(points: List<Pair<Long, Double>>, goal: Double?, modifier: Modifier = Modifier) {
    val color = U.c.sage
    val faint = U.c.faint
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        val values = points.map { it.second } + listOfNotNull(goal)
        val lo = values.min() - 0.6
        val hi = values.max() + 0.6
        val t0 = points.first().first
        val t1 = maxOf(points.last().first, t0 + 1)
        fun x(t: Long) = ((t - t0).toDouble() / (t1 - t0) * size.width).toFloat()
        fun y(v: Double) = (size.height - (v - lo) / (hi - lo) * size.height).toFloat()
        if (goal != null) {
            drawLine(faint, Offset(0f, y(goal)), Offset(size.width, y(goal)), 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }
        val path = Path()
        val fill = Path()
        points.forEachIndexed { i, (t, v) ->
            val px = if (points.size == 1) size.width / 2 else x(t)
            if (i == 0) {
                path.moveTo(px, y(v)); fill.moveTo(px, size.height); fill.lineTo(px, y(v))
            } else {
                val (pt, pv) = points[i - 1]
                val mx = (x(pt) + px) / 2
                path.cubicTo(mx, y(pv), mx, y(v), px, y(v))
                fill.cubicTo(mx, y(pv), mx, y(v), px, y(v))
            }
        }
        fill.lineTo(if (points.size == 1) size.width / 2 else x(points.last().first), size.height)
        fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent)))
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        val (lt, lv) = points.last()
        val lp = Offset(if (points.size == 1) size.width / 2 else x(lt), y(lv))
        drawCircle(Color.White, 5.dp.toPx(), lp)
        drawCircle(color, 3.5.dp.toPx(), lp)
    }
}

/** A quiet glowing coal; brighter with [heat] 0..1. Used for levels and the streak. */
@Composable
fun Ember(heat: Float, modifier: Modifier = Modifier, color: Color = U.c.accent) {
    val t = rememberInfiniteTransition(label = "ember")
    val flicker by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "flicker")
    val gold = U.c.gold
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height * 0.62f)
        val r = size.minDimension / 2
        val wave = sin(flicker * 2 * PI).toFloat()
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.18f + 0.18f * heat), Color.Transparent), c, r), r, c)
        val h = r * (1.05f + 0.06f * wave)
        val w = r * 0.62f
        val flame = Path().apply {
            moveTo(c.x, c.y - h)
            cubicTo(c.x + w * 0.35f, c.y - h * 0.55f, c.x + w, c.y - h * 0.3f, c.x + w * 0.8f, c.y + r * 0.18f)
            cubicTo(c.x + w * 0.6f, c.y + r * 0.42f, c.x - w * 0.6f, c.y + r * 0.42f, c.x - w * 0.8f, c.y + r * 0.18f)
            cubicTo(c.x - w, c.y - h * 0.25f, c.x - w * 0.2f, c.y - h * 0.45f, c.x, c.y - h)
            close()
        }
        drawPath(flame, Brush.verticalGradient(listOf(gold, color), c.y - h, c.y + r * 0.4f))
        val inner = Path().apply {
            val ih = h * (0.5f - 0.04f * wave)
            val iw = w * 0.42f
            moveTo(c.x, c.y - ih)
            cubicTo(c.x + iw, c.y - ih * 0.3f, c.x + iw, c.y + r * 0.2f, c.x, c.y + r * 0.26f)
            cubicTo(c.x - iw, c.y + r * 0.2f, c.x - iw, c.y - ih * 0.3f, c.x, c.y - ih)
            close()
        }
        drawPath(inner, Color.White.copy(alpha = 0.55f))
    }
}

/** A burst of falling confetti for a reached goal; plays once for [key]. */
@Composable
fun Confetti(key: Any, modifier: Modifier = Modifier) {
    val colors = listOf(U.c.accent, U.c.gold, U.c.sage, U.c.sky, U.c.rose, U.c.violet)
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        List(90) { Piece(rnd.nextFloat(), -rnd.nextFloat() * 0.4f, 0.25f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 0.3f, colors[it % colors.size]) }
    }
    var elapsed by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameMillis { it }
        while (elapsed < 3200) {
            elapsed = withFrameMillis { it } - start
        }
    }
    if (elapsed >= 3200) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        val fade = (1f - (t - 2.2f).coerceAtLeast(0f)).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val y = (p.y + p.speed * t * t * 0.6f + p.speed * t * 0.4f) * size.height
            val x = (p.x + p.drift * t + 0.02f * sin(t * 4 + p.rot)) * size.width
            rotate(p.rot + t * 220f * p.drift * 6, Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - 5f, y - 9f), Size(10f, 18f))
            }
        }
    }
}

private class Piece(val x: Float, val y: Float, val speed: Float, val rot: Float, val drift: Float, val color: Color)
