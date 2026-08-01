import ComposableArchitecture
import Foundation

/// One circulation consultation for a title: the freshness verdict plus the
/// copies it carried. `copies` is empty when the reading is `.unavailable`.
struct LibraryAvailabilitySnapshot: Equatable, Sendable {
    var reading: LibraryReading
    var copies: [LibraryCopy]
}

/// The library catalogue (Pergamum, via `api/library/*`). Two-phase by
/// design: the catalogue search answers fast and is reliable; the per-title
/// availability reading is a separate, slower consultation that can degrade
/// or fail — screens render the catalogue immediately and fill circulation
/// in as it lands.
@DependencyClient
struct LibraryRepository: Sendable {
    var overview: @Sendable () async throws -> LibraryOverview
    /// Up to three boolean terms; a plain search is a single term.
    var search: @Sendable (_ terms: [LibrarySearchTerm]) async throws -> [LibraryWork]
    /// Consults circulation for one title. Never throws — degradation is a
    /// state the UI narrates, not an error.
    var checkAvailability: @Sendable (_ workId: String) async -> LibraryAvailabilitySnapshot = { _ in
        LibraryAvailabilitySnapshot(reading: .unavailable, copies: [])
    }
    var clearRecents: @Sendable () async throws -> Void
}

extension LibraryRepository: TestDependencyKey {
    static let testValue = LibraryRepository()
    static let previewValue = LibraryRepository.mock(instant: true)

    /// Fixture-backed implementation for previews and offline development.
    static func mock(instant: Bool = false) -> LibraryRepository {
        LibraryRepository(
            overview: {
                if !instant { try await Task.sleep(for: .milliseconds(250)) }
                let now = Date()
                let all = LibraryFixtures.all(now: now)
                let recents: [(String, LibrarySearchScope)] = [
                    ("cálculo", .all),
                    ("guidorizzi", .author),
                    ("515 A638c", .callNumber),
                    ("literatura de cordel", .subject),
                ]
                return LibraryOverview(
                    recents: recents.map { query, scope in
                        LibraryRecentSearch(
                            query: query,
                            scope: scope,
                            resultCount: all.count { matches($0, query: query, scope: scope) }
                        )
                    },
                    newAcquisitions: LibraryFixtures.newAcquisitions(now: now)
                )
            },
            search: { terms in
                if !instant { try await Task.sleep(for: .milliseconds(550)) }
                guard let first = terms.first else { return [] }
                return LibraryFixtures.all(now: Date()).filter { work in
                    terms.dropFirst().reduce(matches(work, query: first.query, scope: first.scope)) { held, term in
                        let matched = matches(work, query: term.query, scope: term.scope)
                        return switch term.op {
                        case .and: held && matched
                        case .or: held || matched
                        case .not: held && !matched
                        }
                    }
                }
            },
            checkAvailability: { workId in
                if !instant {
                    // Staggered so rows visibly resolve one after the other.
                    let jitter = (workId.hashValue % 5 + 5) * 60
                    try? await Task.sleep(for: .milliseconds(200 + jitter))
                }
                let copies = LibraryFixtures.all(now: Date()).first { $0.id == workId }?.copies ?? []
                return LibraryAvailabilitySnapshot(reading: .fresh(checkedAt: Date()), copies: copies)
            },
            clearRecents: {}
        )
    }

    /// Case- and diacritic-insensitive containment over the scope's fields.
    private static func matches(
        _ work: LibraryWork,
        query: String,
        scope: LibrarySearchScope
    ) -> Bool {
        let fields: [String] =
            switch scope {
            case .all:
                [work.rawTitle] + work.authors + work.subjects
                    + [work.callNumber, work.isbn?.value ?? ""]
            case .title: [work.rawTitle]
            case .author: work.authors
            case .subject: work.subjects
            case .isbn: [work.isbn?.value ?? "", work.isbn?.pretty ?? ""]
            case .callNumber: [work.callNumber]
            }
        let needle = normalize(query)
        return fields.contains { normalize($0).contains(needle) }
    }

    private static func normalize(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "pt-BR"))
    }
}

extension DependencyValues {
    var libraryRepository: LibraryRepository {
        get { self[LibraryRepository.self] }
        set { self[LibraryRepository.self] = newValue }
    }
}
