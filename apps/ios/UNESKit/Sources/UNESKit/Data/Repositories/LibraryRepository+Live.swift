import ComposableArchitecture
import Foundation

private let log = Log.scoped("LibraryRepository")

extension LibraryRepository: DependencyKey {
    static let liveValue = LibraryRepository(
        overview: {
            @Dependency(\.apiClient) var apiClient
            log.debug("overview start")
            do {
                let dto: LibraryOverviewDTO = try await apiClient.get(from: "api/library/overview")
                let overview = dto.domain
                log.info("overview ok recents=\(overview.recents.count) new=\(overview.newAcquisitions.count)")
                return overview
            } catch {
                logFailure("overview", error: error)
                throw error
            }
        },
        search: { terms in
            @Dependency(\.apiClient) var apiClient
            guard let first = terms.first else { return [] }
            log.debug("search start terms=\(terms.count) scope=\(first.scope.rawValue)")
            do {
                // Terms two and three ride as q2/scope2/op2 and q3/scope3/op3
                // triplets, mirroring upstream's E / OU / NÃO.
                var query = [
                    URLQueryItem(name: "q", value: first.query),
                    URLQueryItem(name: "scope", value: first.scope.rawValue),
                ]
                for (index, term) in terms.dropFirst().prefix(2).enumerated() {
                    let slot = index + 2
                    query.append(URLQueryItem(name: "q\(slot)", value: term.query))
                    query.append(URLQueryItem(name: "scope\(slot)", value: term.scope.rawValue))
                    query.append(URLQueryItem(name: "op\(slot)", value: term.op.rawValue))
                }
                let dto: LibrarySearchDTO = try await apiClient.get(from: "api/library/search", query: query)
                log.info("""
                search ok terms=\(terms.count) works=\(dto.works.count) \
                servedFrom=\(dto.servedFrom ?? "?") upstream=\(dto.upstreamAvailable ?? true)
                """)
                return dto.works.map(\.domain)
            } catch {
                logFailure("search", error: error)
                throw error
            }
        },
        checkAvailability: { workId in
            @Dependency(\.apiClient) var apiClient
            do {
                let dto: LibraryAvailabilityDTO = try await apiClient.get(
                    from: "api/library/works/\(workId)/availability"
                )
                return dto.domain
            } catch {
                // Degradation is a state, not an error — the screen narrates it.
                logFailure("availability", error: error)
                return LibraryAvailabilitySnapshot(reading: .unavailable, copies: [])
            }
        },
        clearRecents: {
            @Dependency(\.apiClient) var apiClient
            log.info("clear recents")
            do {
                try await apiClient.delete("api/library/recents")
            } catch {
                logFailure("clearRecents", error: error)
                throw error
            }
        }
    )

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(operation) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(operation) 2xx envelope had null data")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

// MARK: - Instants

/// The backend stamps instants with `toISOString()` — fractional seconds —
/// but a cached reading can round-trip without them; accept both.
private let fractionalISO8601 = Date.ISO8601FormatStyle(includingFractionalSeconds: true)

private func parseInstant(_ raw: String?) -> Date? {
    guard let raw else { return nil }
    return (try? Date(raw, strategy: fractionalISO8601)) ?? (try? Date(raw, strategy: .iso8601))
}

// MARK: - DTOs (`api/library/*`)

private struct LibraryBranchDTO: Decodable {
    var code: String? = nil
    var name: String? = nil
    var slug: String? = nil

    /// The known slugs resolve to the local registry (which knows the campus
    /// and near-ness); anything unmapped is carried through with its upstream
    /// identity so no holding silently disappears.
    func domain(campus: String?) -> LibraryBranch {
        if let slug, let known = LibraryBranch.known.first(where: { $0.id == slug }) {
            return known
        }
        let name = (name?.isEmpty == false ? name : nil) ?? code ?? ""
        return LibraryBranch(
            id: (slug?.isEmpty == false ? slug : nil) ?? (code?.isEmpty == false ? code : nil) ?? name,
            sigla: Self.sigla(for: name),
            name: name,
            campus: campus,
            isNear: false
        )
    }

