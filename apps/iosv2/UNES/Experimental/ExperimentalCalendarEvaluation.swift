#if UNES_IOS27_EXPERIMENT
import AppIntents
import UNESKit

/// §3.8(a) — calendar **entity-only** conformance. The calendar domain's
/// actions (`createEvent` / `deleteEvent` / `updateEvent`) are all writes we
/// can never implement, so the open question is whether the build-time
/// schema-group validator accepts an entity that conforms alone.
///
/// E2 questions, in order:
/// 1. Does this file even build — or does the validator demand the action
///    group the moment the entity conforms?
/// 2. Do the schema's required fields map from what we have? The mapping
///    must not invent data: `title` ← evaluation title, `startDate` ← the
///    scheduled day (all-day; evaluations carry no time), everything else
///    nil/default.
/// 3. On the device, does Siri AI answer "when is my test from Cálculo"
///    better than with the plain `IndexedEntity` from §3.5?
@available(iOS 27, *)
@AssistantEntity(schema: .calendar.event)
struct ExperimentalEvaluationEvent {
    struct Query: EntityStringQuery {
        func entities(for identifiers: [String]) async throws -> [ExperimentalEvaluationEvent] {
            await SpotlightSupport.evaluations(for: identifiers)
                .compactMap(ExperimentalEvaluationEvent.init)
        }

        func entities(matching string: String) async throws -> [ExperimentalEvaluationEvent] {
            try await suggestedEntities().filter { $0.title.localizedStandardContains(string) }
        }

        func suggestedEntities() async throws -> [ExperimentalEvaluationEvent] {
            await SpotlightSupport.suggestedEvaluations()
                .compactMap(ExperimentalEvaluationEvent.init)
        }
    }

    static let defaultQuery = Query()

    let id: String
    var title: String
    var startDate: Date
    var endDate: Date
    var isAllDay: Bool
    var location: String?
    var participants: [IntentPerson]
}

@available(iOS 27, *)
extension ExperimentalEvaluationEvent {
    init?(projection: SpotlightEvaluation) {
        // yyyy-MM-dd → start of that day; evaluations carry no time, so the
        // event is all-day by construction.
        let parts = projection.dateStamp.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3,
              let day = Calendar.current.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
        else { return nil }
        id = projection.id
        title = projection.title
        startDate = day
        endDate = day
        isAllDay = true
        location = nil
        participants = []
    }
}
#endif
