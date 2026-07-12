package com.aria.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.app.data.AppViewModel
import com.aria.app.data.Habit
import com.aria.app.data.Logic
import com.aria.app.data.Task
import kotlin.math.roundToInt

// ── Root: tab scaffold + pushed-route overlay ──────────────────────────────
@Composable
fun MainRoot(vm: AppViewModel) {
    val nav = remember { Nav() }
    BackHandler(enabled = nav.stack.isNotEmpty()) { nav.pop() }
    Box(Modifier.fillMaxSize()) {
        MainScaffold(vm, nav)
        nav.top?.let { route ->
            Box(Modifier.fillMaxSize().background(LocalAria.current.background)) {
                RouteScreen(vm, nav, route)
            }
        }
    }
}

private enum class Tab(val label: String, val on: ImageVector, val off: ImageVector) {
    Today("Today", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarToday),
    Planner("Planner", Icons.Filled.DoneAll, Icons.Outlined.FormatListBulleted),
    Habits("Habits", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    Water("Water", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop),
    Stats("Stats", Icons.Filled.BarChart, Icons.Filled.BarChart),
}

@Composable
fun MainScaffold(vm: AppViewModel, nav: Nav) {
    val a = LocalAria.current
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    Scaffold(
        containerColor = a.background,
        bottomBar = {
            NavigationBar(containerColor = a.surface) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(if (tab == t) t.on else t.off, t.label, modifier = Modifier.size(22.dp)) },
                        label = { Text(t.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = a.primary,
                            selectedTextColor = a.primary,
                            unselectedIconColor = a.textMuted,
                            unselectedTextColor = a.textMuted,
                            indicatorColor = a.primarySoft,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Today -> DashboardScreen(vm, nav)
                Tab.Planner -> PlannerScreen(vm, nav)
                Tab.Habits -> HabitsScreen(vm, nav)
                Tab.Water -> WaterScreen(vm, nav)
                Tab.Stats -> StatsScreen(vm)
            }
        }
    }
}

private fun Modifier.screenScroll() = this

// ── Today / Dashboard ──────────────────────────────────────────────────────
@Composable
private fun DashboardScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    val today = vm.today
    val dayTasks = vm.tasksToday()
    val tasksDone = dayTasks.count { vm.isTaskDone(it) }
    val pending = dayTasks.filter { !vm.isTaskDone(it) }

    val habitRows = vm.activeHabits().map { it to vm.habitStats(it) }
    val scheduled = habitRows.filter { it.second.scheduledToday }
    val habitsDone = scheduled.count { it.second.doneToday }

    val goal = vm.water.value?.daily_goal_ml ?: 4000
    val waterMl = vm.waterToday()
    val glass = vm.water.value?.glass_size_ml ?: 250

    val upcoming = vm.activeReminders()
        .filter { it.is_enabled }
        .mapNotNull { r -> Logic.nextTriggerMillis(r)?.let { r to it } }
        .sortedBy { it.second }

