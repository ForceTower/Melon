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

    /// Emits the pending-evaluation projection on subscription and again
    /// after every write that feeds it — the reminder scheduler's input.
    /// Empties on logout wipe, so the scheduler also cancels everything.
    func evaluationReminderUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<[SpotlightEvaluation]> {
        ValueObservation
            .tracking { db in try Self.spotlightEvaluations(db, now: now()) }
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

    func spotlightEvaluations(ids: [String], now: Date) async throws -> [SpotlightEvaluation] {
        let wanted = Set(ids)
        return try await writer.read { db in
            try Self.spotlightEvaluations(db, now: now).filter { wanted.contains($0.id) }
        }
    }

    /// Scheduled pending evaluations, soonest first — the Shortcuts picker.
    func spotlightSuggestedEvaluations(now: Date) async throws -> [SpotlightEvaluation] {
        try await writer.read { db in try Self.spotlightEvaluations(db, now: now) }
    }

    func spotlightLectures(ids: [String], now: Date) async throws -> [SpotlightLecture] {
        let wanted = Set(ids)
        return try await writer.read { db in
            try Self.activeSpotlightScope(db, now: now)?.spotlightLectures().filter { wanted.contains($0.id) } ?? []
        }
    }

    /// The most recently dated lectures — the Shortcuts picker.
    func spotlightSuggestedLectures(now: Date, limit: Int) async throws -> [SpotlightLecture] {
        try await writer.read { db in
            let lectures = try Self.activeSpotlightScope(db, now: now)?.spotlightLectures() ?? []
            return Array(lectures.filter { $0.dateStamp != nil }.suffix(limit).reversed())
        }
    }

    func spotlightSessions(ids: [String], now: Date) async throws -> [SpotlightSession] {
        let wanted = Set(ids)
        return try await writer.read { db in
            try Self.activeSpotlightScope(db, now: now)?.spotlightSessions().filter { wanted.contains($0.id) } ?? []
        }
    }

    func spotlightSuggestedSessions(now: Date) async throws -> [SpotlightSession] {
        try await writer.read { db in try Self.activeSpotlightScope(db, now: now)?.spotlightSessions() ?? [] }
    }

    func spotlightPersonalEvents(ids: [String]) async throws -> [SpotlightPersonalEvent] {
        let wanted = Set(ids)
        return try await writer.read { db in
            try Self.fetchPersonalEvents(db).map(Self.spotlightPersonalEvent).filter { wanted.contains($0.id) }
        }
    }

    func spotlightSuggestedPersonalEvents() async throws -> [SpotlightPersonalEvent] {
        try await writer.read { db in try Self.fetchPersonalEvents(db).map(Self.spotlightPersonalEvent) }
    }

    // MARK: Fetch

    private static func spotlightSnapshot(_ db: Database, now: Date) throws -> SpotlightSnapshot? {
        guard try lastSyncedAt(db) != nil else { return nil }
        let records = try MessageRecord.order(Column("timestamp").desc).fetchAll(db)
        let scope = try activeSpotlightScope(db, now: now)
        return SpotlightSnapshot(
            disciplines: scope?.spotlightDisciplines() ?? [],
            messages: try spotlightMessages(db, records: records),
            evaluations: scope?.spotlightEvaluations(now: now) ?? [],
            lectures: scope?.spotlightLectures() ?? [],
            sessions: scope?.spotlightSessions() ?? [],
            personalEvents: try fetchPersonalEvents(db).map(spotlightPersonalEvent)
        )
    }

    private static func spotlightDisciplines(_ db: Database, now: Date) throws -> [SpotlightDiscipline] {
        try activeSpotlightScope(db, now: now)?.spotlightDisciplines() ?? []
    }

    private static func spotlightEvaluations(_ db: Database, now: Date) throws -> [SpotlightEvaluation] {
        try activeSpotlightScope(db, now: now)?.spotlightEvaluations(now: now) ?? []
    }

    private static func activeSpotlightScope(_ db: Database, now: Date) throws -> SemesterSnapshot? {
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        guard let active = semesters.map(\.domain).active(today: now.dayStamp),
              let record = semesters.first(where: { $0.id == active.id })
        else { return nil }
        return try spotlightScope(for: record, db: db)
    }

    /// Like `snapshot(for:db:)` minus materials. Grade rows are in scope
    /// since Phase 3 — the evaluation projection needs their names and
    /// dates — but grade *values* still never reach a projection; the
    /// projection tests pin that. Lectures joined in Phase 4.
    private static func spotlightScope(for semester: SemesterRecord, db: Database) throws -> SemesterSnapshot {
        func scoped<R: FetchableRecord & TableRecord>(_ type: R.Type) throws -> [R] {
            try R.filter(Column("semesterId") == semester.id).orderByPrimaryKey().fetchAll(db)
        }
        var snapshot = try SemesterSnapshot(
            semester: semester,
            disciplines: scoped(DisciplineRecord.self),
            disciplineOffers: scoped(DisciplineOfferRecord.self),
            classes: scoped(ClassRecord.self),
            teachers: scoped(TeacherRecord.self),
            classTeachers: scoped(ClassTeacherRecord.self),
            spaces: scoped(SpaceRecord.self),
            allocations: scoped(AllocationRecord.self),
            studentClasses: scoped(StudentClassRecord.self),
            studentGrades: scoped(StudentGradeRecord.self)
        )
        snapshot.lectures = try scoped(LectureRecord.self)
        return snapshot
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
        let body = record.content?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""

        // Subject-less institutional messages are titled by their origin,
        // like the inbox's categorization — the individual sender's name is
        // the wrong headline for a class notice or a rectorate broadcast.
        // Personal (and app) messages keep the sender: there the name is
        // the point.
        let origin = resolveOrigin(source: record.source, scopes: scopes)
        let disciplineCode = disciplineScope?.disciplineCode?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty
        let disciplineName = disciplineScope?.disciplineName?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty
        let originTitle: String? = switch origin {
        case .discipline:
            disciplineName ?? disciplineCode
        case .campus:
            String.localized(.messagesFilterUniversity)
        case .secretariat:
            String.localized(.messagesRoleSecretariat)
        case .direct, .app:
            nil
        }
        let title = subject ?? originTitle ?? sender

        // The origin doubles as the schema entity's "conversation": a
        // stable key per grouping, named the way the inbox names it.
        let originId: String = switch origin {
        case .discipline: "class/\(disciplineCode ?? disciplineScope?.classId ?? "")"
        case .campus: "university"
        case .secretariat: "secretariat"
        case .app: "app"
        case .direct: "sender/\(sender)"
        }

        // The sender rides in the subtitle whenever it isn't the title
        // itself; subject-less rows add a body snippet so several notes
        // under one title stay tellable-apart.
        let subtitleParts = subject != nil
            ? [sender, date]
            : [title == sender ? nil : sender, date, snippet(of: body)]
        return SpotlightMessage(
            id: SpotlightEntityID.message(id: record.id),
            messageId: record.id,
            title: title,
            subtitle: subtitleParts.compactMap { $0 }.joined(separator: " · "),
            body: body,
            keywords: [disciplineCode, disciplineName].compactMap { $0 },
            sender: sender,
            receivedAt: receivedAt,
            isRead: record.read ?? false,
            originId: originId,
            originName: originTitle ?? sender
        )
    }

    /// The body collapsed to one line and capped — enough to tell results
    /// apart in the Spotlight list, not a second body copy.
    private static func snippet(of body: String) -> String? {
        let collapsed = body.split(whereSeparator: \.isWhitespace).joined(separator: " ")
        guard !collapsed.isEmpty else { return nil }
        guard collapsed.count > 80 else { return collapsed }
        return collapsed.prefix(80) + "…"
    }

    static func spotlightPersonalEvent(_ event: PersonalEvent) -> SpotlightPersonalEvent {
        SpotlightPersonalEvent(
            id: SpotlightEntityID.personalEvent(id: event.id),
            eventId: event.id,
            title: event.title,
            start: event.start,
            end: event.end,
            notes: event.notes,
            category: event.category.rawValue,
            disciplineName: event.discipline?.name,
            disciplineCode: event.discipline?.code
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
                    .firstNonNil { index.teacherName(forClass: $0) }?
                    .trimmingCharacters(in: .whitespaces).spotlightNonEmpty
                let week = weekLine(
                    days: daysByDiscipline[discipline.id] ?? [],
                    sessions: sessionsByDiscipline[discipline.id] ?? [],
                    index: index,
                    calendar: calendar
                )
                return SpotlightDiscipline(
                    id: SpotlightEntityID.discipline(semesterId: semester.id, disciplineId: discipline.id),
                    semesterId: semester.id,
                    disciplineId: discipline.id,
                    title: discipline.name,
                    code: code,
                    subtitle: ([code] + week.labels + [week.start, week.room].compactMap { $0 }).joined(separator: " · "),
                    keywords: [code, discipline.name, teacher, semester.code, semester.description]
                        .compactMap { $0?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty },
                    teacher: teacher,
                    room: week.room,
                    schedule: (week.labels + [week.start].compactMap { $0 }).joined(separator: " · ").spotlightNonEmpty
                )
            }
    }

    /// "seg · qua" + "10:50" + "MT-14" — days in the calendar's week order,
    /// the earliest session's start, the first room; absent parts nil.
    private func weekLine(
        days: [Int],
        sessions: [DaySession],
        index: SnapshotIndex,
        calendar: Calendar
    ) -> (labels: [String], start: String?, room: String?) {
        // Upstream days are 0 = Sunday; firstWeekday is 1-based on the same week.
        let weekStart = calendar.firstWeekday - 1
        let labels = days
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
        return (labels, start, room)
    }
}

