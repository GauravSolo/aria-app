import Foundation

// Pure logic mirrored from mobile/src/lib (recurrence, streaks, water, analytics).
// Date keys are "yyyy-MM-dd" and compare correctly as Strings.

enum Cal {
    static var calendar: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.firstWeekday = 1 // Sunday
        return c
    }()

    private static let fmt: DateFormatter = {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = .current
        return f
    }()

    static func date(_ key: String) -> Date { fmt.date(from: key) ?? Date() }
    static func keyOf(_ date: Date) -> String { fmt.string(from: date) }
    /// 0 = Sunday … 6 = Saturday
    static func weekday(_ key: String) -> Int { (calendar.component(.weekday, from: date(key)) - 1) }
    static func add(_ key: String, days: Int) -> String {
        keyOf(calendar.date(byAdding: .day, value: days, to: date(key))!)
    }
    static func diffDays(_ from: String, _ to: String) -> Int {
        calendar.dateComponents([.day], from: date(from), to: date(to)).day ?? 0
    }
    static func startOfWeek(_ key: String) -> String {
        let d = date(key)
        let comps = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: d)
        return keyOf(calendar.date(from: comps)!)
    }
    static func startOfMonth(_ key: String) -> String {
        let d = date(key)
        let comps = calendar.dateComponents([.year, .month], from: d)
        return keyOf(calendar.date(from: comps)!)
    }
}

// ── Tasks / recurrence ───────────────────────────────────────────────────────
func taskAnchorDate(_ t: Task) -> String { t.dueDate ?? String(t.createdAt.prefix(10)) }

func taskOccursOn(_ t: Task, _ key: String) -> Bool {
    if t.deletedAt != nil { return false }
    let anchor = taskAnchorDate(t)
    if t.recurrence == .none { return key == anchor }
    if key < anchor { return false }
    if let end = t.recurrenceEndDate, key > end { return false }
    switch t.recurrence {
    case .daily: return true
    case .weekly:
        let days = t.recurrenceDays.isEmpty ? [Cal.weekday(anchor)] : t.recurrenceDays
        return days.contains(Cal.weekday(key))
    case .monthly:
        return Cal.calendar.component(.day, from: Cal.date(anchor))
            == Cal.calendar.component(.day, from: Cal.date(key))
    case .custom:
        let interval = max(1, t.recurrenceInterval)
        return Cal.diffDays(anchor, key) % interval == 0
    default: return false
    }
}

// ── Habits / streaks ─────────────────────────────────────────────────────────
func isScheduledOn(_ h: Habit, _ key: String) -> Bool {
    if key < h.startDate { return false }
    if h.frequency == .daily { return true }
    let days = h.customDays.isEmpty ? [Cal.weekday(h.startDate)] : h.customDays
    return days.contains(Cal.weekday(key))
}

struct HabitStats {
    var current = 0
    var longest = 0
    var totalCompleted = 0
    var missed = 0
    var successPct = 0
    var weekDone = 0
    var weekTotal = 0
    var monthDone = 0
    var monthTotal = 0
    var doneToday = false
    var scheduledToday = false
    var todayCount = 0
}

