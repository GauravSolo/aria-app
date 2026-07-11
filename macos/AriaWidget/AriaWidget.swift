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
            Text(value).font(.system(size: 20, weight: .bold)).foregroundStyle(color)
            Text(label).font(.system(size: 10)).foregroundStyle(WC.muted)
        }.frame(maxWidth: .infinity)
    }
}

struct AriaWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: AriaEntry

    var body: some View {
        let s = entry.snap
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("ARIA").font(.system(size: 11, weight: .bold)).foregroundStyle(WC.indigo)
                Spacer()
                Label("\(s.topStreak)", systemImage: "flame.fill").font(.system(size: 11)).foregroundStyle(WC.amber)
            }

            HStack(spacing: 4) {
                Stat(value: "\(s.waterPct)%", label: "Water", color: WC.blue)
                Stat(value: "\(s.pendingTasks)", label: "Tasks", color: WC.indigo)
                if family != .systemSmall {
                    Stat(value: "\(s.habitsDone)/\(s.habitsTotal)", label: "Habits", color: WC.green)
                }
            }

            if family != .systemSmall {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.right.circle.fill").foregroundStyle(WC.indigo).font(.system(size: 12))
                    Text(s.nextTaskTitle ?? "No tasks left 🎉")
                        .font(.system(size: 13, weight: .semibold)).foregroundStyle(WC.text)
                        .lineLimit(1)
                    Spacer()
                }
                .padding(8)
                .background(RoundedRectangle(cornerRadius: 10).fill(WC.card))
            }

            if family == .systemLarge {
                Spacer(minLength: 0)
                Text("\(s.totalTasks - s.pendingTasks) of \(s.totalTasks) tasks done today")
                    .font(.system(size: 12)).foregroundStyle(WC.muted)
            }
        }
        .padding(family == .systemSmall ? 12 : 14)
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
        .description("Your day at a glance: tasks, water and streaks.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
