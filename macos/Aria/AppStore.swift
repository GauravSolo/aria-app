import Foundation
import Supabase

/// Online-first data store for the Mac app (the phone remains the offline-first primary).
/// Loads rows from Supabase, applies mutations, and republishes a widget snapshot.
@MainActor
final class AppStore: ObservableObject {
    @Published var tasks: [Task] = []
    @Published var completions: [TaskCompletion] = []
    @Published var habits: [Habit] = []
    @Published var habitLogs: [HabitLog] = []
    @Published var waterLogs: [WaterLog] = []
    @Published var water: WaterSettings = .defaults("")
    @Published var loading = false
    @Published var lastError: String?

    private var uid: String = ""

    func load(uid: String) async {
        print("ARIA load() start")
        self.uid = uid
        await flushPendingWidgetToggles()
        loading = true
        defer { loading = false }
        do {
            async let t: [Task] = fetch("tasks")
            async let c: [TaskCompletion] = fetch("task_completions")
            async let h: [Habit] = fetch("habits")
            async let hl: [HabitLog] = fetch("habit_logs")
            async let wl: [WaterLog] = fetch("water_logs")
            tasks = try await t
            completions = try await c
            habits = try await h
            habitLogs = try await hl
            waterLogs = try await wl
            water = (try? await fetchWater()) ?? .defaults(uid)
            publishWidget()
        } catch {
            lastError = error.localizedDescription
        }
    }

    private func fetch<T: Decodable>(_ table: String) async throws -> [T] {
        try await Supa.client.from(table).select().eq("user_id", value: uid).execute().value
    }

    private func fetchWater() async throws -> WaterSettings {
        try await Supa.client.from("water_settings").select().eq("user_id", value: uid)
            .single().execute().value
    }

    // ── Derived (today) ──────────────────────────────────────────────────────
    var today: String { DayKey.today() }

    func tasksForToday() -> [Task] {
        tasks.filter { $0.deletedAt == nil && taskOccursOn($0, today) }
            .sorted { ($0.startTime ?? "") < ($1.startTime ?? "") }
    }

    func isTaskDone(_ t: Task) -> Bool {
        if t.recurrence == .none { return t.isCompleted }
        return completions.contains { $0.deletedAt == nil && $0.taskId == t.id && $0.occurrenceDate == today }
    }

    var activeHabits: [Habit] {
        habits.filter { $0.deletedAt == nil && !$0.isArchived }
            .sorted { $0.sortOrder < $1.sortOrder }
    }

    func habitCounts(_ habitId: String) -> [String: Int] {
        var m: [String: Int] = [:]
        for l in habitLogs where l.deletedAt == nil && l.habitId == habitId { m[l.logDate] = l.count }
        return m
    }

    func stats(_ h: Habit) -> HabitStats { computeHabitStats(h, counts: habitCounts(h.id), today: today) }

    var waterToday: Int { waterTotal(waterLogs, on: today) }

    // ── Mutations (optimistic: update local state now, persist in background) ────
    func toggleTask(_ t: Task) {
        let now = ISO.now()
        print("ARIA toggleTask id=\(t.id) recur=\(t.recurrence.rawValue) done=\(t.isCompleted)")
        if t.recurrence == .none {
            let done = !t.isCompleted
            guard let i = tasks.firstIndex(where: { $0.id == t.id }) else { print("ARIA toggleTask: index NOT found"); return }
            tasks[i].isCompleted = done
            print("ARIA toggleTask: set isCompleted=\(done) at \(i)")
            tasks[i].completedAt = done ? now : nil
            publishWidget()
            let id = t.id
            let payload: [String: AnyJSON] = [
                "is_completed": .bool(done),
                "completed_at": done ? .string(now) : .null,
                "updated_at": .string(now),
            ]
            push { _ = try await Supa.client.from("tasks").update(payload).eq("id", value: id).execute() }
        } else {
            toggleCompletion(t)
            publishWidget()
        }
    }

    private func toggleCompletion(_ t: Task) {
        let now = ISO.now()
        if let i = completions.firstIndex(where: { $0.taskId == t.id && $0.occurrenceDate == today }) {
            let nowDeleted = completions[i].deletedAt == nil
            completions[i].deletedAt = nowDeleted ? now : nil
            let cid = completions[i].id
            let payload: [String: AnyJSON] = [
                "deleted_at": nowDeleted ? .string(now) : .null,
                "updated_at": .string(now),
            ]
            push { _ = try await Supa.client.from("task_completions").update(payload).eq("id", value: cid).execute() }
        } else {
            let row = TaskCompletion(id: newUUID(), userId: uid, taskId: t.id, occurrenceDate: today,
                                     completedAt: now, createdAt: now, updatedAt: now, deletedAt: nil)
            completions.append(row)
            push { _ = try await Supa.client.from("task_completions").insert(row).execute() }
        }
    }

