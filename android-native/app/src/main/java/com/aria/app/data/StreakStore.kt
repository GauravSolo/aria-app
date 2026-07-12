package com.aria.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.aria.app.widget.StreakWidget
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Cell codes: -1 blank · 0 off/rest · 1 done · 2 missed · 3 today-pending · 4 future. */
@Serializable
data class StreakSnapshot(
    val habitId: String = "",
    val name: String = "",
    val colorHex: String? = null,
    val current: Int = 0,
    val longest: Int = 0,
    val monthLabel: String = "",
    val weeks: List<List<Int>> = emptyList(),
)

/** Per-widget Glance state: which habit is pinned + its rendered streak snapshot. */
object StreakStore {
    val HABIT_KEY = stringPreferencesKey("habit_id")
    val SNAPSHOT_KEY = stringPreferencesKey("streak_snapshot")
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(s: StreakSnapshot): String = json.encodeToString(StreakSnapshot.serializer(), s)
    fun decode(raw: String?): StreakSnapshot? =
        raw?.let { runCatching { json.decodeFromString(StreakSnapshot.serializer(), it) }.getOrNull() }

    /** Build a snapshot for the current month from a habit + its daily counts. */
    fun build(habit: Habit, counts: Map<String, Int>, today: String): StreakSnapshot {
        val st = Logic.computeHabitStats(habit, counts, today)
        val d = java.time.LocalDate.parse(today)
        val weeks = Logic.monthGrid(habit, counts, d.year, d.monthValue, today).map { wk ->
            wk.map { cell ->
                when (cell?.status) {
                    null -> -1
                    Logic.DayStatus.OFF -> 0
                    Logic.DayStatus.COMPLETED -> 1
                    Logic.DayStatus.MISSED -> 2
                    Logic.DayStatus.PENDING -> 3
                    Logic.DayStatus.FUTURE -> 4
                }
            }
        }
        return StreakSnapshot(habit.id, habit.name, habit.color, st.current, st.longest, Logic.monthName(d.monthValue), weeks)
    }

    suspend fun pinnedHabitId(context: Context, glanceId: androidx.glance.GlanceId): String? =
        runCatching { getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)[HABIT_KEY] }.getOrNull()

    /** Pin a habit to one widget instance and write its first snapshot. */
    suspend fun setHabit(context: Context, glanceId: androidx.glance.GlanceId, habitId: String, snapshot: StreakSnapshot) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[HABIT_KEY] = habitId
                this[SNAPSHOT_KEY] = encode(snapshot)
            }
        }
    }

    /** Refresh every pinned streak widget with a freshly-built snapshot. */
    suspend fun push(context: Context, snapshotFor: (habitId: String) -> StreakSnapshot?) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(StreakWidget::class.java)
        ids.forEach { id ->
            val habitId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[HABIT_KEY]
            val snap = habitId?.let { snapshotFor(it) }
            if (snap != null) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply { this[SNAPSHOT_KEY] = encode(snap) }
                }
            }
        }
        StreakWidget().updateAll(context)
    }
}