    val score = Logic.productivityScore(
        if (dayTasks.isNotEmpty()) tasksDone.toDouble() / dayTasks.size else null,
        if (scheduled.isNotEmpty()) habitsDone.toDouble() / scheduled.size else null,
        if (goal > 0) waterMl.toDouble() / goal else null,
    )
    val scoreLabel = if (score >= 80) "On fire 🔥" else if (score >= 50) "Nice progress" else "Let's get going"

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Logic.prettyDate(today).uppercase(), color = a.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(Logic.greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = a.text)
            }
            IconChip(Icons.Outlined.Settings) { nav.push(Route.Settings) }
        }

        // Productivity score
        AriaCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Ring(progress = score / 100f, size = 92.dp, stroke = 9.dp, color = a.primary) {
                    Text("$score", color = a.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Productivity today", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(scoreLabel, color = a.textSecondary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        MiniStat(Icons.Filled.DoneAll, "$tasksDone/${dayTasks.size}", a.primary)
                        MiniStat(Icons.Filled.LocalFireDepartment, "$habitsDone/${scheduled.size}", a.warning)
                        MiniStat(Icons.Filled.WaterDrop, "${if (goal > 0) (waterMl * 100 / goal) else 0}%", a.info)
                    }
                }
            }
        }

        // Quick add
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAdd(Icons.Filled.Add, "Task", a.primary, Modifier.weight(1f)) { nav.push(Route.TaskForm(null, today)) }
            QuickAdd(Icons.Filled.LocalFireDepartment, "Habit", a.warning, Modifier.weight(1f)) { nav.push(Route.HabitForm(null)) }
            QuickAdd(Icons.Filled.WaterDrop, "Water", a.info, Modifier.weight(1f)) { vm.logWater(glass) }
            QuickAdd(Icons.Filled.Notifications, "Remind", a.accent, Modifier.weight(1f)) { nav.push(Route.ReminderForm(null)) }
        }

        // Tasks
        SectionHeader("Today's tasks", "$tasksDone/${dayTasks.size}")
        when {
            dayTasks.isEmpty() -> AriaCard { Text("No tasks today. Tap \"Task\" to plan your day.", color = a.textSecondary, fontSize = 14.sp) }
            pending.isEmpty() -> AriaCard { Text("All tasks done — great work! 🎉", color = a.success, fontSize = 14.sp) }
            else -> pending.forEach { TaskCard(vm, it, today) { nav.push(Route.TaskForm(it.id, today)) } }
        }

        // Habits
        if (scheduled.isNotEmpty()) {
            SectionHeader("Today's habits", "$habitsDone/${scheduled.size}")
            AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    scheduled.forEach { (h, st) ->
                        val color = h.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: a.category(h.category)
                        Row(Modifier.fillMaxWidth().clickable { nav.push(Route.HabitDetail(h.id)) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(h.name, color = a.text, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            if (st.current > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = a.warning, modifier = Modifier.size(13.dp))
                                Text("${st.current}", color = a.textSecondary, fontSize = 13.sp)
                            }
                            HabitToggle(vm, h, st, color)
                        }
                    }
                }
            }
        }

        // Water
        SectionHeader("Water", "${Logic.formatMl(waterMl)} / ${Logic.formatMl(goal)}")
        AriaCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Ring(progress = if (goal > 0) waterMl.toFloat() / goal else 0f, size = 64.dp, stroke = 7.dp, color = a.info) {
                    Icon(Icons.Filled.WaterDrop, null, tint = a.info, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AriaProgressBar(if (goal > 0) waterMl.toFloat() / goal else 0f, a.info)
                    Text(if (waterMl >= goal) "Goal reached 💧" else "${Logic.formatMl(maxOf(0, goal - waterMl))} to go", color = a.textSecondary, fontSize = 13.sp)
                }
                IconChip(Icons.Filled.Add, tint = a.info) { vm.logWater(glass) }
            }
        }

        // Upcoming reminders
        if (upcoming.isNotEmpty()) {
            SectionHeader("Upcoming reminders") { nav.push(Route.Reminders) }
            AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    upcoming.forEach { (r, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(a.accent))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = a.text, fontSize = 14.sp, maxLines = 1)
                                Text(Logic.reminderSummary(r), color = a.textMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, color: Color) {
    val a = LocalAria.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, color = a.textSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun QuickAdd(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val a = LocalAria.current
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SoftIconTile(icon, color)
        Text(label, color = a.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IconChip(icon: ImageVector, tint: Color? = null, onClick: () -> Unit) {
    val a = LocalAria.current
    Box(Modifier.size(40.dp).clip(CircleShape).background(a.surfaceAlt).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint ?: a.text, modifier = Modifier.size(20.dp))
    }
}

// ── Task card ──────────────────────────────────────────────────────────────
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun TaskCard(vm: AppViewModel, task: Task, date: String, onPress: () -> Unit) {
    val a = LocalAria.current
    val done = vm.isTaskDone(task, date)
    val catColor = a.category(task.category)
    val time = Logic.timeRange(task.start_time, task.end_time)
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(a.surface)
            .border(1.dp, a.border, RoundedCornerShape(16.dp)).clickable(onClick = onPress),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(a.priority(task.priority)))
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AriaCheckbox(done, catColor) { vm.toggleTask(task, date) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    task.title, color = if (done) a.textMuted else a.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                )
                task.description?.let { if (it.isNotBlank()) Text(it, color = a.textSecondary, fontSize = 13.sp, maxLines = 1) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (time != null) Chip(time, icon = Icons.Outlined.Schedule)
                    Chip(task.category.replaceFirstChar { it.uppercase() }, color = catColor)
                    if (task.recurrence != "none") Chip(Logic.recurrenceLabel(task), icon = Icons.Filled.Repeat)
                }
            }
        }
    }
}

