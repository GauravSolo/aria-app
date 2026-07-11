import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var store: AppStore

    var body: some View {
        let dayTasks = store.tasksForToday()
        let tasksDone = dayTasks.filter { store.isTaskDone($0) }.count
        let scheduled = store.activeHabits.map { store.stats($0) }.filter { $0.scheduledToday }
        let habitsDone = scheduled.filter { $0.doneToday }.count
        let goal = store.water.dailyGoalMl
        let score = productivityScore(
            taskRate: dayTasks.isEmpty ? nil : Double(tasksDone) / Double(dayTasks.count),
            habitRate: scheduled.isEmpty ? nil : Double(habitsDone) / Double(scheduled.count),
            waterRate: goal > 0 ? Double(store.waterToday) / Double(goal) : nil
        )

        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Card {
                    HStack(spacing: 18) {
                        ZStack {
                            Ring(progress: Double(score) / 100, color: Brand.indigo, size: 96, line: 9)
                            Text("\(score)").font(.title.weight(.bold)).foregroundStyle(Brand.indigo)
                        }
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Productivity today").font(.headline)
                            Text(score >= 80 ? "On fire 🔥" : score >= 50 ? "Nice progress" : "Let’s get going")
                                .foregroundStyle(.secondary)
                            HStack(spacing: 16) {
                                Label("\(tasksDone)/\(dayTasks.count)", systemImage: "checkmark.circle").foregroundStyle(Brand.indigo)
                                Label("\(habitsDone)/\(scheduled.count)", systemImage: "flame").foregroundStyle(Brand.amber)
                                Label("\(goal > 0 ? Int(Double(store.waterToday) / Double(goal) * 100) : 0)%", systemImage: "drop").foregroundStyle(Brand.blue)
                            }.font(.footnote)
                        }
                        Spacer()
                    }
                }

                SectionTitle(text: "Today’s tasks")
                if dayTasks.isEmpty {
                    Card { Text("No tasks today.").foregroundStyle(.secondary) }
                } else {
                    ForEach(dayTasks.prefix(6)) { t in
                        TaskRow(task: t, done: store.isTaskDone(t)) { store.toggleTask(t) }
                    }
                }

                if !scheduled.isEmpty {
                    SectionTitle(text: "Today’s habits")
                    Card {
                        VStack(spacing: 12) {
                            ForEach(store.activeHabits.filter { store.stats($0).scheduledToday }) { h in
                                let s = store.stats(h)
                                HStack(spacing: 12) {
                                    Button { store.toggleHabit(h) } label: {
                                        CheckCircle(done: s.doneToday, color: h.color.map { Color(hex: UInt($0.dropFirst(), radix: 16) ?? 0x6366F1) } ?? Brand.category(h.category))
                                    }.buttonStyle(.plain)
                                    Text(h.name)
                                    Spacer()
                                    if s.current > 0 { Label("\(s.current)", systemImage: "flame").font(.footnote).foregroundStyle(Brand.amber) }
                                }
                            }
                        }
                    }
                }

                SectionTitle(text: "Water")
                Card {
                    HStack(spacing: 14) {
                        ZStack {
                            Ring(progress: goal > 0 ? Double(store.waterToday) / Double(goal) : 0, color: Brand.blue, size: 56, line: 7)
                            Image(systemName: "drop.fill").foregroundStyle(Brand.blue)
                        }
                        VStack(alignment: .leading) {
                            Text("\(formatMl(store.waterToday)) / \(formatMl(goal))").font(.headline)
                            Text(store.waterToday >= goal ? "Goal reached 💧" : "\(formatMl(max(0, goal - store.waterToday))) to go")
                                .font(.footnote).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Button { store.logWater(store.water.glassSizeMl) } label: {
                            Image(systemName: "plus.circle.fill").font(.title).foregroundStyle(Brand.blue)
                        }.buttonStyle(.plain)
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Today")
    }
}

struct TaskRow: View {
    let task: Task
    let done: Bool
    let onToggle: () -> Void
    var body: some View {
        Card {
            HStack(spacing: 12) {
                Button(action: onToggle) { CheckCircle(done: done, color: Brand.category(task.category)) }
                    .buttonStyle(.plain)
                VStack(alignment: .leading, spacing: 3) {
                    Text(task.title).strikethrough(done).foregroundStyle(done ? .secondary : .primary)
                    HStack(spacing: 8) {
                        if let st = task.startTime { Label(timeString(st), systemImage: "clock").font(.caption).foregroundStyle(.secondary) }
                        Text(task.category.label).font(.caption).foregroundStyle(Brand.category(task.category))
                    }
                }
                Spacer()
                Circle().fill(Brand.priority(task.priority)).frame(width: 8, height: 8)
            }
        }
    }
}

func timeString(_ iso: String) -> String {
    let inFmt = ISO8601DateFormatter()
    inFmt.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    let date = inFmt.date(from: iso) ?? ISO8601DateFormatter().date(from: iso)
    guard let date else { return "" }
    let out = DateFormatter(); out.dateFormat = "h:mm a"
    return out.string(from: date)
}
