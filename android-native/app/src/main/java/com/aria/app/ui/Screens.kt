package com.aria.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aria.app.data.AppViewModel
import com.aria.app.data.Habit
import com.aria.app.data.Logic
import com.aria.app.data.Task

private enum class Tab(val label: String) {
    Today("Today"), Planner("Planner"), Habits("Habits"), Water("Water")
}

@Composable
fun MainScaffold(vm: AppViewModel) {
    var tab by remember { mutableStateOf(Tab.Today) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.Today -> Icons.Filled.Today
                                    Tab.Planner -> Icons.Filled.CalendarMonth
                                    Tab.Habits -> Icons.Filled.LocalFireDepartment
                                    Tab.Water -> Icons.Filled.WaterDrop
                                },
                                contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Today -> DashboardScreen(vm)
                Tab.Planner -> TasksScreen(vm)
                Tab.Habits -> HabitsScreen(vm)
                Tab.Water -> WaterScreen(vm)
            }
        }
    }
}

/** Collect the data flows so screens recompose when data changes. */
@Composable
private fun observe(vm: AppViewModel) {
    vm.tasks.collectAsState(); vm.completions.collectAsState(); vm.habits.collectAsState()
    vm.habitLogs.collectAsState(); vm.waterLogs.collectAsState(); vm.water.collectAsState()
}

@Composable
private fun sectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

// ── Dashboard ──────────────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(vm: AppViewModel) {
    observe(vm)
    val email by vm.email.collectAsState()
    val dayTasks = vm.tasksToday()
    val doneCount = dayTasks.count { vm.isTaskDone(it) }
    val scheduled = vm.activeHabits().map { vm.habitStats(it) }.filter { it.scheduledToday }
    val habitsDone = scheduled.count { it.doneToday }
    val goal = vm.water.value?.daily_goal_ml ?: 4000
    val waterMl = vm.waterToday()
    val score = Logic.productivityScore(
        if (dayTasks.isNotEmpty()) doneCount.toDouble() / dayTasks.size else null,
        if (scheduled.isNotEmpty()) habitsDone.toDouble() / scheduled.size else null,
        if (goal > 0) waterMl.toDouble() / goal else null,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                IconButton(onClick = { vm.signOut() }) { Icon(Icons.Filled.Logout, "Sign out") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ring(progress = score / 100f, label = "$score", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Productivity today", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (score >= 80) "On fire 🔥" else if (score >= 50) "Nice progress" else "Let’s get going",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("✓ $doneCount/${dayTasks.size}   🔥 $habitsDone/${scheduled.size}   💧 ${if (goal > 0) waterMl * 100 / goal else 0}%", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.logWater(vm.water.value?.glass_size_ml ?: 250) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.WaterDrop, null); Spacer(Modifier.width(6.dp)); Text("+ Water")
                }
            }
        }
        item { sectionTitle("Today’s tasks") }
        if (dayTasks.isEmpty()) {
            item { Card(Modifier.fillMaxWidth()) { Text("No tasks today.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(dayTasks) { TaskRow(vm, it) }
        }
    }
}

@Composable
private fun Ring(progress: Float, label: String, color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 7.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(label, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun TaskRow(vm: AppViewModel, task: Task) {
    val done = vm.isTaskDone(task)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = done, onCheckedChange = { vm.toggleTask(task) })
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Medium)
                Text(task.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Brand.category(task.category))
            }
            Box(Modifier.size(8.dp)) {
                CircularProgressIndicator(progress = { 1f }, color = Brand.priority(task.priority), strokeWidth = 8.dp)
            }
        }
    }
}

// ── Planner / Tasks ──────────────────────────────────────────────────────────
@Composable
private fun TasksScreen(vm: AppViewModel) {
    observe(vm)
    var showAdd by remember { mutableStateOf(false) }
    val dayTasks = vm.tasksToday().sortedBy { vm.isTaskDone(it) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp)) }
            if (dayTasks.isEmpty()) item { Text("Nothing planned. Tap + to add a task.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(dayTasks) { TaskRow(vm, it) }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "Add task")
        }
    }
    if (showAdd) AddTaskDialog(onDismiss = { showAdd = false }, onSave = { title, cat, pri, rec ->
        vm.addTask(title, cat, pri, vm.today, null, rec, emptyList()); showAdd = false
    })
}

