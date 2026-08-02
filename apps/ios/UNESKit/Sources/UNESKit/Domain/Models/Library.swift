import Foundation

// MARK: - Biblioteca — the university library catalogue (Pergamum)

/// Work types as the catalogue classifies them. Books are the overwhelming
/// majority, so they read neutral; the regional/local tail gets accents.
enum LibraryWorkType: String, Equatable, Sendable, CaseIterable {
    case book = "livro"
    case pamphlet = "folheto"
    case cordel
    case educationalProduct = "produto"
    case dissertation = "dissertacao"
    case thesis = "tese"
    case article = "artigo"
    case periodical = "periodico"
}

/// A campus library. The backend maps upstream branches to the known slugs
/// where it can; anything it cannot map still arrives with its code and name,
/// so this is a struct rather than a closed enum.
struct LibraryBranch: Equatable, Hashable, Sendable {
    /// Stable identity — the app slug when known, else the upstream code.
    var id: String
    var sigla: String
    var name: String
    var campus: String?
    /// On the student's own campus — an available copy here means "walk over
    /// and pick it up today".
    var isNear: Bool

    /// Compact label for refine rows.
    var shortName: String { Self.shortNames[id] ?? name }

    static let central = LibraryBranch(
        id: "bcjc", sigla: "BCJC", name: "Biblioteca Central Julieta Carteado",
        campus: "Feira de Santana", isNear: true
    )
    static let health = LibraryBranch(
        id: "saude", sigla: "BSS", name: "Biblioteca Setorial de Saúde",
        campus: "Feira de Santana", isNear: true
    )
    static let lencois = LibraryBranch(
        id: "lencois", sigla: "BAL", name: "Biblioteca do Campus Avançado",
        campus: "Lençóis", isNear: false
    )
    static let santoAntonio = LibraryBranch(
        id: "saj", sigla: "BSAJ", name: "Biblioteca do Campus Avançado",
        campus: "Santo Antônio de Jesus", isNear: false
    )

    /// The mapped campus libraries, in display order.
    static let known: [LibraryBranch] = [.central, .health, .lencois, .santoAntonio]

    private static let shortNames: [String: String] = [
        "bcjc": "Central Julieta Carteado",
        "saude": "Setorial de Saúde",
        "lencois": "Campus de Lençóis",
        "saj": "Campus de Sto. Antônio de Jesus",
    ]
}

/// One physical copy's circulation status.
enum LibraryCopyStatus: Equatable, Sendable {
    case available
    /// `due` is nil when the loan record carries no credible return date —
    /// the backend nulls decades-old dues rather than forecasting the past.
    case onLoan(due: Date?)
    /// In the catalogue but not on the shelf — excluded from every count.
    case missing
    /// Reference-only material that never leaves the building.
    case localUse(note: String)
}

struct LibraryCopy: Equatable, Sendable {
    var branch: LibraryBranch
    /// Shelf area — "Coleção Geral", "Coleção Cordel".
    var area: String
    /// Full per-copy call number, including edition/volume suffixes.
    var callNumber: String
    var status: LibraryCopyStatus
}

/// One label/value row of the catalogue record ("ficha"). Labels come from
/// the catalogue itself — an ordered listing, not a schema.
struct LibraryRecordField: Equatable, Sendable {
    var label: String
    var value: String
}

