package com.aria.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Pure logic ported from the Expo app (recurrence / streaks / water / score). */
object Logic {

    fun today(): String = LocalDate.now().toString() // ISO yyyy-MM-dd

    private val MONTHS = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    private val DOW_FULL = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    private val DOW_SHORT = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    fun greeting(): String {
        val h = java.time.LocalTime.now().hour
        return when { h < 12 -> "Good morning"; h < 17 -> "Good afternoon"; else -> "Good evening" }
    }

    /** e.g. "Tuesday, Jul 12" */
    fun prettyDate(key: String): String = runCatching {
        val d = LocalDate.parse(key)
        "${DOW_FULL[weekday(d)]}, ${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}"
    }.getOrElse { key }

    /** e.g. "Sat, Jul 12" */
    fun shortDate(key: String): String = runCatching {
        val d = LocalDate.parse(key)
        "${DOW_SHORT[weekday(d)]}, ${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}"
    }.getOrElse { key }

    /** e.g. "Jul 12" */
    fun monthDay(key: String): String = runCatching {
        val d = LocalDate.parse(key)
        "${MONTHS[d.monthValue - 1]} ${d.dayOfMonth}"
    }.getOrElse { key }

    /** Format a start/end ISO timestamp range like "3:00 PM – 4:00 PM". */
    fun timeRange(startIso: String?, endIso: String?): String? {
        if (startIso == null) return null
        fun fmt(iso: String): String? = runCatching {
            val t = java.time.OffsetDateTime.parse(iso).toLocalTime()
            val h = t.hour; val ampm = if (h < 12) "AM" else "PM"
            val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
            "%d:%02d %s".format(h12, t.minute, ampm)
        }.getOrNull()
        val s = fmt(startIso) ?: return null
        val e = endIso?.let { fmt(it) }
        return if (e != null) "$s – $e" else s
    }

    /** Minutes since midnight for a stored start_time ISO — for timeline sorting
     *  by time of day (the ISO also carries a date, which must be ignored). */
    fun minuteOfDay(iso: String?): Int = iso?.let {
        runCatching { val t = java.time.OffsetDateTime.parse(it).toLocalTime(); t.hour * 60 + t.minute }.getOrNull()
    } ?: Int.MAX_VALUE

    /** 0 = Sunday … 6 = Saturday */
    private fun weekday(d: LocalDate): Int = d.dayOfWeek.value % 7

    // ── Tasks / recurrence ────────────────────────────────────────────────
    private fun anchor(t: Task): String = t.due_date ?: t.created_at.take(10)

    fun taskOccursOn(t: Task, key: String): Boolean {
        if (t.deleted_at != null) return false
        val a = anchor(t)
        if (t.recurrence == "none") return key == a
        if (key < a) return false
        t.recurrence_end_date?.let { if (key > it) return false }
        val ad = LocalDate.parse(a)
        val dd = LocalDate.parse(key)
        return when (t.recurrence) {
            "daily" -> true
            "weekly" -> {
                val days = t.recurrence_days.ifEmpty { listOf(weekday(ad)) }
                weekday(dd) in days
            }
            "monthly" -> ad.dayOfMonth == dd.dayOfMonth
            "custom" -> {
                val interval = maxOf(1, t.recurrence_interval)
                (ChronoUnit.DAYS.between(ad, dd).toInt() % interval) == 0
            }
            else -> false
        }
    }

    // ── Habits / streaks ──────────────────────────────────────────────────
    fun isScheduledOn(h: Habit, key: String): Boolean {
        if (key < h.start_date) return false
        if (h.frequency == "daily") return true
        val d = LocalDate.parse(key)
        val days = h.custom_days.ifEmpty { listOf(weekday(LocalDate.parse(h.start_date))) }
        return weekday(d) in days
    }

    data class HabitStats(
        val current: Int = 0,
        val longest: Int = 0,
        val totalCompleted: Int = 0,
        val missed: Int = 0,
        val successPct: Int = 0,
        val weekDone: Int = 0,
        val weekTotal: Int = 0,
        val monthDone: Int = 0,
        val monthTotal: Int = 0,
        val doneToday: Boolean = false,
        val scheduledToday: Boolean = false,
        val todayCount: Int = 0,
    )

