package com.aria.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.aria.app.data.AppViewModel
import com.aria.app.data.HabitInput
import com.aria.app.data.Logic
import com.aria.app.data.ReminderInput
import com.aria.app.data.TaskInput
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

// ── Route dispatcher ──────────────────────────────────────────────────────
@Composable
fun RouteScreen(vm: AppViewModel, nav: Nav, route: Route) {
    when (route) {
        is Route.Settings -> SettingsScreen(vm, nav)
        is Route.Reminders -> RemindersScreen(vm, nav)
        is Route.WaterSettings -> WaterSettingsScreen(vm, nav)
        is Route.TaskForm -> TaskFormScreen(vm, nav, route.id, route.date)
        is Route.HabitForm -> HabitFormScreen(vm, nav, route.id)
        is Route.HabitDetail -> HabitDetailScreen(vm, nav, route.id)
        is Route.ReminderForm -> ReminderFormScreen(vm, nav, route.id)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────
private val CATEGORIES = listOf("work" to "Work", "study" to "Study", "health" to "Health", "personal" to "Personal", "other" to "Other")
private val PRIORITIES = listOf("low" to "Low", "medium" to "Medium", "high" to "High")

@Composable
private fun FormTopBar(title: String, onCancel: () -> Unit, onSave: (() -> Unit)? = null) {
    val a = LocalAria.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GhostButton("Cancel", onClick = onCancel)
        Text(title, color = a.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (onSave != null) PrimaryButton("Save", onClick = onSave) else Spacer(Modifier.width(72.dp))
    }
}

@Composable
private fun AriaTextField(label: String, value: String, onChange: (String) -> Unit, placeholder: String = "", error: String? = null, multiline: Boolean = false, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = !multiline,
        isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DateField(label: String, value: LocalDate, onChange: (LocalDate) -> Unit) {
    val ctx = LocalContext.current
    val a = LocalAria.current
    FieldBox(label, Logic.prettyDate(value.toString()), Icons.Filled.CalendarMonth) {
        DatePickerDialog(ctx, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d)) }, value.year, value.monthValue - 1, value.dayOfMonth).show()
    }
}

@Composable
private fun TimeField(label: String, value: LocalTime?, placeholder: String = "Not set", icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Schedule, onChange: (LocalTime?) -> Unit) {
    val ctx = LocalContext.current
    val display = value?.let { Logic.fmtTime("%02d:%02d".format(it.hour, it.minute)) } ?: placeholder
    FieldBox(label, display, icon) {
        val init = value ?: LocalTime.of(9, 0)
        TimePickerDialog(ctx, { _, h, m -> onChange(LocalTime.of(h, m)) }, init.hour, init.minute, false).show()
    }
}

@Composable
private fun FieldBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val a = LocalAria.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = a.textSecondary, fontSize = 13.sp)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(a.surfaceAlt).clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = a.textMuted, modifier = Modifier.size(18.dp))
            Text(value, color = a.text, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FormLabelBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        content()
    }
}

private fun toColor(hex: String?): Color? = hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
private fun combineIso(dateKey: String, t: LocalTime): String =
    LocalDate.parse(dateKey).atTime(t.hour, t.minute).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
private fun parseTime(iso: String?): LocalTime? = iso?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalTime() }.getOrNull() }
private fun parseHm(hm: String?): LocalTime? = hm?.let { val p = it.split(":"); runCatching { LocalTime.of(p[0].toInt(), p.getOrElse(1){"0"}.toInt()) }.getOrNull() }