// MARK: - Semester snapshot → [SpotlightSession]

extension SemesterSnapshot {
    /// One entry per merged weekly session of an enrolled class, Sunday
    /// first — the rows the Horário rail draws, carrying the room, the
    /// teacher, and the semester window for the calendar recurrence.
    func spotlightSessions() -> [SpotlightSession] {
        let index = SnapshotIndex(snapshot: self)
        var sessions: [SpotlightSession] = []
        for day in 0..<7 {
            for session in mergedSessions(on: day, index: index) {
                guard let discipline = index.discipline(forClass: session.classId) else { continue }
                sessions.append(SpotlightSession(
                    id: SpotlightEntityID.session(
                        semesterId: semester.id,
                        disciplineId: discipline.id,
                        classId: session.classId,
                        day: day,
                        startMinute: session.startMinute
                    ),
                    semesterId: semester.id,
                    disciplineId: discipline.id,
                    classId: session.classId,
                    title: discipline.name,
                    code: index.displayCode(for: discipline),
                    day: day,
                    startMinute: session.startMinute,
                    endMinute: session.endMinute,
                    room: session.spaceId.flatMap { index.spacesById[$0]?.location }?
                        .trimmingCharacters(in: .whitespaces).spotlightNonEmpty,
                    teacher: index.teacherName(forClass: session.classId)?
                        .trimmingCharacters(in: .whitespaces).spotlightNonEmpty,
                    semesterStart: semester.startDate,
                    semesterEnd: semester.endDate
                ))
            }
        }
        return sessions
    }
}

