package app.ember.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.domain.Badge
import app.ember.domain.Badges
import app.ember.domain.Levels
import app.ember.domain.Metric
import app.ember.ui.EmberData
import app.ember.ui.Go
import app.ember.ui.badgeColor
import app.ember.ui.badgeIcon
import app.ember.ui.badgeText
import app.ember.ui.badgeTitle
import app.ember.ui.components.Bar
import app.ember.ui.components.Ember
import app.ember.ui.components.IconDisc
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.UCard
import app.ember.ui.levelTitle
import app.ember.ui.moodEmoji
import app.ember.ui.reasonIcon
import app.ember.ui.reasonLabel
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.whenLabel
import kotlin.math.roundToInt

@Composable
fun MeTab(data: EmberData, go: Go) {
    val s = data.settings
    var open by remember { mutableStateOf<Badge?>(null) }
    val unlocked = data.badges.map { it.id }.toSet()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.name.ifBlank { stringResource(R.string.tab_me) }, style = UType.Title, modifier = Modifier.weight(1f))
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(reasonIcon(s.reason), null, Modifier.size(16.dp), tint = U.c.accent)
            Spacer(Modifier.width(6.dp))
            Text(reasonLabel(s.reason), style = UType.Muted)
        }
        Spacer(Modifier.height(14.dp))

        val xp = data.xp
        val level = data.level
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ember((level / 12f).coerceIn(0.25f, 1f), Modifier.size(72.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.level_n, level).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                    Text(levelTitle(Levels.title(level)), style = UType.Title.copy(fontSize = 24.sp))
                    Text(stringResource(R.string.xp_total, xp), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Bar(Levels.progress(xp), color = U.c.accent)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.xp_next, Levels.need(level + 1) - xp, level + 1), style = UType.Small)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.xp_how), style = UType.Small)
        }

        SectionTitle(stringResource(R.string.badges_n, unlocked.size, Badges.all.size))
        Badges.all.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { b -> BadgeTile(b, b.id in unlocked, Badges.progress(b, data.facts), Modifier.weight(1f)) { open = b } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        val checks = data.checkIns.takeLast(5).reversed()
        if (checks.isNotEmpty()) {
            SectionTitle(stringResource(R.string.check_ins))
            UCard {
                checks.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(whenLabel(c.at), style = UType.Muted)
                            if (c.note.isNotBlank()) Text(c.note, style = UType.Body.copy(fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(
                            listOf("😌", "🙂", "😐", "😋", "😫")[c.hunger - 1] + " " + listOf("🪫", "😴", "😐", "🙂", "⚡")[c.energy - 1] + " " + moodEmoji(c.mood),
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }

    open?.let { b ->
        val (color, soft) = badgeColor(b)
        val got = b.id in unlocked
        AlertDialog(
            onDismissRequest = { open = null },
            containerColor = U.c.bg,
            icon = { IconDisc(if (got) badgeIcon(b) else Icons.Rounded.Lock, 64.dp, if (got) color else U.c.faint, if (got) soft else U.c.surface2) },
            title = { Text(badgeTitle(b), style = UType.Strong.copy(fontSize = 20.sp), textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(badgeText(b), style = UType.Sub, textAlign = TextAlign.Center)
                    if (!got) {
                        Spacer(Modifier.height(14.dp))
                        Bar(Badges.progress(b, data.facts), color = color, track = soft)
                        Spacer(Modifier.height(6.dp))
                        val v = data.facts.value(b.metric)
                        Text(
                            (if (b.metric == Metric.LOST) "%.1f".format(v) else "${v.roundToInt()}") + " / ${b.target}",
                            style = UType.Small,
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text(stringResource(R.string.ok), color = U.c.accent, style = UType.Button) } },
        )
    }
}

@Composable
private fun BadgeTile(b: Badge, unlocked: Boolean, progress: Float, modifier: Modifier, onClick: () -> Unit) {
    val (color, soft) = badgeColor(b)
    Column(
        modifier.clip(U.RMd).background(U.c.surface).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.alpha(if (unlocked) 1f else 0.45f)) {
            IconDisc(if (unlocked) badgeIcon(b) else Icons.Rounded.Lock, 48.dp, if (unlocked) color else U.c.faint, if (unlocked) soft else U.c.surface2)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            badgeTitle(b), style = UType.Small.copy(color = if (unlocked) U.c.ink else U.c.faint, fontSize = 12.sp, lineHeight = 15.sp),
            textAlign = TextAlign.Center, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        // Every tile keeps room for the bar so the grid stays even.
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(44.dp).height(4.dp).clip(CircleShape).background(if (unlocked) Color.Transparent else U.c.line)) {
            if (!unlocked) Box(Modifier.fillMaxWidth(progress).height(4.dp).background(color))
        }
    }
}
