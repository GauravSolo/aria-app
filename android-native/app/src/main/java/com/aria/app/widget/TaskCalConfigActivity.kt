package com.aria.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.aria.app.data.Logic
import com.aria.app.data.Task
import com.aria.app.data.TaskCalStore
import com.aria.app.data.TaskCompletion
import com.aria.app.ui.AriaCard
import com.aria.app.ui.AriaTheme
import com.aria.app.ui.LocalAria
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Shown when the task-calendar widget is placed: pick which recurring task it tracks. */
class TaskCalConfigActivity : ComponentActivity() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        val prefs = getSharedPreferences("aria_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("theme_mode", "system") ?: "system"
        val tasks = runCatching {
            json.decodeFromString(ListSerializer(Task.serializer()), prefs.getString("c_tasks", null) ?: "[]")
        }.getOrDefault(emptyList()).filter { it.deleted_at == null && it.recurrence != "none" }

        setContent {
            AriaTheme(mode = mode) {
                val a = LocalAria.current
                Surface(Modifier.fillMaxSize(), color = a.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Text("Pick a task", color = a.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("The widget will show this task's calendar.", color = a.textSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        if (tasks.isEmpty()) {
                            AriaCard { Text("No repeating tasks yet. Create one in Aria, then add this widget.", color = a.textSecondary) }
                        } else {
                            tasks.forEach { t ->
                                AriaCard(onClick = { pick(t, appWidgetId) }) {
                                    Column {
                                        Text(t.title, color = a.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Text(Logic.recurrenceLabel(t), color = a.textSecondary, fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pick(task: Task, appWidgetId: Int) {
        val prefs = getSharedPreferences("aria_prefs", Context.MODE_PRIVATE)
        val comps = runCatching {
            json.decodeFromString(ListSerializer(TaskCompletion.serializer()), prefs.getString("c_completions", null) ?: "[]")
        }.getOrDefault(emptyList())
        val done = comps.filter { it.deleted_at == null && it.task_id == task.id }.map { it.occurrence_date }.toSet()
        val snap = TaskCalStore.build(task, done, Logic.today())
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@TaskCalConfigActivity).getGlanceIdBy(appWidgetId)
            TaskCalStore.setTask(applicationContext, glanceId, task.id, snap)
            TaskCalWidget().update(applicationContext, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
