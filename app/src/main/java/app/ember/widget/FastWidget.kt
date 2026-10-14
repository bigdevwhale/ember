package app.ember.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import app.ember.MainActivity
import app.ember.R
import app.ember.container
import app.ember.content.Content
import app.ember.domain.Stages
import app.ember.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The fast at a glance: a live timer, the stage and progress toward the goal.
 * Between fasts it counts the time since the last one ended. A tap opens the app.
 */
class FastWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FastWidget::class.java))
            if (ids.isEmpty()) return
            val dao = context.container.db.dao()
            val active = dao.active()
            val now = System.currentTimeMillis()
            val content = Content.of(context.resources.configuration.locales[0])
            val views = RemoteViews(context.packageName, R.layout.widget_fast)
            fun base(since: Long) = SystemClock.elapsedRealtime() - (now - since)
            if (active != null) {
                val hours = active.hours(now)
                val goal = active.goalMinutes * 60_000L
                views.setTextViewText(R.id.label, context.getString(if (now - active.start >= goal) R.string.widget_reached else R.string.widget_fasting))
                views.setChronometer(R.id.timer, base(active.start), null, true)
                views.setViewVisibility(R.id.timer, View.VISIBLE)
                views.setTextViewText(R.id.stage, content.stage(Stages.at(hours).id).title)
                views.setViewVisibility(R.id.progress, View.VISIBLE)
                views.setProgressBar(R.id.progress, 1000, ((now - active.start) * 1000 / goal.coerceAtLeast(1)).toInt().coerceIn(0, 1000), false)
            } else {
                val last = dao.fasts().lastOrNull { it.end != null }
                views.setTextViewText(R.id.label, context.getString(R.string.widget_eating))
                if (last != null) {
                    views.setChronometer(R.id.timer, base(last.end!!), null, true)
                    views.setViewVisibility(R.id.timer, View.VISIBLE)
                } else views.setViewVisibility(R.id.timer, View.GONE)
                views.setTextViewText(R.id.stage, context.getString(R.string.widget_tap_start))
                views.setViewVisibility(R.id.progress, View.GONE)
            }
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Alarms.EXTRA_ROUTE, "fast"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}
