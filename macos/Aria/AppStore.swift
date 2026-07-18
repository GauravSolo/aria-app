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
    @Published var reminders: [Reminder] = []
    @Published var notifications: [NotificationHistory] = []
    @Published var loading = false
    @Published var lastError: String?

    private var uid: String = ""

    func load(uid: String) async {
        self.uid = uid
        await flushPendingWidgetToggles()
        loading = true
        defer { loading = false }
        await reloadData()
        await startRealtime()
    }

    /// Fetch all rows from Supabase and republish. Safe to call repeatedly (used by the
    /// realtime subscription when another device / the widget changes data).
    private func reloadData() async {
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
            reminders = (try? await fetch("reminders")) ?? []
            notifications = (try? await fetch("notification_history")) ?? []
            publishWidget()
            Notifier.reschedule(activeReminders, water: water)
        } catch {
            lastError = error.localizedDescription
        }
    }

    // ── Realtime (live sync across devices + widget) ──────────────────────────
    private var realtimeStarted = false
    private var realtimeSubs: [RealtimeSubscription] = []
    private var realtimeChannel: RealtimeChannelV2?
    private var reloadDebounce: _Concurrency.Task<Void, Never>?

    /// Subscribe to Postgres changes on the user's tables; coalesce bursts into one reload.
    /// Callbacks MUST be registered (`onPostgresChange`) before `subscribe()` — the async
    /// `postgresChange` stream registers lazily on first iteration, which lands after
    /// subscribe and is silently dropped.
    private func startRealtime() async {
        guard !realtimeStarted else { return }
        realtimeStarted = true
        let channel = Supa.client.channel("aria-\(uid)")
        for table in ["tasks", "task_completions", "habits", "habit_logs", "water_logs", "water_settings", "reminders"] {
            let sub = channel.onPostgresChange(AnyAction.self, schema: "public", table: table) { [weak self] _ in
                _Concurrency.Task { @MainActor in self?.scheduleReload() }
            }
            realtimeSubs.append(sub)
        }
        realtimeChannel = channel
        await channel.subscribe()
    }

    private func scheduleReload() {
        reloadDebounce?.cancel()
        reloadDebounce = _Concurrency.Task { [weak self] in
            try? await _Concurrency.Task.sleep(nanoseconds: 700_000_000)
            if _Concurrency.Task.isCancelled { return }
            await self?.reloadData()
        }
    }

    var activeReminders: [Reminder] {
        reminders.filter { $0.deletedAt == nil }.sorted { $0.createdAt < $1.createdAt }
    }

    var recentNotifications: [NotificationHistory] {
        notifications.filter { $0.deletedAt == nil }.sorted { $0.firedAt > $1.firedAt }
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

    func tasksForToday() -> [Task] { tasksOn(today) }

    /// Tasks occurring on a day, timed first (by start time), untimed last.
    func tasksOn(_ date: String) -> [Task] {
        tasks.filter { $0.deletedAt == nil && taskOccursOn($0, date) }
            .sorted { timeKey($0) < timeKey($1) }
    }

    private func timeKey(_ t: Task) -> String {
        guard let st = t.startTime else { return "~" }
        return String(st.suffix(from: st.index(st.startIndex, offsetBy: min(11, st.count)))).prefix(5).description
    }

    func isTaskDone(_ t: Task) -> Bool { isTaskDone(t, on: today) }

    func isTaskDone(_ t: Task, on date: String) -> Bool {
        if t.recurrence == .none { return t.isCompleted }
        return completions.contains { $0.deletedAt == nil && $0.taskId == t.id && $0.occurrenceDate == date }
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
    func toggleTask(_ t: Task) { toggleTask(t, on: today) }

    func toggleTask(_ t: Task, on date: String) {
        let now = ISO.now()
        if t.recurrence == .none {
            let done = !t.isCompleted
            guard let i = tasks.firstIndex(where: { $0.id == t.id }) else { return }
            tasks[i].isCompleted = done
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
            toggleCompletion(t, on: date)
            publishWidget()
        }
    }

    private func toggleCompletion(_ t: Task, on date: String) {
        let now = ISO.now()
        if let i = completions.firstIndex(where: { $0.taskId == t.id && $0.occurrenceDate == date }) {
            let nowDeleted = completions[i].deletedAt == nil
            completions[i].deletedAt = nowDeleted ? now : nil
            let cid = completions[i].id
            let payload: [String: AnyJSON] = [
                "deleted_at": nowDeleted ? .string(now) : .null,
                "updated_at": .string(now),
            ]
            push { _ = try await Supa.client.from("task_completions").update(payload).eq("id", value: cid).execute() }
        } else {
            let row = TaskCompletion(id: newUUID(), userId: uid, taskId: t.id, occurrenceDate: date,
                                     completedAt: now, createdAt: now, updatedAt: now, deletedAt: nil)
            completions.append(row)
            push { _ = try await Supa.client.from("task_completions").insert(row).execute() }
        }
    }

    func addTask(title: String, category: Category, priority: Priority, dueDate: String,
                 startTime: String?, endTime: String?, recurrence: TaskRecurrence, days: [Int],
                 interval: Int = 1, until: String? = nil) {
        let row = Task(id: newUUID(), userId: uid, title: title, description: nil,
                       category: category, priority: priority, startTime: startTime, endTime: endTime,
                       dueDate: dueDate, recurrence: recurrence, recurrenceInterval: max(1, interval),
                       recurrenceDays: days, recurrenceEndDate: until, isCompleted: false,
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

    func updateHabit(_ existing: Habit, name: String, category: Category, frequency: Frequency,
                     target: Int, days: [Int], color: String?, notes: String?) {
        guard let i = habits.firstIndex(where: { $0.id == existing.id }) else { return }
        var h = habits[i]
        h.name = name; h.category = category; h.frequency = frequency; h.targetCount = max(1, target)
        h.customDays = frequency == .daily ? [] : days; h.color = color; h.notes = notes; h.updatedAt = ISO.now()
        habits[i] = h
        let row = h
        push { _ = try await Supa.client.from("habits").update(row).eq("id", value: row.id).execute() }
        publishWidget()
    }

    func deleteHabit(_ h: Habit) {
        guard let i = habits.firstIndex(where: { $0.id == h.id }) else { return }
        let now = ISO.now()
        habits[i].deletedAt = now
        let id = h.id
        let payload: [String: AnyJSON] = ["deleted_at": .string(now), "updated_at": .string(now)]
        push { _ = try await Supa.client.from("habits").update(payload).eq("id", value: id).execute() }
        publishWidget()
    }

    func updateTask(_ existing: Task, title: String, category: Category, priority: Priority, dueDate: String,
                    startTime: String?, endTime: String?, recurrence: TaskRecurrence, days: [Int],
                    interval: Int = 1, until: String? = nil) {
        guard let i = tasks.firstIndex(where: { $0.id == existing.id }) else { return }
        var t = tasks[i]
        t.title = title; t.category = category; t.priority = priority; t.dueDate = dueDate
        t.startTime = startTime; t.endTime = endTime; t.recurrence = recurrence
        t.recurrenceInterval = max(1, interval); t.recurrenceEndDate = until
        t.recurrenceDays = recurrence == .weekly ? days : []; t.updatedAt = ISO.now()
        tasks[i] = t
        let row = t
        push { _ = try await Supa.client.from("tasks").update(row).eq("id", value: row.id).execute() }
        publishWidget()
    }

    func deleteTask(_ t: Task) {
        guard let i = tasks.firstIndex(where: { $0.id == t.id }) else { return }
        let now = ISO.now()
        tasks[i].deletedAt = now
        let id = t.id
        let payload: [String: AnyJSON] = ["deleted_at": .string(now), "updated_at": .string(now)]
        push { _ = try await Supa.client.from("tasks").update(payload).eq("id", value: id).execute() }
        publishWidget()
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

    // ── Reminders ────────────────────────────────────────────────────────────
    func saveReminder(_ existing: Reminder?, title: String, body: String?, repeatMode: ReminderRepeat,
                      repeatDays: [Int], intervalMin: Int?, timeOfDay: String?, nextTriggerAt: String?) {
        let now = ISO.now()
        if let e = existing, let i = reminders.firstIndex(where: { $0.id == e.id }) {
            var r = reminders[i]
            r.title = title; r.body = body; r.repeat = repeatMode; r.repeatDays = repeatDays
            r.intervalMin = intervalMin; r.timeOfDay = timeOfDay; r.nextTriggerAt = nextTriggerAt
            r.snoozeUntil = nil; r.updatedAt = now
            reminders[i] = r
            let row = r
            push { _ = try await Supa.client.from("reminders").update(row).eq("id", value: row.id).execute() }
        } else {
            let r = Reminder(id: newUUID(), userId: uid, title: title, body: body, kind: .custom, refId: nil,
                             repeat: repeatMode, repeatDays: repeatDays, intervalMin: intervalMin, timeOfDay: timeOfDay,
                             nextTriggerAt: nextTriggerAt, isEnabled: true, snoozeUntil: nil, localNotificationId: nil,
                             createdAt: now, updatedAt: now, deletedAt: nil)
            reminders.append(r)
            let row = r
            push { _ = try await Supa.client.from("reminders").insert(row).execute() }
        }
        Notifier.reschedule(activeReminders, water: water)
    }

    func toggleReminder(_ r: Reminder) {
        guard let i = reminders.firstIndex(where: { $0.id == r.id }) else { return }
        reminders[i].isEnabled.toggle()
        reminders[i].updatedAt = ISO.now()
        let row = reminders[i]
        push { _ = try await Supa.client.from("reminders").update(row).eq("id", value: row.id).execute() }
        Notifier.reschedule(activeReminders, water: water)
    }

    func deleteReminder(_ r: Reminder) {
        guard let i = reminders.firstIndex(where: { $0.id == r.id }) else { return }
        let now = ISO.now()
        reminders[i].deletedAt = now
        let id = r.id
        let payload: [String: AnyJSON] = ["deleted_at": .string(now), "updated_at": .string(now)]
        push { _ = try await Supa.client.from("reminders").update(payload).eq("id", value: id).execute() }
        Notifier.reschedule(activeReminders, water: water)
    }

    /// Apply task checkboxes tapped on the widget while the app was closed.
    private func flushPendingWidgetToggles() async {
        let pending = WidgetBridge.readPending()
        guard !pending.isEmpty else { return }
        var allOK = true
        for p in pending {
            do {
                if p.recurrence == "none" {
                    let payload: [String: AnyJSON] = [
                        "is_completed": .bool(true), "completed_at": .string(ISO.now()), "updated_at": .string(ISO.now()),
                    ]
                    try await Supa.client.from("tasks").update(payload).eq("id", value: p.id).execute()
                } else {
                    // Upsert on (task_id, occurrence_date): a prior toggle leaves a soft-deleted
                    // row and the table is unique — a plain insert would 409. Merge + clear delete.
                    let row = TaskCompletion(id: newUUID(), userId: uid, taskId: p.id, occurrenceDate: today,
                                             completedAt: ISO.now(), createdAt: ISO.now(), updatedAt: ISO.now(), deletedAt: nil)
                    try await Supa.client.from("task_completions")
                        .upsert(row, onConflict: "task_id,occurrence_date").execute()
                }
            } catch { allOK = false }
        }
        if allOK { WidgetBridge.clearPending() }
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
