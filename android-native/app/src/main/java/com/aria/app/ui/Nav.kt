package com.aria.app.ui

import androidx.compose.runtime.mutableStateListOf

/** Pushed (secondary) screens layered over the tab scaffold. */
sealed interface Route {
    data object Settings : Route
    data object Reminders : Route
    data object WaterSettings : Route
    data class TaskForm(val id: String?, val date: String) : Route
    data class HabitForm(val id: String?) : Route
    data class HabitDetail(val id: String) : Route
    data class ReminderForm(val id: String?) : Route
}

/** Minimal in-memory navigation stack (single-Activity, Compose). */
class Nav {
    val stack = mutableStateListOf<Route>()
    fun push(route: Route) { stack.add(route) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) }
    fun replaceTop(route: Route) { pop(); push(route) }
    val top: Route? get() = stack.lastOrNull()
}
