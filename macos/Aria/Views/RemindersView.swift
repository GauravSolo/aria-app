import SwiftUI

struct RemindersView: View {
    @EnvironmentObject var store: AppStore
    @State private var showForm = false
    @State private var editing: Reminder?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    SectionTitle(text: "Reminders")
                    Spacer()
                    Button { editing = nil; showForm = true } label: { Label("Add", systemImage: "plus") }
                }

                if store.activeReminders.isEmpty {
                    Card { Text("No reminders yet.").foregroundStyle(.secondary) }
                } else {
                    ForEach(store.activeReminders) { r in
                        Card {
                            HStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(r.title).font(.headline)
                                    Text(reminderSummary(r)).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Toggle("", isOn: Binding(get: { r.isEnabled }, set: { _ in store.toggleReminder(r) }))
                                    .labelsHidden()
                                Button { editing = r; showForm = true } label: { Image(systemName: "pencil") }
                                    .buttonStyle(.plain)
                                Button { store.deleteReminder(r) } label: { Image(systemName: "trash") }
                                    .buttonStyle(.plain).foregroundStyle(Brand.red)
                            }
                        }
                    }
                }

                if !store.recentNotifications.isEmpty {
                    SectionTitle(text: "History")
                    ForEach(store.recentNotifications.prefix(20)) { n in
                        Card {
                            HStack {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(n.title).font(.subheadline)
                                    Text(fmtDateTime(n.firedAt)).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                Text(n.status.rawValue.capitalized).font(.caption).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .padding(20)
        }
        .sheet(isPresented: $showForm) {
            ReminderForm(existing: editing).environmentObject(store)
        }
    }
}

private struct ReminderForm: View {
    @EnvironmentObject var store: AppStore
    @Environment(\.dismiss) private var dismiss
    var existing: Reminder?

    @State private var title = ""
    @State private var note = ""
    @State private var repeatMode: ReminderRepeat = .once
    @State private var time = Date()
    @State private var days: Set<Int> = []
    @State private var interval = 60

    private let weekdayLabels = ["S", "M", "T", "W", "T", "F", "S"]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(existing == nil ? "New reminder" : "Edit reminder").font(.title2.bold())
            TextField("Title", text: $title)
            TextField("Note (optional)", text: $note)
            Picker("Repeat", selection: $repeatMode) {
                Text("Once").tag(ReminderRepeat.once)
                Text("Daily").tag(ReminderRepeat.daily)
                Text("Weekly").tag(ReminderRepeat.weekly)
                Text("Interval").tag(ReminderRepeat.interval)
            }
            .pickerStyle(.segmented)

            switch repeatMode {
            case .once:
                DatePicker("When", selection: $time)
            case .daily:
                DatePicker("Time", selection: $time, displayedComponents: .hourAndMinute)
            case .weekly:
                DatePicker("Time", selection: $time, displayedComponents: .hourAndMinute)
                HStack(spacing: 6) {
                    ForEach(0..<7, id: \.self) { d in
                        let on = days.contains(d)
                        Button(weekdayLabels[d]) {
                            if on { days.remove(d) } else { days.insert(d) }
                        }
                        .buttonStyle(.plain)
                        .frame(width: 30, height: 30)
                        .background(Circle().fill(on ? Brand.indigo : Color.secondary.opacity(0.15)))
                        .foregroundStyle(on ? .white : .primary)
                    }
                }
            case .interval:
                Stepper("Every \(interval) min", value: $interval, in: 15...480, step: 15)
            }

            HStack {
                Button("Cancel") { dismiss() }
                Spacer()
                Button("Save") { save() }.keyboardShortcut(.defaultAction).disabled(title.isEmpty)
            }
        }
        .padding(20)
        .frame(width: 440)
        .onAppear(perform: loadExisting)
    }

    private func hmString(_ d: Date) -> String {
        let c = Calendar.current.dateComponents([.hour, .minute], from: d)
        return String(format: "%02d:%02d", c.hour ?? 9, c.minute ?? 0)
    }

    private func isoString(_ d: Date) -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f.string(from: d)
    }

    private func save() {
        store.saveReminder(
            existing,
            title: title,
            body: note.isEmpty ? nil : note,
            repeatMode: repeatMode,
            repeatDays: repeatMode == .weekly ? Array(days) : [],
            intervalMin: repeatMode == .interval ? interval : nil,
            timeOfDay: (repeatMode == .daily || repeatMode == .weekly) ? hmString(time) : nil,
            nextTriggerAt: repeatMode == .once ? isoString(time) : nil
        )
        dismiss()
    }

    private func loadExisting() {
        guard let e = existing else { return }
        title = e.title
        note = e.body ?? ""
        repeatMode = e.repeat
        days = Set(e.repeatDays)
        interval = e.intervalMin ?? 60
        if let hm = e.timeOfDay {
            let p = hm.split(separator: ":")
            var c = Calendar.current.dateComponents([.year, .month, .day], from: Date())
            c.hour = Int(p.first ?? "9"); c.minute = p.count > 1 ? Int(p[1]) : 0
            time = Calendar.current.date(from: c) ?? Date()
        }
    }
}
