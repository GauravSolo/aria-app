package com.aria.app.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.aria.app.data.StreakStore
import com.aria.app.data.TaskCalStore

class TaskCalWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            CalendarWidgetContent(
                StreakStore.decode(prefs[TaskCalStore.SNAPSHOT_KEY]),
                "Add this widget again to pick a task.",
            )
        }
    }
}

class TaskCalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskCalWidget()
}
