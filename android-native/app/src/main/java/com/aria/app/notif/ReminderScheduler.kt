package com.aria.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aria.app.data.Logic
import com.aria.app.data.Reminder
import com.aria.app.data.WaterSettings

/**
 * Best-effort local reminder scheduling with AlarmManager. Uses inexact alarms
 * (no special permission) — each enabled reminder's next fire time is scheduled;
 * the app re-schedules on every data refresh so repeats keep firing.
 */
object ReminderScheduler {
    const val CHANNEL_ID = "aria_reminders"
    const val ACTION_FIRE = "com.aria.app.REMINDER_FIRE"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_BODY = "reminder_body"
    const val EXTRA_KIND = "reminder_kind"

    // Water pings use a dedicated request-code range so they can be cancelled/reset
    // independently of the reminder alarms.
    private const val WATER_REQ_BASE = 900_000
    private const val WATER_CAP = 16

    fun reschedule(context: Context, reminders: List<Reminder>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (r in reminders) {
            val pi = pendingIntent(context, r)
            am.cancel(pi)   // always cancel first — covers deleted/disabled so leaked alarms stop
            if (r.deleted_at != null || !r.is_enabled) continue
            val at = Logic.nextTriggerMillis(r) ?: continue
            if (at <= System.currentTimeMillis()) continue
            runCatching { am.set(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    /** Schedule (or clear) the recurring hydration nudges from water settings. */
    fun scheduleWater(context: Context, ws: WaterSettings?) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0 until WATER_CAP) am.cancel(waterIntent(context, i))  // clear old set first
        if (ws == null || !ws.reminder_enabled) return
        val now = System.currentTimeMillis()
        Logic.waterPingMillis(ws, cap = WATER_CAP).forEachIndexed { i, at ->
            if (at > now) runCatching { am.set(AlarmManager.RTC_WAKEUP, at, waterIntent(context, i)) }
        }
    }

    private fun waterIntent(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ID, "water")
            putExtra(EXTRA_TITLE, "Time to hydrate 💧")
            putExtra(EXTRA_BODY, "Have a glass of water to stay on track.")
            putExtra(EXTRA_KIND, "water")
        }
        return PendingIntent.getBroadcast(
            context,
            WATER_REQ_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(context: Context, r: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ID, r.id)
            putExtra(EXTRA_TITLE, r.title)
            putExtra(EXTRA_BODY, r.body)
            putExtra(EXTRA_KIND, r.kind)
        }
        return PendingIntent.getBroadcast(
            context,
            r.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