// ── Planner ────────────────────────────────────────────────────────────────
@Composable
private fun PlannerScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    var selected by rememberSaveable { mutableStateOf(vm.today) }
    var mode by rememberSaveable { mutableStateOf("list") }
    val dayTasks = vm.tasksOn(selected)
    val done = dayTasks.count { vm.isTaskDone(it, selected) }
    val marked = vm.markedDates()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Planner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = a.text)
                Text(Logic.prettyDate(selected), color = a.textSecondary, fontSize = 14.sp)
            }
            DateStrip(selected, marked) { selected = it }
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dayTasks.isNotEmpty()) AriaCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("$done of ${dayTasks.size} done", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text("${if (dayTasks.isNotEmpty()) (done * 100 / dayTasks.size) else 0}%", color = a.textSecondary, fontSize = 14.sp)
                        }
                        AriaProgressBar(if (dayTasks.isNotEmpty()) done.toFloat() / dayTasks.size else 0f)
                    }
                }
                Segmented(listOf("list" to "List", "timeline" to "Timeline"), mode) { mode = it }
            }
            if (dayTasks.isEmpty()) {
                EmptyState(Icons.Filled.DoneAll, "Nothing planned", "Tap the + button to add your first task for this day.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mode == "list") {
                        dayTasks.sortedBy { vm.isTaskDone(it, selected) }.forEach { TaskCard(vm, it, selected) { nav.push(Route.TaskForm(it.id, selected)) } }
                    } else {
                        val timed = dayTasks.filter { it.start_time != null }.sortedBy { it.start_time }
                        val untimed = dayTasks.filter { it.start_time == null }
                        timed.forEach { t ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(Logic.timeRange(t.start_time, null) ?: "", color = a.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp).padding(top = 16.dp))
                                Box(Modifier.weight(1f)) { TaskCard(vm, t, selected) { nav.push(Route.TaskForm(t.id, selected)) } }
                            }
                        }
                        untimed.forEachIndexed { i, t ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(if (i == 0) "Anytime" else "", color = a.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp).padding(top = 16.dp))
                                Box(Modifier.weight(1f)) { TaskCard(vm, t, selected) { nav.push(Route.TaskForm(t.id, selected)) } }
                            }
                        }
                    }
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
        Fab { nav.push(Route.TaskForm(null, selected)) }
    }
}

@Composable
private fun DateStrip(selected: String, marked: Set<String>, onSelect: (String) -> Unit) {
    val a = LocalAria.current
    val today = Logic.today()
    val base = java.time.LocalDate.now()
    val days = (-7..30).map { base.plusDays(it.toLong()) }
    val dow = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, top = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { d ->
            val key = d.toString()
            val active = key == selected
            val isToday = key == today
            Column(
                Modifier.width(48.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (active) a.primary else a.surface)
                    .border(1.dp, if (active) a.primary else a.border, RoundedCornerShape(16.dp))
                    .clickable { onSelect(key) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(dow[d.dayOfWeek.value % 7], color = if (active) a.onPrimary else a.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("${d.dayOfMonth}", color = if (active) a.onPrimary else if (isToday) a.primary else a.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (marked.contains(key)) (if (active) a.onPrimary else a.primary) else Color.Transparent))
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun Fab(onClick: () -> Unit) {
    val a = LocalAria.current
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        FloatingActionButton(onClick = onClick, containerColor = a.primary, contentColor = a.onPrimary) {
            Icon(Icons.Filled.Add, "Add")
        }
    }
}

// ── Habits ─────────────────────────────────────────────────────────────────
@Composable
private fun HabitsScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    val today = vm.today
    val rows = vm.activeHabits().map { it to vm.habitStats(it) }
    val scheduled = rows.filter { it.second.scheduledToday }
    val doneCount = scheduled.count { it.second.doneToday }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Habits", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = a.text)
                Text(Logic.prettyDate(today), color = a.textSecondary, fontSize = 14.sp)
            }
            if (rows.isEmpty()) {
                EmptyState(Icons.Outlined.LocalFireDepartment, "Build your first habit", "Track daily routines like reading, workouts or coding — and watch your streaks grow.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (scheduled.isNotEmpty()) AriaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Today's habits", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                Text("$doneCount/${scheduled.size}", color = a.textSecondary, fontSize = 14.sp)
                            }
                            AriaProgressBar(if (scheduled.isNotEmpty()) doneCount.toFloat() / scheduled.size else 0f)
                        }
                    }
                    rows.forEach { (h, st) -> HabitCard(vm, h, st) { nav.push(Route.HabitDetail(h.id)) } }
                    Spacer(Modifier.height(72.dp))
                }
            }
        }
        Fab { nav.push(Route.HabitForm(null)) }
    }
}

