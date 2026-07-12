import SwiftUI
import WidgetKit

private enum WC {
    static let bg = Color(hex: 0x15171C)
    static let card = Color(hex: 0x1A1D24)
    static let text = Color(hex: 0xF2F3F5)
    static let muted = Color(hex: 0xA6ACB8)
    static let indigo = Color(hex: 0xA5B4FC)
    static let blue = Color(hex: 0x60A5FA)
    static let amber = Color(hex: 0xFBBF24)
    static let green = Color(hex: 0x34D399)
}

struct AriaEntry: TimelineEntry {
    let date: Date
    let snap: WidgetSnapshot
}

struct AriaProvider: TimelineProvider {
    func placeholder(in context: Context) -> AriaEntry { AriaEntry(date: Date(), snap: .empty) }

    func getSnapshot(in context: Context, completion: @escaping (AriaEntry) -> Void) {
        completion(AriaEntry(date: Date(), snap: WidgetBridge.read()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<AriaEntry>) -> Void) {
        let entry = AriaEntry(date: Date(), snap: WidgetBridge.read())
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
        completion(Timeline(entries: [entry], policy: .after(next)))
    }
}

private struct Stat: View {
    let value: String
    let label: String
    let color: Color
    var body: some View {
        VStack(spacing: 2) {
            Text(value).font(.system(size: 22, weight: .bold)).foregroundStyle(color)
            Text(label).font(.system(size: 11)).foregroundStyle(WC.muted)
        }.frame(maxWidth: .infinity)
    }
}

struct AriaWidgetEntryView: View {
    var entry: AriaEntry

    var body: some View {
        let s = entry.snap
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("ARIA").font(.system(size: 12, weight: .bold)).foregroundStyle(WC.indigo)
                Spacer()
                Label("\(s.topStreak)", systemImage: "flame.fill").font(.system(size: 12)).foregroundStyle(WC.amber)
            }

            HStack(spacing: 4) {
                Stat(value: "\(s.waterPct)%", label: "Water", color: WC.blue)
                Stat(value: "\(s.pendingTasks)", label: "Tasks", color: WC.indigo)
                Stat(value: "\(s.habitsDone)/\(s.habitsTotal)", label: "Habits", color: WC.green)
            }

            if s.tasks.isEmpty {
                Spacer(minLength: 0)
                Text("No tasks left 🎉").font(.system(size: 13, weight: .medium)).foregroundStyle(WC.muted)
                Spacer(minLength: 0)
            } else {
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(s.tasks.prefix(6)) { t in
                        Button(intent: ToggleTaskIntent(taskId: t.id, recurrence: t.recurrence)) {
                            HStack(spacing: 8) {
                                Image(systemName: "circle").font(.system(size: 14)).foregroundStyle(WC.muted)
                                Text(t.title).font(.system(size: 13, weight: .medium)).foregroundStyle(WC.text).lineLimit(1)
                                Spacer(minLength: 0)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                    if s.pendingTasks > s.tasks.prefix(6).count {
                        Text("+\(s.pendingTasks - s.tasks.prefix(6).count) more")
                            .font(.system(size: 11)).foregroundStyle(WC.muted)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .padding(16)
        .containerBackground(WC.bg, for: .widget)
    }
}

struct AriaWidget: Widget {
    let kind = "AriaWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: AriaProvider()) { entry in
            AriaWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Aria — Today")
        .description("Today's tasks, water and streaks — tap to check off tasks.")
        .supportedFamilies([.systemLarge])
    }
}