func computeHabitStats(_ h: Habit, counts: [String: Int], today: String = DayKey.today()) -> HabitStats {
    let target = max(1, h.targetCount)
    func done(_ k: String) -> Bool { (counts[k] ?? 0) >= target }
    var s = HabitStats()
    s.todayCount = counts[today] ?? 0
    if h.startDate > today { return s }

    let weekStart = Cal.startOfWeek(today)
    let monthStart = Cal.startOfMonth(today)

    var cur = h.startDate
    let minStart = Cal.add(today, days: -730)
    if cur < minStart { cur = minStart }

    var scheduled: [(date: String, done: Bool)] = []
    var scheduledPast = 0, completedPast = 0

    while cur <= today {
        if isScheduledOn(h, cur) {
            let d = done(cur)
            scheduled.append((cur, d))
            if d { s.totalCompleted += 1 }
            if cur < today {
                scheduledPast += 1
                if d { completedPast += 1 } else { s.missed += 1 }
            } else {
                s.scheduledToday = true
                s.doneToday = d
            }
            if cur >= weekStart { s.weekTotal += 1; if d { s.weekDone += 1 } }
            if cur >= monthStart { s.monthTotal += 1; if d { s.monthDone += 1 } }
        }
        cur = Cal.add(cur, days: 1)
    }

    var run = 0
    for item in scheduled {
        if item.done { run += 1; s.longest = max(s.longest, run) } else { run = 0 }
    }

    var arr = scheduled
    if let last = arr.last, last.date == today, !last.done { arr.removeLast() }
    var c = 0
    for item in arr.reversed() { if item.done { c += 1 } else { break } }
    s.current = c

    s.successPct = scheduledPast > 0
        ? Int((Double(completedPast) / Double(scheduledPast) * 100).rounded())
        : (s.doneToday ? 100 : 0)
    return s
}

// ── Water ────────────────────────────────────────────────────────────────────
func waterTotal(_ logs: [WaterLog], on key: String) -> Int {
    logs.reduce(0) { $0 + ($1.deletedAt == nil && $1.logDate == key ? $1.amountMl : 0) }
}

func formatMl(_ ml: Int) -> String {
    if ml >= 1000 {
        let l = Double(ml) / 1000
        return ml % 1000 == 0 ? "\(Int(l)) L" : String(format: "%.1f L", l)
    }
    return "\(ml) ml"
}

func productivityScore(taskRate: Double?, habitRate: Double?, waterRate: Double?) -> Int {
    var vals: [Double] = []
    if let t = taskRate { vals.append(min(1, max(0, t))) }
    if let h = habitRate { vals.append(min(1, max(0, h))) }
    if let w = waterRate { vals.append(min(1, max(0, w))) }
    guard !vals.isEmpty else { return 0 }
    return Int((vals.reduce(0, +) / Double(vals.count) * 100).rounded())
}

// ── Month calendar cells (habit streak + task calendar widgets) ───────────────
// Cell codes: -1 blank · 0 off · 1 done · 2 missed · 3 today-pending · 4 future.
private func monthWeeks(year: Int, month: Int, codeFor: (String) -> Int) -> [[Int]] {
    var first = DateComponents(); first.year = year; first.month = month; first.day = 1
    guard let firstDate = Cal.calendar.date(from: first) else { return [] }
    let daysInMonth = Cal.calendar.range(of: .day, in: .month, for: firstDate)?.count ?? 30
    let lead = Cal.calendar.component(.weekday, from: firstDate) - 1 // 0 = Sunday
    var cells = Array(repeating: -1, count: max(0, lead))
    for d in 1...daysInMonth {
        var c = DateComponents(); c.year = year; c.month = month; c.day = d
        let key = Cal.keyOf(Cal.calendar.date(from: c)!)
        cells.append(codeFor(key))
    }
    while cells.count % 7 != 0 { cells.append(-1) }
    return stride(from: 0, to: cells.count, by: 7).map { Array(cells[$0 ..< $0 + 7]) }
}

private func currentYearMonth(_ today: String) -> (Int, Int) {
    let d = Cal.date(today)
    return (Cal.calendar.component(.year, from: d), Cal.calendar.component(.month, from: d))
}

func habitMonthCells(_ h: Habit, counts: [String: Int], today: String = DayKey.today()) -> [[Int]] {
    let target = max(1, h.targetCount)
    let (year, month) = currentYearMonth(today)
    return monthWeeks(year: year, month: month) { key in
        if key > today { return 4 }
        if !isScheduledOn(h, key) { return 0 }
        let cnt = counts[key] ?? 0
        if cnt >= target { return 1 }
        if key == today { return 3 }
        return 2
    }
}