    fun computeHabitStats(h: Habit, counts: Map<String, Int>, today: String = today()): HabitStats {
        val target = maxOf(1, h.target_count)
        fun done(k: String) = (counts[k] ?: 0) >= target
        if (h.start_date > today) return HabitStats(todayCount = counts[today] ?: 0)

        val end = LocalDate.parse(today)
        val weekStart = end.minusDays((end.dayOfWeek.value % 7).toLong()).toString()
        val monthStart = end.withDayOfMonth(1).toString()

        var cur = LocalDate.parse(h.start_date)
        val minStart = end.minusDays(730)
        if (cur.isBefore(minStart)) cur = minStart

        val scheduled = ArrayList<Pair<String, Boolean>>()
        var scheduledPast = 0
        var completedPast = 0
        var totalCompleted = 0
        var missed = 0
        var weekDone = 0
        var weekTotal = 0
        var monthDone = 0
        var monthTotal = 0
        var scheduledToday = false
        var doneToday = false

        while (!cur.isAfter(end)) {
            val key = cur.toString()
            if (isScheduledOn(h, key)) {
                val d = done(key)
                scheduled.add(key to d)
                if (d) totalCompleted++
                if (key < today) {
                    scheduledPast++
                    if (d) completedPast++ else missed++
                } else {
                    scheduledToday = true
                    doneToday = d
                }
                if (key >= weekStart) {
                    weekTotal++
                    if (d) weekDone++
                }
                if (key >= monthStart) {
                    monthTotal++
                    if (d) monthDone++
                }
            }
            cur = cur.plusDays(1)
        }

        var run = 0
        var longest = 0
        for ((_, d) in scheduled) {
            if (d) {
                run++
                if (run > longest) longest = run
            } else run = 0
        }

        val arr = scheduled.toMutableList()
        if (arr.isNotEmpty() && arr.last().first == today && !arr.last().second) arr.removeAt(arr.size - 1)
        var current = 0
        for (i in arr.indices.reversed()) {
            if (arr[i].second) current++ else break
        }

        val successPct = when {
            scheduledPast > 0 -> (completedPast * 100f / scheduledPast).roundToInt()
            doneToday -> 100
            else -> 0
        }

        return HabitStats(
            current, longest, totalCompleted, missed, successPct,
            weekDone, weekTotal, monthDone, monthTotal, doneToday, scheduledToday, counts[today] ?: 0,
        )
    }

    // ── Water / score ─────────────────────────────────────────────────────
    fun waterTotal(logs: List<WaterLog>, key: String): Int =
        logs.filter { it.deleted_at == null && it.log_date == key }.sumOf { it.amount_ml }

    fun formatMl(ml: Int): String =
        if (ml >= 1000) {
            val l = ml / 1000.0
            if (ml % 1000 == 0) "${l.toInt()} L" else String.format("%.1f L", l)
        } else "$ml ml"

    fun productivityScore(taskRate: Double?, habitRate: Double?, waterRate: Double?): Int {
        val v = listOfNotNull(taskRate, habitRate, waterRate).map { it.coerceIn(0.0, 1.0) }
        if (v.isEmpty()) return 0
        return (v.average() * 100).roundToInt()
    }

    private fun pct(done: Int, total: Int): Int = if (total > 0) (done * 100f / total).roundToInt() else 0

    // ── Water series ──────────────────────────────────────────────────────────
    private fun sumByDate(logs: List<WaterLog>): Map<String, Int> {
        val out = HashMap<String, Int>()
        for (l in logs) {
            if (l.deleted_at != null) continue
            out[l.log_date] = (out[l.log_date] ?: 0) + l.amount_ml
        }
        return out
    }

    private val WEEKDAY_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    data class DayPoint(val date: String, val label: String, val ml: Int, val score: Int = 0)

    /** ml per day for the current week (Sun→Sat). */
    fun weekSeries(logs: List<WaterLog>, today: String = today()): List<DayPoint> {
        val byDate = sumByDate(logs)
        val end = LocalDate.parse(today)
        val start = end.minusDays((end.dayOfWeek.value % 7).toLong())
        return (0 until 7).map { i ->
            val d = start.plusDays(i.toLong())
            val key = d.toString()
            DayPoint(key, WEEKDAY_SHORT[weekday(d)].take(2), byDate[key] ?: 0)
        }
    }

