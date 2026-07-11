import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// Shared between the app and the widget extension via the App Group **container file**
/// (not UserDefaults — `UserDefaults(suiteName:)` for an app group is blocked by the
/// macOS sandbox with a "accessing preferences outside an application's container"
/// error). Reading/writing a file in `containerURL(forSecurityApplicationGroupIdentifier:)`
/// is sandbox-permitted for both processes.
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
    private static var fileURL: URL? {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: ariaAppGroup)?
            .appendingPathComponent("widget-snapshot.json")
    }

    static func write(_ snap: WidgetSnapshot) {
        guard let url = fileURL, let data = try? JSONEncoder().encode(snap) else { return }
        try? data.write(to: url, options: .atomic)
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }

    static func read() -> WidgetSnapshot {
        guard let url = fileURL,
              let data = try? Data(contentsOf: url),
              let snap = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return .empty }
        return snap
    }
}
