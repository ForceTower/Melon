import ComposableArchitecture
import Foundation

/// The library catalogue (Pergamum). Two-phase by design: the catalogue
/// search answers fast and is reliable; the per-title availability reading
/// is a separate, slower consultation that can degrade or fail — screens
/// render the catalogue immediately and fill circulation in as it lands.
@DependencyClient
struct LibraryRepository: Sendable {
    var overview: @Sendable () async throws -> LibraryOverview
    var search: @Sendable (_ query: String, _ scope: LibrarySearchScope) async throws -> [LibraryWork]
    /// Consults circulation for one title. Never throws — degradation is a
    /// state the UI narrates, not an error.
    var checkAvailability: @Sendable (_ workId: String) async -> LibraryReading = { _ in .unavailable }
}

// The whole feature runs on this mock until the backend endpoint lands, so
// the mock IS the live value for now.
extension LibraryRepository: DependencyKey {
    static let liveValue = LibraryRepository.mock()
    static let previewValue = LibraryRepository.mock(instant: true)
    static let testValue = LibraryRepository()

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
            search: { query, scope in
                if !instant { try await Task.sleep(for: .milliseconds(550)) }
                return LibraryFixtures.all(now: Date())
                    .filter { matches($0, query: query, scope: scope) }
            },
            checkAvailability: { workId in
                if !instant {
                    // Staggered so rows visibly resolve one after the other.
                    let jitter = (workId.hashValue % 5 + 5) * 60
                    try? await Task.sleep(for: .milliseconds(200 + jitter))
                }
                return .fresh(checkedAt: Date())
            }
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