// ── Task form ──────────────────────────────────────────────────────────────
@Composable
private fun TaskFormScreen(vm: AppViewModel, nav: Nav, id: String?, date: String) {
    val a = LocalAria.current
    val existing = id?.let { vm.taskById(it) }
    val anchor = existing?.due_date ?: date

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "other") }
    var priority by remember { mutableStateOf(existing?.priority ?: "medium") }
    var due by remember { mutableStateOf(LocalDate.parse(anchor)) }
    var start by remember { mutableStateOf(parseTime(existing?.start_time)) }
    var end by remember { mutableStateOf(parseTime(existing?.end_time)) }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: "none") }
    var interval by remember { mutableStateOf(existing?.recurrence_interval ?: 2) }
    var days by remember { mutableStateOf(existing?.recurrence_days ?: emptyList()) }
    var until by remember { mutableStateOf(existing?.recurrence_end_date?.let { LocalDate.parse(it) }) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (title.isBlank()) { error = "Please enter a title."; return }
        val key = due.toString()
        vm.saveTask(existing, TaskInput(
            title = title, description = description, category = category, priority = priority,
            startTime = start?.let { combineIso(key, it) }, endTime = end?.let { combineIso(key, it) },
            dueDate = key, recurrence = recurrence,
            recurrenceInterval = if (recurrence == "custom") maxOf(1, interval) else 1,
            recurrenceDays = if (recurrence == "weekly") days else emptyList(),
            recurrenceEndDate = if (recurrence != "none") until?.toString() else null,
        ))
        nav.pop()
    }

    FormScaffold {
        FormTopBar(if (existing != null) "Edit task" else "New task", { nav.pop() }, ::save)
        AriaTextField("Title", title, { title = it }, "What do you need to do?", error)
        AriaTextField("Description", description, { description = it }, "Optional details", multiline = true)
        FormLabelBlock("CATEGORY") { ChipSelect(CATEGORIES, category, CATEGORIES.associate { it.first to a.category(it.first) }) { category = it } }
        FormLabelBlock("PRIORITY") { Segmented(PRIORITIES, priority) { priority = it } }
        FormLabelBlock("SCHEDULE") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Date", due) { due = it }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { TimeField("Start", start) { start = it } }
                    Box(Modifier.weight(1f)) { TimeField("End", end) { end = it } }
                }
            }
        }
        FormLabelBlock("REPEAT") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipSelect(listOf("none" to "One-time", "daily" to "Daily", "weekly" to "Weekly", "monthly" to "Monthly", "custom" to "Custom"), recurrence) { recurrence = it }
                if (recurrence == "weekly") WeekdayPicker(days) { days = it }
                if (recurrence == "custom") Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Every", color = a.textSecondary)
                    Stepper(interval, { interval = it }, min = 1, max = 90, suffix = if (interval > 1) "days" else "day")
                }
                if (recurrence != "none") DateField("Repeat until (optional)", until ?: LocalDate.now()) { until = it }
            }
        }
        if (existing != null) GhostButton("Delete task", icon = Icons.Filled.DeleteOutline, tint = a.danger) { vm.deleteTask(existing.id); nav.pop() }
        Spacer(Modifier.height(12.dp))
    }
}

// ── Habit form ─────────────────────────────────────────────────────────────
private val COLOR_PRESETS = listOf("#6366F1", "#8B5CF6", "#10B981", "#F59E0B", "#EF4444", "#3B82F6", "#EC4899")

@Composable
private fun HabitFormScreen(vm: AppViewModel, nav: Nav, id: String?) {
    val a = LocalAria.current
    val existing = id?.let { vm.habitById(it) }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var kind by remember { mutableStateOf(existing?.kind ?: "build") }
    var category by remember { mutableStateOf(existing?.category ?: "health") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: "daily") }
    var days by remember { mutableStateOf(existing?.custom_days ?: emptyList()) }
    var target by remember { mutableStateOf(existing?.target_count ?: 1) }
    var color by remember { mutableStateOf(existing?.color) }
    var startDate by remember { mutableStateOf(existing?.start_date?.let { LocalDate.parse(it) } ?: LocalDate.now()) }
    var reminder by remember { mutableStateOf(parseHm(existing?.reminder_time)) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (name.isBlank()) { error = "Please name your habit."; return }
        vm.saveHabit(existing, HabitInput(
            name = name, kind = kind, category = category, frequency = frequency, targetCount = target,
            customDays = if (frequency == "daily") emptyList() else days,
            reminderTime = reminder?.let { "%02d:%02d".format(it.hour, it.minute) },
            startDate = startDate.toString(), notes = notes, color = color,
        ))
        nav.pop()
    }

    FormScaffold {
        FormTopBar(if (existing != null) "Edit habit" else "New habit", { nav.pop() }, ::save)
        AriaTextField("Habit name", name, { name = it }, "e.g. Read 20 pages", error)
        FormLabelBlock("TYPE") { Segmented(listOf("build" to "Build", "quit" to "Quit"), kind) { kind = it } }
        FormLabelBlock("CATEGORY") { ChipSelect(CATEGORIES, category, CATEGORIES.associate { it.first to a.category(it.first) }) { category = it } }
        FormLabelBlock("FREQUENCY") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(listOf("daily" to "Daily", "weekly" to "Weekly", "custom" to "Custom"), frequency) { frequency = it }
                if (frequency != "daily") WeekdayPicker(days) { days = it }
            }
        }
        FormLabelBlock("TARGET PER DAY") { Stepper(target, { target = it }, min = 1, max = 50, suffix = if (target > 1) "times" else "time") }
        FormLabelBlock("COLOR") {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                COLOR_PRESETS.forEach { c ->
                    val col = toColor(c)!!
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(col)
                            .border(2.dp, if (color == c) a.text else Color.Transparent, CircleShape)
                            .clickable { color = c },
                    )
                }
                Box(
                    Modifier.size(36.dp).clip(CircleShape).border(2.dp, if (color == null) a.text else a.border, CircleShape).clickable { color = null },
                    contentAlignment = Alignment.Center,
                ) { Text("Auto", color = a.textMuted, fontSize = 10.sp) }
            }
        }
        FormLabelBlock("SCHEDULE") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("Start date", startDate) { startDate = it }
                TimeField("Reminder time (optional)", reminder, icon = Icons.Filled.Notifications) { reminder = it }
            }
        }
        AriaTextField("Notes", notes, { notes = it }, "Optional", multiline = true)
        if (existing != null) GhostButton("Delete habit", icon = Icons.Filled.DeleteOutline, tint = a.danger) { vm.deleteHabit(existing.id); nav.pop() }
        Spacer(Modifier.height(12.dp))
    }
}

