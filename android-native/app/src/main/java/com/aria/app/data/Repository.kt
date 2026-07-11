package com.aria.app.data

import io.github.jan.supabase.postgrest.postgrest
import java.time.Instant
import java.util.UUID

/** Thin Supabase data access. Updates use upsert on the primary key (id/user_id). */
object Repository {
    fun now(): String = Instant.now().toString()
    fun uuid(): String = UUID.randomUUID().toString()

    private val pg get() = Supa.client.postgrest

    suspend fun tasks(uid: String): List<Task> =
        pg.from("tasks").select { filter { eq("user_id", uid) } }.decodeList()

    suspend fun completions(uid: String): List<TaskCompletion> =
        pg.from("task_completions").select { filter { eq("user_id", uid) } }.decodeList()

    suspend fun habits(uid: String): List<Habit> =
        pg.from("habits").select { filter { eq("user_id", uid) } }.decodeList()

    suspend fun habitLogs(uid: String): List<HabitLog> =
        pg.from("habit_logs").select { filter { eq("user_id", uid) } }.decodeList()

    suspend fun waterLogs(uid: String): List<WaterLog> =
        pg.from("water_logs").select { filter { eq("user_id", uid) } }.decodeList()

    suspend fun waterSettings(uid: String): WaterSettings? =
        pg.from("water_settings").select { filter { eq("user_id", uid) } }.decodeSingleOrNull()

    suspend fun upsertTask(t: Task) { pg.from("tasks").upsert(t) }
    suspend fun insertTask(t: Task) { pg.from("tasks").insert(t) }
    suspend fun insertHabit(h: Habit) { pg.from("habits").insert(h) }
    suspend fun upsertCompletion(c: TaskCompletion) { pg.from("task_completions").upsert(c) }
    suspend fun upsertHabitLog(l: HabitLog) { pg.from("habit_logs").upsert(l) }
    suspend fun insertWater(w: WaterLog) { pg.from("water_logs").insert(w) }
    suspend fun upsertWaterSettings(s: WaterSettings) { pg.from("water_settings").upsert(s) }
}
