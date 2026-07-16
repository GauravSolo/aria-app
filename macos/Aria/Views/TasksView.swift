import SwiftUI

struct TasksView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var editing: Task?
    @State private var selected = DayKey.today()

    var body: some View {
        let dayTasks = store.tasksOn(selected)
        VStack(spacing: 0) {
            DateStrip(selected: $selected)
                .padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 8)
            Divider()
            ScrollView {
                VStack(alignment: .leading, spacing: 8) {
                    if dayTasks.isEmpty {
                        Card { Text("Nothing planned for \(dayLabel). Add a task to get started.").foregroundStyle(.secondary) }
                    } else {
                        ForEach(dayTasks) { t in
                            HStack(alignment: .top, spacing: 10) {
                                Text(timeLabel(t)).font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                                    .frame(width: 54, alignment: .trailing).padding(.top, 10)
                                TaskRow(task: t, done: store.isTaskDone(t, on: selected)) { store.toggleTask(t, on: selected) }
                                    .contextMenu {
                                        Button("Edit") { editing = t }
                                        Button("Delete", role: .destructive) { store.deleteTask(t) }
                                    }
                            }
                        }
                    }
                }
                .padding(20)
            }
        }
        .navigationTitle("Planner")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showAdd = true } label: { Label("Add task", systemImage: "plus") }
            }
        }
        .sheet(isPresented: $showAdd) { TaskForm(existing: nil, defaultDate: selected).environmentObject(store) }
        .sheet(item: $editing) { TaskForm(existing: $0, defaultDate: selected).environmentObject(store) }
    }

    private var dayLabel: String { selected == DayKey.today() ? "today" : Fmt.medium(Cal.date(selected)) }

    private func timeLabel(_ t: Task) -> String {
        guard let st = t.startTime else { return "—" }
        let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let d = f.date(from: st) ?? ISO8601DateFormatter().date(from: st) else { return "—" }
        let out = DateFormatter(); out.dateFormat = "h:mm a"
        return out.string(from: d)
    }
}

/// Horizontal 14-day picker centered on today.
struct DateStrip: View {
    @Binding var selected: String
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(-3...10, id: \.self) { offset in
                        let day = Cal.add(DayKey.today(), days: offset)
                        let on = day == selected
                        let date = Cal.date(day)
                        Button { selected = day } label: {
                            VStack(spacing: 2) {
                                Text(Fmt.weekdayShort(date)).font(.caption2)
                                Text(Fmt.dayNum(date)).font(.headline)
                            }
                            .frame(width: 44, height: 52)
                            .background(on ? Brand.indigo : Color.secondary.opacity(0.12))
                            .foregroundStyle(on ? .white : .primary)
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }.buttonStyle(.plain).id(day)
                    }
                }
            }
            .onAppear { proxy.scrollTo(selected, anchor: .center) }
        }
    }
}

