import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// App Groups don't provision on a free Apple ID, so instead of a group container
/// the **un-sandboxed app** writes into the **widget extension's own sandbox
/// container**, and the still-sandboxed widget reads/writes there too. Both sides
/// resolve to the same files under:
///   ~/Library/Containers/<widget-bundle-id>/Data/Library/Application Support/
let ariaWidgetBundleID = "com.aria.planner.AriaWidget"
private let snapshotFileName = "widget-snapshot.json"
private let pendingFileName = "widget-pending.json"
private let calendarFileName = "widget-calendar.json"

/// A habit or recurring task rendered as a month calendar for the calendar widgets.
/// `weeks` cells: -1 blank · 0 off · 1 done · 2 missed · 3 today-pending · 4 future.
struct CalendarEntry: Codable, Identifiable {
    var id: String
    var name: String
    var colorHex: String?
    var current: Int
    var longest: Int
    var weeks: [[Int]]
}

struct CalendarData: Codable {
    var habits: [CalendarEntry]
    var tasks: [CalendarEntry]
    static let empty = CalendarData(habits: [], tasks: [])
}

struct WidgetTask: Codable, Identifiable {
    var id: String
    var title: String
    var recurrence: String
}

struct PendingToggle: Codable {
    var id: String
    var recurrence: String
}

struct WidgetSnapshot: Codable {
    var generatedAt: String
    var nextTaskTitle: String?
    var tasks: [WidgetTask]
    var pendingTasks: Int
    var totalTasks: Int
    var waterMl: Int
    var waterGoal: Int
    var habitsDone: Int
    var habitsTotal: Int
    var topStreak: Int

    var waterPct: Int { waterGoal > 0 ? Int((Double(waterMl) / Double(waterGoal) * 100).rounded()) : 0 }

    static let empty = WidgetSnapshot(
        generatedAt: "", nextTaskTitle: nil, tasks: [], pendingTasks: 0, totalTasks: 0,
        waterMl: 0, waterGoal: 4000, habitsDone: 0, habitsTotal: 0, topStreak: 0
    )
}

enum WidgetBridge {
    /// Widget side — its own sandbox container's Application Support dir.
    private static func ownDir() -> URL? {
        try? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
    }

    /// App side — the widget extension's container Application Support dir (real home,
    /// since the app is un-sandboxed). Created if missing.
    private static func widgetContainerDir() -> URL? {
        let dir = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent("Library/Containers/\(ariaWidgetBundleID)/Data/Library/Application Support", isDirectory: true)
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }

    private static func reload() {
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }

    // ── App side ────────────────────────────────────────────────────────────
    static func write(_ snap: WidgetSnapshot) {
        guard let url = widgetContainerDir()?.appendingPathComponent(snapshotFileName),
              let data = try? JSONEncoder().encode(snap) else { return }
        try? data.write(to: url, options: .atomic)
        reload()
    }

    /// Widget checkbox taps that happened while the app was closed, for the app to sync.
    static func readPending() -> [PendingToggle] {
        guard let url = widgetContainerDir()?.appendingPathComponent(pendingFileName),
              let data = try? Data(contentsOf: url) else { return [] }
        return (try? JSONDecoder().decode([PendingToggle].self, from: data)) ?? []
    }

    static func clearPending() {
        guard let url = widgetContainerDir()?.appendingPathComponent(pendingFileName) else { return }
        try? FileManager.default.removeItem(at: url)
    }

    // ── Widget side ─────────────────────────────────────────────────────────
    static func read() -> WidgetSnapshot {
        guard let url = ownDir()?.appendingPathComponent(snapshotFileName),
              let data = try? Data(contentsOf: url),
              let snap = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return .empty }
        return snap
    }

    /// Optimistic snapshot update from a widget interaction (same file the app writes).
    static func writeOptimistic(_ snap: WidgetSnapshot) {
        guard let url = ownDir()?.appendingPathComponent(snapshotFileName),
              let data = try? JSONEncoder().encode(snap) else { return }
        try? data.write(to: url, options: .atomic)
    }

    // ── Calendar data (habit streak + task calendar widgets) ─────────────────
    static func writeCalendar(_ data: CalendarData) {
        guard let url = widgetContainerDir()?.appendingPathComponent(calendarFileName),
              let bytes = try? JSONEncoder().encode(data) else { return }
        try? bytes.write(to: url, options: .atomic)
        reload()
    }

    static func readCalendar() -> CalendarData {
        guard let url = ownDir()?.appendingPathComponent(calendarFileName),
              let data = try? Data(contentsOf: url),
              let cal = try? JSONDecoder().decode(CalendarData.self, from: data)
        else { return .empty }
        return cal
    }

    static func enqueuePending(_ p: PendingToggle) {
        guard let url = ownDir()?.appendingPathComponent(pendingFileName) else { return }
        var arr: [PendingToggle] = []
        if let data = try? Data(contentsOf: url) {
            arr = (try? JSONDecoder().decode([PendingToggle].self, from: data)) ?? []
        }
        arr.append(p)
        if let data = try? JSONEncoder().encode(arr) { try? data.write(to: url, options: .atomic) }
    }
}
