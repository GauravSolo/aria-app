import Foundation
import UserNotifications

/// Local notifications for reminders. Schedules the next fire per reminder and is
/// re-run whenever reminders change or the app loads (so recurring ones re-arm).
enum Notifier {
    static func requestAuth() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    static func reschedule(_ reminders: [Reminder]) {
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
        for r in reminders {
            guard let fire = reminderNextFire(r) else { continue }
            let interval = fire.timeIntervalSinceNow
            guard interval > 1 else { continue }
            let content = UNMutableNotificationContent()
            content.title = r.title
            if let b = r.body, !b.isEmpty { content.body = b }
            content.sound = .default
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
            let req = UNNotificationRequest(identifier: "aria-\(r.id)", content: content, trigger: trigger)
            center.add(req)
        }
    }
}
