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
        self.uid = uid
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

    // ── Mutations ──────────────────────────────────────────────────────────────
    func toggleTask(_ t: Task) {
        Foundation.Task {
            do {
                if t.recurrence == .none {
                    let done = !t.isCompleted
                    try await Supa.client.from("tasks").update([
                        "is_completed": .bool(done),
                        "completed_at": done ? .string(ISO.now()) : .null,
                        "updated_at": .string(ISO.now()),
                    ]).eq("id", value: t.id).execute()
                    if let i = tasks.firstIndex(where: { $0.id == t.id }) {
                        tasks[i].isCompleted = done
                        tasks[i].completedAt = done ? ISO.now() : nil
                    }
                } else {
                    try await toggleCompletion(t)
                }
                publishWidget()
            } catch { lastError = error.localizedDescription }
        }
    }

    private func toggleCompletion(_ t: Task) async throws {
        if let existing = completions.first(where: { $0.taskId == t.id && $0.occurrenceDate == today }) {
            let nowDeleted = existing.deletedAt == nil
            try await Supa.client.from("task_completions").update([
                "deleted_at": nowDeleted ? .string(ISO.now()) : .null,
                "updated_at": .string(ISO.now()),
            ]).eq("id", value: existing.id).execute()
            if let i = completions.firstIndex(where: { $0.id == existing.id }) {
                completions[i].deletedAt = nowDeleted ? ISO.now() : nil
            }
        } else {
            let row = TaskCompletion(id: newUUID(), userId: uid, taskId: t.id, occurrenceDate: today,
                                     completedAt: ISO.now(), createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
            try await Supa.client.from("task_completions").insert(row).execute()
            completions.append(row)
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
        Foundation.Task {
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
        Foundation.Task {
            do {
                if let existing = habitLogs.first(where: { $0.habitId == h.id && $0.logDate == today }) {
                    if count <= 0 {
                        try await Supa.client.from("habit_logs").update([
                            "deleted_at": .string(ISO.now()), "updated_at": .string(ISO.now()),
                        ]).eq("id", value: existing.id).execute()
                        if let i = habitLogs.firstIndex(where: { $0.id == existing.id }) { habitLogs[i].deletedAt = ISO.now() }
                    } else {
                        try await Supa.client.from("habit_logs").update([
                            "count": .integer(count), "deleted_at": .null, "updated_at": .string(ISO.now()),
                        ]).eq("id", value: existing.id).execute()
                        if let i = habitLogs.firstIndex(where: { $0.id == existing.id }) {
                            habitLogs[i].count = count; habitLogs[i].deletedAt = nil
                        }
                    }
                } else if count > 0 {
                    let row = HabitLog(id: newUUID(), userId: uid, habitId: h.id, logDate: today,
                                       count: count, createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
                    try await Supa.client.from("habit_logs").insert(row).execute()
                    habitLogs.append(row)
                }
                publishWidget()
            } catch { lastError = error.localizedDescription }
        }
    }

    func addHabit(name: String, category: Category, frequency: Frequency, target: Int, days: [Int], color: String?) {
        let row = Habit(id: newUUID(), userId: uid, name: name, kind: .build, category: category,
                        frequency: frequency, targetCount: max(1, target),
                        customDays: frequency == .daily ? [] : days, reminderTime: nil,
                        startDate: today, notes: nil, color: color, icon: nil, isArchived: false,
                        sortOrder: activeHabits.count, createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
        habits.append(row)
        Foundation.Task {
            do { try await Supa.client.from("habits").insert(row).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    func logWater(_ ml: Int) {
        let row = WaterLog(id: newUUID(), userId: uid, logDate: today, amountMl: ml,
                           loggedAt: ISO.now(), createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
        waterLogs.append(row)
        Foundation.Task {
            do { try await Supa.client.from("water_logs").insert(row).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    func saveWaterSettings(_ s: WaterSettings) {
        water = s
        Foundation.Task {
            do { try await Supa.client.from("water_settings").upsert(s).execute(); publishWidget() }
            catch { lastError = error.localizedDescription }
        }
    }

    // ── Widget ───────────────────────────────────────────────────────────────
    private func publishWidget() {
        let dayTasks = tasksForToday()
        let pending = dayTasks.filter { !isTaskDone($0) }
        let scheduled = activeHabits.map { stats($0) }.filter { $0.scheduledToday }
        let snap = WidgetSnapshot(
            generatedAt: ISO.now(),
            nextTaskTitle: pending.first?.title,
            pendingTasks: pending.count,
            totalTasks: dayTasks.count,
            waterMl: waterToday,
            waterGoal: water.dailyGoalMl,
            habitsDone: scheduled.filter { $0.doneToday }.count,
            habitsTotal: scheduled.count,
            topStreak: activeHabits.map { stats($0).current }.max() ?? 0
        )
        WidgetBridge.write(snap)
    }
}
