import Foundation
import UserNotifications

/// Local notifications for reminders. Schedules the next fire per reminder and is
/// re-run whenever reminders change or the app loads (so recurring ones re-arm).
enum Notifier {
    static func requestAuth() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    static func reschedule(_ reminders: [Reminder], water: WaterSettings? = nil) {
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
        scheduleWater(water, on: center)
    }

    /// Recurring hydration nudges: one daily-repeating calendar trigger per interval slot
    /// within active hours. Cleared automatically since reschedule removes all pending first.
    private static func scheduleWater(_ ws: WaterSettings?, on center: UNUserNotificationCenter) {
        guard let ws, ws.reminderEnabled else { return }
        func hm(_ s: String) -> (Int, Int) {
            let p = s.split(separator: ":")
            return (Int(p.first ?? "0") ?? 0, p.count > 1 ? (Int(p[1]) ?? 0) : 0)
        }
        let (sh, sm) = hm(ws.activeStart)
        let (eh, em) = hm(ws.activeEnd)
        let interval = max(15, ws.reminderIntervalMin)
        var t = sh * 60 + sm
        let end = eh * 60 + em
        var i = 0
        while t <= end && i < 24 {
            let content = UNMutableNotificationContent()
            content.title = "Time to hydrate 💧"
            content.body = "Have a glass of water to stay on track."
            content.sound = .default
            var dc = DateComponents(); dc.hour = t / 60; dc.minute = t % 60
            let trigger = UNCalendarNotificationTrigger(dateMatching: dc, repeats: true)
            center.add(UNNotificationRequest(identifier: "aria-water-\(i)", content: content, trigger: trigger))
            t += interval; i += 1
        }
    }
}
