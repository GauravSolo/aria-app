package com.aria.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aria.app.MainActivity
import com.aria.app.data.WidgetSnapshot
import com.aria.app.data.WidgetStore
import com.aria.app.data.WidgetSync

/** Passed to ToggleTaskAction so it knows which task to complete. */
val taskIdKey = ActionParameters.Key<String>("task_id")
val recurrenceKey = ActionParameters.Key<String>("recurrence")

/** Widget checkbox tap → mark the task done + refresh the widget. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        parameters[taskIdKey]?.let { WidgetSync.completeTask(context, it, parameters[recurrenceKey] ?: "none") }
    }
}

class AriaWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            WidgetContent(WidgetStore.decode(prefs[WidgetStore.SNAPSHOT_KEY]))
        }
    }
}

private val Bg = Color(0xFF15171C)
private val Text0 = Color(0xFFF2F3F5)
private val Muted = Color(0xFFA6ACB8)
private val Indigo = Color(0xFFA5B4FC)
private val Blue = Color(0xFF60A5FA)
private val Green = Color(0xFF34D399)
private val Amber = Color(0xFFFBBF24)

@Composable
private fun WidgetContent(snap: WidgetSnapshot) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ARIA", style = TextStyle(color = ColorProvider(Indigo), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text("🔥 ${snap.topStreak}", style = TextStyle(color = ColorProvider(Amber), fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatCol("${snap.waterPct}%", "Water", Blue, GlanceModifier.defaultWeight())
            StatCol("${snap.pendingTasks}", "Tasks", Indigo, GlanceModifier.defaultWeight())
            StatCol("${snap.habitsDone}/${snap.habitsTotal}", "Habits", Green, GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(8.dp))
        if (snap.tasks.isEmpty()) {
            Text(
                "No tasks left 🎉",
                maxLines = 1,
                style = TextStyle(color = ColorProvider(Muted), fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(snap.tasks, itemId = { it.id.hashCode().toLong() }) { task ->
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<ToggleTaskAction>(
                            actionParametersOf(taskIdKey to task.id, recurrenceKey to task.recurrence),
                        ),
                        text = task.title,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(Text0), fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        colors = CheckboxDefaults.colors(
                            checkedColor = ColorProvider(Green),
                            uncheckedColor = ColorProvider(Muted),
                        ),
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCol(value: String, label: String, color: Color, modifier: GlanceModifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = TextStyle(color = ColorProvider(color), fontSize = 20.sp, fontWeight = FontWeight.Bold))
        Text(label, style = TextStyle(color = ColorProvider(Muted), fontSize = 10.sp))
    }
}
