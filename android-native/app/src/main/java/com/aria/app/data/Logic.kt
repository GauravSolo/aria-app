package com.aria.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Pure logic ported from the Expo app (recurrence / streaks / water / score). */
object Logic {

    fun today(): String = LocalDate.now().toString() // ISO yyyy-MM-dd

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
}
