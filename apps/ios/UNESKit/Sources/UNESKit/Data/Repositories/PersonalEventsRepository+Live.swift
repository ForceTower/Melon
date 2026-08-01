import ComposableArchitecture
import Foundation
import UserNotifications

private let log = Log.scoped("PersonalEvents")

extension PersonalEventsRepository: DependencyKey {
    static let liveValue = PersonalEventsRepository(
        observe: {
            @Dependency(\.database) var database
            let mirror = MirrorStore(writer: database)
            log.debug("observe subscribed")
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await events in mirror.personalEventUpdates() {
                            continuation.yield(events)
                        }
                    } catch {
                        log.error("observe failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        },
        save: { event in
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)
            do {
                try await mirror.savePersonalEvent(event)
                log.info("saved id=\(event.id) category=\(event.category.rawValue) reminder=\(event.reminder.rawValue)")
            } catch {
                log.error("save failed id=\(event.id)", error: error)
                throw error
            }
            await reconcile(mirror: mirror, now: date.now)
        },
        delete: { id in
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)
            do {
                try await mirror.deletePersonalEvent(id: id)
                log.info("deleted id=\(id)")
            } catch {
                log.error("delete failed id=\(id)", error: error)
                throw error
            }
            await reconcile(mirror: mirror, now: date.now)
        },
        reconcileReminders: {
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            await reconcile(mirror: MirrorStore(writer: database), now: date.now)
        }
    )

    private static func reconcile(mirror: MirrorStore, now: Date) async {
        let events = (try? await mirror.personalEvents()) ?? []
        await PersonalEventReminderScheduler.reconcile(events: events, now: now)
    }
}

/// Diffs the reminders the stored entries ask for against the pending
/// requests. Identifiers ride the event id, so editing an entry rewrites its
/// own request instead of piling up a second one.
///
/// There is no global switch the way evaluation reminders have one: a
/// personal reminder only exists because the student picked it on that entry.
enum PersonalEventReminderScheduler {
    static let identifierPrefix = "personal-event/"
    static let fireHour = EvaluationReminderScheduler.fireHour

    struct Reminder: Equatable {
        let identifier: String
        let body: String
        let fire: DateComponents
    }

    static func reconcile(events: [PersonalEvent], now: Date) async {
        let center = UNUserNotificationCenter.current()
        let desired = desiredReminders(events: events, now: now)
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
                // Same-identifier adds replace, so an edited entry lands
                // without an explicit removal first.
                try await center.add(request(for: reminder))
                scheduled += 1
            } catch {
                log.warn("reminder schedule failed id=\(reminder.identifier)", error: error)
            }
        }
        if scheduled > 0 || !staleIds.isEmpty {
            log.info("reminders reconciled scheduled=\(scheduled) cancelled=\(staleIds.count) pending=\(desired.count)")
        }
    }

    /// One reminder per entry that asked for one and whose fire moment is
    /// still ahead. Entries are day-only, so it lands at `fireHour` on the
    /// day `reminder` days before the start.
    static func desiredReminders(
        events: [PersonalEvent],
        now: Date,
        calendar: Calendar = .current
    ) -> [Reminder] {
        events.compactMap { event in
            guard event.reminder != .none,
                  let start = CalendarFormat.parse(event.start, calendar: calendar),
                  let day = calendar.date(byAdding: .day, value: -event.reminder.rawValue, to: start),
                  let fireDate = calendar.date(bySettingHour: fireHour, minute: 0, second: 0, of: day),
                  fireDate > now
            else { return nil }
            return Reminder(
                identifier: identifierPrefix + event.id,
                body: event.title,
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
        content.title = String.localized(.calendarPersonalReminderNotificationTitle)
        content.body = reminder.body
        content.sound = .default
        // Rides the push-tap path: the delegate posts this as a deeplink.
        content.userInfo = ["url": "unes://calendar"]
        return UNNotificationRequest(
            identifier: reminder.identifier,
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: reminder.fire, repeats: false)
        )
    }
}
