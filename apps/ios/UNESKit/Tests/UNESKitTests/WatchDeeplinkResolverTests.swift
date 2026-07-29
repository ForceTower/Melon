import Testing

@testable import UNESKit

/// The watch projection of the deeplink registry — every URI parsed with the
/// shared grammar, then resolved against the preview snapshot (messages
/// wm1…wm5, disciplines d1…d5 with codes ALGI/CALC/LPOO/FIS2/PROJ).
struct WatchDeeplinkResolverTests {
    @Test
    func tabHostsLandOnTheirWatchDestinations() {
        #expect(resolve("unes://home") == .root)
        #expect(resolve("unes://schedule") == .week)
        #expect(resolve("unes://classes") == .root)
        #expect(resolve("unes://messages") == .messages)
        #expect(resolve("unes://me") == .root)
    }

    @Test
    func mirroredMessageLandsOnItsDetail() {
        #expect(resolve("unes://messages/wm2") == .message(id: "wm2"))
    }

    @Test
    func unmirroredMessageFallsBackToTheInbox() {
        #expect(resolve("unes://messages/not-mirrored") == .messages)
    }

    @Test
    func materialRoutesAreAPlainOpen() {
        #expect(resolve("unes://materials/mat-1") == .root)
        #expect(resolve("unes://materials/discipline/d1") == .root)
    }

    @Test
    func gradePushDataRefinesClassesToTheDiscipline() {
        let route = WatchDeeplinkResolver.route(for: [
            "url": "unes://classes", "kind": "grade_added", "disciplineCode": "CALC",
        ])
        #expect(route == .classesDiscipline(code: "CALC"))
        #expect(WatchDeeplinkResolver.resolve(route!, snapshot: .preview()) == .discipline(id: "d2"))
    }

    @Test
    func disciplineCodeIsTrimmedButNeverFuzzyMatched() {
        let route = WatchDeeplinkResolver.route(for: ["url": "unes://classes", "disciplineCode": " CALC "])
        #expect(route == .classesDiscipline(code: "CALC"))
        #expect(
            WatchDeeplinkResolver.resolve(.classesDiscipline(code: "calc"), snapshot: .preview())
                == .root
        )
    }

    @Test
    func unmatchedDisciplineCodeFallsBackToRoot() {
        #expect(
            WatchDeeplinkResolver.resolve(.classesDiscipline(code: "NOPE"), snapshot: .preview())
                == .root
        )
    }

    @Test
    func emptyDisciplineCodeStaysOnTheClassesTab() {
        let route = WatchDeeplinkResolver.route(for: ["url": "unes://classes", "disciplineCode": " "])
        #expect(route == .tab(.classes))
    }

    @Test
    func disciplineCodeOnlyRefinesTheClassesHost() {
        let route = WatchDeeplinkResolver.route(for: ["url": "unes://schedule", "disciplineCode": "CALC"])
        #expect(route == .tab(.schedule))
    }

    @Test
    func payloadsWithoutARouteAreDropped() {
        #expect(WatchDeeplinkResolver.route(for: [:]) == nil)
        #expect(WatchDeeplinkResolver.route(for: ["kind": "lecture_added", "disciplineCode": "CALC"]) == nil)
        #expect(WatchDeeplinkResolver.route(for: ["url": "unes://reauth"]) == nil)
    }

    @Test
    func defensiveRoutesResolveToRoot() {
        #expect(
            WatchDeeplinkResolver.resolve(
                .discipline(semesterId: "s1", disciplineId: "d1"), snapshot: .preview()
            ) == .root
        )
    }

    @Test
    func nilSnapshotResolvesContentRoutesToTheirFallbacks() {
        #expect(WatchDeeplinkResolver.resolve(.message(id: "wm1"), snapshot: nil) == .messages)
        #expect(WatchDeeplinkResolver.resolve(.classesDiscipline(code: "CALC"), snapshot: nil) == .root)
    }

    @Test
    func onlyContentRoutesNeedTheSnapshot() {
        #expect(WatchDeeplinkResolver.needsSnapshot(.message(id: "x")))
        #expect(WatchDeeplinkResolver.needsSnapshot(.classesDiscipline(code: "X")))
        #expect(!WatchDeeplinkResolver.needsSnapshot(.tab(.home)))
        #expect(!WatchDeeplinkResolver.needsSnapshot(.tab(.messages)))
        #expect(!WatchDeeplinkResolver.needsSnapshot(.material(id: "x")))
        #expect(!WatchDeeplinkResolver.needsSnapshot(.materialsDiscipline(disciplineId: "x")))
        #expect(!WatchDeeplinkResolver.needsSnapshot(.discipline(semesterId: "s", disciplineId: "d")))
    }

    private func resolve(_ url: String) -> WatchDeeplinkDestination? {
        Deeplinks.parse(url).map { WatchDeeplinkResolver.resolve($0, snapshot: .preview()) }
    }
}
