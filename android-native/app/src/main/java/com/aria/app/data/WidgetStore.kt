package com.aria.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val Context.widgetDataStore by preferencesDataStore(name = "aria_widget")

@Serializable
data class WidgetTask(val id: String, val title: String)

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

/** In-process shared store the app writes and the Glance widget reads. */
object WidgetStore {
    private val KEY = stringPreferencesKey("snapshot")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun write(context: Context, snapshot: WidgetSnapshot) {
        context.widgetDataStore.edit { it[KEY] = json.encodeToString(WidgetSnapshot.serializer(), snapshot) }
    }

    suspend fun read(context: Context): WidgetSnapshot {
        val raw = context.widgetDataStore.data.first()[KEY] ?: return WidgetSnapshot()
        return runCatching { json.decodeFromString(WidgetSnapshot.serializer(), raw) }.getOrDefault(WidgetSnapshot())
    }
}