/// One catalogue work, keeping the record's dirty raw strings: the title
/// carries edition and trailing junk, the year is text (one real value was
/// "m"), the ISBN field mixes notes with the number. Parsing happens at the
/// edge so the UI can be honest about what the catalogue actually holds.
struct LibraryWork: Equatable, Sendable, Identifiable {
    /// The catalogue's `cod_acervo`.
    var id: String
    /// The raw `obra` string — dirty: "Cálculo : um novo horizonte - 6. ed / 0000".
    var rawTitle: String
    /// The classification / call number — "515 A638c".
    var callNumber: String
    var type: LibraryWorkType
    /// Raw `ano_publicacao` — TEXT in the source, not always a year.
    var rawYear: String?
    /// From the reliable facet array; empty when only the degraded shape came.
    var authors: [String]
    var subjects: [String]
    var branches: [LibraryBranch]
    /// Raw `isbn_issn` — dirty: "85-7307-655-3: (Broch.)", "(Broch.)", nil.
    var rawISBN: String?
    var language: String?
    var volumes: String?
    var collection: String?
    var series: String?
    /// Ready ABNT citation as inline markdown, when the listing carried one.
    var reference: String?
    var record: [LibraryRecordField]
    var copies: [LibraryCopy]
    /// Came from the "novas aquisições" listing — the degraded record shape.
    var isNewAcquisition = false
    /// The backend's decomposition of `rawTitle`, parsed once at scrape time.
    /// Nil only for fixture data, which falls back to the local parser.
    var serverTitle: LibraryWorkTitle? = nil
}

// MARK: - Dirty-string parsing

/// The `obra` string decomposed: title, subtitle, inline edition, and the
/// trailing "/ 0000" junk the catalogue appends.
struct LibraryWorkTitle: Equatable, Sendable {
    var title: String
    var subtitle: String?
    var edition: String?
    /// The trailing year-or-zeros fragment, kept so the UI can call it noise.
    var junkYear: String?
}

/// `ano_publicacao` is text in the source; sometimes it isn't a year at all.
enum LibraryYear: Equatable, Sendable {
    case year(String)
    case illegible(String)
    case none
}

struct LibraryISBN: Equatable, Sendable {
    /// Digits only, 10 or 13 — enough to look up covers or copy.
    var value: String?
    /// As printed in the record, hyphens kept.
    var pretty: String?
    /// Whatever else the field carried — "Broch." and friends.
    var note: String?
}

extension LibraryWork {
    var parsedTitle: LibraryWorkTitle { serverTitle ?? Self.parseTitle(rawTitle) }

    var year: LibraryYear {
        guard let rawYear, !rawYear.isEmpty else { return .none }
        if let match = rawYear.firstMatch(of: #/\d{4}/#) {
            return .year(String(match.output))
        }
        return .illegible(rawYear)
    }

    var isbn: LibraryISBN? {
        guard let rawISBN, !rawISBN.isEmpty else { return nil }
        return Self.parseISBN(rawISBN)
    }

    /// Edition for the meta line — the one parsed out of the title wins over
    /// the (rarer) dedicated field.
    var edition: String? { parsedTitle.edition }

