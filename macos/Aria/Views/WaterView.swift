import SwiftUI

struct WaterView: View {
    @EnvironmentObject var store: AppStore

    var body: some View {
        let goal = store.water.dailyGoalMl
        let total = store.waterToday
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Card {
                    VStack(spacing: 12) {
                        ZStack {
                            Ring(progress: goal > 0 ? Double(total) / Double(goal) : 0, color: Brand.blue, size: 170, line: 14)
                            VStack {
                                Text("\(goal > 0 ? Int(Double(total) / Double(goal) * 100) : 0)%")
                                    .font(.system(size: 34, weight: .bold)).foregroundStyle(Brand.blue)
                                Text("\(formatMl(total)) / \(formatMl(goal))").font(.callout).foregroundStyle(.secondary)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }

                HStack {
                    Button { store.logWater(store.water.glassSizeMl) } label: {
                        Label("Glass · \(store.water.glassSizeMl)ml", systemImage: "drop.fill")
                            .frame(maxWidth: .infinity)
                    }.buttonStyle(.borderedProminent).tint(Brand.blue)
                    Button { store.logWater(500) } label: {
                        Label("500ml", systemImage: "plus").frame(maxWidth: .infinity)
                    }.buttonStyle(.bordered)
                }

                let week = weekData
                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("This week").font(.headline)
                        HStack(alignment: .bottom, spacing: 8) {
                            ForEach(week, id: \.key) { d in
                                VStack(spacing: 4) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(d.ml >= goal && goal > 0 ? Brand.blue : Brand.blue.opacity(0.45))
                                        .frame(height: max(3, CGFloat(goal > 0 ? min(1, Double(d.ml) / Double(goal)) : 0) * 110))
                                        .frame(maxWidth: .infinity)
                                    Text(d.label).font(.caption2).foregroundStyle(.secondary)
                                }
                            }
                        }
                        .frame(height: 130)
                    }
                }

                let m = monthStats
                HStack(spacing: 12) {
                    miniStat("30-day avg", formatMl(m.avg), Brand.blue)
                    miniStat("Goal met", "\(m.metDays)/30", Brand.green)
                    miniStat("Best day", formatMl(m.best), Brand.indigo)
                }

                SectionTitle(text: "Settings")
                Card {
                    VStack(alignment: .leading, spacing: 14) {
                        Stepper("Daily goal: \(formatMl(store.water.dailyGoalMl))",
                                onIncrement: { changeGoal(250) }, onDecrement: { changeGoal(-250) })
                        Stepper("Glass size: \(store.water.glassSizeMl) ml",
                                onIncrement: { changeGlass(50) }, onDecrement: { changeGlass(-50) })
                        Toggle("Reminders", isOn: Binding(
                            get: { store.water.reminderEnabled },
                            set: { v in var w = store.water; w.reminderEnabled = v; w.updatedAt = ISO.now(); store.saveWaterSettings(w) }
                        ))
                        if store.water.reminderEnabled {
                            Stepper("Every \(store.water.reminderIntervalMin) min",
                                    onIncrement: { changeInterval(15) }, onDecrement: { changeInterval(-15) })
                            Stepper("Active from \(store.water.activeStart)",
                                    onIncrement: { changeHour(\.activeStart, 1) }, onDecrement: { changeHour(\.activeStart, -1) })
                            Stepper("Active until \(store.water.activeEnd)",
                                    onIncrement: { changeHour(\.activeEnd, 1) }, onDecrement: { changeHour(\.activeEnd, -1) })
                        }
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Water")
    }

    private func miniStat(_ label: String, _ value: String, _ c: Color) -> some View {
        Card {
            VStack(alignment: .leading, spacing: 4) {
                Text(value).font(.title3.weight(.bold)).foregroundStyle(c)
                Text(label).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private var weekData: [(key: String, label: String, ml: Int)] {
        let today = store.today
        return (0...6).reversed().map { off in
            let day = Cal.add(today, days: -off)
            return (day, Fmt.weekdayShort(Cal.date(day)).prefix(1).uppercased(), waterTotal(store.waterLogs, on: day))
        }.map { (key: $0.0, label: $0.1, ml: $0.2) }
    }

    private var monthStats: (avg: Int, metDays: Int, best: Int) {
        let today = store.today
        let goal = store.water.dailyGoalMl
        var sum = 0, met = 0, best = 0
        for off in 0..<30 {
            let ml = waterTotal(store.waterLogs, on: Cal.add(today, days: -off))
            sum += ml; best = max(best, ml)
            if goal > 0 && ml >= goal { met += 1 }
        }
        return (sum / 30, met, best)
    }

    private func changeInterval(_ delta: Int) {
        var w = store.water
        w.reminderIntervalMin = max(15, min(240, w.reminderIntervalMin + delta))
        w.updatedAt = ISO.now(); store.saveWaterSettings(w)
    }

    private func changeHour(_ kp: WritableKeyPath<WaterSettings, String>, _ delta: Int) {
        var w = store.water
        let hour = Int(w[keyPath: kp].prefix(2)) ?? 8
        let nh = max(0, min(23, hour + delta))
        w[keyPath: kp] = String(format: "%02d:00", nh)
        w.updatedAt = ISO.now(); store.saveWaterSettings(w)
    }

    private func changeGoal(_ delta: Int) {
        var w = store.water
        w.dailyGoalMl = max(500, min(8000, w.dailyGoalMl + delta))
        w.updatedAt = ISO.now()
        store.saveWaterSettings(w)
    }

    private func changeGlass(_ delta: Int) {
        var w = store.water
        w.glassSizeMl = max(50, min(1000, w.glassSizeMl + delta))
        w.updatedAt = ISO.now()
        store.saveWaterSettings(w)
    }
}
