import Foundation
import GRDB

// MARK: - Mirror → Spotlight projection

extension MirrorStore {
    /// Emits the Spotlight projection on subscription and again after every
    /// write that feeds it; nil until the first successful refresh lands (or
    /// after a full wipe), so the indexer can empty the index. The read/star
    /// overlay is deliberately not part of the projection: `messageStates`
    /// is never read, and the re-fetch a star toggle triggers (it also flips
    /// `messages.starred`) dies in `removeDuplicates()`.
    func spotlightUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<SpotlightSnapshot?> {
        ValueObservation
            .tracking { db in try Self.spotlightSnapshot(db, now: now()) }
            .removeDuplicates()
            .values(in: writer)
    }

    // MARK: One-shot lookups (entity queries)

    func spotlightDisciplines(ids: [String], now: Date) async throws -> [SpotlightDiscipline] {
        let wanted = Set(ids)
        return try await writer.read { db in
            try Self.spotlightDisciplines(db, now: now).filter { wanted.contains($0.id) }
        }
    }

    /// Active semester's disciplines in Turmas order — the Shortcuts picker.
    func spotlightSuggestedDisciplines(now: Date) async throws -> [SpotlightDiscipline] {
        try await writer.read { db in try Self.spotlightDisciplines(db, now: now) }
    }

    /// Name/code match, case- and diacritic-insensitive ("calculo" finds
    /// "Cálculo") — what Shortcuts and Siri parameter-filling key on.
    func spotlightDisciplines(matching text: String, now: Date) async throws -> [SpotlightDiscipline] {
        try await writer.read { db in
            try Self.spotlightDisciplines(db, now: now).filter {
                $0.title.localizedStandardContains(text) || $0.code.localizedStandardContains(text)
            }
        }
    }

    func spotlightMessages(ids: [String]) async throws -> [SpotlightMessage] {
        let messageIds = ids.compactMap { id in
            if case let .message(messageId) = SpotlightEntityID.parse(id) { messageId } else { nil as String? }
        }
        return try await writer.read { db in
            try Self.spotlightMessages(db, records: MessageRecord.filter(keys: messageIds).fetchAll(db))
        }
    }

    func spotlightRecentMessages(limit: Int) async throws -> [SpotlightMessage] {
        try await writer.read { db in
            let records = try MessageRecord.order(Column("timestamp").desc).limit(limit).fetchAll(db)
            return try Self.spotlightMessages(db, records: records)
        }
    }

    // MARK: Fetch

    private static func spotlightSnapshot(_ db: Database, now: Date) throws -> SpotlightSnapshot? {
        guard try lastSyncedAt(db) != nil else { return nil }
        let records = try MessageRecord.order(Column("timestamp").desc).fetchAll(db)
        return SpotlightSnapshot(
            disciplines: try spotlightDisciplines(db, now: now),
            messages: try spotlightMessages(db, records: records)
        )
    }

    private static func spotlightDisciplines(_ db: Database, now: Date) throws -> [SpotlightDiscipline] {
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        guard let active = semesters.map(\.domain).active(today: now.dayStamp),
              let record = semesters.first(where: { $0.id == active.id })
        else { return [] }
        return try spotlightScope(for: record, db: db).spotlightDisciplines()
    }

    /// Like `snapshot(for:db:)` minus grades, lectures, and materials — the
    /// no-grades rule is structural: grade writes aren't even part of the
    /// observation's tracked region.
    private static func spotlightScope(for semester: SemesterRecord, db: Database) throws -> SemesterSnapshot {
        func scoped<R: FetchableRecord & TableRecord>(_ type: R.Type) throws -> [R] {
            try R.filter(Column("semesterId") == semester.id).orderByPrimaryKey().fetchAll(db)
        }
        return try SemesterSnapshot(
            semester: semester,
            disciplines: scoped(DisciplineRecord.self),
            disciplineOffers: scoped(DisciplineOfferRecord.self),
            classes: scoped(ClassRecord.self),
            teachers: scoped(TeacherRecord.self),
            classTeachers: scoped(ClassTeacherRecord.self),
            spaces: scoped(SpaceRecord.self),
            allocations: scoped(AllocationRecord.self),
            studentClasses: scoped(StudentClassRecord.self)
        )
    }

    private static func spotlightMessages(_ db: Database, records: [MessageRecord]) throws -> [SpotlightMessage] {
        let scopesByMessage = Dictionary(
            grouping: try MessageScopeRecord.filter(records.map(\.id).contains(Column("messageId"))).fetchAll(db),
            by: \.messageId
        )
        return records.map { spotlightMessage($0, scopes: scopesByMessage[$0.id] ?? []) }
    }

