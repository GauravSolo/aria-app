import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// App Groups don't provision on a free Apple ID (`containerURL(forSecurityApplicationGroupIdentifier:)`
/// returns nil), so we can't share via a group container. Instead the **un-sandboxed
/// app** writes the snapshot straight into the **widget extension's own sandbox
/// container**, and the still-sandboxed widget reads it from its own container.
/// Both sides resolve to the same file:
///   ~/Library/Containers/<widget-bundle-id>/Data/Library/Application Support/widget-snapshot.json
let ariaWidgetBundleID = "com.aria.planner.AriaWidget"
private let snapshotFileName = "widget-snapshot.json"

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
    /// Widget side — its own Application Support dir (inside its sandbox container).
    private static var ownURL: URL? {
        try? FileManager.default
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appendingPathComponent(snapshotFileName)
    }

    /// App side — the widget extension's container Application Support dir. Uses the
    /// real home directory (the app is un-sandboxed), and creates the path if needed.
    private static var widgetContainerURL: URL? {
        let dir = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Containers/\(ariaWidgetBundleID)/Data/Library/Application Support", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent(snapshotFileName)
    }

    /// Called by the app to publish a new snapshot + refresh the widget.
    static func write(_ snap: WidgetSnapshot) {
        guard let url = widgetContainerURL, let data = try? JSONEncoder().encode(snap) else { return }
        try? data.write(to: url, options: .atomic)
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }

    /// Called by the widget to load the latest snapshot.
    static func read() -> WidgetSnapshot {
        guard let url = ownURL,
              let data = try? Data(contentsOf: url),
              let snap = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return .empty }
        return snap
    }
}
