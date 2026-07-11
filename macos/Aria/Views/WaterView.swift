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

                SectionTitle(text: "Settings")
                Card {
                    VStack(alignment: .leading, spacing: 14) {
                        Stepper("Daily goal: \(formatMl(store.water.dailyGoalMl))",
                                onIncrement: { changeGoal(250) }, onDecrement: { changeGoal(-250) })
                        Stepper("Glass size: \(store.water.glassSizeMl) ml",
                                onIncrement: { changeGlass(50) }, onDecrement: { changeGlass(-50) })
                        Toggle("Reminders", isOn: Binding(
                            get: { store.water.reminderEnabled },
                            set: { var w = store.water; w.reminderEnabled = $0; w.updatedAt = ISO.now(); store.saveWaterSettings(w) }
                        ))
                    }
                }
            }
            .padding(20)
        }
        .navigationTitle("Water")
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
