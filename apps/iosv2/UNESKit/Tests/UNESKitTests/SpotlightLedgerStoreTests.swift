import Foundation
import Testing

@testable import UNESKit

/// The mirror-backed Spotlight ledger: round-trips, the wipe signals, and
/// the legacy JSON-file marker. Serialized because one test plants the
/// legacy marker file in the shared Application Support directory.
@Suite(.serialized)
struct SpotlightLedgerStoreTests {
    private var populated: SpotlightIndexLedger {
        var ledger = SpotlightIndexLedger()
        ledger.disciplines["discipline/sem1/d1"] = "digest-d"
        ledger.messages["message/m1"] = "digest-m"
        ledger.evaluations["evaluation/sem1/d1/g1"] = "digest-e"
        return ledger
    }

    @Test
    func freshDatabaseSignalsAWipe() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())

        // No version row (fresh install, or a DEBUG schema erase that
        // emptied the tables while Spotlight kept its entries): the caller
        // must wipe the index before indexing anything.
        #expect(try await store.spotlightLedger() == nil)
    }

    @Test
    func savedLedgerRoundTrips() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())

        try await store.saveSpotlightLedger(populated)

        #expect(try await store.spotlightLedger() == populated)
    }

    @Test
    func saveIsAFullRewrite() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.saveSpotlightLedger(populated)

        var next = SpotlightIndexLedger()
        next.messages["message/m2"] = "digest-m2"
        try await store.saveSpotlightLedger(next)

        #expect(try await store.spotlightLedger() == next)
    }

    @Test
    func anotherSchemaVersionSignalsAWipe() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        var stale = populated
        stale.version = SpotlightIndexLedger.schemaVersion - 1

        try await store.saveSpotlightLedger(stale)

        #expect(try await store.spotlightLedger() == nil)
    }

    @Test
    func mirrorWipeLeavesTheLedgerIntact() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.saveSpotlightLedger(populated)

        try await store.wipe()

        // The logout wipe must NOT clear the ledger — the nil-snapshot
        // emission needs it non-empty to issue the index delete-all.
        #expect(try await store.spotlightLedger() == populated)
    }

    @Test
    func legacyJsonFileSignalsAWipeUntilDeleted() async throws {
        let store = MirrorStore(writer: try inMemoryDatabase())
        try await store.saveSpotlightLedger(populated)

        let url = try FileManager.default
            .url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
            .appending(path: "spotlight-index-ledger.json")
        try Data("{}".utf8).write(to: url)
        defer { try? FileManager.default.removeItem(at: url) }

        // Present file marks a legacy install — same treatment as a schema
        // bump, until the caller wipes the index and deletes the marker.
        #expect(try await store.spotlightLedger() == nil)

        MirrorStore.deleteLegacySpotlightLedgerFile()

        #expect(try await store.spotlightLedger() == populated)
    }
}
