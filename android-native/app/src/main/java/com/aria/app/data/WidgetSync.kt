package com.aria.app.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.aria.app.widget.AriaWidget
import io.github.jan.supabase.auth.auth

/** Lets the home-screen widget complete a task and rebuild its snapshot on its
 *  own, independent of the running app / ViewModel (fetches fresh from Supabase). */
object WidgetSync {

    private suspend fun uid(ctx: Context): String? =
        runCatching { PrefsSessionManager(ctx.applicationContext).loadSession().user?.id }.getOrNull()

    /** Mark a pending task done from the widget checkbox. Updates the widget
     *  instantly (optimistic), then persists in the background — no re-fetch or
     *  rebuild, so it stays snappy like the Google Tasks widget. */
    suspend fun completeTask(ctx: Context, taskId: String, recurrence: String) {
        val cur = WidgetStore.read(ctx)
        if (cur.tasks.any { it.id == taskId }) {
            WidgetStore.push(ctx, cur.copy(
                tasks = cur.tasks.filter { it.id != taskId },
                pendingTasks = maxOf(0, cur.pendingTasks - 1),
                nextTaskTitle = cur.tasks.firstOrNull { it.id != taskId }?.title,
            ))
        }

        Supa.init(ctx.applicationContext)
        runCatching { Supa.client.auth.awaitInitialization() }
        val uid = uid(ctx) ?: return
        val now = Repository.now()
        runCatching {
            if (recurrence == "none") {
                Repository.markTaskComplete(taskId, now)
            } else {
                Repository.upsertCompletion(
                    TaskCompletion(id = Repository.uuid(), user_id = uid, task_id = taskId, occurrence_date = Logic.today(), completed_at = now, created_at = now, updated_at = now),
                )
            }
        }
    }

    /** Recompute the widget snapshot from fresh Supabase data + repaint the widget. */
    suspend fun rebuild(ctx: Context) {
        Supa.init(ctx.applicationContext)
        runCatching { Supa.client.auth.awaitInitialization() }
        val uid = uid(ctx) ?: return
        val today = Logic.today()
        val tasks = Repository.tasks(uid).filter { it.deleted_at == null }
        val completions = Repository.completions(uid)
        val habits = Repository.habits(uid).filter { it.deleted_at == null && !it.is_archived }
        val habitLogs = Repository.habitLogs(uid)
        val waterLogs = Repository.waterLogs(uid)
        val water = runCatching { Repository.waterSettings(uid) }.getOrNull()

        fun taskDone(t: Task): Boolean =
            if (t.recurrence == "none") t.is_completed
            else completions.any { it.deleted_at == null && it.task_id == t.id && it.occurrence_date == today }

        fun statsOf(h: Habit) = Logic.computeHabitStats(
            h, habitLogs.filter { it.deleted_at == null && it.habit_id == h.id }.associate { it.log_date to it.count }, today,
        )

        val dayTasks = tasks.filter { Logic.taskOccursOn(it, today) }.sortedBy { Logic.minuteOfDay(it.start_time) }
        val pending = dayTasks.filter { !taskDone(it) }
        val scheduled = habits.map { statsOf(it) }.filter { it.scheduledToday }

        val snap = WidgetSnapshot(
            nextTaskTitle = pending.firstOrNull()?.title,
            tasks = pending.take(50).map { WidgetTask(it.id, it.title, it.recurrence) },
            pendingTasks = pending.size,
            totalTasks = dayTasks.size,
            waterMl = Logic.waterTotal(waterLogs.filter { it.deleted_at == null }, today),
            waterGoal = water?.daily_goal_ml ?: 4000,
            habitsDone = scheduled.count { it.doneToday },
            habitsTotal = scheduled.size,
            topStreak = habits.maxOfOrNull { statsOf(it).current } ?: 0,
        )
        WidgetStore.push(ctx, snap)
    }
}
