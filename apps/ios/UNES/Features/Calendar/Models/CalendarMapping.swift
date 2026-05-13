import Foundation
@preconcurrency import Umbrella

// Projects KMP `CalendarCalendarEventFeed` values into the existing Swift
// `CalendarEvent` presentation type so the agenda / hero / filter code stays
// oblivious to the shared-layer shapes.
enum CalendarMapping {
    static func map(_ feed: CalendarCalendarEventFeed) -> CalendarEvent {
        let start = parseDate(feed.start)
        let end = feed.end.map(parseDate)
        return CalendarEvent(
            id: feed.id,
            description: feed.text,
            start: start,
            end: end,
            fixed: feed.fixed,
            closed: feed.closed,
            scope: mapScope(feed.scope),
            origin: mapOrigin(feed.origin)
        )
    }

    private static func mapScope(_ raw: CalendarCalendarFeedScope) -> CalendarScope {
        switch raw {
        case .general:  return .general
        case .faculty:  return .faculty
        case .course:   return .course
        // SKIE renames the `CLASS` Kotlin case to `theClass` to avoid the
        // Swift `class` keyword clash.
        case .theClass: return .classScope
        case .campus:   return .campus
        }
    }

    private static func mapOrigin(_ raw: CalendarCalendarFeedOrigin) -> CalendarOrigin {
        switch raw {
        case .manual:      return .manual
        case .evaluation:  return .evaluation
        case .finalExam:   return .finalExam
        case .secondCall:  return .secondCall
        case .secondEpoch: return .secondEpoch
        }
    }

    // KMP `LocalDate` round-trips through SKIE as `Kotlinx_datetimeLocalDate`.
    // Resolve to a local-midnight Date so the rest of the screen (which works
    // in Calendar.current days) can compare/groupBy without re-parsing.
    // `month` returns the `Month` enum (january..december); SKIE doesn't surface
    // the `Month.number` extension, so derive the 1-based month from `ordinal`.
    private static func parseDate(_ date: Kotlinx_datetimeLocalDate) -> Date {
        var comps = DateComponents()
        comps.year = Int(date.year)
        comps.month = Int(date.month.ordinal) + 1
        comps.day = Int(date.day)
        return Calendar.current.date(from: comps) ?? Date()
    }
}
