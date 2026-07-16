import SwiftUI

struct HabitDetailView: View {
    @EnvironmentObject var store: AppStore
    let habit: Habit

    private var color: Color {
        if let hex = habit.color, let v = UInt(hex.replacingOccurrences(of: "#", with: ""), radix: 16) {
            return Color(hex: v)
        }
        return Brand.category(habit.category)
    }

    var body: some View {
        let s = store.stats(habit)
        let weeks = habitMonthCells(habit, counts: store.habitCounts(habit.id))
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(habit.name).font(.largeTitle.weight(.bold))
                    Text("\(frequencyText) · target \(max(1, habit.targetCount))/day")
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 12) {
                    statTile("Current streak", "\(s.current)", Brand.amber)
                    statTile("Longest", "\(s.longest)", Brand.indigo)
                    statTile("Total done", "\(s.totalCompleted)", Brand.green)
                    statTile("Success", "\(s.successPct)%", Brand.violet)
                }

                Card {
                    VStack(alignment: .leading, spacing: 12) {
                        progress("This week", s.weekDone, s.weekTotal)
                        progress("This month", s.monthDone, s.monthTotal)
                    }
                }

                Card {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Completion calendar").font(.headline)
                        CalendarGrid(weeks: weeks, color: color)
                    }
                }
            }
            .padding(20)
        }
    }

    private var frequencyText: String {
        if habit.frequency == .daily { return "Every day" }
        let names = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
        let days = habit.customDays.isEmpty ? [] : habit.customDays
        return days.sorted().map { names[$0] }.joined(separator: ", ")
    }

    private func statTile(_ label: String, _ value: String, _ c: Color) -> some View {
        Card {
            VStack(alignment: .leading, spacing: 4) {
                Text(value).font(.title2.weight(.bold)).foregroundStyle(c)
                Text(label).font(.caption).foregroundStyle(.secondary)
            }
        }
    }

    private func progress(_ label: String, _ done: Int, _ total: Int) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(label).font(.subheadline)
                Spacer()
                Text("\(done)/\(total)").font(.subheadline).foregroundStyle(.secondary)
            }
            ProgressView(value: total > 0 ? Double(done) / Double(total) : 0).tint(color)
        }
    }
}

/// Month grid: weekday header + week rows. Cell codes -1 blank/0 off/1 done/2 missed/3 today/4 future.
struct CalendarGrid: View {
    let weeks: [[Int]]
    let color: Color
    private let labels = ["S", "M", "T", "W", "T", "F", "S"]

    var body: some View {
        VStack(spacing: 5) {
            HStack(spacing: 5) {
                ForEach(0..<7, id: \.self) { i in
                    Text(labels[i]).font(.caption2).foregroundStyle(.secondary).frame(maxWidth: .infinity)
                }
            }
            ForEach(Array(weeks.enumerated()), id: \.offset) { _, week in
                HStack(spacing: 5) {
                    ForEach(Array(week.enumerated()), id: \.offset) { _, code in
                        RoundedRectangle(cornerRadius: 6)
                            .fill(fill(code))
                            .overlay(code == 3 ? RoundedRectangle(cornerRadius: 6).stroke(color, lineWidth: 2) : nil)
                            .frame(height: 30)
                            .frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }

    private func fill(_ code: Int) -> Color {
        switch code {
        case 1: return color
        case 2: return Brand.red.opacity(0.35)
        case -1: return .clear
        default: return Color.secondary.opacity(0.12)
        }
    }
}
