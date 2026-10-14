package app.ember

import android.app.Application
import android.content.Context
import app.ember.data.DemoSeed
import app.ember.data.EmberDatabase
import app.ember.data.SettingsStore
import app.ember.notify.Alarms
import app.ember.widget.FastWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = EmberDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend EmberDatabase.() -> Unit) {
        scope.launch {
            db.block()
            changed()
        }
    }

    /** Re-arm alarms, redraw the ongoing notification and the home-screen widget. */
    suspend fun changed() {
        Alarms.reschedule(context)
        FastWidget.refresh(context)
    }
}

class EmberApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@EmberApp, container.db, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as EmberApp).container
