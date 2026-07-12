package com.aria.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.aria.app.data.Habit
import com.aria.app.data.HabitLog
import com.aria.app.data.Logic
import com.aria.app.data.StreakStore
import com.aria.app.ui.AriaCard
import com.aria.app.ui.AriaTheme
import com.aria.app.ui.LocalAria
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Shown when the streak widget is placed: pick which habit it tracks. */
class StreakConfigActivity : ComponentActivity() {
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
        val habits = runCatching {
            json.decodeFromString(ListSerializer(Habit.serializer()), prefs.getString("c_habits", null) ?: "[]")
        }.getOrDefault(emptyList()).filter { it.deleted_at == null && !it.is_archived }

        setContent {
            AriaTheme(mode = mode) {
                val a = LocalAria.current
                Surface(Modifier.fillMaxSize(), color = a.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                        Text("Pick a habit", color = a.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("The widget will show this habit's streak.", color = a.textSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        if (habits.isEmpty()) {
                            AriaCard { Text("Open Aria once to load your habits, then add this widget.", color = a.textSecondary) }
                        } else {
                            habits.forEach { h ->
                                AriaCard(onClick = { pick(h, appWidgetId) }) {
                                    Column {
                                        Text(h.name, color = a.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        Text(Logic.frequencyLabel(h), color = a.textSecondary, fontSize = 13.sp)
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

    private fun pick(habit: Habit, appWidgetId: Int) {
        val prefs = getSharedPreferences("aria_prefs", Context.MODE_PRIVATE)
        val logs = runCatching {
            json.decodeFromString(ListSerializer(HabitLog.serializer()), prefs.getString("c_habitLogs", null) ?: "[]")
        }.getOrDefault(emptyList())
        val counts = logs.filter { it.deleted_at == null && it.habit_id == habit.id }.associate { it.log_date to it.count }
        val snap = StreakStore.build(habit, counts, Logic.today())
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@StreakConfigActivity).getGlanceIdBy(appWidgetId)
            StreakStore.setHabit(applicationContext, glanceId, habit.id, snap)
            StreakWidget().update(applicationContext, glanceId)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
