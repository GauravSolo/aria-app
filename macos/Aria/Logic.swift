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