// ── Habit detail ─────────────────────────────────────────────────────────────
@Composable
private fun HabitDetailScreen(vm: AppViewModel, nav: Nav, id: String) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    val today = vm.today
    val habit = vm.habitById(id)
    if (habit == null || habit.deleted_at != null) {
        FormScaffold {
            Row(Modifier.fillMaxWidth()) { IconChip(Icons.AutoMirrored.Filled.ArrowBack) { nav.pop() } }
            EmptyState(Icons.Outlined.ErrorOutline, "Habit not found")
        }
        return
    }
    val counts = vm.habitCounts(id)
    val st = Logic.computeHabitStats(habit, counts, today)
    val color = toColor(habit.color) ?: a.category(habit.category)
    val target = maxOf(1, habit.target_count)

    FormScaffold {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconChip(Icons.AutoMirrored.Filled.ArrowBack) { nav.pop() }
            Spacer(Modifier.weight(1f))
            IconChip(Icons.Filled.Edit) { nav.replaceTop(Route.HabitForm(habit.id)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(habit.name, color = a.text, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("${Logic.frequencyLabel(habit)} · target $target/day", color = a.textSecondary, fontSize = 15.sp)
        }
        AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldLabel("TODAY")
                when {
                    !st.scheduledToday -> Text("Not scheduled today — enjoy your rest day.", color = a.textSecondary)
                    target == 1 -> {
                        if (st.doneToday) SecondaryButton("Completed ✓", Modifier.fillMaxWidth(), icon = Icons.Filled.CheckCircle) { vm.toggleHabit(habit) }
                        else PrimaryButton("Mark as done", Modifier.fillMaxWidth(), icon = Icons.Outlined.RadioButtonUnchecked) { vm.toggleHabit(habit) }
                    }
                    else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${st.todayCount} / $target", color = a.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(if (st.doneToday) "Goal reached 🎉" else "Keep going", color = a.textSecondary, fontSize = 13.sp)
                        }
                        Stepper(st.todayCount, { vm.adjustHabit(habit, it - st.todayCount) }, min = 0, max = target * 4)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { StatTile("Current streak", "${st.current}", Icons.Filled.CalendarMonth, a.warning, "days") }
            Box(Modifier.weight(1f)) { StatTile("Longest streak", "${st.longest}", Icons.Filled.CheckCircle, a.primary, "days") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { StatTile("Total done", "${st.totalCompleted}", Icons.Filled.CheckCircle, a.success) }
            Box(Modifier.weight(1f)) { StatTile("Success rate", "${st.successPct}%", Icons.Filled.CheckCircle, a.accent) }
        }
        AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProgressRow("This week", st.weekDone, st.weekTotal, color)
                ProgressRow("This month", st.monthDone, st.monthTotal, color)
            }
        }
        AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Completion calendar", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                HabitMonthCalendar(habit, counts, color, today)
            }
        }
        if (!habit.notes.isNullOrBlank()) AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel("NOTES")
                Text(habit.notes!!, color = a.textSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ProgressRow(label: String, done: Int, total: Int, color: Color) {
    val a = LocalAria.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = a.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("$done/$total", color = a.textSecondary, fontSize = 14.sp)
        }
        AriaProgressBar(if (total > 0) done.toFloat() / total else 0f, color)
    }
}

