import SwiftUI

struct Card<Content: View>: View {
    @ViewBuilder var content: () -> Content
    var body: some View {
        content()
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(nsColor: .controlBackgroundColor))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(Color.primary.opacity(0.08), lineWidth: 1)
            )
    }
}

struct Ring: View {
    var progress: Double
    var color: Color
    var size: CGFloat = 120
    var line: CGFloat = 12
    var body: some View {
        ZStack {
            Circle().stroke(color.opacity(0.15), lineWidth: line)
            Circle()
                .trim(from: 0, to: max(0, min(1, progress)))
                .stroke(color, style: StrokeStyle(lineWidth: line, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .frame(width: size, height: size)
    }
}

struct StatBox: View {
    var value: String
    var label: String
    var icon: String
    var color: Color
    var body: some View {
        Card {
            VStack(alignment: .leading, spacing: 4) {
                Image(systemName: icon).foregroundStyle(color)
                Text(value).font(.system(size: 24, weight: .bold)).foregroundStyle(color)
                Text(label).font(.footnote).foregroundStyle(.secondary)
            }
        }
    }
}

struct SectionTitle: View {
    var text: String
    var body: some View {
        Text(text).font(.title3.weight(.semibold)).frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct CheckCircle: View {
    var done: Bool
    var color: Color
    var body: some View {
        ZStack {
            Circle().stroke(done ? color : Color.secondary.opacity(0.5), lineWidth: 2)
                .background(Circle().fill(done ? color : .clear))
                .frame(width: 24, height: 24)
            if done { Image(systemName: "checkmark").font(.system(size: 12, weight: .bold)).foregroundStyle(.white) }
        }
        .frame(width: 26, height: 26)
        .contentShape(Rectangle())
    }
}
