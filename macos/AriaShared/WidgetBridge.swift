import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// Shared between the app and the widget extension (App Group). The app writes a
/// snapshot after data changes; the widget reads it to render.
let ariaAppGroup = "group.com.aria.planner"

struct WidgetSnapshot: Codable {
    var generatedAt: String
    var nextTaskTitle: String?
    var pendingTasks: Int
    var totalTasks: Int
    var waterMl: Int
    var waterGoal: Int
    var habitsDone: Int
    var habitsTotal: Int
    var topStreak: Int

    var waterPct: Int { waterGoal > 0 ? Int((Double(waterMl) / Double(waterGoal) * 100).rounded()) : 0 }

    static let empty = WidgetSnapshot(
        generatedAt: "", nextTaskTitle: nil, pendingTasks: 0, totalTasks: 0,
        waterMl: 0, waterGoal: 4000, habitsDone: 0, habitsTotal: 0, topStreak: 0
    )
}

enum WidgetBridge {
    static let key = "aria.widget.snapshot"

    static func write(_ snap: WidgetSnapshot) {
        guard let data = try? JSONEncoder().encode(snap),
              let store = UserDefaults(suiteName: ariaAppGroup) else { return }
        store.set(data, forKey: key)
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }

    static func read() -> WidgetSnapshot {
        guard let store = UserDefaults(suiteName: ariaAppGroup),
              let data = store.data(forKey: key),
              let snap = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return .empty }
        return snap
    }
}
