package app.ember.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.ember.BuildConfig
import app.ember.R
import app.ember.container
import app.ember.data.CheckIn
import app.ember.data.Fast
import app.ember.data.Settings
import app.ember.data.WaterDay
import app.ember.data.WeightEntry
import app.ember.domain.Badge
import app.ember.domain.Badges
import app.ember.domain.Challenge
import app.ember.domain.Challenges
import app.ember.domain.Facts
import app.ember.domain.Levels
import app.ember.domain.Plans
import app.ember.domain.Stats
import app.ember.notify.Alarms
import app.ember.ui.components.CelebrateDialog
import app.ember.ui.components.rememberNow
import app.ember.ui.fast.EditFastScreen
import app.ember.ui.fast.EndFastScreen
import app.ember.ui.fast.FastTab
import app.ember.ui.fast.HungerScreen
import app.ember.ui.fast.PlanScreen
import app.ember.ui.learn.ArticleScreen
import app.ember.ui.learn.LearnTab
import app.ember.ui.learn.StagesScreen
import app.ember.ui.me.MeTab
import app.ember.ui.onboarding.OnboardingScreen
import app.ember.ui.progress.HistoryScreen
import app.ember.ui.progress.ProgressTab
import app.ember.ui.progress.WeightScreen
import app.ember.ui.settings.SettingsScreen
import app.ember.ui.theme.Inter
import app.ember.ui.theme.U
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class Tab { FAST, PROGRESS, LEARN, ME }

/** Everything stored, plus what is derived from it, recomputed once per minute or change. */
@Immutable
class EmberData(
    val fasts: List<Fast>,
    val weights: List<WeightEntry>,
    val water: List<WaterDay>,
    val checkIns: List<CheckIn>,
    val settings: Settings,
    val now: Long,
) {
    val stats = Stats(fasts, now, ZoneId.systemDefault())
    val active: Fast? get() = stats.active
    val facts = Facts.of(stats, weights, water, settings.waterGoal, checkIns, settings.hungerWins)
    val xp = Levels.xp(stats)
    val level = Levels.level(xp)
    val badges: List<Badge> = Badges.unlocked(facts)
    val challenge: Challenge = Challenges.of(stats, Plans.of(settings), water, settings.waterGoal, checkIns, ZoneId.systemDefault())
    fun glassesToday(): Int = water.firstOrNull { it.day == stats.today.toEpochDay() }?.glasses ?: 0
}

/** Everything a tab can open. */
class Go(private val nav: NavHostController) {
    fun plans() = nav.navigate("plans")
    fun end() = nav.navigate("end")
    fun fast(id: Long) = nav.navigate("fast/$id")
    fun hunger() = nav.navigate("hunger")
    fun article(id: String) = nav.navigate("article/$id")
    fun stages(id: String = "") = nav.navigate("stages?id=$id")
    fun weight() = nav.navigate("weight")
    fun history() = nav.navigate("history")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
}

@Composable
fun EmberRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.FAST) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.db.dao()
    val fasts by dao.observeFasts().collectAsStateWithLifecycle(initialValue = null)
    val weights by dao.observeWeights().collectAsStateWithLifecycle(initialValue = emptyList())
    val water by dao.observeWater().collectAsStateWithLifecycle(initialValue = emptyList())
    val checkIns by dao.observeCheckIns().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val data = remember(fasts, weights, water, checkIns, settings, minute) {
        EmberData(fasts.orEmpty(), weights, water, checkIns, settings, System.currentTimeMillis())
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded, fasts != null) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded || fasts == null) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        tab = Tab.FAST
        if (route == "end" && data.active != null) go.end()
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("welcome") {
            OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
        }
        composable("main") { MainTabs(tab, { tab = it }, go, data) }
        composable("plans") { PlanScreen(settings, data.active, onBack = { go.back() }) }
        composable("end") { EndFastScreen(data, onDone = { go.back() }) }
        composable("fast/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
            EditFastScreen(e.arguments?.getLong("id") ?: 0L, settings, onDone = { go.back() })
        }
        composable("hunger") { HungerScreen(data, onClose = { go.back() }) }
        composable("article/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            ArticleScreen(e.arguments?.getString("id").orEmpty(), go)
        }
        composable("stages?id={id}", arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" })) { e ->
            StagesScreen(e.arguments?.getString("id").orEmpty(), data, onBack = { go.back() })
        }
        composable("weight") { WeightScreen(data, onBack = { go.back() }) }
        composable("history") { HistoryScreen(data, go) }
        composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
    }

    if (settings.onboarded && fasts != null) Celebrations(data)
}

/** New badges, a new level and a finished weekly challenge, each celebrated once. */
@Composable
private fun Celebrations(data: EmberData) {
    val container = LocalContext.current.container
    val s = data.settings
    val fresh = data.badges.filter { it.id !in s.seenBadges }
    when {
        fresh.isNotEmpty() -> {
            val b = fresh.last()
            val (color, soft) = badgeColor(b)
            CelebrateDialog(
                icon = badgeIcon(b), color = color, soft = soft,
                eyebrow = stringResource(R.string.badge_unlocked),
                title = badgeTitle(b),
                text = badgeText(b) + if (fresh.size > 1) "\n" + stringResource(R.string.badge_more, fresh.size - 1) else "",
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + fresh.map { f -> f.id }) } } }
        }
        data.level > s.levelSeen -> CelebrateDialog(
            icon = Icons.Rounded.LocalFireDepartment, color = U.c.accent, soft = U.c.accentSoft,
            eyebrow = stringResource(R.string.level_up),
            title = stringResource(R.string.level_n, data.level) + " · " + levelTitle(Levels.title(data.level)),
            text = stringResource(R.string.level_up_text),
        ) { container.scope.launch { container.settings.update { it.copy(levelSeen = data.level) } } }
        data.challenge.done && s.challengeSeen != data.challenge.week -> CelebrateDialog(
            icon = Icons.Rounded.Insights, color = U.c.gold, soft = U.c.goldSoft,
            eyebrow = stringResource(R.string.challenge_done),
            title = challengeTitle(data.challenge),
            text = stringResource(R.string.challenge_done_text),
        ) { container.scope.launch { container.settings.update { it.copy(challengeSeen = data.challenge.week) } } }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: EmberData) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.FAST -> FastTab(data, go, onTab)
                Tab.PROGRESS -> ProgressTab(data, go)
                Tab.LEARN -> LearnTab(data, go)
                Tab.ME -> MeTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.LocalFireDepartment, stringResource(R.string.tab_fast), tab == Tab.FAST) { onTab(Tab.FAST) }
            NavItem(Icons.Rounded.Insights, stringResource(R.string.tab_progress), tab == Tab.PROGRESS) { onTab(Tab.PROGRESS) }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_learn), tab == Tab.LEARN) { onTab(Tab.LEARN) }
            NavItem(Icons.Rounded.Person, stringResource(R.string.tab_me), tab == Tab.ME) { onTab(Tab.ME) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