    static func spotlightMessage(
        _ record: MessageRecord,
        scopes: [MessageScopeRecord],
        locale: Locale = .autoupdatingCurrent
    ) -> SpotlightMessage {
        let subject = record.subject?.trimmingCharacters(in: .whitespacesAndNewlines).spotlightNonEmpty
        let sender = record.senderName?.trimmingCharacters(in: .whitespacesAndNewlines).spotlightNonEmpty ?? "UNES"
        let receivedAt = record.timestamp.flatMap { try? Date($0, strategy: timestampFormat) }
        let date = receivedAt?.formatted(.dateTime.day().month(.abbreviated).year().locale(locale))
        let disciplineScope = scopes.first { $0.scope == "class" }
        // Subject-less messages already show the sender as their title —
        // repeating it in the subtitle reads as a glitch.
        let subtitleSender = subject == nil ? nil : sender
        return SpotlightMessage(
            id: SpotlightEntityID.message(id: record.id),
            messageId: record.id,
            title: subject ?? sender,
            subtitle: [subtitleSender, date].compactMap { $0 }.joined(separator: " · "),
            body: record.content?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            keywords: [
                disciplineScope?.disciplineCode?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty,
                disciplineScope?.disciplineName?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty,
            ].compactMap { $0 }
        )
    }
}

// MARK: - Semester snapshot → [SpotlightDiscipline]

extension SemesterSnapshot {
    /// One entry per enrolled discipline, merged per offer — exactly the
    /// Turmas cards — with the weekly line denormalized for the index.
    func spotlightDisciplines(calendar: Calendar = .current) -> [SpotlightDiscipline] {
        let index = SnapshotIndex(snapshot: self)

        var classIdsByDiscipline: [String: [String]] = [:]
        for classId in index.enrolledClassIds.sorted() {
            guard let discipline = index.discipline(forClass: classId) else { continue }
            classIdsByDiscipline[discipline.id, default: []].append(classId)
        }

        var sessionsByDiscipline: [String: [DaySession]] = [:]
        var daysByDiscipline: [String: [Int]] = [:]
        for day in 0..<7 {
            for session in mergedSessions(on: day, index: index) {
                guard let discipline = index.discipline(forClass: session.classId) else { continue }
                sessionsByDiscipline[discipline.id, default: []].append(session)
                if daysByDiscipline[discipline.id]?.contains(day) != true {
                    daysByDiscipline[discipline.id, default: []].append(day)
                }
            }
        }

        return index.sortedDisciplines
            .filter { classIdsByDiscipline[$0.id] != nil }
            .map { discipline in
                let code = index.displayCode(for: discipline)
                let teacher = classIdsByDiscipline[discipline.id]?
                    .firstNonNil { index.teacherName(forClass: $0) }
                return SpotlightDiscipline(
                    id: SpotlightEntityID.discipline(semesterId: semester.id, disciplineId: discipline.id),
                    semesterId: semester.id,
                    disciplineId: discipline.id,
                    title: discipline.name,
                    code: code,
                    subtitle: subtitle(
                        code: code,
                        days: daysByDiscipline[discipline.id] ?? [],
                        sessions: sessionsByDiscipline[discipline.id] ?? [],
                        index: index,
                        calendar: calendar
                    ),
                    keywords: [code, discipline.name, teacher, semester.code, semester.description]
                        .compactMap { $0?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty }
                )
            }
    }

    /// "MAT202 · seg · qua · 10:50 · MT-14" — days in the calendar's week
    /// order, the earliest session's start, the first room; absent parts drop.
    private func subtitle(
        code: String,
        days: [Int],
        sessions: [DaySession],
        index: SnapshotIndex,
        calendar: Calendar
    ) -> String {
        // Upstream days are 0 = Sunday; firstWeekday is 1-based on the same week.
        let weekStart = calendar.firstWeekday - 1
        let dayLabels = days
            .sorted { ($0 - weekStart + 7) % 7 < ($1 - weekStart + 7) % 7 }
            .map { day in
                let symbol = calendar.shortWeekdaySymbols[day]
                return symbol.hasSuffix(".") ? String(symbol.dropLast()) : symbol
            }
        let start = sessions.map(\.startMinute).min()
            .map { String(format: "%02d:%02d", $0 / 60, $0 % 60) }
        let room = sessions
            .firstNonNil { $0.spaceId.flatMap { index.spacesById[$0]?.location } }?
            .trimmingCharacters(in: .whitespaces).spotlightNonEmpty
        return ([code] + dayLabels + [start, room].compactMap { $0 }).joined(separator: " · ")
    }
}

extension String {
    fileprivate var spotlightNonEmpty: String? {
        isEmpty ? nil : self
    }
}
