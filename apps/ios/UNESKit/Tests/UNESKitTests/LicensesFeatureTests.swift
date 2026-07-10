import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct LicensesFeatureTests {
    @Test
    func groupsFollowTheBreakdownLargestFirst() {
        let state = LicensesFeature.State()

        #expect(state.groups.map(\.family) == [.mit, .apache2])
        #expect(state.groups.map(\.packages.count) == [13, 2])
    }

    @Test
    func familyChipAndQueryNarrowTheGroups() async {
        let store = TestStore(initialState: LicensesFeature.State()) {
            LicensesFeature()
        }

        await store.send(.familySelected(.apache2)) {
            $0.family = .apache2
        }
        #expect(store.state.groups.map(\.family) == [.apache2])

        await store.send(.familySelected(nil)) {
            $0.family = nil
        }
        await store.send(.queryChanged("gwendal")) {
            $0.query = "gwendal"
        }
        #expect(store.state.groups.flatMap(\.packages).map(\.name) == ["GRDB.swift"])

        // Categories match too, diacritics-insensitively.
        await store.send(.queryChanged("concorrencia")) {
            $0.query = "concorrencia"
        }
        #expect(store.state.groups.flatMap(\.packages).count == 3)

        await store.send(.queryChanged("não existe")) {
            $0.query = "não existe"
        }
        #expect(store.state.groups.isEmpty)
    }

    @Test
    func rowsExpandOneAtATime() async {
        let store = TestStore(initialState: LicensesFeature.State()) {
            LicensesFeature()
        }

        await store.send(.packageToggled("swift-syntax")) {
            $0.expandedID = "swift-syntax"
        }
        await store.send(.packageToggled("GRDB.swift")) {
            $0.expandedID = "GRDB.swift"
        }
        await store.send(.packageToggled("GRDB.swift")) {
            $0.expandedID = nil
        }
    }

    @Test
    func copyPutsThePinOnThePasteboardThenExpires() async {
        let clock = TestClock()
        let copied = LockIsolated<[String]>([])

        let store = TestStore(initialState: LicensesFeature.State()) {
            LicensesFeature()
        } withDependencies: {
            $0.continuousClock = clock
            $0.pasteboard.copy = { text in copied.withValue { $0.append(text) } }
        }

        await store.send(.copyTapped("swift-composable-architecture")) {
            $0.copiedID = "swift-composable-architecture"
        }
        await clock.advance(by: .milliseconds(1400))
        await store.receive(.copyFeedbackExpired) {
            $0.copiedID = nil
        }
        #expect(copied.value == ["swift-composable-architecture@1.26.0"])
    }

    @Test
    func pillsOpenTheRepoAndTheLicenseText() async {
        let opened = LockIsolated<[URL]>([])

        let store = TestStore(initialState: LicensesFeature.State()) {
            LicensesFeature()
        } withDependencies: {
            $0.openURL = OpenURLEffect { url in
                opened.withValue { $0.append(url) }
                return true
            }
        }

        await store.send(.homepageTapped("GRDB.swift"))
        await store.finish()
        await store.send(.licenseTextTapped(.apache2))
        await store.finish()

        #expect(opened.value == [
            URL(string: "https://github.com/groue/GRDB.swift")!,
            URL(string: "https://www.apache.org/licenses/LICENSE-2.0")!,
        ])
    }

    @Test
    func sbomListsTheWholeCatalog() throws {
        let document = try #require(
            JSONSerialization.jsonObject(with: LicensesSBOM.data) as? [String: Any]
        )

        #expect(document["bomFormat"] as? String == "CycloneDX")

        let components = try #require(document["components"] as? [[String: Any]])
        #expect(components.count == LicenseCatalog.packages.count)
    }
}
