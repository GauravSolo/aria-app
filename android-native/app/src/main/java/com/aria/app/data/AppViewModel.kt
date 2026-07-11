package com.aria.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.glance.appwidget.updateAll
import com.aria.app.widget.AriaWidget
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class AuthStatus { Loading, SignedIn, SignedOut }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()

    val status = MutableStateFlow(AuthStatus.Loading)
    val email = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val completions = MutableStateFlow<List<TaskCompletion>>(emptyList())
    val habits = MutableStateFlow<List<Habit>>(emptyList())
    val habitLogs = MutableStateFlow<List<HabitLog>>(emptyList())
    val waterLogs = MutableStateFlow<List<WaterLog>>(emptyList())
    val water = MutableStateFlow<WaterSettings?>(null)

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
            }.onFailure { error.value = it.message }
            publishWidget()
        }
    }

    // ── Derived (today) ────────────────────────────────────────────────────
    val today get() = Logic.today()

    fun isTaskDone(t: Task): Boolean =
        if (t.recurrence == "none") t.is_completed
        else completions.value.any { it.deleted_at == null && it.task_id == t.id && it.occurrence_date == today }

    fun tasksToday(): List<Task> =
        tasks.value.filter { it.deleted_at == null && Logic.taskOccursOn(it, today) }
            .sortedBy { it.start_time ?: "" }

    fun activeHabits(): List<Habit> =
        habits.value.filter { it.deleted_at == null && !it.is_archived }.sortedBy { it.sort_order }

    fun habitCounts(id: String): Map<String, Int> =
        habitLogs.value.filter { it.deleted_at == null && it.habit_id == id }.associate { it.log_date to it.count }

    fun habitStats(h: Habit) = Logic.computeHabitStats(h, habitCounts(h.id), today)

    fun waterToday(): Int = Logic.waterTotal(waterLogs.value.filter { it.deleted_at == null }, today)

    // ── Mutations ────────────────────────────────────────────────────────────
    fun toggleTask(t: Task) = mutate {
        if (t.recurrence == "none") {
            val done = !t.is_completed
            Repository.upsertTask(t.copy(is_completed = done, completed_at = if (done) Repository.now() else null, updated_at = Repository.now()))
        } else {
            val existing = completions.value.firstOrNull { it.task_id == t.id && it.occurrence_date == today }
            if (existing != null) {
                val del = existing.deleted_at == null
                Repository.upsertCompletion(existing.copy(deleted_at = if (del) Repository.now() else null, updated_at = Repository.now()))
            } else {
                Repository.upsertCompletion(
                    TaskCompletion(id = Repository.uuid(), user_id = uid!!, task_id = t.id, occurrence_date = today, completed_at = Repository.now(), created_at = Repository.now(), updated_at = Repository.now())
                )
            }
        }
    }

    fun addTask(title: String, category: String, priority: String, dueDate: String, startTime: String?, recurrence: String, days: List<Int>) = mutate {
        Repository.insertTask(
            Task(id = Repository.uuid(), user_id = uid!!, title = title, category = category, priority = priority, start_time = startTime, due_date = dueDate, recurrence = recurrence, recurrence_days = days, created_at = Repository.now(), updated_at = Repository.now())
        )
    }

    fun toggleHabit(h: Habit) = mutate {
        val target = maxOf(1, h.target_count)
        val cur = habitCounts(h.id)[today] ?: 0
        setHabitCount(h, if (cur >= target) 0 else target)
    }

    fun adjustHabit(h: Habit, delta: Int) = mutate {
        val cur = habitCounts(h.id)[today] ?: 0
        setHabitCount(h, maxOf(0, cur + delta))
    }

    private suspend fun setHabitCount(h: Habit, count: Int) {
        val existing = habitLogs.value.firstOrNull { it.habit_id == h.id && it.log_date == today }
        when {
            count <= 0 -> if (existing != null && existing.deleted_at == null)
                Repository.upsertHabitLog(existing.copy(deleted_at = Repository.now(), updated_at = Repository.now()))
            existing != null -> Repository.upsertHabitLog(existing.copy(count = count, deleted_at = null, updated_at = Repository.now()))
            else -> Repository.upsertHabitLog(HabitLog(id = Repository.uuid(), user_id = uid!!, habit_id = h.id, log_date = today, count = count, created_at = Repository.now(), updated_at = Repository.now()))
        }
    }

    fun addHabit(name: String, category: String, frequency: String, target: Int, days: List<Int>) = mutate {
        Repository.insertHabit(
            Habit(id = Repository.uuid(), user_id = uid!!, name = name, category = category, frequency = frequency, target_count = maxOf(1, target), custom_days = if (frequency == "daily") emptyList() else days, start_date = today, sort_order = activeHabits().size, created_at = Repository.now(), updated_at = Repository.now())
        )
    }

    fun logWater(ml: Int) = mutate {
        Repository.insertWater(WaterLog(id = Repository.uuid(), user_id = uid!!, log_date = today, amount_ml = ml, logged_at = Repository.now(), created_at = Repository.now(), updated_at = Repository.now()))
    }

    fun saveWater(s: WaterSettings) = mutate {
        val next = s.copy(updated_at = Repository.now())
        Repository.upsertWaterSettings(next)
        water.value = next
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