struct TaskForm: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss
    var existing: Task?
    var defaultDate: String = DayKey.today()

    @State private var title = ""
    @State private var category: Category = .other
    @State private var priority: Priority = .medium
    @State private var due = Date()
    @State private var hasStart = false
    @State private var start = Date()
    @State private var hasEnd = false
    @State private var end = Date()
    @State private var recurrence: TaskRecurrence = .none
    @State private var days: Set<Int> = []
    @State private var interval = 1
    @State private var hasUntil = false
    @State private var until = Date()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(existing == nil ? "New task" : "Edit task").font(.title2.weight(.bold))
            TextField("Title", text: $title).textFieldStyle(.roundedBorder)
            HStack {
                Picker("Category", selection: $category) {
                    ForEach(Category.allCases) { Text($0.label).tag($0) }
                }
                Picker("Priority", selection: $priority) {
                    ForEach(Priority.allCases) { Text($0.label).tag($0) }
                }
            }
            DatePicker("Date", selection: $due, displayedComponents: .date)
            Toggle("Set start time", isOn: $hasStart)
            if hasStart { DatePicker("Start", selection: $start, displayedComponents: .hourAndMinute) }
            Toggle("Set end time", isOn: $hasEnd)
            if hasEnd { DatePicker("End", selection: $end, displayedComponents: .hourAndMinute) }
            Picker("Repeat", selection: $recurrence) {
                Text("Once").tag(TaskRecurrence.none)
                Text("Daily").tag(TaskRecurrence.daily)
                Text("Weekly").tag(TaskRecurrence.weekly)
                Text("Monthly").tag(TaskRecurrence.monthly)
                Text("Custom").tag(TaskRecurrence.custom)
            }.pickerStyle(.segmented)
            if recurrence == .weekly { WeekdayRow(selected: $days) }
            if recurrence == .custom { Stepper("Every \(interval) day\(interval == 1 ? "" : "s")", value: $interval, in: 1...90) }
            if recurrence != .none {
                Toggle("Ends on a date", isOn: $hasUntil)
                if hasUntil { DatePicker("Until", selection: $until, in: due..., displayedComponents: .date) }
            }

            HStack {
                if existing != nil {
                    Button(role: .destructive) {
                        if let e = existing { store.deleteTask(e) }
                        dismiss()
                    } label: { Label("Delete", systemImage: "trash") }
                }
                Button("Cancel") { dismiss() }
                Spacer()
                Button("Save") { save() }.keyboardShortcut(.defaultAction).disabled(title.isEmpty)
            }
        }
        .padding(20)
        .frame(width: 440)
        .onAppear(perform: loadExisting)
    }

    private func save() {
        let dayKey = DayKey.of(due)
        let startISO = hasStart ? isoFrom(day: due, time: start) : nil
        let endISO = hasEnd ? isoFrom(day: due, time: end) : nil
        let d = recurrence == .weekly ? Array(days).sorted() : []
        let iv = recurrence == .custom ? interval : 1
        let untilKey = (recurrence != .none && hasUntil) ? DayKey.of(until) : nil
        if let e = existing {
            store.updateTask(e, title: title, category: category, priority: priority, dueDate: dayKey,
                             startTime: startISO, endTime: endISO, recurrence: recurrence, days: d,
                             interval: iv, until: untilKey)
        } else {
            store.addTask(title: title, category: category, priority: priority, dueDate: dayKey,
                          startTime: startISO, endTime: endISO, recurrence: recurrence, days: d,
                          interval: iv, until: untilKey)
        }
        dismiss()
    }

    private func loadExisting() {
        guard let e = existing else {
            due = Cal.date(defaultDate)
            return
        }
        title = e.title; category = e.category; priority = e.priority
        due = Cal.date(e.dueDate ?? DayKey.today())
        recurrence = e.recurrence; days = Set(e.recurrenceDays)
        interval = max(1, e.recurrenceInterval)
        if let u = e.recurrenceEndDate { hasUntil = true; until = Cal.date(u) }
        hasStart = loadTime(e.startTime) { start = $0 }
        hasEnd = loadTime(e.endTime) { end = $0 }
    }

    private func loadTime(_ iso: String?, _ set: (Date) -> Void) -> Bool {
        guard let st = iso else { return false }
        let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let d = f.date(from: st) ?? ISO8601DateFormatter().date(from: st) else { return false }
        set(d); return true
    }
}

struct WeekdayRow: View {
    @Binding var selected: Set<Int>
    private let labels = ["S", "M", "T", "W", "T", "F", "S"]
    var body: some View {
        HStack {
            ForEach(0..<7, id: \.self) { d in
                let on = selected.contains(d)
                Button(labels[d]) {
                    if on { selected.remove(d) } else { selected.insert(d) }
                }
                .buttonStyle(.borderedProminent)
                .tint(on ? Brand.indigo : Color.secondary.opacity(0.2))
            }
        }
    }
}

func isoFrom(day: Date, time: Date) -> String {
    let cal = Calendar.current
    let d = cal.dateComponents([.year, .month, .day], from: day)
    let t = cal.dateComponents([.hour, .minute], from: time)
    var merged = DateComponents()
    merged.year = d.year; merged.month = d.month; merged.day = d.day
    merged.hour = t.hour; merged.minute = t.minute
    let date = cal.date(from: merged) ?? day
    let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    return f.string(from: date)
}