@Composable
fun HabitCard(vm: AppViewModel, h: Habit, st: Logic.HabitStats, onPress: () -> Unit) {
    val a = LocalAria.current
    val color = h.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: a.category(h.category)
    val target = maxOf(1, h.target_count)
    AriaCard(onClick = onPress) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(h.name, color = a.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.Repeat, null, tint = a.textMuted, modifier = Modifier.size(12.dp))
                    Text(Logic.frequencyLabel(h), color = a.textSecondary, fontSize = 13.sp)
                    if (st.current > 0) {
                        Text("·", color = a.textMuted, fontSize = 13.sp)
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = a.warning, modifier = Modifier.size(12.dp))
                        Text("${st.current}", color = a.textSecondary, fontSize = 13.sp)
                    }
                    if (target > 1) {
                        Text("·", color = a.textMuted, fontSize = 13.sp)
                        Text("${st.todayCount}/$target", color = a.textSecondary, fontSize = 13.sp)
                    }
                }
            }
            if (st.scheduledToday) HabitToggle(vm, h, st, color)
            else Text("Rest day", color = a.textMuted, fontSize = 12.sp)
        }
    }
}

/** A single checkbox-style control. Each tap adds one toward the daily target
 *  (multi-target habits show their progress in the row's meta text); tapping when
 *  already full resets to 0. */
@Composable
fun HabitToggle(vm: AppViewModel, h: Habit, st: Logic.HabitStats, color: Color) {
    AriaCheckbox(st.doneToday, color) { vm.stepHabit(h) }
}

// ── Water ──────────────────────────────────────────────────────────────────
@Composable
private fun WaterScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    val goal = vm.water.value?.daily_goal_ml ?: 4000
    val glass = vm.water.value?.glass_size_ml ?: 250
    val total = vm.waterToday()
    val progress = if (goal > 0) total.toFloat() / goal else 0f
    val week = vm.waterWeek()
    val month = vm.waterMonth(goal)
    var customOpen by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Water", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = a.text)
                    Text("Hydration", color = a.textSecondary, fontSize = 14.sp)
                }
                IconChip(Icons.Outlined.Settings) { nav.push(Route.WaterSettings) }
            }
        }

        AriaCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Ring(progress = progress, size = 190.dp, stroke = 14.dp, color = a.info) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(progress * 100).roundToInt()}%", color = a.info, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${Logic.formatMl(total)} / ${Logic.formatMl(goal)}", color = a.textSecondary, fontSize = 15.sp)
                    }
                }
                Text(if (total >= goal) "Goal reached — nice work! 💧" else "${Logic.formatMl(maxOf(0, goal - total))} to go", color = a.textMuted, fontSize = 13.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Glass · ${glass}ml", Modifier.weight(1f), icon = Icons.Filled.WaterDrop) { vm.logWater(glass) }
            SecondaryButton("Custom", icon = Icons.Filled.Add) { customOpen = true }
        }
        GhostButton("Undo last glass") { vm.undoLastWater() }

        AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("This week", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("goal ${Logic.formatMl(goal)}/day", color = a.textSecondary, fontSize = 13.sp)
                }
                BarChart(week.map { ChartBar(it.label, it.ml.toFloat(), it.date == vm.today) }, a.info, goal.toFloat())
            }
        }

        FieldLabel("THIS MONTH")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { StatTile("Total", Logic.formatMl(month.total), Icons.Outlined.WaterDrop, a.info) }
            Box(Modifier.weight(1f)) { StatTile("Daily average", Logic.formatMl(month.average), Icons.Filled.BarChart, a.primary) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { StatTile("Days goal met", "${month.daysMetGoal}", Icons.Filled.EmojiEvents, a.success) }
            Box(Modifier.weight(1f)) { StatTile("Days tracked", "${month.daysTracked}", Icons.Filled.CalendarMonth, a.accent) }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (customOpen) CustomWaterDialog(glass, onDismiss = { customOpen = false }) { vm.logWater(it); customOpen = false }
}

