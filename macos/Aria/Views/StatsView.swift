import SwiftUI

struct StatsView: View {
    @EnvironmentObject var store: AppStore
    @State private var days = 7

    var body: some View {
        let a = compute(days)
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Picker("Range", selection: $days) {
                    Text("7 days").tag(7)
                    Text("30 days").tag(30)
                }.pickerStyle(.segmented).frame(width: 240)

                HStack(spacing: 12) {
                    statCard("Task rate", "\(a.taskRate)%", Brand.indigo)
                    statCard("Habit rate", "\(a.habitRate)%", Brand.amber)
                    statCard("Water avg", "\(a.waterRate)%", Brand.blue)
                }

                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Productivity").font(.headline)
                        HStack(alignment: .bottom, spacing: 4) {
                            ForEach(Array(a.daily.enumerated()), id: \.offset) { _, v in
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Brand.indigo.opacity(0.85))
                                    .frame(height: max(3, CGFloat(v) / 100 * 120))
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .frame(height: 120)
                        Text("Avg \(a.avgScore) · last \(days) days").font(.caption).foregroundStyle(.secondary)
                    }
                }

                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Habits").font(.headline)
                        ForEach(store.activeHabits) { h in
                            let s = store.stats(h)
                            HStack {
                                Text(h.name)
                                Spacer()
                                Label("\(s.current)", systemImage: "flame").foregroundStyle(Brand.amber).font(.caption)
                                Text("\(s.successPct)%").foregroundStyle(.secondary).font(.caption).frame(width: 44, alignment: .trailing)
                            }
                        }
                        if store.activeHabits.isEmpty { Text("No habits.").foregroundStyle(.secondary).font(.caption) }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Stats")
    }

    private func statCard(_ label: String, _ value: String, _ c: Color) -> some View {
        Card {
            VStack(alignment: .leading, spacing: 4) {
                Text(value).font(.title.weight(.bold)).foregroundStyle(c)
                Text(label).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private struct Result { var taskRate = 0; var habitRate = 0; var waterRate = 0; var avgScore = 0; var daily: [Int] = [] }

    private func compute(_ n: Int) -> Result {
        let today = store.today
        var taskSched = 0, taskDone = 0, habitSched = 0, habitDone = 0
        var waterSum = 0.0, waterCount = 0
        var scores: [Int] = []
        let goal = store.water.dailyGoalMl

        for offset in stride(from: n - 1, through: 0, by: -1) {
            let day = Cal.add(today, days: -offset)

            let st = store.tasks.filter { $0.deletedAt == nil && taskOccursOn($0, day) }
            let sd = st.filter { t in
                t.recurrence == .none ? t.isCompleted
                    : store.completions.contains { $0.deletedAt == nil && $0.taskId == t.id && $0.occurrenceDate == day }
            }
            taskSched += st.count; taskDone += sd.count

            let hs = store.activeHabits.filter { isScheduledOn($0, day) }
            let hd = hs.filter { (store.habitCounts($0.id)[day] ?? 0) >= max(1, $0.targetCount) }
            habitSched += hs.count; habitDone += hd.count

            let w = waterTotal(store.waterLogs, on: day)
            if goal > 0 { waterSum += min(1, Double(w) / Double(goal)); waterCount += 1 }

            let tRate = st.isEmpty ? nil : Double(sd.count) / Double(st.count)
            let hRate = hs.isEmpty ? nil : Double(hd.count) / Double(hs.count)
            let wRate = goal > 0 ? min(1, Double(w) / Double(goal)) : nil
            scores.append(productivityScore(taskRate: tRate, habitRate: hRate, waterRate: wRate))
        }

        var r = Result()
        r.taskRate = taskSched > 0 ? Int(Double(taskDone) / Double(taskSched) * 100) : 0
        r.habitRate = habitSched > 0 ? Int(Double(habitDone) / Double(habitSched) * 100) : 0
        r.waterRate = waterCount > 0 ? Int(waterSum / Double(waterCount) * 100) : 0
        r.daily = scores
        r.avgScore = scores.isEmpty ? 0 : scores.reduce(0, +) / scores.count
        return r
    }
}