// MARK: - Semester snapshot → [SpotlightLecture]

extension SemesterSnapshot {
    /// One entry per lecture with a posted subject in an enrolled class,
    /// chronological (undated last) — the class content students search
    /// for before a test. Placeholder rows without a subject are dropped,
    /// as the detail screen drops them.
    func spotlightLectures(
        calendar: Calendar = .current,
        locale: Locale = .autoupdatingCurrent
    ) -> [SpotlightLecture] {
        let index = SnapshotIndex(snapshot: self)
        return lectures
            .filter { index.enrolledClassIds.contains($0.classId) }
            .compactMap { lecture -> SpotlightLecture? in
                guard let subject = lecture.subject?.trimmingCharacters(in: .whitespacesAndNewlines).spotlightNonEmpty,
                      let discipline = index.discipline(forClass: lecture.classId)
                else { return nil }
                let dateLine = lecture.date
                    .flatMap { parseDayStamp($0, calendar: calendar) }
                    .map { $0.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated).locale(locale)) }
                return SpotlightLecture(
                    id: SpotlightEntityID.lecture(
                        semesterId: semester.id, disciplineId: discipline.id, lectureId: lecture.id
                    ),
                    semesterId: semester.id,
                    disciplineId: discipline.id,
                    lectureId: lecture.id,
                    title: subject,
                    subtitle: [dateLine, discipline.name].compactMap { $0 }.joined(separator: " · "),
                    dateStamp: lecture.date,
                    keywords: [subject, index.displayCode(for: discipline), discipline.name]
                )
            }
            .sorted { ($0.dateStamp ?? "9999-99-99", $0.lectureId) < ($1.dateStamp ?? "9999-99-99", $1.lectureId) }
    }
}

