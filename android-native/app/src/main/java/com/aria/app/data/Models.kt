package com.aria.app.data

import kotlinx.serialization.Serializable

/**
 * Mirrors docs/DATA_MODEL.md. Property names are snake_case to map 1:1 to the
 * Postgres columns (no @SerialName noise). Temporal values are ISO / YYYY-MM-DD strings.
 */

@Serializable
data class Task(
    val id: String,
    val user_id: String,
    val title: String,
    val description: String? = null,
    val category: String = "other",
    val priority: String = "medium",
    val start_time: String? = null,
    val end_time: String? = null,
    val due_date: String? = null,
    val recurrence: String = "none",
    val recurrence_interval: Int = 1,
    val recurrence_days: List<Int> = emptyList(),
    val recurrence_end_date: String? = null,
    val is_completed: Boolean = false,
    val completed_at: String? = null,
    val sort_order: Int = 0,
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class TaskCompletion(
    val id: String,
    val user_id: String,
    val task_id: String,
    val occurrence_date: String,
    val completed_at: String = "",
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class Habit(
    val id: String,
    val user_id: String,
    val name: String,
    val kind: String = "build",
    val category: String = "health",
    val frequency: String = "daily",
    val target_count: Int = 1,
    val custom_days: List<Int> = emptyList(),
    val reminder_time: String? = null,
    val start_date: String,
    val notes: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val is_archived: Boolean = false,
    val sort_order: Int = 0,
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class HabitLog(
    val id: String,
    val user_id: String,
    val habit_id: String,
    val log_date: String,
    val count: Int = 1,
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class WaterLog(
    val id: String,
    val user_id: String,
    val log_date: String,
    val amount_ml: Int,
    val logged_at: String = "",
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class WaterSettings(
    val user_id: String,
    val daily_goal_ml: Int = 4000,
    val glass_size_ml: Int = 250,
    val reminder_interval_min: Int = 45,
    val reminder_enabled: Boolean = true,
    val active_start: String = "08:00",
    val active_end: String = "22:00",
    val updated_at: String = "",
)

@Serializable
data class Reminder(
    val id: String,
    val user_id: String,
    val title: String,
    val body: String? = null,
    val kind: String = "custom",
    val ref_id: String? = null,
    val repeat: String = "once",
    val repeat_days: List<Int> = emptyList(),
    val interval_min: Int? = null,
    val time_of_day: String? = null,
    val next_trigger_at: String? = null,
    val is_enabled: Boolean = true,
    val snooze_until: String? = null,
    val local_notification_id: String? = null,
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)

@Serializable
data class NotificationHistory(
    val id: String,
    val user_id: String,
    val reminder_id: String? = null,
    val title: String,
    val body: String? = null,
    val kind: String = "custom",
    val fired_at: String = "",
    val status: String = "delivered",
    val created_at: String = "",
    val updated_at: String = "",
    val deleted_at: String? = null,
)