private val WEEKDAY_INITIALS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
private fun HabitMonthCalendar(habit: com.aria.app.data.Habit, counts: Map<String, Int>, color: Color, today: String) {
    val a = LocalAria.current
    val now = remember { LocalDate.now() }
    var year by rememberSaveable { mutableStateOf(now.year) }
    var month by rememberSaveable { mutableStateOf(now.monthValue) }
    var monthMenu by remember { mutableStateOf(false) }
    var yearMenu by remember { mutableStateOf(false) }

    fun shift(delta: Int) {
        val d = LocalDate.of(year, month, 1).plusMonths(delta.toLong())
        year = d.year; month = d.monthValue
    }
    val atCurrent = year > now.year || (year == now.year && month >= now.monthValue)
    val grid = Logic.monthGrid(habit, counts, year, month, today)
    val years = (now.year - 10..now.year).toList().reversed()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header: year at the left, month centered with prev/next arrows.
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.align(Alignment.CenterStart)) {
                Text(
                    "$year", color = a.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { yearMenu = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                    years.forEach { y ->
                        DropdownMenuItem(text = { Text("$y") }, onClick = { year = y; yearMenu = false })
                    }
                }
            }
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(32.dp).clip(CircleShape).clickable { shift(-1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = a.text, modifier = Modifier.size(22.dp))
                }
                Box {
                    Text(
                        Logic.monthName(month), color = a.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { monthMenu = true }.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }) {
                        Logic.MONTH_FULL.forEachIndexed { i, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { month = i + 1; monthMenu = false })
                        }
                    }
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable(enabled = !atCurrent) { shift(1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.ChevronRight, "Next month", tint = if (atCurrent) a.textMuted else a.text, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Grid: weekday initials down the left, one column per week of the month.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WEEKDAY_INITIALS.forEach { letter ->
                    Box(Modifier.size(width = 14.dp, height = 26.dp), contentAlignment = Alignment.Center) {
                        Text(letter, color = a.textMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            grid.forEach { week ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    for (wd in 0 until 7) {
                        val cell = week.getOrNull(wd)
                        if (cell == null) {
                            Box(Modifier.size(26.dp))
                        } else {
                            val bg = when (cell.status) {
                                Logic.DayStatus.COMPLETED -> color
                                Logic.DayStatus.MISSED -> a.dangerSoft
                                Logic.DayStatus.PENDING -> a.surface
                                Logic.DayStatus.OFF -> a.track
                                Logic.DayStatus.FUTURE -> a.surfaceAlt
                            }
                            val fg = if (cell.status == Logic.DayStatus.COMPLETED) a.onPrimary else a.textMuted
                            Box(
                                Modifier.size(26.dp).clip(RoundedCornerShape(6.dp)).background(bg)
                                    .then(if (cell.status == Logic.DayStatus.PENDING) Modifier.border(1.5.dp, color, RoundedCornerShape(6.dp)) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${cell.date.substring(8).trimStart('0')}", color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendDot(color, "Done"); LegendDot(a.dangerSoft, "Missed"); LegendDot(a.track, "Off")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val a = LocalAria.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, color = a.textMuted, fontSize = 12.sp)
    }
}

// ── Reminders ─────────────────────────────────────────────────────────────
@Composable
private fun RemindersScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    var tab by remember { mutableStateOf("reminders") }
    val reminders = vm.activeReminders()
    val history = vm.history.value.filter { it.deleted_at == null }.sortedByDescending { it.fired_at }.take(100)
    val statusColor = mapOf("delivered" to a.info, "done" to a.success, "snoozed" to a.warning, "dismissed" to a.textMuted)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPaddingCompat()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconChip(Icons.AutoMirrored.Filled.ArrowBack) { nav.pop() }
                Text("Reminders", color = a.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                Spacer(Modifier.width(40.dp))
            }
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Segmented(listOf("reminders" to "Reminders", "history" to "History"), tab) { tab = it }
            }
            if (tab == "reminders") {
                if (reminders.isEmpty()) EmptyState(Icons.Outlined.Notifications, "No reminders yet", "Add one-time, daily, weekly or interval reminders. They fire on your device, even offline.")
                else Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    reminders.forEach { r ->
                        AriaCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(r.title, color = if (r.is_enabled) a.text else a.textMuted, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(Icons.Outlined.Repeat, null, tint = a.textMuted, modifier = Modifier.size(13.dp))
                                        Text(Logic.reminderSummary(r), color = a.textSecondary, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconChip(Icons.Filled.Edit) { nav.push(Route.ReminderForm(r.id)) }
                                        IconChip(Icons.Filled.Schedule) { vm.snoozeReminder(r.id, 10) }
                                        IconChip(Icons.Outlined.CheckCircle, tint = a.success) { vm.markReminderDone(r.id) }
                                    }
                                }
                                Switch(checked = r.is_enabled, onCheckedChange = { vm.toggleReminder(r.id) }, colors = SwitchDefaults.colors(checkedTrackColor = a.primary))
                            }
                        }
                    }
                    Spacer(Modifier.height(72.dp))
                }
            } else {
                if (history.isEmpty()) EmptyState(Icons.Filled.Schedule, "No notification history", "Reminders you receive will be listed here.")
                else Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    history.forEach { h ->
                        AriaCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(h.title, color = a.text, fontSize = 14.sp)
                                    Text(Logic.fmtDateTime(h.fired_at), color = a.textMuted, fontSize = 12.sp)
                                }
                                Chip(h.status, color = statusColor[h.status] ?: a.info)
                            }
                        }
                    }
                }
            }
        }
        if (tab == "reminders") Fab { nav.push(Route.ReminderForm(null)) }
    }
}