private val CATEGORIES = listOf("work", "study", "health", "personal", "other")
private val PRIORITIES = listOf("low", "medium", "high")
private val RECURRENCES = listOf("none", "daily", "weekly", "monthly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("other") }
    var priority by remember { mutableStateOf("medium") }
    var recurrence by remember { mutableStateOf("none") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Category", style = MaterialTheme.typography.labelMedium)
                ChipRow(CATEGORIES, category) { category = it }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                ChipRow(PRIORITIES, priority) { priority = it }
                Text("Repeat", style = MaterialTheme.typography.labelMedium)
                ChipRow(RECURRENCES, recurrence) { recurrence = it }
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onSave(title, category, priority, recurrence) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { opt ->
            FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) })
        }
    }
}

// ── Habits ─────────────────────────────────────────────────────────────────
@Composable
private fun HabitsScreen(vm: AppViewModel) {
    observe(vm)
    var showAdd by remember { mutableStateOf(false) }
    val habits = vm.activeHabits()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Habits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp)) }
            if (habits.isEmpty()) item { Text("No habits yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(habits) { h ->
                val s = vm.habitStats(h)
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (s.scheduledToday) Checkbox(checked = s.doneToday, onCheckedChange = { vm.toggleHabit(h) })
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.name, fontWeight = FontWeight.Medium)
                            Text("🔥 ${s.current}   ·   ${s.successPct}%   ·   best ${s.longest}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "Add habit")
        }
    }
    if (showAdd) AddHabitDialog(onDismiss = { showAdd = false }, onSave = { name, cat, freq, target ->
        vm.addHabit(name, cat, freq, target, emptyList()); showAdd = false
    })
}

private val FREQUENCIES = listOf("daily", "weekly", "custom")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("health") }
    var frequency by remember { mutableStateOf("daily") }
    var target by remember { mutableStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Habit name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Category", style = MaterialTheme.typography.labelMedium)
                ChipRow(CATEGORIES, category) { category = it }
                Text("Frequency", style = MaterialTheme.typography.labelMedium)
                ChipRow(FREQUENCIES, frequency) { frequency = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Target/day: $target", Modifier.weight(1f))
                    IconButton(onClick = { if (target > 1) target-- }) { Text("–", style = MaterialTheme.typography.titleLarge) }
                    IconButton(onClick = { target++ }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name, category, frequency, target) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Water ────────────────────────────────────────────────────────────────────
@Composable
private fun WaterScreen(vm: AppViewModel) {
    observe(vm)
    val settings = vm.water.value
    val goal = settings?.daily_goal_ml ?: 4000
    val glass = settings?.glass_size_ml ?: 250
    val total = vm.waterToday()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Water", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Ring(progress = if (goal > 0) total.toFloat() / goal else 0f, label = "${if (goal > 0) total * 100 / goal else 0}%", color = Brand.blue)
                Spacer(Modifier.height(10.dp))
                Text("${Logic.formatMl(total)} / ${Logic.formatMl(goal)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.logWater(glass) }, modifier = Modifier.weight(1f)) { Text("Glass · ${glass}ml") }
            OutlinedButton(onClick = { vm.logWater(500) }, modifier = Modifier.weight(1f)) { Text("+500ml") }
        }
        sectionTitle("Daily goal")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Logic.formatMl(goal), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { settings?.let { vm.saveWater(it.copy(daily_goal_ml = (goal - 250).coerceAtLeast(500))) } }) { Text("–250") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { settings?.let { vm.saveWater(it.copy(daily_goal_ml = (goal + 250).coerceAtMost(8000))) } }) { Text("+250") }
        }
        LinearProgressIndicator(progress = { if (goal > 0) (total.toFloat() / goal).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth(), color = Brand.blue)
    }
}
