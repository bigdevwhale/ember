package app.ember.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.data.Reason
import app.ember.domain.Plans
import app.ember.ui.clockTime
import app.ember.ui.components.Ember
import app.ember.ui.components.IconDisc
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.TimeDialog
import app.ember.ui.components.UTextField
import app.ember.ui.planName
import app.ember.ui.planRatio
import app.ember.ui.planText
import app.ember.ui.reasonIcon
import app.ember.ui.reasonLabel
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import kotlinx.coroutines.launch

private const val PAGES = 5

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val container = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var reason by rememberSaveable { mutableStateOf(Reason.HEALTH) }
    var experience by rememberSaveable { mutableIntStateOf(-1) }
    var plan by rememberSaveable { mutableStateOf("16_8") }
    var startAt by rememberSaveable { mutableIntStateOf(20 * 60) }
    var name by rememberSaveable { mutableStateOf("") }
    var pickTime by rememberSaveable { mutableStateOf(false) }

    fun finish() {
        container.scope.launch {
            container.settings.update { it.copy(onboarded = true, reason = reason, plan = plan, startAt = startAt, name = name.trim()) }
            container.changed()
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(PAGES) { i ->
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= page) U.c.accent else U.c.line))
            }
        }
        AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { p ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (p) {
                    0 -> Welcome()
                    1 -> Safety()
                    2 -> {
                        Title(stringResource(R.string.ob_reason_title), stringResource(R.string.ob_reason_text))
                        Reason.entries.forEach { r ->
                            Option(reasonIcon(r), reasonLabel(r), null, reason == r) { reason = r }
                        }
                    }
                    3 -> {
                        Title(stringResource(R.string.ob_exp_title), stringResource(R.string.ob_exp_text))
                        listOf(R.string.ob_exp_new, R.string.ob_exp_some, R.string.ob_exp_regular).forEachIndexed { i, label ->
                            Option(null, stringResource(label), null, experience == i) {
                                experience = i
                                plan = Plans.suggest(i)
                            }
                        }
                        if (experience >= 0) {
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.ob_plan_pick), style = UType.Eyebrow, modifier = Modifier.padding(bottom = 8.dp))
                            Plans.all.filter { it.daily }.forEach { pl ->
                                Option(
                                    null, planRatio(pl) + " · " + planName(pl),
                                    if (pl.id == Plans.suggest(experience)) stringResource(R.string.ob_suggested) + " · " + planText(pl) else planText(pl),
                                    plan == pl.id,
                                ) { plan = pl.id }
                            }
                        }
                    }
                    else -> {
                        val pl = Plans.byId(plan)
                        Title(stringResource(R.string.ob_time_title), stringResource(R.string.ob_time_text))
                        Box(
                            Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable { pickTime = true }.padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.ob_fast_starts), style = UType.Muted)
                                Text(clockTime(startAt), style = UType.Timer.copy(color = U.c.accent))
                                Text(stringResource(R.string.schedule_window, clockTime(startAt + pl.fastHours * 60), clockTime(startAt)), style = UType.Muted)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.ob_name), style = UType.Strong)
                        Spacer(Modifier.height(8.dp))
                        UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.ob_name_hint))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            PrimaryButton(
                stringResource(
                    when (page) {
                        0 -> R.string.ob_begin
                        1 -> R.string.ob_understand
                        PAGES - 1 -> R.string.ob_finish
                        else -> R.string.next
                    }
                ),
                { if (page < PAGES - 1) page++ else finish() },
                Modifier.fillMaxWidth(),
                enabled = page != 3 || experience >= 0,
            )
            if (page > 0) {
                TextButton(onClick = { page-- }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.back), color = U.c.muted, style = UType.Button)
                }
            }
        }
    }

    if (pickTime) TimeDialog(startAt, onDismiss = { pickTime = false }) { startAt = it }
}

@Composable
private fun ColumnScope.Welcome() {
    Spacer(Modifier.height(24.dp))
    Ember(1f, Modifier.size(120.dp).align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.app_name), style = UType.Display.copy(fontSize = 40.sp), modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(6.dp))
    Text(stringResource(R.string.ob_tagline), style = UType.Sub.copy(fontSize = 16.sp), modifier = Modifier.align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(28.dp))
    Feature(Icons.Rounded.Timer, stringResource(R.string.ob_f1_title), stringResource(R.string.ob_f1_text))
    Feature(Icons.Rounded.EmojiEvents, stringResource(R.string.ob_f2_title), stringResource(R.string.ob_f2_text))
    Feature(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.ob_f3_title), stringResource(R.string.ob_f3_text))
    Feature(Icons.Rounded.Lock, stringResource(R.string.ob_f4_title), stringResource(R.string.ob_f4_text))
}

@Composable
private fun Safety() {
    Spacer(Modifier.height(12.dp))
    IconDisc(Icons.Rounded.HealthAndSafety, 64.dp, U.c.rose, U.c.roseSoft)
    Spacer(Modifier.height(16.dp))
    Title(stringResource(R.string.ob_safety_title), stringResource(R.string.ob_safety_text))
    listOf(R.string.ob_safety_1, R.string.ob_safety_2, R.string.ob_safety_3, R.string.ob_safety_4, R.string.ob_safety_5).forEach {
        Row(Modifier.padding(vertical = 5.dp)) {
            Text("•", style = UType.Body.copy(color = U.c.rose))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(it), style = UType.Body)
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.ob_safety_stop), style = UType.Muted)
}

@Composable
private fun Title(title: String, text: String) {
    Text(title, style = UType.Title)
    Spacer(Modifier.height(6.dp))
    Text(text, style = UType.Sub)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconDisc(icon, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = UType.Strong)
            Text(text, style = UType.Muted)
        }
    }
}

@Composable
private fun Option(icon: ImageVector?, title: String, text: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(U.RMd)
            .background(if (selected) U.c.accentSoft else U.c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) U.c.accent else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) U.c.accent else U.c.muted)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            if (text != null) Text(text, style = UType.Muted)
        }
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.accent)
    }
}