// ── Reminder form ────────────────────────────────────────────────────────────
@Composable
private fun ReminderFormScreen(vm: AppViewModel, nav: Nav, id: String?) {
    val a = LocalAria.current
    val existing = id?.let { vm.reminderById(it) }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var body by remember { mutableStateOf(existing?.body ?: "") }
    var repeat by remember { mutableStateOf(existing?.repeat ?: "once") }
    var date by remember { mutableStateOf(existing?.next_trigger_at?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull() } ?: LocalDate.now()) }
    var time by remember { mutableStateOf(parseHm(existing?.time_of_day) ?: parseTime(existing?.next_trigger_at) ?: LocalTime.now().plusHours(1).withMinute(0)) }
    var days by remember { mutableStateOf(existing?.repeat_days ?: emptyList()) }
    var interval by remember { mutableStateOf(existing?.interval_min ?: 60) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (title.isBlank()) { error = "Please enter a title."; return }
        val hm = "%02d:%02d".format(time.hour, time.minute)
        vm.saveReminder(existing, ReminderInput(
            title = title, body = body, repeat = repeat,
            timeOfDay = if (repeat == "daily" || repeat == "weekly") hm else null,
            repeatDays = if (repeat == "weekly") days else emptyList(),
            intervalMin = if (repeat == "interval") maxOf(15, interval) else null,
            nextTriggerAt = if (repeat == "once") combineIso(date.toString(), time) else null,
        ))
        nav.pop()
    }

    FormScaffold {
        FormTopBar(if (existing != null) "Edit reminder" else "New reminder", { nav.pop() }, ::save)
        AriaTextField("Title", title, { title = it }, "What should I remind you about?", error)
        AriaTextField("Note (optional)", body, { body = it }, "Extra details")
        FormLabelBlock("REPEAT") {
            Segmented(listOf("once" to "Once", "daily" to "Daily", "weekly" to "Weekly", "interval" to "Interval"), repeat) { repeat = it }
        }
        when (repeat) {
            "once" -> FormLabelBlock("WHEN") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { DateField("Date", date) { date = it } }
                    Box(Modifier.weight(1f)) { TimeField("Time", time) { it?.let { t -> time = t } } }
                }
            }
            "daily" -> FormLabelBlock("TIME") { TimeField("Time", time) { it?.let { t -> time = t } } }
            "weekly" -> FormLabelBlock("DAYS & TIME") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WeekdayPicker(days) { days = it }
                    TimeField("Time", time) { it?.let { t -> time = t } }
                }
            }
            "interval" -> FormLabelBlock("EVERY") { Stepper(interval, { interval = it }, min = 15, max = 480, step = 15, suffix = "min") }
        }
        if (existing != null) GhostButton("Delete reminder", icon = Icons.Filled.DeleteOutline, tint = a.danger) { vm.deleteReminder(existing.id); nav.pop() }
        Spacer(Modifier.height(12.dp))
    }
}

