import SwiftUI

struct HabitsView: View {
    @EnvironmentObject var store: AppStore
    @State private var showAdd = false

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
        .sheet(isPresented: $showAdd) { AddHabitSheet().environmentObject(store) }
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

struct AddHabitSheet: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var category: Category = .health
    @State private var frequency: Frequency = .daily
    @State private var target = 1
    @State private var days: Set<Int> = []

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("New habit").font(.title2.weight(.bold))
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

            HStack {
                Button("Cancel") { dismiss() }
                Spacer()
                Button("Save") {
                    store.addHabit(name: name, category: category, frequency: frequency,
                                   target: target, days: Array(days).sorted(), color: nil)
                    dismiss()
                }.keyboardShortcut(.defaultAction).disabled(name.isEmpty)
            }
        }
        .padding(20)
        .frame(width: 420)
    }
}
