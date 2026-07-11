import SwiftUI

struct TasksView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

    var body: some View {
        let dayTasks = store.tasksForToday()
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if dayTasks.isEmpty {
                    Card { Text("Nothing planned for today. Add a task to get started.").foregroundStyle(.secondary) }
                } else {
                    let sorted = dayTasks.sorted { Bool2Int(store.isTaskDone($0)) < Bool2Int(store.isTaskDone($1)) }
                    ForEach(sorted) { t in
                        TaskRow(task: t, done: store.isTaskDone(t)) { store.toggleTask(t) }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Planner")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showAdd = true } label: { Label("Add task", systemImage: "plus") }
            }
        }
        .sheet(isPresented: $showAdd) { AddTaskSheet().environmentObject(store) }
    }
}

private func Bool2Int(_ b: Bool) -> Int { b ? 1 : 0 }

struct AddTaskSheet: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @State private var title = ""
    @State private var category: Category = .other
    @State private var priority: Priority = .medium
    @State private var due = Date()
    @State private var hasStart = false
    @State private var start = Date()
    @State private var recurrence: TaskRecurrence = .none
    @State private var days: Set<Int> = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("New task").font(.title2.weight(.bold))
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
            Picker("Repeat", selection: $recurrence) {
                Text("One-time").tag(TaskRecurrence.none)
                Text("Daily").tag(TaskRecurrence.daily)
                Text("Weekly").tag(TaskRecurrence.weekly)
                Text("Monthly").tag(TaskRecurrence.monthly)
            }.pickerStyle(.segmented)
            if recurrence == .weekly { WeekdayRow(selected: $days) }

            HStack {
                Button("Cancel") { dismiss() }
                Spacer()
                Button("Save") { save() }.keyboardShortcut(.defaultAction).disabled(title.isEmpty)
            }
        }
        .padding(20)
        .frame(width: 420)
    }

    private func save() {
        let dayKey = DayKey.of(due)
        store.addTask(
            title: title, category: category, priority: priority, dueDate: dayKey,
            startTime: hasStart ? isoFrom(day: due, time: start) : nil,
            endTime: nil, recurrence: recurrence, days: Array(days).sorted()
        )
        dismiss()
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