    static func parseTitle(_ raw: String) -> LibraryWorkTitle {
        var text = raw.trimmingCharacters(in: .whitespaces)
        var junkYear: String?
        if let slash = text.range(of: " / ", options: .backwards) {
            let trailing = String(text[slash.upperBound...]).trimmingCharacters(in: .whitespaces)
            if trailing.wholeMatch(of: #/0+|\d{4}/#) != nil {
                junkYear = trailing
            }
            text = String(text[..<slash.lowerBound]).trimmingCharacters(in: .whitespaces)
        }
        var edition: String?
        if let match = text.firstMatch(of: #/\s[-–]\s(\d+\.?\s?ed[^/]*)$/#.ignoresCase()) {
            edition = String(match.output.1)
                .trimmingCharacters(in: .whitespaces)
                .replacing(#/\s+/#, with: " ")
            text = String(text[..<match.range.lowerBound]).trimmingCharacters(in: .whitespaces)
        }
        let parts = text.split(separator: #/\s+:\s+/#, maxSplits: 1).map(String.init)
        return LibraryWorkTitle(
            title: parts.first ?? text,
            subtitle: parts.count > 1 ? parts[1] : nil,
            edition: edition,
            junkYear: junkYear
        )
    }

    static func parseISBN(_ raw: String) -> LibraryISBN {
        guard let match = raw.firstMatch(of: #/[\dX][\dX\- ]{8,}[\dX]/#.ignoresCase()) else {
            let note = raw.replacing(#/[():]/#, with: "").trimmingCharacters(in: .whitespaces)
            return LibraryISBN(value: nil, pretty: nil, note: note.isEmpty ? nil : note)
        }
        let pretty = String(match.output).trimmingCharacters(in: .whitespaces)
        let digits = pretty.filter { $0.isNumber || $0 == "X" || $0 == "x" }
        let note = raw
            .replacingOccurrences(of: String(match.output), with: "")
            .replacing(#/[():]/#, with: "")
            .trimmingCharacters(in: .whitespaces)
        guard digits.count == 10 || digits.count == 13 else {
            return LibraryISBN(value: nil, pretty: nil, note: note.isEmpty ? nil : note)
        }
        return LibraryISBN(value: digits, pretty: pretty, note: note.isEmpty ? nil : note)
    }
}

// MARK: - Availability

/// Copy counts with missing copies excluded from every denominator: a title
/// with 122 catalogued copies and 22 missing reads "51 de 100", never
/// "51 de 122".
struct LibraryAvailability: Equatable, Sendable {
    struct Branch: Equatable, Sendable {
        var branch: LibraryBranch
        var available: Int
        var onLoan: Int
        var missing: Int
        var localUse: Int
        /// Copies that count — everything but missing.
        var total: Int
        var areas: [String]
    }

    var available: Int
    var onLoan: Int
    var missing: Int
    var localUse: Int
    /// Copies that count — everything but missing.
    var total: Int
    /// Earliest future due date across loans; past dues are stale records.
    var nextDue: Date?
    var branches: [Branch]

    /// Copies that circulate at all — local-use ones never leave the building.
    var lendable: Int { total - localUse }

    enum Verdict: Equatable, Sendable {
        /// At least one copy free on a shelf.
        case available
        /// Every circulating copy is out.
        case allOnLoan
        /// Nothing circulates — reference-only material.
        case localUseOnly
    }

    var verdict: Verdict {
        if lendable == 0, localUse > 0 { return .localUseOnly }
        if available > 0 { return .available }
        return .allOnLoan
    }

    /// A free copy on the student's own campus.
    var hasNearAvailable: Bool {
        branches.contains { $0.branch.isNear && $0.available > 0 }
    }
}

extension LibraryWork {
    /// `now` decides which due dates still count as "coming back".
    func availability(now: Date) -> LibraryAvailability {
        var available = 0, onLoan = 0, missing = 0, localUse = 0
        var nextDue: Date?
        var order: [LibraryBranch] = []
        var byBranch: [LibraryBranch: LibraryAvailability.Branch] = [:]
        for copy in copies {
            if byBranch[copy.branch] == nil { order.append(copy.branch) }
            var branch = byBranch[copy.branch] ?? LibraryAvailability.Branch(
                branch: copy.branch, available: 0, onLoan: 0, missing: 0,
                localUse: 0, total: 0, areas: []
            )
            switch copy.status {
            case .available:
                available += 1
                branch.available += 1
                branch.total += 1
            case let .onLoan(due):
                onLoan += 1
                branch.onLoan += 1
                branch.total += 1
                if let due, due > now, nextDue.map({ due < $0 }) ?? true {
                    nextDue = due
                }
            case .missing:
                missing += 1
                branch.missing += 1
            case .localUse:
                localUse += 1
                branch.localUse += 1
                branch.total += 1
            }
            if !branch.areas.contains(copy.area) {
                branch.areas.append(copy.area)
            }
            byBranch[copy.branch] = branch
        }
        let branches = order.compactMap { byBranch[$0] }
        return LibraryAvailability(
            available: available,
            onLoan: onLoan,
            missing: missing,
            localUse: localUse,
            total: copies.count - missing,
            nextDue: nextDue,
            branches: branches
        )
    }
}

// MARK: - Availability reading

/// One consultation of the circulation system. The catalogue itself is
/// reliable; the copy counts are only as good as the last time Pergamum
/// answered — the UI always says when that was.
enum LibraryReading: Equatable, Sendable {
    case fresh(checkedAt: Date)
    /// Pergamum stopped answering; this is the last known reading.
    case stale(checkedAt: Date)
    /// Pergamum is down and there is no reading to fall back to.
    case unavailable
}

// MARK: - Search

enum LibrarySearchScope: String, Equatable, Sendable, CaseIterable {
    case all
    case title = "titulo"
    case author = "autor"
    case subject = "assunto"
    case isbn
    case callNumber = "chamada"
}

/// How an advanced-search term joins the one before it — E / OU / NÃO.
/// Raw values are the wire contract.
enum LibrarySearchOperator: String, Equatable, Sendable, CaseIterable {
    case and, or, not
}

/// One term of a (possibly boolean) catalogue search. A plain search is a
/// single term; the advanced sheet sends up to three.
struct LibrarySearchTerm: Equatable, Sendable {
    var query: String
    var scope: LibrarySearchScope
    /// Ignored on the first term.
    var op: LibrarySearchOperator = .and
}

extension [LibrarySearchTerm] {
    /// Compact one-line rendering — "cálculo + guidorizzi − geometria".
    var display: String {
        guard let first else { return "" }
        return dropFirst().reduce(first.query) { text, term in
            let joiner = switch term.op {
            case .and: " + "
            case .or: " / "
            case .not: " − "
            }
            return text + joiner + term.query
        }
    }
}

enum LibrarySort: String, Equatable, Sendable, CaseIterable {
    case relevance
    case newest
    case oldest
    case titleAZ

    /// The backend's `sort` parameter value.
    var wireValue: String {
        self == .titleAZ ? "title" : rawValue
    }
}

/// One page of a search response. Sorting, faceting and pagination run
/// server-side over the full result set; `facets` counts the unfaceted set
/// and is identical on every page of the same query.
struct LibrarySearchPage: Equatable, Sendable {
    var works: [LibraryWork]
    var total: Int
    var offset: Int
    var facets: [LibraryFacetGroup: [LibraryFacetValue]]
}

/// One search call: the terms plus the server-side page/sort/facet options.
struct LibrarySearchRequest: Equatable, Sendable {
    var terms: [LibrarySearchTerm]
    var sort: LibrarySort = .relevance
    var facets: LibraryFacetSelection = [:]
    var offset = 0
    var limit = 25
}

struct LibraryRecentSearch: Equatable, Sendable, Identifiable {
    var query: String
    var scope: LibrarySearchScope
    var resultCount: Int

    var id: String { "\(scope.rawValue)|\(query)" }
}

/// The search-entry payload: recent searches plus the "novas no acervo"
/// shelf (which arrives in the degraded record shape).
struct LibraryOverview: Equatable, Sendable {
    var recents: [LibraryRecentSearch]
    var newAcquisitions: [LibraryWork]
}

// MARK: - Facets

enum LibraryFacetGroup: String, Equatable, Sendable, CaseIterable {
    case type
    case branch
    case subject
    case author
    case language
    case year
}

/// One refine option with its count over the unfiltered result set.
struct LibraryFacetValue: Equatable, Sendable, Identifiable {
    var key: String
    var label: String
    var count: Int

    var id: String { key }
}

/// Selected facet keys per group. Within a group selections add up (OR);
/// across groups they narrow (AND).
typealias LibraryFacetSelection = [LibraryFacetGroup: Set<String>]

/// Publication-decade buckets for the year facet.
enum LibraryYearBucket: String, Equatable, Sendable, CaseIterable {
    case from2020 = "2020"
    case decade2010 = "2010"
    case decade2000 = "2000"
    case decade1990 = "1990"
    case before1990 = "old"
    case unknown = "none"

    static func bucket(for work: LibraryWork) -> LibraryYearBucket {
        guard case let .year(text) = work.year, let value = Int(text) else { return .unknown }
        switch value {
        case 2020...: return .from2020
        case 2010...2019: return .decade2010
        case 2000...2009: return .decade2000
        case 1990...1999: return .decade1990
        default: return .before1990
        }
    }
}

extension LibraryWork {
    /// The keys this work carries per facet group.
    func facetKeys(for group: LibraryFacetGroup) -> [String] {
        switch group {
        case .type: [type.rawValue]
        case .branch: branches.map(\.id)
        case .subject: subjects
        case .author: authors
        case .language: language.map { [$0] } ?? []
        case .year: [LibraryYearBucket.bucket(for: self).rawValue]
        }
    }

    func matches(_ selection: LibraryFacetSelection) -> Bool {
        selection.allSatisfy { group, keys in
            keys.isEmpty || !Set(facetKeys(for: group)).isDisjoint(with: keys)
        }
    }

    /// Numeric year for sorting; unparseable years read as 0.
    var sortYear: Int {
        if case let .year(text) = year, let value = Int(text) { return value }
        return 0
    }
}

// MARK: - Mock search pipeline

/// The server's search pipeline mirrored over in-memory works — the mock
/// repository and previews stand in for the real backend with this.
extension LibrarySearchPage {
    static func compute(
        over works: [LibraryWork],
        facets selection: LibraryFacetSelection = [:],
        sort: LibrarySort = .relevance,
        offset: Int = 0,
        limit: Int = 25
    ) -> LibrarySearchPage {
        let filtered = works.filter { $0.matches(selection) }
        let sorted: [LibraryWork] = switch sort {
        case .relevance: filtered
        case .newest: filtered.sorted { $0.sortYear > $1.sortYear }
        case .oldest: filtered.sorted { $0.sortYear < $1.sortYear }
        case .titleAZ: filtered.sorted {
            $0.parsedTitle.title.localizedStandardCompare($1.parsedTitle.title) == .orderedAscending
        }
        }
        return LibrarySearchPage(
            works: Array(sorted.dropFirst(offset).prefix(limit)),
            total: filtered.count,
            offset: offset,
            facets: facetValues(over: works)
        )
    }

    /// Counts over the unfaceted set; a work can carry several subjects and
    /// live in more than one branch, so counts sum past the total. Labels are
    /// the wire fallbacks — display localization happens at the screen.
    static func facetValues(over works: [LibraryWork]) -> [LibraryFacetGroup: [LibraryFacetValue]] {
        var out: [LibraryFacetGroup: [LibraryFacetValue]] = [:]
        for group in LibraryFacetGroup.allCases {
            var counts: [String: Int] = [:]
            for work in works {
                for key in work.facetKeys(for: group) where !key.isEmpty {
                    counts[key, default: 0] += 1
                }
            }
            switch group {
            case .type:
                out[group] = LibraryWorkType.allCases.compactMap { type in
                    counts[type.rawValue].map {
                        LibraryFacetValue(key: type.rawValue, label: type.rawValue, count: $0)
                    }
                }
            case .branch:
                var names: [String: String] = [:]
                for work in works {
                    for branch in work.branches where names[branch.id] == nil {
                        names[branch.id] = branch.name
                    }
                }
                let knownIds = LibraryBranch.known.map(\.id)
                let known = knownIds.filter { counts[$0] != nil }
                let rest = counts.keys
                    .filter { !knownIds.contains($0) }
                    .sorted { (names[$0] ?? $0) < (names[$1] ?? $1) }
                out[group] = (known + rest).map {
                    LibraryFacetValue(key: $0, label: names[$0] ?? $0, count: counts[$0] ?? 0)
                }
            case .year:
                out[group] = LibraryYearBucket.allCases.compactMap { bucket in
                    counts[bucket.rawValue].map {
                        LibraryFacetValue(key: bucket.rawValue, label: bucket.rawValue, count: $0)
                    }
                }
            case .subject, .author, .language:
                out[group] = counts
                    .map { LibraryFacetValue(key: $0.key, label: $0.key, count: $0.value) }
                    .sorted { lhs, rhs in
                        lhs.count != rhs.count ? lhs.count > rhs.count : lhs.label < rhs.label
                    }
            }
        }
        return out
    }
}
