import Foundation
import UserNotifications

/// Schedules periodic health reminders (hydration, exercise, sleep) using
/// `UNUserNotificationCenter`. All requests use identifiers prefixed with
/// `health_reminder_` so they can be cancelled individually or as a group.
@MainActor
public final class HealthReminders {

    // MARK: - Hydration

    /// Schedules repeating hydration reminders every 2 hours.
    public static func scheduleHydrationReminder() async {
        let center = UNUserNotificationCenter.current()

        let content = UNMutableNotificationContent()
        content.title = "Time to hydrate! 💧"
        content.body = "Stay on track — drink a glass of water."
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: 2 * 60 * 60, // 2 hours
            repeats: true
        )

        let request = UNNotificationRequest(
            identifier: "health_reminder_hydration",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    // MARK: - Exercise

    /// Schedules a daily exercise reminder at the given hour (default 7 AM).
    public static func scheduleExerciseReminder(hour: Int = 7, minute: Int = 0) async {
        let center = UNUserNotificationCenter.current()

        let content = UNMutableNotificationContent()
        content.title = "Ready for today's workout? 🏋️"
        content.body = "Your daily exercise session is waiting."
        content.sound = .default

        var components = DateComponents()
        components.hour = hour
        components.minute = minute

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: components,
            repeats: true
        )

        let request = UNNotificationRequest(
            identifier: "health_reminder_exercise",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    // MARK: - Sleep

    /// Schedules a nightly wind-down reminder 30 minutes before bedtime.
    public static func scheduleSleepReminder(bedtimeHour: Int = 22, bedtimeMinute: Int = 0) async {
        let center = UNUserNotificationCenter.current()

        var bedtime = DateComponents()
        bedtime.hour = bedtimeHour
        bedtime.minute = bedtimeMinute

        let calendar = Calendar.current
        let refDate = calendar.date(from: bedtime) ?? Date()
        let reminderDate = calendar.date(byAdding: .minute, value: -30, to: refDate) ?? refDate
        let reminderComponents = calendar.dateComponents([.hour, .minute], from: reminderDate)

        let content = UNMutableNotificationContent()
        content.title = "Wind down time 🌙"
        content.body = "Your sleep goal is in 30 minutes."
        content.sound = .default

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: reminderComponents,
            repeats: true
        )

        let request = UNNotificationRequest(
            identifier: "health_reminder_sleep",
            content: content,
            trigger: trigger
        )
        try? await center.add(request)
    }

    // MARK: - Cancellation

    public static func cancelHydration() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["health_reminder_hydration"])
    }

    public static func cancelExercise() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["health_reminder_exercise"])
    }

    public static func cancelSleep() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["health_reminder_sleep"])
    }

    public static func cancelAll() {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: [
                "health_reminder_hydration",
                "health_reminder_exercise",
                "health_reminder_sleep",
            ])
    }
}
