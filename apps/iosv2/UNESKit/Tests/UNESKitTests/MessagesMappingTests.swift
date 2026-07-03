import Foundation
import Testing

@testable import UNESKit

/// Mapping of mirrored message rows into the Mensagens inbox: origin
/// resolution, the local read/star overlay, date buckets, and time labels.
struct MessagesMappingTests {
    let calendar = Calendar.current

    private func date(month: Int = 4, day: Int, hour: Int = 12, minute: Int = 0) -> Date {
        calendar.date(from: DateComponents(year: 2026, month: month, day: day, hour: hour, minute: minute))!
    }

    private func scope(_ scope: String, code: String? = nil, name: String? = nil) -> MessageScopeRecord {
        MessageScopeRecord(
            id: UUID().uuidString,
            messageId: "m1",
            scope: scope,
            disciplineCode: code,
            disciplineName: name
        )
    }

    // MARK: Origin resolution

    @Test
    func appSourceBeatsEveryScope() {
        #expect(MirrorStore.resolveOrigin(source: "app", scopes: [scope("class")]) == .app)
    }

    @Test
    func scopelessUpstreamFallsBackToCampus() {
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: []) == .campus)
    }

    @Test
    func mostSpecificScopeWins() {
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("university"), scope("class")]) == .discipline)
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("coordination"), scope("personal")]) == .direct)
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("coordination")]) == .secretariat)
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("course")]) == .campus)
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("university")]) == .campus)
        #expect(MirrorStore.resolveOrigin(source: "upstream", scopes: [scope("list")]) == .app)
    }

    // MARK: Item mapping

    @Test
    func mapsARecordWithItsDisciplineScope() throws {
        let record = MessageRecord(
            id: "m1",
            subject: "  ",
            content: "  Pessoal,\n\nGabarito no moodle.  ",
            senderName: " Adriana Lima ",
            timestamp: "2026-04-18T11:14:00.000Z",
            read: false,
            source: "upstream",
            senderType: 2,
            starred: false
        )
        let item = MirrorStore.messageItem(
            record,
            scopes: [scope("class", code: "exa801", name: "Física II")],
            attachments: [
                MessageAttachmentRecord(id: "a1", messageId: "m1", kind: "pdf", name: "gabarito.pdf", url: "https://x.dev/g.pdf", position: 0),
            ],
            state: nil,
            colorIndexByCode: ["EXA801": 3]
        )

        #expect(item.origin == .discipline)
        #expect(item.disciplineCode == "exa801")
        #expect(item.disciplineName == "Física II")
        #expect(item.disciplineColorIndex == 3)
        // Blank subjects collapse so rows fall back to the preview-only layout.
        #expect(item.subject == nil)
        #expect(item.senderName == "Adriana Lima")
        #expect(item.preview == "Pessoal, Gabarito no moodle.")
        #expect(item.unread)
        #expect(!item.starred)
        #expect(item.attachments == [MessageAttachment(id: "a1", kind: .pdf, name: "gabarito.pdf", url: "https://x.dev/g.pdf")])
        #expect(item.receivedAt == (try Date("2026-04-18T11:14:00.000Z", strategy: MirrorStore.timestampFormat)))
    }

    @Test
    func localOverlayFlipsReadAndStar() {
        let record = MessageRecord(id: "m1", subject: nil, content: "Oi", senderName: "UNES", timestamp: nil, read: false)
        let state = MessageStateRecord(messageId: "m1", readAt: "2026-04-18T12:00:00.000Z", starred: true)

        let item = MirrorStore.messageItem(record, scopes: [], attachments: [], state: state)

        #expect(!item.unread)
        #expect(item.starred)
    }

    // MARK: Buckets

    @Test
    func bucketsSplitByCalendarDayNotBy24hBlocks() {
        let now = date(day: 18, hour: 13, minute: 20)

        // Yesterday evening is less than 24h ago but still "Ontem".
        #expect(MessagesFormat.bucket(for: date(day: 17, hour: 18, minute: 42), now: now, calendar: calendar) == .yesterday)
        #expect(MessagesFormat.bucket(for: date(day: 18, hour: 9), now: now, calendar: calendar) == .today)
        #expect(MessagesFormat.bucket(for: date(day: 15), now: now, calendar: calendar) == .thisWeek)
        #expect(MessagesFormat.bucket(for: date(day: 11), now: now, calendar: calendar) == .thisWeek)
        #expect(MessagesFormat.bucket(for: date(day: 10), now: now, calendar: calendar) == .thisMonth)
        #expect(MessagesFormat.bucket(for: date(month: 3, day: 18), now: now, calendar: calendar) == .thisMonth)
        #expect(MessagesFormat.bucket(for: date(month: 3, day: 17), now: now, calendar: calendar) == .older)
    }

    @Test
    func relativeTimeMatchesTheInboxScale() {
        let now = date(day: 18, hour: 13, minute: 20)

        // The row copy follows the resource-bundle language, so assert against
        // the same symbols the formatter resolves; the last row falls through to
        // HomeFormat.shortDate, which is locale-driven.
        #expect(MessagesFormat.relativeTime(for: date(day: 18, hour: 13, minute: 20), now: now, calendar: calendar) == String.localized(.messagesTimeNow))
        #expect(MessagesFormat.relativeTime(for: date(day: 18, hour: 13, minute: 8), now: now, calendar: calendar) == String.localized(.messagesTimeMinutesAgo(12)))
        #expect(MessagesFormat.relativeTime(for: date(day: 18, hour: 9, minute: 14), now: now, calendar: calendar) == String.localized(.messagesTimeHoursAgo(4)))
        #expect(MessagesFormat.relativeTime(for: date(day: 17, hour: 18, minute: 42), now: now, calendar: calendar) == String.localized(.messagesTimeYesterday))
        #expect(MessagesFormat.relativeTime(for: date(day: 14), now: now, calendar: calendar) == String.localized(.messagesTimeDaysAgo(4)))
        #expect(MessagesFormat.relativeTime(for: date(day: 10), now: now, calendar: calendar) == HomeFormat.shortDate(for: date(day: 10)))
    }

    // MARK: Filters

    @Test
    func filtersFoldOriginsIntoCategories() {
        let messages = MessagesOverview.preview(now: date(day: 18, hour: 13)).messages

        #expect(messages.count { MessageFilter.disciplines.matches($0) } == 3)
        #expect(messages.count { MessageFilter.university.matches($0) } == 3)
        #expect(messages.count { MessageFilter.app.matches($0) } == 2)
        #expect(messages.count { MessageFilter.unread.matches($0) } == 3)
        #expect(messages.count { MessageFilter.starred.matches($0) } == 1)
        #expect(messages.count { MessageFilter.all.matches($0) } == messages.count)
    }

    // MARK: Mirror round-trip

    @Test
    func localReadStateSurvivesAResync() async throws {
        let mirror = MirrorStore(writer: try inMemoryDatabase())
        let page = MessageMirrorPage(
            messages: [
                MessageRecord(
                    id: "m1", subject: nil, content: "Olá", senderName: "Reitoria",
                    timestamp: "2026-04-18T09:14:00.000Z", read: false, source: "upstream"
                ),
            ],
            scopes: [MessageScopeRecord(id: "s1", messageId: "m1", scope: "university")],
            attachments: []
        )
        let now = date(day: 18, hour: 13)

        try await mirror.upsertMessages(page, syncedAt: now)
        try await mirror.markAllMessagesRead(now: now)
        // The next sync still says read=false upstream — the overlay wins.
        try await mirror.upsertMessages(page, syncedAt: now.addingTimeInterval(60))

        let overview = try await mirror.messagesOverview(now: now)
        #expect(overview.messages.count == 1)
        #expect(overview.messages.first?.unread == false)
        #expect(overview.messages.first?.origin == .campus)
        #expect(overview.unreadCount == 0)

        let summary = try await mirror.messagesSummary()
        #expect(summary?.unreadCount == 0)
    }

    @Test
    func starOverlayRoundTrips() async throws {
        let mirror = MirrorStore(writer: try inMemoryDatabase())
        let page = MessageMirrorPage(
            messages: [
                MessageRecord(
                    id: "m1", subject: nil, content: "Olá", senderName: "UNES",
                    timestamp: "2026-04-18T09:14:00.000Z", read: true, source: "app"
                ),
            ],
            scopes: [],
            attachments: []
        )
        let now = date(day: 18, hour: 13)
        try await mirror.upsertMessages(page, syncedAt: now)

        try await mirror.setMessageStarred(id: "m1", starred: true)
        var overview = try await mirror.messagesOverview(now: now)
        #expect(overview.messages.first?.starred == true)

        try await mirror.setMessageStarred(id: "m1", starred: false)
        overview = try await mirror.messagesOverview(now: now)
        #expect(overview.messages.first?.starred == false)
    }
}
