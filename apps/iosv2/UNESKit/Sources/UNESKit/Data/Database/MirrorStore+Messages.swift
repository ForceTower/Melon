import Foundation
import GRDB

private let log = Log.scoped("MirrorStore")

/// One `api/sync/messages` page as mirror rows.
struct MessageMirrorPage: Sendable {
    var messages: [MessageRecord]
    var scopes: [MessageScopeRecord]
    var attachments: [MessageAttachmentRecord]
}

extension MirrorStore {
    private static let messagesSyncedAtKey = "messagesSyncedAt"

    // MARK: Write path

    /// Upserts a fetched page, replacing each message's scope and attachment
    /// rows. `messageStates` (the local read/star overlay) is deliberately
    /// left alone so sync never resurrects an unread dot.
    func upsertMessages(_ page: MessageMirrorPage, syncedAt: Date) async throws {
        do {
            try await writer.write { db in
                for message in page.messages {
                    try message.upsert(db)
                }
                let ids = page.messages.map(\.id)
                try MessageScopeRecord.filter(ids.contains(Column("messageId"))).deleteAll(db)
                try MessageAttachmentRecord.filter(ids.contains(Column("messageId"))).deleteAll(db)
                for scope in page.scopes { try scope.insert(db) }
                for attachment in page.attachments { try attachment.insert(db) }
                let stamp = syncedAt.formatted(Self.timestampFormat)
                try SyncStateRecord(key: Self.messagesSyncedAtKey, value: stamp).upsert(db)
            }
            log.info("upsert messages count=\(page.messages.count)")
        } catch {
            log.error("upsert messages failed count=\(page.messages.count)", error: error)
            throw error
        }
    }

    func markMessageRead(id: String, now: Date) async throws {
        do {
            try await writer.write { db in
                var state = try MessageStateRecord.fetchOne(db, key: id)
                    ?? MessageStateRecord(messageId: id, readAt: nil, starred: false)
                guard state.readAt == nil else { return }
                state.readAt = now.formatted(Self.timestampFormat)
                try state.upsert(db)
            }
            log.debug("mark message read id=\(id)")
        } catch {
            log.error("mark message read failed id=\(id)", error: error)
            throw error
        }
    }

    func markAllMessagesRead(now: Date) async throws {
        do {
            try await writer.write { db in
                let stamp = now.formatted(Self.timestampFormat)
                let states = try MessageStateRecord.fetchAll(db)
                    .reduce(into: [String: MessageStateRecord]()) { $0[$1.messageId] = $1 }
                for message in try MessageRecord.fetchAll(db) where message.read != true {
                    var state = states[message.id]
                        ?? MessageStateRecord(messageId: message.id, readAt: nil, starred: false)
                    guard state.readAt == nil else { continue }
                    state.readAt = stamp
                    try state.upsert(db)
                }
            }
            log.debug("mark all messages read")
        } catch {
            log.error("mark all messages read failed", error: error)
            throw error
        }
    }

    func setMessageStarred(id: String, starred: Bool) async throws {
        do {
            try await writer.write { db in
                var state = try MessageStateRecord.fetchOne(db, key: id)
                    ?? MessageStateRecord(messageId: id, readAt: nil, starred: false)
                state.starred = starred
                try state.upsert(db)
                // Also flip the mirrored server value: the display OR-merges
                // it with the overlay, so an unstar would otherwise stay
                // shadowed until the ack round-trips through a refresh.
                if var message = try MessageRecord.fetchOne(db, key: id) {
                    message.starred = starred
                    try message.update(db)
                }
            }
            log.debug("set message starred id=\(id) starred=\(starred)")
        } catch {
            log.error("set message starred failed id=\(id)", error: error)
            throw error
        }
    }

    // MARK: Read path

    /// The inbox as mirrored on disk; nil until a first page has landed.
    func cachedMessagesOverview(now: Date) async throws -> MessagesOverview? {
        try await writer.read { db in try Self.cachedMessagesOverview(db, now: now) }
    }

    /// Emits the mirrored inbox on subscription and again after every write
    /// that feeds it — message pages, read/star overlays.
    func messagesOverviewUpdates(now: @escaping @Sendable () -> Date) -> AsyncValueObservation<MessagesOverview?> {
        ValueObservation
            .tracking { db in try Self.cachedMessagesOverview(db, now: now()) }
            .values(in: writer)
    }

    private static func cachedMessagesOverview(_ db: Database, now: Date) throws -> MessagesOverview? {
        guard try messagesSyncedAt(db) != nil || lastSyncedAt(db) != nil else { return nil }
        return try messagesOverview(db, now: now)
    }

    func messagesOverview(now: Date) async throws -> MessagesOverview {
        try await writer.read { db in try Self.messagesOverview(db, now: now) }
    }

    func messagesSummary() async throws -> MessagesSummary? {
        try await writer.read { db in try Self.messagesSummary(db) }
    }