func taskMonthCells(_ t: Task, done: Set<String>, today: String = DayKey.today()) -> [[Int]] {
    let (year, month) = currentYearMonth(today)
    return monthWeeks(year: year, month: month) { key in
        if !taskOccursOn(t, key) { return 0 }
        if done.contains(key) { return 1 }
        if key > today { return 4 }
        if key == today { return 3 }
        return 2
    }
}

func taskStreaks(_ t: Task, done: Set<String>, today: String = DayKey.today()) -> (current: Int, longest: Int) {
    var run = 0, longest = 0
    var key = Cal.add(today, days: -365)
    while key <= today {
        if taskOccursOn(t, key) {
            if done.contains(key) { run += 1; longest = max(longest, run) }
            else if key < today { run = 0 }
        }
        key = Cal.add(key, days: 1)
    }
    return (run, longest)
}

// ── Reminders ─────────────────────────────────────────────────────────────────
private let weekdayShort = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]

func reminderSummary(_ r: Reminder) -> String {
    switch r.repeat {
    case .once:
        if let t = r.nextTriggerAt { return fmtDateTime(t) }
        return "One-time"
    case .daily:
        return "Every day" + (r.timeOfDay.map { " · " + fmtTime($0) } ?? "")
    case .weekly:
        let days = r.repeatDays.sorted().map { weekdayShort[$0] }.joined(separator: ", ")
        return (days.isEmpty ? "Weekly" : days) + (r.timeOfDay.map { " · " + fmtTime($0) } ?? "")
    case .interval:
        return "Every \(r.intervalMin ?? 0) min"
    }
}

func fmtTime(_ hm: String) -> String {
    let parts = hm.split(separator: ":")
    let h = Int(parts.first ?? "0") ?? 0
    let m = parts.count > 1 ? (Int(parts[1]) ?? 0) : 0
    let ampm = h < 12 ? "AM" : "PM"
    let h12 = h == 0 ? 12 : (h > 12 ? h - 12 : h)
    return String(format: "%d:%02d %@", h12, m, ampm)
}

func fmtDateTime(_ iso: String) -> String {
    let f = ISO8601DateFormatter()
    f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let d = f.date(from: iso) ?? ISO8601DateFormatter().date(from: iso) ?? Date()
    let out = DateFormatter()
    out.dateFormat = "MMM d · h:mm a"
    return out.string(from: d)
}

/// Next fire Date for scheduling a local notification (nil = don't schedule).
func reminderNextFire(_ r: Reminder, now: Date = Date()) -> Date? {
    guard r.isEnabled, r.deletedAt == nil else { return nil }
    let cal = Cal.calendar
    func hm(_ s: String?) -> (Int, Int) {
        let p = (s ?? "9:00").split(separator: ":")
        return (Int(p.first ?? "9") ?? 9, p.count > 1 ? (Int(p[1]) ?? 0) : 0)
    }
    switch r.repeat {
    case .once:
        guard let iso = r.nextTriggerAt else { return nil }
        let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let d = f.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
        return (d.map { $0 > now } == true) ? d : nil
    case .interval:
        let m = max(15, r.intervalMin ?? 60)
        return now.addingTimeInterval(Double(m) * 60)
    case .daily:
        let (h, m) = hm(r.timeOfDay)
        var comps = cal.dateComponents([.year, .month, .day], from: now)
        comps.hour = h; comps.minute = m
        var fire = cal.date(from: comps)!
        if fire <= now { fire = cal.date(byAdding: .day, value: 1, to: fire)! }
        return fire
    case .weekly:
        let (h, m) = hm(r.timeOfDay)
        let days = r.repeatDays.isEmpty ? [cal.component(.weekday, from: now) - 1] : r.repeatDays
        for offset in 0...7 {
            let day = cal.date(byAdding: .day, value: offset, to: now)!
            if days.contains(cal.component(.weekday, from: day) - 1) {
                var comps = cal.dateComponents([.year, .month, .day], from: day)
                comps.hour = h; comps.minute = m
                if let fire = cal.date(from: comps), fire > now { return fire }
            }
        }
        return nil
    }
}
