import AppIntents
import SwiftUI
import WidgetKit

private enum CC {
    static let bg = Color(hex: 0x15171C)
    static let text = Color(hex: 0xF2F3F5)
    static let muted = Color(hex: 0xA6ACB8)
    static let amber = Color(hex: 0xFBBF24)
    static let track = Color(hex: 0x2A2E37)
    static let missed = Color(hex: 0x7F3B3B)
    static let defaultDone = Color(hex: 0x6366F1)
}

private func doneColor(_ hex: String?) -> Color {
    guard let hex, let v = UInt(hex.replacingOccurrences(of: "#", with: ""), radix: 16) else { return CC.defaultDone }
    return Color(hex: v)
}

// MARK: - Shared calendar view

struct CalendarGridView: View {
    @Environment(\.widgetFamily) private var family
    let entry: CalendarEntry?
    let emptyHint: String

    private var small: Bool { family == .systemSmall }

    var body: some View {
        VStack(alignment: .leading, spacing: small ? 6 : 10) {
            if let e = entry {
                HStack {
                    Text(e.name).font(.system(size: small ? 12 : 14, weight: .bold)).foregroundStyle(CC.text).lineLimit(1)
                    Spacer()
                    Label("\(e.current)", systemImage: "flame.fill").font(.system(size: small ? 11 : 12, weight: .bold)).foregroundStyle(CC.amber)
                }
                let done = doneColor(e.colorHex)
                HStack(alignment: .top, spacing: small ? 2 : 4) {
                    ForEach(Array(e.weeks.enumerated()), id: \.offset) { _, week in
                        VStack(spacing: small ? 2 : 4) {
                            ForEach(Array(week.enumerated()), id: \.offset) { _, code in
                                RoundedRectangle(cornerRadius: small ? 2 : 4)
                                    .fill(cellColor(code, done: done))
                                    .aspectRatio(1, contentMode: .fit)
                            }
                        }
                    }
                }
            } else {
                Text("Streak").font(.system(size: 12, weight: .bold)).foregroundStyle(CC.defaultDone)
                Text(emptyHint).font(.system(size: 12)).foregroundStyle(CC.muted)
                Spacer()
            }
        }
        .padding(small ? 10 : 14)
        .containerBackground(CC.bg, for: .widget)
    }

    private func cellColor(_ code: Int, done: Color) -> Color {
        switch code {
        case 1: return done
        case 2: return CC.missed
        case -1: return .clear
        default: return CC.track
        }
    }
}

// MARK: - Habit streak widget

struct HabitEntity: AppEntity, Identifiable {
    var id: String
    var name: String
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Habit"
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }
    static var defaultQuery = HabitEntityQuery()
}

struct HabitEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [HabitEntity] {
        WidgetBridge.readCalendar().habits.filter { identifiers.contains($0.id) }.map { HabitEntity(id: $0.id, name: $0.name) }
    }
    func suggestedEntities() async throws -> [HabitEntity] {
        WidgetBridge.readCalendar().habits.map { HabitEntity(id: $0.id, name: $0.name) }
    }
}

struct SelectHabitIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Pick a habit"
    @Parameter(title: "Habit") var habit: HabitEntity?
}

struct HabitCalEntry: TimelineEntry { let date: Date; let entry: CalendarEntry? }

struct HabitCalProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> HabitCalEntry { HabitCalEntry(date: Date(), entry: nil) }
    func snapshot(for configuration: SelectHabitIntent, in context: Context) async -> HabitCalEntry {
        HabitCalEntry(date: Date(), entry: resolve(configuration))
    }
    func timeline(for configuration: SelectHabitIntent, in context: Context) async -> Timeline<HabitCalEntry> {
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
        return Timeline(entries: [HabitCalEntry(date: Date(), entry: resolve(configuration))], policy: .after(next))
    }
    private func resolve(_ c: SelectHabitIntent) -> CalendarEntry? {
        let habits = WidgetBridge.readCalendar().habits
        if let id = c.habit?.id { return habits.first { $0.id == id } }
        return habits.first
    }
}

struct HabitCalWidget: Widget {
    let kind = "AriaHabitCalWidget"
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: SelectHabitIntent.self, provider: HabitCalProvider()) { entry in
            CalendarGridView(entry: entry.entry, emptyHint: "Edit the widget to pick a habit.")
        }
        .configurationDisplayName("Aria — Habit streak")
        .description("A habit's streak calendar.")
        .supportedFamilies([.systemSmall])
    }
}

// MARK: - Task calendar widget

struct TaskEntity: AppEntity, Identifiable {
    var id: String
    var name: String
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Task"
    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(name)") }
    static var defaultQuery = TaskEntityQuery()
}

struct TaskEntityQuery: EntityQuery {
    func entities(for identifiers: [String]) async throws -> [TaskEntity] {
        WidgetBridge.readCalendar().tasks.filter { identifiers.contains($0.id) }.map { TaskEntity(id: $0.id, name: $0.name) }
    }
    func suggestedEntities() async throws -> [TaskEntity] {
        WidgetBridge.readCalendar().tasks.map { TaskEntity(id: $0.id, name: $0.name) }
    }
}

struct SelectTaskIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Pick a task"
    @Parameter(title: "Task") var task: TaskEntity?
}

struct TaskCalEntry: TimelineEntry { let date: Date; let entry: CalendarEntry? }

struct TaskCalProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> TaskCalEntry { TaskCalEntry(date: Date(), entry: nil) }
    func snapshot(for configuration: SelectTaskIntent, in context: Context) async -> TaskCalEntry {
        TaskCalEntry(date: Date(), entry: resolve(configuration))
    }
    func timeline(for configuration: SelectTaskIntent, in context: Context) async -> Timeline<TaskCalEntry> {
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
        return Timeline(entries: [TaskCalEntry(date: Date(), entry: resolve(configuration))], policy: .after(next))
    }
    private func resolve(_ c: SelectTaskIntent) -> CalendarEntry? {
        let tasks = WidgetBridge.readCalendar().tasks
        if let id = c.task?.id { return tasks.first { $0.id == id } }
        return tasks.first
    }
}

struct TaskCalWidget: Widget {
    let kind = "AriaTaskCalWidget"
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: kind, intent: SelectTaskIntent.self, provider: TaskCalProvider()) { entry in
            CalendarGridView(entry: entry.entry, emptyHint: "Edit the widget to pick a task.")
        }
        .configurationDisplayName("Aria — Task calendar")
        .description("A repeating task's completion calendar.")
        .supportedFamilies([.systemSmall])
    }
}
