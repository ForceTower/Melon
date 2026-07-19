import ComposableArchitecture
import Foundation
import UserNotifications

private let log = Log.scoped("EvaluationReminders")

extension EvaluationReminderClient: DependencyKey {
    static let liveValue = EvaluationReminderClient(
        run: {
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)
            log.debug("run subscribed")
            // Observation only fails if the database itself is gone; there
            // is nothing left to schedule then.
            do {
                for try await evaluations in mirror.evaluationReminderUpdates(now: { date.now }) {
                    await EvaluationReminderScheduler.reconcile(evaluations: evaluations, now: date.now)
                }
            } catch {
                log.error("run failed", error: error)
            }
        },
        reconcile: {
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)
            let evaluations = (try? await mirror.spotlightSuggestedEvaluations(now: date.now)) ?? []
            await EvaluationReminderScheduler.reconcile(evaluations: evaluations, now: date.now)
        }
    )
}

/// The device-local switch in Configurações. Not part of the server-synced
/// notification preferences: the reminder fires from this device's own
/// schedule, so the choice stays per-device like the theme.
enum EvaluationReminderPreference {
    static let key = "evaluationRemindersEnabled"

    static var isOn: Bool {
        UserDefaults.standard.object(forKey: key) as? Bool ?? true
    }
}

/// Diffs the desired reminder set against the pending requests. Identifiers
/// ride the Spotlight entity id, which is keyed on `platformId ?? id`, so a
/// full-scope rewrite (sync wipes and reinserts the semester) maps each
/// evaluation back onto its existing request instead of duplicating it.
enum EvaluationReminderScheduler {
    static let identifierPrefix = "evaluation-reminder/"
    /// Evening-before fire hour, device-local time.
    static let fireHour = 20

    struct Reminder: Equatable {
        let identifier: String
        let body: String
        let fire: DateComponents
    }

    static var isEnabled: Bool {
        #if DEBUG
        EvaluationReminderPreference.isOn
        #else
        UserDefaults.standard.bool(forKey: FeatureFlags.evaluationRemindersEnabledKey)
            && EvaluationReminderPreference.isOn
        #endif
    }

    static func reconcile(evaluations: [SpotlightEvaluation], now: Date) async {
        let center = UNUserNotificationCenter.current()
        let desired = isEnabled ? desiredReminders(evaluations: evaluations, now: now) : []
        let pending = await center.pendingNotificationRequests()
            .filter { $0.identifier.hasPrefix(identifierPrefix) }

        let desiredIds = Set(desired.map(\.identifier))
        let staleIds = pending.map(\.identifier).filter { !desiredIds.contains($0) }
        if !staleIds.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: staleIds)
        }

        var scheduled = 0
        for reminder in desired {
            guard !pending.contains(where: { matches($0, reminder) }) else { continue }
            do {
                // Same-identifier adds replace, so a date or copy change on
                // an existing evaluation lands without an explicit removal.
                try await center.add(request(for: reminder))
                scheduled += 1
            } catch {
                log.warn("schedule failed id=\(reminder.identifier)", error: error)
            }
        }
        if scheduled > 0 || !staleIds.isEmpty {
            log.info("reconciled scheduled=\(scheduled) cancelled=\(staleIds.count) pending=\(desired.count)")
        }
    }

    /// One reminder per evaluation whose evening-before moment is still
    /// ahead: day-only dates make "20:00 na véspera" the closest honest
    /// stand-in for "before the exam".
    static func desiredReminders(
        evaluations: [SpotlightEvaluation],
        now: Date,
        calendar: Calendar = .current
    ) -> [Reminder] {
        evaluations.compactMap { evaluation in
            let parts = evaluation.dateStamp.split(separator: "-").compactMap { Int($0) }
            guard parts.count == 3,
                  let day = calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2])),
                  let eve = calendar.date(byAdding: .day, value: -1, to: day),
                  let fireDate = calendar.date(bySettingHour: fireHour, minute: 0, second: 0, of: eve),
                  fireDate > now
            else { return nil }
            return Reminder(
                identifier: identifierPrefix + evaluation.id,
                body: evaluation.title,
                fire: calendar.dateComponents([.year, .month, .day, .hour, .minute], from: fireDate)
            )
        }
    }

    private static func matches(_ request: UNNotificationRequest, _ reminder: Reminder) -> Bool {
        request.identifier == reminder.identifier
            && request.content.body == reminder.body
            && (request.trigger as? UNCalendarNotificationTrigger)?.dateComponents == reminder.fire
    }

    private static func request(for reminder: Reminder) -> UNNotificationRequest {
        let content = UNMutableNotificationContent()
        content.title = String.localized(.evaluationReminderNotificationTitle)
        content.body = reminder.body
        content.sound = .default
        // Rides the push-tap path: the delegate posts this as a deeplink.
        content.userInfo = ["url": "unes://classes"]
        return UNNotificationRequest(
            identifier: reminder.identifier,
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: reminder.fire, repeats: false)
        )
    }
}