// MARK: - Semester snapshot → [SpotlightEvaluation]

extension SemesterSnapshot {
    /// One entry per scheduled, still-pending evaluation, soonest first —
    /// the calendar-shaped data. The backend replicates the discipline-level
    /// grade set onto every group row, so rows deduplicate by the detail
    /// screen's grade key (`platformId ?? id`). Carries the evaluation name,
    /// date, and discipline linkage only — never a value.
    func spotlightEvaluations(
        now: Date,
        calendar: Calendar = .current,
        locale: Locale = .autoupdatingCurrent
    ) -> [SpotlightEvaluation] {
        let index = SnapshotIndex(snapshot: self)
        let today = now.dayStamp
        var seenKeys: Set<String> = []
        return studentGrades
            .sorted { ($0.date ?? "", $0.ordinal, $0.id) < ($1.date ?? "", $1.ordinal, $1.id) }
            .compactMap { grade -> SpotlightEvaluation? in
                guard grade.value == nil,
                      let date = grade.date, date >= today,
                      let studentClass = index.studentClassesById[grade.studentClassId],
                      let discipline = index.discipline(forClass: studentClass.classId)
                else { return nil }
                let gradeId = grade.platformId ?? grade.id
                guard seenKeys.insert("\(discipline.id)/\(gradeId)").inserted else { return nil }
                let title = gradeTitle(grade)
                let code = index.displayCode(for: discipline)
                return SpotlightEvaluation(
                    id: SpotlightEntityID.evaluation(
                        semesterId: semester.id,
                        disciplineId: discipline.id,
                        gradeId: gradeId
                    ),
                    semesterId: semester.id,
                    disciplineId: discipline.id,
                    gradeId: gradeId,
                    title: "\(title) — \(discipline.name)",
                    subtitle: evaluationSubtitle(dateStamp: date, calendar: calendar, locale: locale),
                    dateStamp: date,
                    keywords: [grade.name, grade.nameShort, code, discipline.name]
                        .compactMap { $0?.trimmingCharacters(in: .whitespaces).spotlightNonEmpty }
                )
            }
    }

    /// "qui., 15 de ago." — the localized date line under the evaluation.
    private func evaluationSubtitle(dateStamp: String, calendar: Calendar, locale: Locale) -> String {
        guard let date = parseDayStamp(dateStamp, calendar: calendar) else { return dateStamp }
        return date.formatted(
            .dateTime.weekday(.abbreviated).day().month(.abbreviated).locale(locale)
        )
    }
}

extension String {
    fileprivate var spotlightNonEmpty: String? {
        isEmpty ? nil : self
    }
}