    data class WaterRange(val total: Int, val average: Int, val daysMetGoal: Int, val daysTracked: Int)

    fun monthStats(logs: List<WaterLog>, goalMl: Int, today: String = today()): WaterRange {
        val byDate = sumByDate(logs)
        val end = LocalDate.parse(today)
        val start = end.withDayOfMonth(1)
        var total = 0; var daysMet = 0; var daysTracked = 0
        val numDays = ChronoUnit.DAYS.between(start, end).toInt() + 1
        for (i in 0 until numDays) {
            val key = start.plusDays(i.toLong()).toString()
            val ml = byDate[key] ?: 0
            total += ml
            if (ml > 0) daysTracked++
            if (goalMl > 0 && ml >= goalMl) daysMet++
        }
        return WaterRange(total, if (numDays > 0) (total / numDays) else 0, daysMet, daysTracked)
    }

    // ── Habit calendar + labels ──────────────────────────────────────────────
    enum class DayStatus { COMPLETED, MISSED, PENDING, OFF, FUTURE }
    data class DayCell(val date: String, val status: DayStatus, val count: Int)

    val MONTH_FULL = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    fun monthName(month: Int): String = MONTH_FULL[(month - 1).coerceIn(0, 11)]

    /** One calendar month laid out as weeks (each 7 cells, Sun→Sat).
     *  `null` marks a slot before the 1st or after the last day of the month. */
    fun monthGrid(h: Habit, counts: Map<String, Int>, year: Int, month: Int, today: String = today()): List<List<DayCell?>> {
        val target = maxOf(1, h.target_count)
        val first = LocalDate.of(year, month, 1)
        val daysInMonth = first.lengthOfMonth()
        val lead = first.dayOfWeek.value % 7 // 0 = Sunday
        val cells = ArrayList<DayCell?>()
        repeat(lead) { cells.add(null) }
        for (dnum in 1..daysInMonth) {
            val key = LocalDate.of(year, month, dnum).toString()
            val count = counts[key] ?: 0
            val status = when {
                key > today -> DayStatus.FUTURE
                !isScheduledOn(h, key) -> DayStatus.OFF
                count >= target -> DayStatus.COMPLETED
                key == today -> DayStatus.PENDING
                else -> DayStatus.MISSED
            }
            cells.add(DayCell(key, status, count))
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }

    /** Month grid for a (recurring) task: OFF when it doesn't occur that day,
     *  otherwise completed / missed / pending / future based on [doneDates]. */
    fun taskMonthGrid(t: Task, doneDates: Set<String>, year: Int, month: Int, today: String = today()): List<List<DayCell?>> {
        val first = LocalDate.of(year, month, 1)
        val daysInMonth = first.lengthOfMonth()
        val lead = first.dayOfWeek.value % 7
        val cells = ArrayList<DayCell?>()
        repeat(lead) { cells.add(null) }
        for (dnum in 1..daysInMonth) {
            val key = LocalDate.of(year, month, dnum).toString()
            val status = when {
                !taskOccursOn(t, key) -> DayStatus.OFF
                key in doneDates -> DayStatus.COMPLETED
                key > today -> DayStatus.FUTURE
                key == today -> DayStatus.PENDING
                else -> DayStatus.MISSED
            }
            cells.add(DayCell(key, status, 0))
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }

    /** Contribution-style grid: list of weeks (columns), each 7 days (Sun→Sat). */
    fun buildCalendar(h: Habit, counts: Map<String, Int>, weeks: Int = 16, today: String = today()): List<List<DayCell>> {
        val target = maxOf(1, h.target_count)
        val end0 = LocalDate.parse(today)
        val endOfWeek = end0.plusDays((6 - (end0.dayOfWeek.value % 7)).toLong())
        var d = endOfWeek.minusDays((weeks * 7 - 1).toLong())
        val result = ArrayList<List<DayCell>>()
        for (w in 0 until weeks) {
            val col = ArrayList<DayCell>()
            for (i in 0 until 7) {
                val key = d.toString()
                val count = counts[key] ?: 0
                val status = when {
                    key > today -> DayStatus.FUTURE
                    !isScheduledOn(h, key) -> DayStatus.OFF
                    count >= target -> DayStatus.COMPLETED
                    key == today -> DayStatus.PENDING
                    else -> DayStatus.MISSED
                }
                col.add(DayCell(key, status, count))
                d = d.plusDays(1)
            }
            result.add(col)
        }
        return result
    }

    fun frequencyLabel(h: Habit): String {
        if (h.frequency == "daily") return "Every day"
        val days = h.custom_days.ifEmpty { listOf(weekday(LocalDate.parse(h.start_date))) }
        if (days.size == 7) return "Every day"
        return days.sorted().joinToString(", ") { WEEKDAY_SHORT[it] }
    }

    fun recurrenceLabel(t: Task): String = when (t.recurrence) {
        "none" -> "One-time"
        "daily" -> "Every day"
        "weekly" -> "Weekly"
        "monthly" -> "Monthly"
        "custom" -> {
            val n = maxOf(1, t.recurrence_interval)
            if (n == 1) "Every day" else "Every $n days"
        }
        else -> ""
    }

    // ── Reminders ──────────────────────────────────────────────────────────────
    fun fmtTime(hm: String): String {
        val parts = hm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return "%d:%02d %s".format(h12, m, ampm)
    }

    fun reminderSummary(r: Reminder): String = when (r.repeat) {
        "once" -> r.next_trigger_at?.let { fmtDateTime(it) } ?: "One-time"
        "daily" -> if (r.time_of_day != null) "Every day · ${fmtTime(r.time_of_day)}" else "Every day"
        "weekly" -> {
            val days = r.repeat_days.sorted().joinToString(", ") { WEEKDAY_SHORT[it] }
            (days.ifEmpty { "Weekly" }) + (r.time_of_day?.let { " · ${fmtTime(it)}" } ?: "")
        }
        "interval" -> "Every ${r.interval_min ?: 0} min"
        else -> ""
    }

    /** e.g. "Jul 12 · 3:30 PM" from an ISO timestamp. */
    fun fmtDateTime(iso: String): String = runCatching {
        val inst = java.time.OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val h = inst.hour
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        "%s %d · %d:%02d %s".format(months[inst.monthValue - 1], inst.dayOfMonth, h12, inst.minute, ampm)
    }.getOrElse { iso }

    /** Best-effort next fire time (epoch millis) for sorting upcoming reminders. */
    fun nextTriggerMillis(r: Reminder, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Long? {
        if (!r.is_enabled) return null
        r.snooze_until?.let {
            val s = runCatching { java.time.OffsetDateTime.parse(it).toLocalDateTime() }.getOrNull()
            if (s != null && s.isAfter(now)) return s.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        fun toMillis(dt: java.time.LocalDateTime) = dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        when (r.repeat) {
            "once" -> return r.next_trigger_at?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
            "interval" -> return r.interval_min?.let { toMillis(now.plusMinutes(it.toLong())) }
            "daily" -> {
                val hm = r.time_of_day ?: return null
                val (h, m) = hm.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
                var t = now.withHour(h).withMinute(m).withSecond(0).withNano(0)
                if (t.isBefore(now)) t = t.plusDays(1)
                return toMillis(t)
            }
            "weekly" -> {
                val hm = r.time_of_day ?: return null
                val (h, m) = hm.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
                for (offset in 0..7) {
                    val day = now.plusDays(offset.toLong())
                    if (weekday(day.toLocalDate()) in r.repeat_days) {
                        val cand = day.withHour(h).withMinute(m).withSecond(0).withNano(0)
                        if (cand.isAfter(now)) return toMillis(cand)
                    }
                }
                return null
            }
        }
        return null
    }

    // ── Range analytics (ported from lib/analytics.ts) ──────────────────────────
    data class HabitBreakdown(val id: String, val name: String, val color: String?, val rate: Int, val current: Int)
    data class MissedTask(val id: String, val title: String, val date: String)
    data class Analytics(
        val days: Int,
        val goalMl: Int,
        val taskTotal: Int,
        val taskDone: Int,
        val taskMissed: Int,
        val taskRate: Int,
        val habitTotal: Int,
        val habitDone: Int,
        val habitRate: Int,
        val topCurrentStreak: Int,
        val topLongestStreak: Int,
        val waterDaysMet: Int,
        val waterConsistency: Int,
        val perDay: List<DayPoint>,
        val bestDay: DayPoint?,
        val missedTasks: List<MissedTask>,
        val habitBreakdown: List<HabitBreakdown>,
    )

    fun computeAnalytics(
        tasks: List<Task>,
        completions: List<TaskCompletion>,
        habits: List<Habit>,
        habitLogs: List<HabitLog>,
        waterLogs: List<WaterLog>,
        goalMl: Int,
        days: Int,
        today: String = today(),
    ): Analytics {
        val end = LocalDate.parse(today)
        val start = end.minusDays((days - 1).toLong())

        val completionSet = completions.filter { it.deleted_at == null }
            .map { "${it.task_id}|${it.occurrence_date}" }.toHashSet()
        fun taskDoneOn(t: Task, key: String) =
            if (t.recurrence == "none") t.is_completed else completionSet.contains("${t.id}|$key")

        val habitCounts = HashMap<String, HashMap<String, Int>>()
        for (l in habitLogs) {
            if (l.deleted_at != null) continue
            habitCounts.getOrPut(l.habit_id) { HashMap() }[l.log_date] = l.count
        }
        val byDate = sumByDate(waterLogs)

        var taskTotal = 0; var taskDone = 0; var taskMissed = 0
        var habitTotal = 0; var habitDone = 0; var waterDaysMet = 0
        val missed = ArrayList<MissedTask>()
        val habitAcc = HashMap<String, IntArray>() // [total, done]
        val perDay = ArrayList<DayPoint>()

        for (i in 0 until days) {
            val d = start.plusDays(i.toLong())
            val key = d.toString()
            val isPast = key < today
            var dTaskTotal = 0; var dTaskDone = 0
            for (t in tasks) {
                if (!taskOccursOn(t, key)) continue
                taskTotal++; dTaskTotal++
                if (taskDoneOn(t, key)) { taskDone++; dTaskDone++ }
                else if (isPast) { taskMissed++; missed.add(MissedTask(t.id, t.title, key)) }
            }
            var dHabitTotal = 0; var dHabitDone = 0
            for (h in habits) {
                if (!isScheduledOn(h, key)) continue
                val target = maxOf(1, h.target_count)
                val done = (habitCounts[h.id]?.get(key) ?: 0) >= target
                habitTotal++; dHabitTotal++
                val acc = habitAcc.getOrPut(h.id) { intArrayOf(0, 0) }
                acc[0]++
                if (done) { habitDone++; dHabitDone++; acc[1]++ }
            }
            val ml = byDate[key] ?: 0
            if (goalMl > 0 && ml >= goalMl) waterDaysMet++
            val score = productivityScore(
                if (dTaskTotal > 0) dTaskDone.toDouble() / dTaskTotal else null,
                if (dHabitTotal > 0) dHabitDone.toDouble() / dHabitTotal else null,
                if (goalMl > 0) ml.toDouble() / goalMl else null,
            )
            val label = if (days <= 7) WEEKDAY_SHORT[weekday(d)].take(2) else d.dayOfMonth.toString()
            perDay.add(DayPoint(key, label, ml, score))
        }

        var bestDay: DayPoint? = null
        for (p in perDay) if (p.score > 0 && (bestDay == null || p.score > bestDay!!.score)) bestDay = p

        var topCurrent = 0; var topLongest = 0
        val breakdown = habits.map { h ->
            val st = computeHabitStats(h, habitCounts[h.id] ?: emptyMap(), today)
            topCurrent = maxOf(topCurrent, st.current)
            topLongest = maxOf(topLongest, st.longest)
            val acc = habitAcc[h.id] ?: intArrayOf(0, 0)
            HabitBreakdown(h.id, h.name, h.color, pct(acc[1], acc[0]), st.current)
        }

        return Analytics(
            days = days, goalMl = goalMl,
            taskTotal = taskTotal, taskDone = taskDone, taskMissed = taskMissed, taskRate = pct(taskDone, taskTotal),
            habitTotal = habitTotal, habitDone = habitDone, habitRate = pct(habitDone, habitTotal),
            topCurrentStreak = topCurrent, topLongestStreak = topLongest,
            waterDaysMet = waterDaysMet, waterConsistency = pct(waterDaysMet, days),
            perDay = perDay, bestDay = bestDay,
            missedTasks = missed.takeLast(12).reversed(),
            habitBreakdown = breakdown,
        )
    }
}
