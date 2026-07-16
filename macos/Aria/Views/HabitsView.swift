import SwiftUI

struct HabitsView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false
    @State private var editing: Habit?
    @State private var detail: Habit?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if store.activeHabits.isEmpty {
                    Card { Text("No habits yet. Add one to start a streak.").foregroundStyle(.secondary) }
                } else {
                    ForEach(store.activeHabits) { h in
                        let s = store.stats(h)
                        Card {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(spacing: 12) {
                                    if s.scheduledToday {
                                        Button { store.toggleHabit(h) } label: {
                                            CheckCircle(done: s.doneToday, color: Brand.category(h.category))
                                        }.buttonStyle(.plain)
                                    } else {
                                        Image(systemName: "moon.zzz").foregroundStyle(.secondary)
                                    }
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(h.name).font(.headline)
                                        Text(frequencyText(h)).font(.caption).foregroundStyle(.secondary)
                                    }
                                    Spacer()
                                    Label("\(s.current)", systemImage: "flame").foregroundStyle(Brand.amber)
                                    Button { detail = h } label: { Image(systemName: "calendar") }.buttonStyle(.plain)
                                    Button { editing = h } label: { Image(systemName: "pencil") }.buttonStyle(.plain)
                                    Button { store.deleteHabit(h) } label: { Image(systemName: "trash") }
                                        .buttonStyle(.plain).foregroundStyle(Brand.red)
                                }
                                HStack(spacing: 16) {
                                    miniStat("Longest", "\(s.longest)")
                                    miniStat("Done", "\(s.totalCompleted)")
                                    miniStat("Success", "\(s.successPct)%")
                                }
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Habits")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button { showAdd = true } label: { Label("Add habit", systemImage: "plus") }
            }
        }
        .sheet(isPresented: $showAdd) { HabitForm(existing: nil).environmentObject(store) }
        .sheet(item: $editing) { HabitForm(existing: $0).environmentObject(store) }
        .sheet(item: $detail) { h in
            NavigationStack {
                HabitDetailView(habit: store.habits.first { $0.id == h.id } ?? h).environmentObject(store)
            }
            .frame(minWidth: 460, minHeight: 560)
        }
    }

    private func miniStat(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading) {
            Text(value).font(.title3.weight(.semibold))
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
    }

    private func frequencyText(_ h: Habit) -> String {
        if h.frequency == .daily { return "Every day" }
        let names = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
        let days = h.customDays.isEmpty ? [Cal.weekday(h.startDate)] : h.customDays
        return days.sorted().map { names[$0] }.joined(separator: ", ")
    }
}

struct HabitForm: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss
    var existing: Habit?

    @State private var name = ""
    @State private var category: Category = .health
    @State private var frequency: Frequency = .daily
    @State private var target = 1
    @State private var days: Set<Int> = []
    @State private var notes = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(existing == nil ? "New habit" : "Edit habit").font(.title2.weight(.bold))
            TextField("Habit name", text: $name).textFieldStyle(.roundedBorder)
            Picker("Category", selection: $category) {
                ForEach(Category.allCases) { Text($0.label).tag($0) }
            }
            Picker("Frequency", selection: $frequency) {
                Text("Daily").tag(Frequency.daily)
                Text("Weekly").tag(Frequency.weekly)
                Text("Custom").tag(Frequency.custom)
            }.pickerStyle(.segmented)
            if frequency != .daily { WeekdayRow(selected: $days) }
            Stepper("Target per day: \(target)", value: $target, in: 1...50)
            TextField("Notes (optional)", text: $notes).textFieldStyle(.roundedBorder)

            HStack {
                if existing != nil {
                    Button(role: .destructive) {
                        if let e = existing { store.deleteHabit(e) }
                        dismiss()
                    } label: { Label("Delete", systemImage: "trash") }
                }
                Button("Cancel") { dismiss() }
                Spacer()
                Button("Save") { save() }.keyboardShortcut(.defaultAction).disabled(name.isEmpty)
            }
        }
        .padding(20)
        .frame(width: 440)
        .onAppear {
            guard let e = existing else { return }
            name = e.name; category = e.category; frequency = e.frequency
            target = max(1, e.targetCount); days = Set(e.customDays); notes = e.notes ?? ""
        }
    }

    private func save() {
        let d = Array(days).sorted()
        if let e = existing {
            store.updateHabit(e, name: name, category: category, frequency: frequency,
                              target: target, days: d, color: e.color, notes: notes.isEmpty ? nil : notes)
        } else {
            store.addHabit(name: name, category: category, frequency: frequency, target: target, days: d, color: nil)
        }
        dismiss()
    }
}
