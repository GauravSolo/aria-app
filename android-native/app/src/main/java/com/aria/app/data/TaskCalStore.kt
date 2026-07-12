package com.aria.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.aria.app.widget.TaskCalWidget

/** Per-widget Glance state for the task-calendar widget: pinned task id + snapshot.
 *  Reuses [StreakSnapshot] (name / current / longest / month / weeks). */
object TaskCalStore {
    val TASK_KEY = stringPreferencesKey("task_id")
    val SNAPSHOT_KEY = stringPreferencesKey("snapshot")

    /** Build a snapshot for the current month from a recurring task + its done dates. */
    fun build(task: Task, done: Set<String>, today: String): StreakSnapshot {
        val (cur, longest) = Logic.taskStreaks(task, done, today)
        val d = java.time.LocalDate.parse(today)
        val weeks = Logic.taskMonthGrid(task, done, d.year, d.monthValue, today).map { wk ->
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
        return StreakSnapshot(task.id, task.title, null, cur, longest, Logic.monthName(d.monthValue), weeks)
    }

    suspend fun setTask(context: Context, glanceId: androidx.glance.GlanceId, taskId: String, snapshot: StreakSnapshot) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[TASK_KEY] = taskId
                this[SNAPSHOT_KEY] = StreakStore.encode(snapshot)
            }
        }
    }

    suspend fun push(context: Context, snapshotFor: (taskId: String) -> StreakSnapshot?) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(TaskCalWidget::class.java)
        ids.forEach { id ->
            val taskId = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[TASK_KEY]
            val snap = taskId?.let { snapshotFor(it) }
            if (snap != null) {
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply { this[SNAPSHOT_KEY] = StreakStore.encode(snap) }
                }
            }
        }
        TaskCalWidget().updateAll(context)
    }
}