// ── Water settings ────────────────────────────────────────────────────────────
@Composable
private fun WaterSettingsScreen(vm: AppViewModel, nav: Nav) {
    vm.dataRev.collectAsState().value
    val a = LocalAria.current
    val s = vm.water.value ?: return

    FormScaffold {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Water settings", color = a.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            PrimaryButton("Done") { nav.pop() }
        }
        SettingRowCard("Daily goal", Logic.formatMl(s.daily_goal_ml)) {
            Stepper(s.daily_goal_ml, { vm.saveWater(s.copy(daily_goal_ml = it)) }, min = 500, max = 8000, step = 250)
        }
        SettingRowCard("Glass size", "One-tap add amount") {
            Stepper(s.glass_size_ml, { vm.saveWater(s.copy(glass_size_ml = it)) }, min = 50, max = 1000, step = 50)
        }
        FieldLabel("REMINDERS")
        SettingRowCard("Remind me to drink", "Schedules repeating notifications") {
            Switch(checked = s.reminder_enabled, onCheckedChange = { vm.saveWater(s.copy(reminder_enabled = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = a.primary))
        }
        if (s.reminder_enabled) {
            SettingRowCard("Every", "Reminder interval") {
                Stepper(s.reminder_interval_min, { vm.saveWater(s.copy(reminder_interval_min = it)) }, min = 15, max = 240, step = 15, suffix = "min")
            }
            AriaCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Active hours", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { TimeField("From", parseHm(s.active_start)) { it?.let { t -> vm.saveWater(s.copy(active_start = "%02d:%02d".format(t.hour, t.minute))) } } }
                        Box(Modifier.weight(1f)) { TimeField("Until", parseHm(s.active_end)) { it?.let { t -> vm.saveWater(s.copy(active_end = "%02d:%02d".format(t.hour, t.minute))) } } }
                    }
                }
            }
        }
        Text("Reminders fire on your device even offline. Manage all reminders in the Reminders area.", color = a.textMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
    }
}

@Composable
private fun SettingRowCard(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    val a = LocalAria.current
    AriaCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(subtitle, color = a.textSecondary, fontSize = 13.sp)
            }
            trailing()
        }
    }
}

// ── Settings ─────────────────────────────────────────────────────────────────
@Composable
private fun SettingsScreen(vm: AppViewModel, nav: Nav) {
    val a = LocalAria.current
    val mode by vm.themeMode.collectAsState()
    val email by vm.email.collectAsState()

    FormScaffold {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", color = a.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconChip(Icons.Filled.Close) { nav.pop() }
        }
        FieldLabel("APPEARANCE")
        AriaCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Theme", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Choose how Aria looks. \"System\" follows your device.", color = a.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                Segmented(listOf("system" to "System", "light" to "Light", "dark" to "Dark"), mode) { vm.setThemeMode(it) }
            }
        }
        FieldLabel("NOTIFICATIONS")
        AriaCard(onClick = { nav.replaceTop(Route.Reminders) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(a.primarySoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Notifications, null, tint = a.primary)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reminders", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Manage reminders & notification history", color = a.textSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = a.textMuted)
            }
        }
        FieldLabel("ACCOUNT")
        AriaCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(a.primarySoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, null, tint = a.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(email ?: "Signed in", color = a.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Synced across your devices", color = a.textSecondary, fontSize = 13.sp)
                    }
                }
                GhostButton("Sign out", Modifier.padding(top = 14.dp), icon = Icons.AutoMirrored.Filled.Logout) { vm.signOut() }
            }
        }
        FieldLabel("ABOUT")
        AriaCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Aria", color = a.text, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("v1.0.0", color = a.textSecondary, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ── Custom water dialog ──────────────────────────────────────────────────────
@Composable
fun CustomWaterDialog(initial: Int, onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    val a = LocalAria.current
    var ml by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add water") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose an amount in millilitres.", color = a.textSecondary, fontSize = 13.sp)
                Stepper(ml, { ml = it }, min = 50, max = 2000, step = 50, suffix = "ml")
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(ml) }) { Text("Add ${ml}ml") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Scrollable form container ────────────────────────────────────────────────
@Composable
private fun FormScaffold(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPaddingCompat().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

private fun Modifier.statusBarsPaddingCompat(): Modifier = this.statusBarsPadding()