    static func messagesSummary(_ db: Database) throws -> MessagesSummary? {
        let messages = try MessageRecord.order(Column("timestamp").desc).fetchAll(db)
        guard let latest = messages.first else { return nil }
        let readLocally = Set(
            try MessageStateRecord.filter(Column("readAt") != nil).fetchAll(db).map(\.messageId)
        )
        let preview = [latest.content, latest.subject]
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .first { !$0.isEmpty }
        return MessagesSummary(
            unreadCount: messages.count { $0.read == false && !readLocally.contains($0.id) },
            latestSenderName: latest.senderName?.trimmingCharacters(in: .whitespacesAndNewlines),
            latestPreview: preview
        )
    }

    // MARK: Mapping

    private static func messagesOverview(_ db: Database, now: Date) throws -> MessagesOverview {
        let records = try MessageRecord.order(Column("timestamp").desc).fetchAll(db)
        let scopesByMessage = Dictionary(
            grouping: try MessageScopeRecord.fetchAll(db),
            by: \.messageId
        )
        let attachmentsByMessage = Dictionary(
            grouping: try MessageAttachmentRecord.order(Column("position")).fetchAll(db),
            by: \.messageId
        )
        let statesByMessage = try MessageStateRecord.fetchAll(db)
            .reduce(into: [String: MessageStateRecord]()) { $0[$1.messageId] = $1 }
        let colorIndexByCode = try disciplineColorIndexByCode(db, today: now.dayStamp)

        let items = records.map { record in
            messageItem(
                record,
                scopes: scopesByMessage[record.id] ?? [],
                attachments: attachmentsByMessage[record.id] ?? [],
                state: statesByMessage[record.id],
                colorIndexByCode: colorIndexByCode
            )
        }
        return MessagesOverview(messages: items, syncedAt: try messagesSyncedAt(db) ?? lastSyncedAt(db))
    }

    static func messageItem(
        _ record: MessageRecord,
        scopes: [MessageScopeRecord],
        attachments: [MessageAttachmentRecord],
        state: MessageStateRecord?,
        colorIndexByCode: [String: Int] = [:]
    ) -> MessageItem {
        let origin = resolveOrigin(source: record.source, scopes: scopes)
        let disciplineScope = scopes.first { $0.scope == "class" }
        let code = disciplineScope?.disciplineCode?.trimmingCharacters(in: .whitespaces)
        return MessageItem(
            id: record.id,
            origin: origin,
            disciplineCode: code,
            disciplineName: disciplineScope?.disciplineName?.trimmingCharacters(in: .whitespaces),
            disciplineColorIndex: code.flatMap { colorIndexByCode[$0.uppercased()] },
            subject: record.subject?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty,
            body: record.content?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "",
            senderName: record.senderName?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "UNES",
            receivedAt: record.timestamp.flatMap { try? Date($0, strategy: timestampFormat) } ?? .distantPast,
            unread: record.read != true && state?.readAt == nil,
            starred: record.starred == true || state?.starred == true,
            attachments: attachments.map {
                MessageAttachment(
                    id: $0.id,
                    kind: MessageAttachment.Kind(rawValue: $0.kind) ?? .other,
                    name: $0.name,
                    url: $0.url
                )
            }
        )
    }

    /// The KMP `MessageOriginResolver` rule: app-authored messages are always
    /// `.app`; scopeless upstream ones fall back to `.campus`; otherwise the
    /// most specific scope decides.
    static func resolveOrigin(source: String?, scopes: [MessageScopeRecord]) -> MessageOrigin {
        guard source != "app" else { return .app }
        let winner = scopes.min { priority($0.scope) < priority($1.scope) }
        return switch winner?.scope {
        case "class": .discipline
        case "personal": .direct
        case "coordination": .secretariat
        case "course", "university": .campus
        case "list": .app
        default: .campus
        }
    }

    private static func priority(_ scope: String) -> Int {
        switch scope {
        case "class": 0
        case "personal": 1
        case "coordination": 2
        case "course": 3
        case "university": 4
        case "list": 5
        default: 6
        }
    }

    /// Tint indexes of the active semester's disciplines, keyed by uppercased
    /// code — the same locale-aware name order `SnapshotIndex` uses, so an
    /// inbox swatch matches the discipline's card in Turmas.
    private static func disciplineColorIndexByCode(_ db: Database, today: String) throws -> [String: Int] {
        let semesters = try SemesterRecord.order(Column("startDate").desc).fetchAll(db)
        guard let active = semesters.map(\.domain).active(today: today) else { return [:] }
        let disciplines = try DisciplineRecord.filter(Column("semesterId") == active.id).fetchAll(db)
            .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        var indexes: [String: Int] = [:]
        for (index, discipline) in disciplines.enumerated() {
            if let code = discipline.code?.trimmingCharacters(in: .whitespaces).uppercased(), !code.isEmpty {
                indexes[code] = index
            }
        }
        return indexes
    }

    private static func messagesSyncedAt(_ db: Database) throws -> Date? {
        try SyncStateRecord.fetchOne(db, key: messagesSyncedAtKey)
            .flatMap { try? Date($0.value, strategy: timestampFormat) }
    }
}

extension String {
    fileprivate var nonEmpty: String? {
        isEmpty ? nil : self
    }
}
