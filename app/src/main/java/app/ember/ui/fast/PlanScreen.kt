package app.ember.ui.fast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.data.Fast
import app.ember.data.Settings
import app.ember.domain.Plan
import app.ember.domain.Plans
import app.ember.ui.clockTime
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TimeDialog
import app.ember.ui.components.TopBar
import app.ember.ui.components.UCard
import app.ember.ui.levelLabel
import app.ember.ui.planName
import app.ember.ui.planRatio
import app.ember.ui.planText
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import kotlinx.coroutines.launch

/** Choose the rhythm and when the daily fast begins. Changing the plan also moves the current fast's goal. */
@Composable
fun PlanScreen(settings: Settings, active: Fast?, onBack: () -> Unit) {
    val container = LocalContext.current.container
    var pickTime by remember { mutableStateOf(false) }
    fun choose(id: String, hours: Int = settings.customHours) {
        container.scope.launch {
            container.settings.update { it.copy(plan = id, customHours = hours) }
            val p = Plans.byId(id, hours)
            active?.let { container.db.dao().updateFast(it.copy(goalMinutes = p.fastHours * 60, plan = p.id)) }
            container.changed()
        }
    }
    val current = Plans.of(settings)

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.plans), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (active != null) Text(stringResource(R.string.plan_applies_now), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            SectionTitle(stringResource(R.string.plans_daily))
            Plans.all.filter { it.daily }.forEach { p -> PlanCard(p, current.id == p.id) { choose(p.id) } }
            SectionTitle(stringResource(R.string.plans_long))
            Row(
                Modifier.fillMaxWidth().clip(U.RSm).background(U.c.roseSoft).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WarningAmber, null, tint = U.c.rose, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.plans_long_warning), style = UType.Muted.copy(color = U.c.ink))
            }
            Spacer(Modifier.height(10.dp))
            Plans.all.filter { !it.daily }.forEach { p -> PlanCard(p, current.id == p.id) { choose(p.id) } }

            SectionTitle(stringResource(R.string.plan_custom))
            val custom = Plans.byId(Plans.CUSTOM, settings.customHours)
            UCard(onClick = { choose(Plans.CUSTOM) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.hours_short, custom.fastHours), style = UType.Title.copy(fontSize = 22.sp, color = if (current.id == Plans.CUSTOM) U.c.accent else U.c.ink))
                        Text(planText(custom), style = UType.Muted)
                    }
                    IconButton(onClick = { choose(Plans.CUSTOM, (settings.customHours - 1).coerceAtLeast(Plans.MIN_CUSTOM)) }) {
                        Icon(Icons.Rounded.Remove, stringResource(R.string.less), tint = U.c.accent)
                    }
                    IconButton(onClick = { choose(Plans.CUSTOM, (settings.customHours + 1).coerceAtMost(Plans.MAX_CUSTOM)) }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.more), tint = U.c.accent)
                    }
                }
            }

            SectionTitle(stringResource(R.string.schedule))
            UCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = U.c.accent)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clickable(enabled = settings.startAt >= 0) { pickTime = true }) {
                        Text(stringResource(R.string.schedule_start), style = UType.Strong)
                        Text(
                            if (settings.startAt >= 0) clockTime(settings.startAt) else stringResource(R.string.off),
                            style = UType.Muted.copy(color = if (settings.startAt >= 0) U.c.accent else U.c.muted),
                        )
                    }
                    Switch(
                        checked = settings.startAt >= 0,
                        onCheckedChange = { on -> container.scope.launch { container.settings.update { it.copy(startAt = if (on) 20 * 60 else -1) }; container.changed() } },
                        colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent),
                    )
                }
                if (settings.startAt >= 0 && current.daily) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = U.c.line)
                    val eatFrom = settings.startAt + current.fastHours * 60
                    Text(stringResource(R.string.schedule_window, clockTime(eatFrom), clockTime(settings.startAt)), style = UType.Sub)
                }
            }
        }
    }

    if (pickTime) {
        TimeDialog(settings.startAt.coerceAtLeast(0), onDismiss = { pickTime = false }) { m ->
            container.scope.launch { container.settings.update { it.copy(startAt = m) }; container.changed() }
        }
    }
}

@Composable
private fun PlanCard(p: Plan, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(U.RMd)
            .background(if (selected) U.c.accentSoft else U.c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) U.c.accent else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(planRatio(p), style = UType.Title.copy(fontSize = 22.sp, color = if (selected) U.c.accent else U.c.ink))
            Spacer(Modifier.width(10.dp))
            Text(planName(p), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.accent)
        }
        Spacer(Modifier.height(4.dp))
        Text(planText(p), style = UType.Muted)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                (1..5).forEach { i -> Box(Modifier.size(width = 14.dp, height = 5.dp).clip(CircleShape).background(if (i <= p.level) U.c.accent else U.c.line)) }
            }
            Spacer(Modifier.width(8.dp))
            Text(levelLabel(p.level), style = UType.Small)
        }
    }
}
