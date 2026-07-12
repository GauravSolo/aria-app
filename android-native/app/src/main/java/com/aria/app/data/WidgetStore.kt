package com.aria.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.aria.app.widget.AriaWidget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WidgetTask(val id: String, val title: String, val recurrence: String = "none")

@Serializable
data class WidgetSnapshot(
    val nextTaskTitle: String? = null,
    val tasks: List<WidgetTask> = emptyList(),
    val pendingTasks: Int = 0,
    val totalTasks: Int = 0,
    val waterMl: Int = 0,
    val waterGoal: Int = 4000,
    val habitsDone: Int = 0,
    val habitsTotal: Int = 0,
    val topStreak: Int = 0,
) {
    val waterPct: Int get() = if (waterGoal > 0) (waterMl * 100f / waterGoal).toInt() else 0
}

/**
 * The snapshot is stored in each widget's Glance state (not a private DataStore),
 * so `currentState` inside the composable updates reactively — otherwise a value
 * read once in provideGlance is captured at bind time and never refreshes.
 */
object WidgetStore {
    val SNAPSHOT_KEY = stringPreferencesKey("snapshot")
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(snapshot: WidgetSnapshot): String =
        json.encodeToString(WidgetSnapshot.serializer(), snapshot)

    fun decode(raw: String?): WidgetSnapshot =
        raw?.let { runCatching { json.decodeFromString(WidgetSnapshot.serializer(), it) }.getOrNull() } ?: WidgetSnapshot()

    /** Current snapshot (from the first placed widget), for optimistic edits. */
    suspend fun read(context: Context): WidgetSnapshot {
        val id = GlanceAppWidgetManager(context).getGlanceIds(AriaWidget::class.java).firstOrNull() ?: return WidgetSnapshot()
        return runCatching { decode(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[SNAPSHOT_KEY]) }.getOrDefault(WidgetSnapshot())
    }

    /** Write the snapshot into every placed widget's Glance state and repaint. */
    suspend fun push(context: Context, snapshot: WidgetSnapshot) {
        val raw = encode(snapshot)
        val ids = GlanceAppWidgetManager(context).getGlanceIds(AriaWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply { this[SNAPSHOT_KEY] = raw }
            }
        }
        AriaWidget().updateAll(context)
    }
}
