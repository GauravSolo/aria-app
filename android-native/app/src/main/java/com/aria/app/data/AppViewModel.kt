package com.aria.app.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.aria.app.notif.ReminderScheduler
import com.aria.app.widget.AriaWidget
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class AuthStatus { Loading, SignedIn, SignedOut }

/** Full-fidelity inputs mirroring the Expo task/habit/reminder forms. */
data class TaskInput(
    val title: String,
    val description: String? = null,
    val category: String = "other",
    val priority: String = "medium",
    val startTime: String? = null,
    val endTime: String? = null,
    val dueDate: String,
    val recurrence: String = "none",
    val recurrenceInterval: Int = 1,
    val recurrenceDays: List<Int> = emptyList(),
    val recurrenceEndDate: String? = null,
)

data class HabitInput(
    val name: String,
    val kind: String = "build",
    val category: String = "health",
    val frequency: String = "daily",
    val targetCount: Int = 1,
    val customDays: List<Int> = emptyList(),
    val reminderTime: String? = null,
    val startDate: String,
    val notes: String? = null,
    val color: String? = null,
)

data class ReminderInput(
    val title: String,
    val body: String? = null,
    val repeat: String = "once",
    val repeatDays: List<Int> = emptyList(),
    val intervalMin: Int? = null,
    val timeOfDay: String? = null,
    val nextTriggerAt: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val prefs = app.getSharedPreferences("aria_prefs", Context.MODE_PRIVATE)

    val status = MutableStateFlow(AuthStatus.Loading)
    val email = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")

    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val completions = MutableStateFlow<List<TaskCompletion>>(emptyList())
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val habitLogs = MutableStateFlow<List<HabitLog>>(emptyList())
    val waterLogs = MutableStateFlow<List<WaterLog>>(emptyList())
    val water = MutableStateFlow<WaterSettings?>(null)
    val reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val history = MutableStateFlow<List<NotificationHistory>>(emptyList())

    private var uid: String? = null

    init {
        viewModelScope.launch {
            Supa.client.auth.sessionStatus.collect { st ->
                when (st) {
                    is SessionStatus.Authenticated -> {
                        uid = st.session.user?.id
                        email.value = st.session.user?.email
                        status.value = AuthStatus.SignedIn
                        refresh()
                    }
                    is SessionStatus.NotAuthenticated -> {
                        uid = null
                        status.value = AuthStatus.SignedOut
                    }
                    else -> status.value = AuthStatus.Loading
                }
            }
        }
    }

    fun setThemeMode(mode: String) {
        themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    // ── Auth ──────────────────────────────────────────────────────────────
    fun signIn(e: String, p: String) = runAuth {
        Supa.client.auth.signInWith(Email) { email = e.trim(); password = p }
    }

    fun signUp(e: String, p: String, name: String) = runAuth {
        Supa.client.auth.signUpWith(Email) {
            email = e.trim(); password = p
            if (name.isNotBlank()) data = buildJsonObject { put("display_name", name.trim()) }
        }
    }

    fun signOut() = viewModelScope.launch { runCatching { Supa.client.auth.signOut() } }

    private fun runAuth(block: suspend () -> Unit) = viewModelScope.launch {
        busy.value = true; error.value = null
        runCatching { block() }.onFailure { error.value = it.message }
        busy.value = false
    }

    // ── Load ──────────────────────────────────────────────────────────────
    fun refresh() {
        val u = uid ?: return
        viewModelScope.launch {
            runCatching {
                tasks.value = Repository.tasks(u)
                completions.value = Repository.completions(u)
                habits.value = Repository.habits(u)
                habitLogs.value = Repository.habitLogs(u)
                waterLogs.value = Repository.waterLogs(u)
                water.value = Repository.waterSettings(u) ?: WaterSettings(user_id = u)
                reminders.value = runCatching { Repository.reminders(u) }.getOrDefault(emptyList())
                history.value = runCatching { Repository.notificationHistory(u) }.getOrDefault(emptyList())
            }.onFailure { error.value = it.message }
            publishWidget()
            runCatching { ReminderScheduler.reschedule(ctx, activeReminders()) }
        }
    }

    // ── Derived ──────────────────────────────────────────────────────────
    val today get() = Logic.today()

    fun isTaskDone(t: Task, date: String = today): Boolean =
        if (t.recurrence == "none") t.is_completed
        else completions.value.any { it.deleted_at == null && it.task_id == t.id && it.occurrence_date == date }

    fun tasksOn(date: String): List<Task> =
        tasks.value.filter { it.deleted_at == null && Logic.taskOccursOn(it, date) }
            .sortedBy { it.start_time ?: "" }

    fun tasksToday(): List<Task> = tasksOn(today)

    /** Dates in [-7, +30] that have at least one task occurrence (for the date strip dots). */
    fun markedDates(): Set<String> {
        val all = tasks.value.filter { it.deleted_at == null }
        val set = HashSet<String>()
        val base = java.time.LocalDate.now()
        for (i in -7..30) {
            val key = base.plusDays(i.toLong()).toString()
            if (all.any { Logic.taskOccursOn(it, key) }) set.add(key)
        }
        return set
    }

    fun activeHabits(): List<Habit> =
        habits.value.filter { it.deleted_at == null && !it.is_archived }
            .sortedWith(compareBy({ it.sort_order }, { it.created_at }))

    fun habitCounts(id: String): Map<String, Int> =
        habitLogs.value.filter { it.deleted_at == null && it.habit_id == id }.associate { it.log_date to it.count }

    fun habitStats(h: Habit) = Logic.computeHabitStats(h, habitCounts(h.id), today)

    fun waterToday(): Int = Logic.waterTotal(waterLogs.value.filter { it.deleted_at == null }, today)

    fun waterWeek() = Logic.weekSeries(waterLogs.value.filter { it.deleted_at == null }, today)
    fun waterMonth(goal: Int) = Logic.monthStats(waterLogs.value.filter { it.deleted_at == null }, goal, today)

    fun analytics(days: Int): Logic.Analytics = Logic.computeAnalytics(
        tasks = tasks.value.filter { it.deleted_at == null },
        completions = completions.value,
        habits = habits.value.filter { it.deleted_at == null && !it.is_archived },
        habitLogs = habitLogs.value,
        waterLogs = waterLogs.value,
        goalMl = water.value?.daily_goal_ml ?: 4000,
        days = days,
        today = today,
    )

    fun activeReminders(): List<Reminder> =
        reminders.value.filter { it.deleted_at == null }.sortedBy { it.created_at }

    fun reminderById(id: String): Reminder? = reminders.value.firstOrNull { it.id == id }
    fun taskById(id: String): Task? = tasks.value.firstOrNull { it.id == id }
    fun habitById(id: String): Habit? = habits.value.firstOrNull { it.id == id }

    // ── Task mutations ──────────────────────────────────────────────────────
    fun toggleTask(t: Task, date: String = today) = mutate {
        if (t.recurrence == "none") {
            val done = !t.is_completed
            Repository.upsertTask(t.copy(is_completed = done, completed_at = if (done) Repository.now() else null, updated_at = Repository.now()))
        } else {
            val existing = completions.value.firstOrNull { it.task_id == t.id && it.occurrence_date == date }
            if (existing != null) {
                val del = existing.deleted_at == null
                Repository.upsertCompletion(existing.copy(deleted_at = if (del) Repository.now() else null, updated_at = Repository.now()))
            } else {
                Repository.upsertCompletion(
                    TaskCompletion(id = Repository.uuid(), user_id = uid!!, task_id = t.id, occurrence_date = date, completed_at = Repository.now(), created_at = Repository.now(), updated_at = Repository.now())
                )
            }
        }
    }

    fun saveTask(existing: Task?, input: TaskInput) = mutate {
        if (existing != null) {
            Repository.upsertTask(
                existing.copy(
                    title = input.title.trim(), description = input.description?.trim()?.ifBlank { null },
                    category = input.category, priority = input.priority,
                    start_time = input.startTime, end_time = input.endTime, due_date = input.dueDate,
                    recurrence = input.recurrence, recurrence_interval = input.recurrenceInterval,
                    recurrence_days = input.recurrenceDays, recurrence_end_date = input.recurrenceEndDate,
                    updated_at = Repository.now(),
                )
            )
        } else {
            Repository.insertTask(
                Task(
                    id = Repository.uuid(), user_id = uid!!, title = input.title.trim(),
                    description = input.description?.trim()?.ifBlank { null },
                    category = input.category, priority = input.priority,
                    start_time = input.startTime, end_time = input.endTime, due_date = input.dueDate,
                    recurrence = input.recurrence, recurrence_interval = input.recurrenceInterval,
                    recurrence_days = input.recurrenceDays, recurrence_end_date = input.recurrenceEndDate,
                    created_at = Repository.now(), updated_at = Repository.now(),
                )
            )
        }
    }

    fun deleteTask(id: String) = mutate {
        val t = taskById(id) ?: return@mutate
        Repository.upsertTask(t.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
    }

    // ── Habit mutations ──────────────────────────────────────────────────────
    fun toggleHabit(h: Habit, date: String = today) = mutate {
        val target = maxOf(1, h.target_count)
        val cur = habitCounts(h.id)[date] ?: 0
        setHabitCount(h, date, if (cur >= target) 0 else target)
    }

    fun adjustHabit(h: Habit, delta: Int, date: String = today) = mutate {
        val cur = habitCounts(h.id)[date] ?: 0
        setHabitCount(h, date, maxOf(0, cur + delta))
    }

    private suspend fun setHabitCount(h: Habit, date: String, count: Int) {
        val existing = habitLogs.value.firstOrNull { it.habit_id == h.id && it.log_date == date }
        when {
            count <= 0 -> if (existing != null && existing.deleted_at == null)
                Repository.upsertHabitLog(existing.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
            existing != null -> Repository.upsertHabitLog(existing.copy(count = count, deleted_at = null, updated_at = Repository.now()))
            else -> Repository.upsertHabitLog(HabitLog(id = Repository.uuid(), user_id = uid!!, habit_id = h.id, log_date = date, count = count, created_at = Repository.now(), updated_at = Repository.now()))
        }
    }

    fun saveHabit(existing: Habit?, input: HabitInput) = mutate {
        val days = if (input.frequency == "daily") emptyList() else input.customDays
        if (existing != null) {
            Repository.upsertHabit(
                existing.copy(
                    name = input.name.trim(), kind = input.kind, category = input.category,
                    frequency = input.frequency, target_count = maxOf(1, input.targetCount),
                    custom_days = days, reminder_time = input.reminderTime, start_date = input.startDate,
                    notes = input.notes?.trim()?.ifBlank { null }, color = input.color, updated_at = Repository.now(),
                )
            )
        } else {
            Repository.insertHabit(
                Habit(
                    id = Repository.uuid(), user_id = uid!!, name = input.name.trim(), kind = input.kind,
                    category = input.category, frequency = input.frequency, target_count = maxOf(1, input.targetCount),
                    custom_days = days, reminder_time = input.reminderTime, start_date = input.startDate,
                    notes = input.notes?.trim()?.ifBlank { null }, color = input.color,
                    sort_order = activeHabits().size, created_at = Repository.now(), updated_at = Repository.now(),
                )
            )
        }
    }

    fun deleteHabit(id: String) = mutate {
        val h = habitById(id) ?: return@mutate
        Repository.upsertHabit(h.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
    }

    // ── Water mutations ──────────────────────────────────────────────────────
    fun logWater(ml: Int) = mutate {
        Repository.insertWater(WaterLog(id = Repository.uuid(), user_id = uid!!, log_date = today, amount_ml = ml, logged_at = Repository.now(), created_at = Repository.now(), updated_at = Repository.now()))
    }

    fun undoLastWater() = mutate {
        val last = waterLogs.value.filter { it.deleted_at == null && it.log_date == today }
            .maxByOrNull { it.logged_at }
        if (last != null) Repository.upsertWater(last.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
    }

    fun saveWater(s: WaterSettings) = mutate {
        val next = s.copy(updated_at = Repository.now())
        Repository.upsertWaterSettings(next)
        water.value = next
    }

    // ── Reminder mutations ──────────────────────────────────────────────────
    fun saveReminder(existing: Reminder?, input: ReminderInput) = mutate {
        if (existing != null) {
            Repository.upsertReminder(
                existing.copy(
                    title = input.title.trim(), body = input.body?.trim()?.ifBlank { null },
                    repeat = input.repeat, repeat_days = input.repeatDays, interval_min = input.intervalMin,
                    time_of_day = input.timeOfDay, next_trigger_at = input.nextTriggerAt,
                    snooze_until = null, updated_at = Repository.now(),
                )
            )
        } else {
            Repository.insertReminder(
                Reminder(
                    id = Repository.uuid(), user_id = uid!!, title = input.title.trim(),
                    body = input.body?.trim()?.ifBlank { null }, kind = "custom", repeat = input.repeat,
                    repeat_days = input.repeatDays, interval_min = input.intervalMin, time_of_day = input.timeOfDay,
                    next_trigger_at = input.nextTriggerAt, is_enabled = true, created_at = Repository.now(), updated_at = Repository.now(),
                )
            )
        }
    }

    fun deleteReminder(id: String) = mutate {
        val r = reminderById(id) ?: return@mutate
        Repository.upsertReminder(r.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
    }

    fun toggleReminder(id: String) = mutate {
        val r = reminderById(id) ?: return@mutate
        Repository.upsertReminder(r.copy(is_enabled = !r.is_enabled, updated_at = Repository.now()))
    }

    fun snoozeReminder(id: String, minutes: Int = 10) = mutate {
        val r = reminderById(id) ?: return@mutate
        val until = java.time.Instant.now().plusSeconds(minutes * 60L).toString()
        Repository.upsertReminder(r.copy(snooze_until = until, is_enabled = true, updated_at = Repository.now()))
        recordHistory(r.id, r.title, r.kind, "snoozed")
    }

    fun markReminderDone(id: String) = mutate {
        val r = reminderById(id) ?: return@mutate
        recordHistory(r.id, r.title, r.kind, "done")
        if (r.repeat == "once") Repository.upsertReminder(r.copy(is_enabled = false, updated_at = Repository.now()))
    }

    private suspend fun recordHistory(reminderId: String?, title: String, kind: String, statusStr: String) {
        runCatching {
            Repository.insertHistory(
                NotificationHistory(
                    id = Repository.uuid(), user_id = uid!!, reminder_id = reminderId, title = title,
                    kind = kind, fired_at = Repository.now(), status = statusStr,
                    created_at = Repository.now(), updated_at = Repository.now(),
                )
            )
        }
    }

    private fun mutate(block: suspend () -> Unit) = viewModelScope.launch {
        runCatching { block() }.onFailure { error.value = it.message }
        refresh()
    }

    private suspend fun publishWidget() {
        val dayTasks = tasksToday()
        val pending = dayTasks.filter { !isTaskDone(it) }
        val scheduled = activeHabits().map { habitStats(it) }.filter { it.scheduledToday }
        val snap = WidgetSnapshot(
            nextTaskTitle = pending.firstOrNull()?.title,
            pendingTasks = pending.size,
            totalTasks = dayTasks.size,
            waterMl = waterToday(),
            waterGoal = water.value?.daily_goal_ml ?: 4000,
            habitsDone = scheduled.count { it.doneToday },
            habitsTotal = scheduled.size,
            topStreak = activeHabits().maxOfOrNull { habitStats(it).current } ?: 0,
        )
        WidgetStore.write(ctx, snap)
        runCatching { AriaWidget().updateAll(ctx) }
    }
}
