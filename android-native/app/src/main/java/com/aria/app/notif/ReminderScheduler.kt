package com.aria.app.notif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aria.app.data.Logic
import com.aria.app.data.Reminder

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

    fun reschedule(context: Context, reminders: List<Reminder>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (r in reminders) {
            val pi = pendingIntent(context, r)
            am.cancel(pi)
            if (!r.is_enabled) continue
            val at = Logic.nextTriggerMillis(r) ?: continue
            if (at <= System.currentTimeMillis()) continue
            runCatching { am.set(AlarmManager.RTC_WAKEUP, at, pi) }
        }
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
