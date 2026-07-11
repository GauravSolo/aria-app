import SwiftUI

enum Brand {
    static let indigo = Color(hex: 0x6366F1)
    static let violet = Color(hex: 0x8B5CF6)
    static let blue = Color(hex: 0x3B82F6)
    static let green = Color(hex: 0x10B981)
    static let amber = Color(hex: 0xF59E0B)
    static let red = Color(hex: 0xEF4444)

    static func category(_ c: Category) -> Color {
        switch c {
        case .work: return indigo
        case .study: return violet
        case .health: return green
        case .personal: return amber
        case .other: return Color(hex: 0x64748B)
        }
    }

    static func priority(_ p: Priority) -> Color {
        switch p {
        case .low: return green
        case .medium: return amber
        case .high: return red
        }
    }
}