    func addTask(title: String, category: Category, priority: Priority, dueDate: String,
                 startTime: String?, endTime: String?, recurrence: TaskRecurrence, days: [Int]) {
        let row = Task(id: newUUID(), userId: uid, title: title, description: nil,
                       category: category, priority: priority, startTime: startTime, endTime: endTime,
                       dueDate: dueDate, recurrence: recurrence, recurrenceInterval: 1,
                       recurrenceDays: days, recurrenceEndDate: nil, isCompleted: false,
                       completedAt: nil, sortOrder: 0, createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
        tasks.append(row)
        _Concurrency.Task {
            do { try await Supa.client.from("tasks").insert(row).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    func toggleHabit(_ h: Habit) {
        let target = max(1, h.targetCount)
        let cur = habitCounts(h.id)[today] ?? 0
        setHabitCount(h, count: cur >= target ? 0 : target)
    }

    func setHabitCount(_ h: Habit, count: Int) {
        let now = ISO.now()
        print("ARIA setHabitCount habit=\(h.id) count=\(count)")
        if let i = habitLogs.firstIndex(where: { $0.habitId == h.id && $0.logDate == today }) {
            let logId = habitLogs[i].id
            if count <= 0 {
                habitLogs[i].deletedAt = now
                let payload: [String: AnyJSON] = ["deleted_at": .string(now), "updated_at": .string(now)]
                push { _ = try await Supa.client.from("habit_logs").update(payload).eq("id", value: logId).execute() }
            } else {
                habitLogs[i].count = count; habitLogs[i].deletedAt = nil
                let payload: [String: AnyJSON] = ["count": .integer(count), "deleted_at": .null, "updated_at": .string(now)]
                push { _ = try await Supa.client.from("habit_logs").update(payload).eq("id", value: logId).execute() }
            }
        } else if count > 0 {
            let row = HabitLog(id: newUUID(), userId: uid, habitId: h.id, logDate: today,
                               count: count, createdAt: now, updatedAt: now, deletedAt: nil)
            habitLogs.append(row)
            push { _ = try await Supa.client.from("habit_logs").insert(row).execute() }
        }
        publishWidget()
    }

    /// Runs a Supabase write in the background; surfaces failures in `lastError`.
    private func push(_ work: @escaping () async throws -> Void) {
        _Concurrency.Task {
            do { try await work() }
            catch { lastError = error.localizedDescription }
        }
    }

    func addHabit(name: String, category: Category, frequency: Frequency, target: Int, days: [Int], color: String?) {
        let row = Habit(id: newUUID(), userId: uid, name: name, kind: .build, category: category,
                        frequency: frequency, targetCount: max(1, target),
                        customDays: frequency == .daily ? [] : days, reminderTime: nil,
                        startDate: today, notes: nil, color: color, icon: nil, isArchived: false,
                        sortOrder: activeHabits.count, createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
        habits.append(row)
        _Concurrency.Task {
            do { try await Supa.client.from("habits").insert(row).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    func logWater(_ ml: Int) {
        let row = WaterLog(id: newUUID(), userId: uid, logDate: today, amountMl: ml,
                           loggedAt: ISO.now(), createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
        waterLogs.append(row)
        _Concurrency.Task {
            do { try await Supa.client.from("water_logs").insert(row).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    func saveWaterSettings(_ s: WaterSettings) {
        water = s
        _Concurrency.Task {
            do { try await Supa.client.from("water_settings").upsert(s).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    /// Apply task checkboxes tapped on the widget while the app was closed.
    private func flushPendingWidgetToggles() async {
        let pending = WidgetBridge.readPending()
        guard !pending.isEmpty else { return }
        for p in pending {
            do {
                if p.recurrence == "none" {
                    let payload: [String: AnyJSON] = [
                        "is_completed": .bool(true), "completed_at": .string(ISO.now()), "updated_at": .string(ISO.now()),
                    ]
                    try await Supa.client.from("tasks").update(payload).eq("id", value: p.id).execute()
                } else {
                    let row = TaskCompletion(id: newUUID(), userId: uid, taskId: p.id, occurrenceDate: today,
                                             completedAt: ISO.now(), createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
                    try await Supa.client.from("task_completions").insert(row).execute()
                }
            } catch { /* best-effort; stays queued only if we don't clear */ }
        }
        WidgetBridge.clearPending()
    }

    // ── Widget ───────────────────────────────────────────────────────────────
    private func publishWidget() {
        let dayTasks = tasksForToday()
        let pending = dayTasks.filter { !isTaskDone($0) }
        let scheduled = activeHabits.map { stats($0) }.filter { $0.scheduledToday }
        let snap = WidgetSnapshot(
            generatedAt: ISO.now(),
            nextTaskTitle: pending.first?.title,
            tasks: pending.prefix(12).map { WidgetTask(id: $0.id, title: $0.title, recurrence: $0.recurrence.rawValue) },
            pendingTasks: pending.count,
            totalTasks: dayTasks.count,
            waterMl: waterToday,
            waterGoal: water.dailyGoalMl,
            habitsDone: scheduled.filter { $0.doneToday }.count,
            habitsTotal: scheduled.count,
            topStreak: activeHabits.map { stats($0).current }.max() ?? 0
        )
        WidgetBridge.write(snap)
        publishCalendars()
    }

    /// Per-habit and per-recurring-task month calendars for the calendar widgets.
    private func publishCalendars() {
        let habitEntries: [CalendarEntry] = activeHabits.map { h in
            let s = stats(h)
            return CalendarEntry(id: h.id, name: h.name, colorHex: h.color, current: s.current,
                                 longest: s.longest, weeks: habitMonthCells(h, counts: habitCounts(h.id)))
        }
        let taskEntries: [CalendarEntry] = tasks
            .filter { $0.deletedAt == nil && $0.recurrence != .none }
            .map { t in
                let done = Set(completions.filter { $0.deletedAt == nil && $0.taskId == t.id }.map { $0.occurrenceDate })
                let st = taskStreaks(t, done: done)
                return CalendarEntry(id: t.id, name: t.title, colorHex: nil, current: st.current,
                                     longest: st.longest, weeks: taskMonthCells(t, done: done))
            }
        WidgetBridge.writeCalendar(CalendarData(habits: habitEntries, tasks: taskEntries))
    }
}