    /// Initials of the significant words — "Biblioteca do Campus" → "BC".
    private static func sigla(for name: String) -> String {
        let initials = name
            .split(separator: " ")
            .filter { $0.count > 2 }
            .compactMap(\.first)
            .prefix(4)
        return initials.isEmpty ? String(name.prefix(3)).uppercased() : String(initials).uppercased()
    }
}

private struct LibraryCopyDTO: Decodable {
    var branch: LibraryBranchDTO? = nil
    var campus: String? = nil
    var area: String? = nil
    var callNumber: String? = nil
    var status: String
    var situation: String? = nil
    var dueDate: String? = nil

    func domain(workCallNumber: String?) -> LibraryCopy {
        let status: LibraryCopyStatus =
            switch status {
            case "available": .available
            case "on_loan": .onLoan(due: parseInstant(dueDate))
            case "missing": .missing
            default: .localUse(note: situation ?? "")
            }
        return LibraryCopy(
            branch: (branch ?? LibraryBranchDTO()).domain(campus: campus),
            area: area ?? "",
            callNumber: callNumber ?? workCallNumber ?? "",
            status: status
        )
    }
}

private struct LibraryRecordFieldDTO: Decodable {
    var label: String
    var value: String
}

private struct LibraryWorkDTO: Decodable {
    var id: String
    var title: String
    var callNumber: String? = nil
    var type: String? = nil
    var year: String? = nil
    var authors: [String]? = nil
    var subjects: [String]? = nil
    var branches: [LibraryBranchDTO]? = nil
    var isbn: String? = nil
    var language: String? = nil
    var edition: String? = nil
    var collection: String? = nil
    var series: String? = nil
    var reference: String? = nil
    var record: [LibraryRecordFieldDTO]? = nil
    var copies: [LibraryCopyDTO]? = nil
    var isNewAcquisition: Bool? = nil

    var domain: LibraryWork {
        var seen = Set<String>()
        let branches = (branches ?? []).map { $0.domain(campus: nil) }
            .filter { seen.insert($0.id).inserted }
        return LibraryWork(
            id: id,
            rawTitle: title,
            callNumber: callNumber ?? "",
            // Books are the overwhelming majority, so an unmapped upstream
            // type reads as the neutral case rather than being dropped.
            type: type.flatMap(LibraryWorkType.init(rawValue:)) ?? .book,
            rawYear: year,
            authors: authors ?? [],
            subjects: subjects ?? [],
            branches: branches,
            rawISBN: isbn,
            language: language,
            collection: collection,
            series: series,
            reference: reference,
            record: (record ?? []).map { LibraryRecordField(label: $0.label, value: $0.value) },
            copies: (copies ?? []).map { $0.domain(workCallNumber: callNumber) },
            isNewAcquisition: isNewAcquisition ?? false
        )
    }
}

private struct LibraryRecentSearchDTO: Decodable {
    var query: String
    var scope: String? = nil
    var resultCount: Int? = nil
}

private struct LibraryOverviewDTO: Decodable {
    var recents: [LibraryRecentSearchDTO]? = nil
    var newAcquisitions: [LibraryWorkDTO]? = nil

    var domain: LibraryOverview {
        LibraryOverview(
            recents: (recents ?? []).map { recent in
                LibraryRecentSearch(
                    query: recent.query,
                    scope: recent.scope.flatMap(LibrarySearchScope.init(rawValue:)) ?? .all,
                    resultCount: recent.resultCount ?? 0
                )
            },
            newAcquisitions: (newAcquisitions ?? []).map(\.domain)
        )
    }
}

private struct LibrarySearchDTO: Decodable {
    var works: [LibraryWorkDTO]
    var servedFrom: String? = nil
    var upstreamAvailable: Bool? = nil
}

private struct LibraryAvailabilityDTO: Decodable {
    var status: String
    var checkedAt: String? = nil
    var copies: [LibraryCopyDTO]? = nil

    var domain: LibraryAvailabilitySnapshot {
        let checkedAt = parseInstant(checkedAt) ?? Date()
        switch status {
        case "fresh":
            return LibraryAvailabilitySnapshot(
                reading: .fresh(checkedAt: checkedAt),
                copies: (copies ?? []).map { $0.domain(workCallNumber: nil) }
            )
        case "stale":
            return LibraryAvailabilitySnapshot(
                reading: .stale(checkedAt: checkedAt),
                copies: (copies ?? []).map { $0.domain(workCallNumber: nil) }
            )
        default:
            return LibraryAvailabilitySnapshot(reading: .unavailable, copies: [])
        }
    }
}
