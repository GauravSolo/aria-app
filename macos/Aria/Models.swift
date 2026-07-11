import Foundation

// Swift mirror of docs/DATA_MODEL.md. Temporal values are kept as ISO/`YYYY-MM-DD`
// Strings (matching the TS app and avoiding date-decoding pitfalls). The Supabase
// client is configured with snake_case key conversion, so properties stay camelCase.

enum Category: String, Codable, CaseIterable, Identifiable {
    case work, study, health, personal, other
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum Priority: String, Codable, CaseIterable, Identifiable {
    case low, medium, high
    var id: String { rawValue }
    var label: String { rawValue.capitalized }
}

enum TaskRecurrence: String, Codable { case none, daily, weekly, monthly, custom }
enum HabitKind: String, Codable { case build, quit }
enum Frequency: String, Codable, CaseIterable { case daily, weekly, custom }
enum ReminderKind: String, Codable { case task, habit, water, custom }
enum ReminderRepeat: String, Codable { case once, daily, weekly, interval }
enum NotifStatus: String, Codable { case delivered, done, snoozed, dismissed }

struct Task: Codable, Identifiable, Equatable {
    var id: String
    var userId: String
    var title: String
    var description: String?
    var category: Category
    var priority: Priority
    var startTime: String?
    var endTime: String?
    var dueDate: String?
    var recurrence: TaskRecurrence
    var recurrenceInterval: Int
    var recurrenceDays: [Int]
    var recurrenceEndDate: String?
    var isCompleted: Bool
    var completedAt: String?
    var sortOrder: Int
    var createdAt: String
    var updatedAt: String
    var deletedAt: String?
}

struct TaskCompletion: Codable, Identifiable, Equatable {
    var id: String
    var userId: String
    var taskId: String
    var occurrenceDate: String
    var completedAt: String
    var createdAt: String
    var updatedAt: String
    var deletedAt: String?
}

struct Habit: Codable, Identifiable, Equatable {
    var id: String
    var userId: String
    var name: String
    var kind: HabitKind
    var category: Category
    var frequency: Frequency
    var targetCount: Int
    var customDays: [Int]
    var reminderTime: String?
    var startDate: String
    var notes: String?
    var color: String?
    var icon: String?
    var isArchived: Bool
    var sortOrder: Int
    var createdAt: String
    var updatedAt: String
    var deletedAt: String?
}

struct HabitLog: Codable, Identifiable, Equatable {
    var id: String
    var userId: String
    var habitId: String
    var logDate: String
    var count: Int
    var createdAt: String
    var updatedAt: String
    var deletedAt: String?
}

struct WaterSettings: Codable, Equatable {
    var userId: String
    var dailyGoalMl: Int
    var glassSizeMl: Int
    var reminderIntervalMin: Int
    var reminderEnabled: Bool
    var activeStart: String
    var activeEnd: String
    var updatedAt: String

    static func defaults(_ uid: String) -> WaterSettings {
        WaterSettings(userId: uid, dailyGoalMl: 4000, glassSizeMl: 250,
                      reminderIntervalMin: 45, reminderEnabled: true,
                      activeStart: "08:00", activeEnd: "22:00", updatedAt: ISO.now())
    }
}

struct WaterLog: Codable, Identifiable, Equatable {
    var id: String
    var userId: String
    var logDate: String
    var amountMl: Int
    var loggedAt: String
    var createdAt: String
    var updatedAt: String
    var deletedAt: String?
}

// ── Small helpers shared across the app ──────────────────────────────────────
enum ISO {
    static func now() -> String {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f.string(from: Date())
    }
}

enum DayKey {
    static func today() -> String { of(Date()) }
    static func of(_ date: Date) -> String {
        let f = DateFormatter()
        f.calendar = Calendar(identifier: .gregorian)
        f.dateFormat = "yyyy-MM-dd"
        return f.string(from: date)
    }
}

func newUUID() -> String { UUID().uuidString.lowercased() }
