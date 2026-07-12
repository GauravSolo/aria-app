package com.aria.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aria.app.MainActivity
import com.aria.app.data.StreakSnapshot
import com.aria.app.data.StreakStore

private val Bg = Color(0xFF15171C)
private val Text0 = Color(0xFFF2F3F5)
private val Muted = Color(0xFFA6ACB8)
private val Indigo = Color(0xFFA5B4FC)
private val Amber = Color(0xFFFBBF24)
private val Track = Color(0xFF2A2E37)
private val MissedC = Color(0xFF7F3B3B)
private val DefaultDone = Color(0xFF6366F1)

class StreakWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            StreakContent(StreakStore.decode(prefs[StreakStore.SNAPSHOT_KEY]))
        }
    }
}

private fun parseColor(hex: String?): Color =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: DefaultDone

@Composable
private fun StreakContent(snap: StreakSnapshot?) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Bg).cornerRadius(20.dp).padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        if (snap == null || snap.habitId.isEmpty()) {
            Text("Streak", style = TextStyle(color = ColorProvider(Indigo), fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(8.dp))
            Text("Add this widget again to pick a habit.", style = TextStyle(color = ColorProvider(Muted), fontSize = 12.sp))
            return@Column
        }
        val done = parseColor(snap.colorHex)
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(snap.name, maxLines = 1, style = TextStyle(color = ColorProvider(Text0), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text("🔥 ${snap.current}", style = TextStyle(color = ColorProvider(Amber), fontSize = 13.sp, fontWeight = FontWeight.Bold))
        }
        Text("${snap.monthLabel} · best ${snap.longest}", style = TextStyle(color = ColorProvider(Muted), fontSize = 11.sp))
        Spacer(GlanceModifier.height(8.dp))
        // Month grid: weekday initials down the left, one column per week (vertical).
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column {
                WEEKDAY_INITIALS.forEach { letter ->
                    Box(GlanceModifier.width(14.dp).height(22.dp), contentAlignment = Alignment.Center) {
                        Text(letter, style = TextStyle(color = ColorProvider(Muted), fontSize = 9.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
            snap.weeks.forEach { week ->
                Column(modifier = GlanceModifier.defaultWeight()) {
                    for (wd in 0 until 7) {
                        val code = week.getOrNull(wd) ?: -1
                        val c = when (code) {
                            1 -> done
                            2 -> MissedC
                            else -> Track // off / today / future / blank
                        }
                        Box(GlanceModifier.height(22.dp), contentAlignment = Alignment.Center) {
                            if (code == -1) Spacer(GlanceModifier.size(18.dp))
                            else Box(GlanceModifier.size(18.dp).cornerRadius(5.dp).background(c)) {}
                        }
                    }
                }
            }
        }
    }
}

private val WEEKDAY_INITIALS = listOf("S", "M", "T", "W", "T", "F", "S")