// ── Stats / Analytics ────────────────────────────────────────────────────────
@Composable
private fun StatsScreen(vm: AppViewModel) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    var days by rememberSaveable { mutableStateOf(7) }
    val goal = vm.water.value?.daily_goal_ml ?: 4000
    val r = vm.analytics(days)
    val hasData = r.taskTotal > 0 || r.habitTotal > 0 || r.perDay.any { it.ml > 0 }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Stats", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = a.text)
            Text("Insights", color = a.textSecondary, fontSize = 14.sp)
        }
        Segmented(listOf(7 to "Week", 30 to "Month"), days) { days = it }

        if (!hasData) {
            EmptyState(Icons.Filled.BarChart, "No data yet", "Complete tasks, habits and log water to see your trends and summaries here.")
        } else {
            AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Productivity trend", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("last $days days", color = a.textSecondary, fontSize = 13.sp)
                    }
                    BarChart(r.perDay.map { ChartBar(it.label, it.score.toFloat(), it.date == vm.today) }, a.primary, 100f)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { StatTile("Task completion", "${r.taskRate}%", Icons.Filled.DoneAll, a.primary, "${r.taskDone}/${r.taskTotal} tasks") }
                Box(Modifier.weight(1f)) { StatTile("Habit completion", "${r.habitRate}%", Icons.Filled.LocalFireDepartment, a.warning, "${r.habitDone}/${r.habitTotal} done") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { StatTile("Water consistency", "${r.waterConsistency}%", Icons.Filled.WaterDrop, a.info, "${r.waterDaysMet} days met goal") }
                Box(Modifier.weight(1f)) { StatTile("Best day", r.bestDay?.let { "${it.score}" } ?: "—", Icons.Filled.EmojiEvents, a.success, r.bestDay?.let { Logic.shortDate(it.date) } ?: "No data") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { StatTile("Top current streak", "${r.topCurrentStreak}", Icons.Filled.LocalFireDepartment, a.warning, "days") }
                Box(Modifier.weight(1f)) { StatTile("Longest streak", "${r.topLongestStreak}", Icons.Filled.EmojiEvents, a.accent, "days") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { StatTile("Tasks completed", "${r.taskDone}", Icons.Filled.CheckCircle, a.success) }
                Box(Modifier.weight(1f)) { StatTile("Tasks missed", "${r.taskMissed}", Icons.Outlined.ErrorOutline, a.danger) }
            }
            AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Water intake", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("goal ${Logic.formatMl(goal)}/day", color = a.textSecondary, fontSize = 13.sp)
                    }
                    BarChart(r.perDay.map { ChartBar(it.label, it.ml.toFloat(), it.date == vm.today) }, a.info, goal.toFloat())
                }
            }
            if (r.habitBreakdown.isNotEmpty()) AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Habit progress", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    r.habitBreakdown.forEach { h ->
                        val color = h.color?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: a.primary
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(h.name, color = a.text, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                Text("${h.rate}% · 🔥 ${h.current}", color = a.textSecondary, fontSize = 13.sp)
                            }
                            AriaProgressBar(h.rate / 100f, color)
                        }
                    }
                }
            }
            if (r.missedTasks.isNotEmpty()) AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Missed tasks", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    r.missedTasks.forEach { m ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(m.title, color = a.text, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(Logic.monthDay(m.date), color = a.textMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
