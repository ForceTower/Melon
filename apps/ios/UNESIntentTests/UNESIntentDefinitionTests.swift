import AppIntentsTesting
import XCTest

/// Drives the installed UNES app's intents and entities out of process,
/// the way Siri and Shortcuts do, and asserts what the app declares and
/// returns. No doubles: data comes from the app's own mirror, so the
/// data-dependent checks skip on a signed-out simulator.
final class UNESIntentDefinitionTests: XCTestCase {
    private let definitions = IntentDefinitions(bundleIdentifier: "dev.forcetower.unes.ios")

    /// The system registers an app's intent metadata on its first launch;
    /// until then every out-of-process call fails with "bundle is not
    /// present". One launch per test keeps the run order irrelevant.
    override func setUpWithError() throws {
        try super.setUpWithError()
        continueAfterFailure = false
        XCUIApplication().launch()
    }

    private static let intentNames = [
        "NextClassIntent", "TodayScheduleIntent", "OpenTabIntent", "ScoreIntent",
        "UnreadMessagesIntent", "FinalExamIntent", "DisciplineNextClassIntent", "DisciplineGradesIntent",
    ]

    private static let entityNames = [
        "DisciplineEntity", "MessageEntity", "EvaluationEntity", "LectureEntity",
        "AcademicEventEntity", "MessageEventEntity",
    ]

    func testDeclaresEveryIntent() {
        for name in Self.intentNames {
            XCTAssertEqual(definitions.intents[name].identifier, name)
        }
    }

    func testDeclaresEveryEntity() {
        for name in Self.entityNames {
            XCTAssertEqual(definitions.entities[name].typeIdentifier, name)
        }
    }

    /// Signed out they answer the sign-in dialog, signed in the schedule;
    /// either way `perform()` completes without throwing.
    func testScheduleIntentsAnswer() async throws {
        _ = try await definitions.intents["NextClassIntent"].makeIntent().run()
        _ = try await definitions.intents["TodayScheduleIntent"].makeIntent().run()
    }

    func testDisciplineQueriesResolve() async throws {
        let discipline = definitions.entities["DisciplineEntity"]
        let suggested = try await discipline.suggestedEntities()
        let matching = try await discipline.entities(matching: "zzz-no-such-discipline")
        XCTAssertTrue(matching.isEmpty)
        try XCTSkipIf(suggested.isEmpty, "Needs a signed-in simulator with a mirrored semester.")
        // A discipline the picker offers must also resolve by name.
        let title: String = try suggested[0].title
        let found = try await discipline.entities(matching: title)
        XCTAssertFalse(found.isEmpty)
    }

    /// The first automated answer to "is it really in the index".
    func testSpotlightHoldsTheSuggestedDisciplines() async throws {
        let discipline = definitions.entities["DisciplineEntity"]
        let suggested = try await discipline.suggestedEntities()
        try XCTSkipIf(suggested.isEmpty, "Needs a signed-in simulator with a mirrored semester.")
        let indexed = try await discipline.spotlightQuery()
        XCTAssertGreaterThanOrEqual(indexed.count, suggested.count)
    }
}
