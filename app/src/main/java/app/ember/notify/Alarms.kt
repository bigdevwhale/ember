package app.ember.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.ember.MainActivity
import app.ember.R
import app.ember.container
import app.ember.content.Content
import app.ember.data.Fast
import app.ember.data.Settings
import app.ember.domain.Plans
import app.ember.domain.Stages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Everything that happens while the app is closed: the ongoing timer notification,
 * "goal reached", a quiet note at each stage, and the daily nudges around the eating window.
 */
object Alarms {
    private const val CHANNEL_TIMER = "timer_v1"
    private const val CHANNEL_ALERTS = "alerts_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_GOAL = "app.ember.notify.GOAL"
    private const val ACTION_STAGE = "app.ember.notify.STAGE"
    private const val ACTION_START = "app.ember.notify.START"
    private const val ACTION_WINDOW = "app.ember.notify.WINDOW"
    const val ACTION_START_NOW = "app.ember.notify.START_NOW"

    private const val ID_ONGOING = 1
    private const val ID_GOAL = 2
    private const val ID_STAGE = 3
    private const val ID_START = 4
    private const val ID_WINDOW = 5

    private const val HOUR = 3_600_000L

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_TIMER) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TIMER, context.getString(R.string.channel_timer), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.channel_timer_desc)
                    setShowBadge(false)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ALERTS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_alerts_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** When the next stage after [now] begins, or null past the last one. */
    fun nextStageAt(f: Fast, now: Long): Long? = Stages.next(f.hours(now))?.let { f.start + it.hour * HOUR }

    suspend fun reschedule(context: Context) {
        val c = context.container
        val s = c.settings.now()
        val active = c.db.dao().active()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val plan = Plans.of(s)

        fun set(action: String, code: Int, at: Long?) {
            val p = pi(context, action, code)
            // Reminders don't need to-the-second precision; this avoids the exact-alarm permission.
            if (at == null || at <= now) am.cancel(p) else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        }

        val goalAt = active?.let { it.start + it.goalMinutes * 60_000L }
        set(ACTION_GOAL, 1, goalAt?.takeIf { s.remindGoal })
        // Always armed while fasting: it also moves the ongoing notification and the widget to the next stage.
        set(ACTION_STAGE, 2, active?.let { nextStageAt(it, now) })
        val scheduled = s.startAt >= 0 && plan.daily
        val nowLocal = LocalDateTime.now(zone)
        set(ACTION_START, 3, if (scheduled && s.remindStart) nextAt(s.startAt, nowLocal).atZone(zone).toInstant().toEpochMilli() else null)
        set(ACTION_WINDOW, 4, if (scheduled && s.remindWindow) nextAt(s.startAt - 30, nowLocal).atZone(zone).toInstant().toEpochMilli() else null)

        showOngoing(context, active, s)
    }

    private fun showOngoing(context: Context, active: Fast?, s: Settings) {
        val nm = NotificationManagerCompat.from(context)
        if (active == null || !s.ongoing || !canNotify(context)) {
            nm.cancel(ID_ONGOING)
            return
        }
        val now = System.currentTimeMillis()
        val content = Content.of(context.resources.configuration.locales[0])
        val hours = active.hours(now)
        val stage = content.stage(Stages.at(hours).id)
        val goalAt = active.start + active.goalMinutes * 60_000L
        val reached = now >= goalAt
        val title = if (reached) context.getString(R.string.notif_ongoing_reached)
        else context.getString(R.string.notif_ongoing_title, clock(context, goalAt))
        val n = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(stage.title + " · " + stage.short)
            .setSubText(Plans.byId(active.plan, (active.goalMinutes / 60)).let { planLabel(context, it.id, it.fastHours) })
            .setWhen(active.start)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setProgress(1000, ((now - active.start) * 1000 / (goalAt - active.start).coerceAtLeast(1)).toInt().coerceIn(0, 1000), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(open(context, 200, "fast"))
            .addAction(0, context.getString(R.string.end_fast), open(context, 201, "end"))
            .build()
        try {
            nm.notify(ID_ONGOING, n)
        } catch (_: SecurityException) {
        }
    }

    private fun planLabel(context: Context, id: String, hours: Int): String =
        if (id == Plans.CUSTOM || hours >= 24) context.resources.getQuantityString(R.plurals.hours, hours, hours)
        else if (id == "omad") "OMAD" else "$hours:${24 - hours}"

    private fun clock(context: Context, ms: Long): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(context.resources.configuration.locales[0])
            .format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

    suspend fun fire(context: Context, action: String) {
        val c = context.container
        val s = c.settings.now()
        val active = c.db.dao().active()
        val content = Content.of(context.resources.configuration.locales[0])
        val now = System.currentTimeMillis()
        when (action) {
            ACTION_GOAL -> if (active != null && s.remindGoal && active.minutes(now) >= active.goalMinutes) {
                val h = active.goalMinutes / 60
                notify(context, ID_GOAL, context.getString(R.string.notif_goal_title, context.resources.getQuantityString(R.plurals.hours, h, h)), content.cheer(active.start), "fast")
            }
            ACTION_STAGE -> if (active != null && s.remindStages) {
                val st = Stages.at(active.hours(now))
                // Only the stage that just began, not one an hour-late alarm skipped past.
                if (st.hour > 0 && now - (active.start + st.hour * HOUR) < HOUR) {
                    val t = content.stage(st.id)
                    notify(context, ID_STAGE, context.getString(R.string.notif_stage_title, t.title), t.short, "fast", quiet = true)
                }
            }
            ACTION_START -> if (active == null && s.remindStart) {
                val p = Plans.of(s)
                notify(
                    context, ID_START, context.getString(R.string.notif_start_title),
                    context.getString(R.string.notif_start_text, planLabel(context, p.id, p.fastHours)), "fast",
                    startNow = true,
                )
            }
            ACTION_WINDOW -> if (active == null && s.remindWindow) {
                notify(context, ID_WINDOW, context.getString(R.string.notif_window_title), context.getString(R.string.notif_window_text), "fast")
            }
            ACTION_START_NOW -> {
                NotificationManagerCompat.from(context).cancel(ID_START)
                if (active == null) {
                    val p = Plans.of(s)
                    c.db.dao().insertFast(Fast(start = now, goalMinutes = p.fastHours * 60, plan = p.id))
                }
            }
        }
    }

    private fun open(context: Context, code: Int, route: String) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, id: Int, title: String, text: String, route: String, quiet: Boolean = false, startNow: Boolean = false) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id, route))
            .setSilent(quiet)
            .setAutoCancel(true)
        if (startNow) b.addAction(0, context.getString(R.string.start_now), pi(context, ACTION_START_NOW, 9))
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_GOAL, ACTION_STAGE, ACTION_START, ACTION_WINDOW, ACTION_START_NOW)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Alarms.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context, action)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}
