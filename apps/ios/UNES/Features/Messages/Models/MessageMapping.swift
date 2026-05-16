import Foundation
@preconcurrency import Umbrella

// Projects KMP feed types (`MessagesMessageFeedItem`, `MessagesMessageFeedDetail`)
// into the existing `Message` presentation struct so the views stay oblivious
// to the shared-layer shapes. Fields the DB doesn't track (moduleId, link
// title/host, attachment size, sender role) are either left nil or derived
// here from what we *do* have.
enum MessageMapping {
    static func map(_ item: MessagesMessageFeedItem) -> Message {
        let origin = mapOrigin(item.origin)
        let disciplineCode = item.disciplineCode
        return Message(
            id: item.id,
            origin: origin,
            disciplineCode: origin == .discipline ? disciplineCode : nil,
            moduleId: nil,
            sender: MessageSender(
                name: item.senderName,
                role: roleFor(origin: origin, disciplineName: item.disciplineName)
            ),
            subject: normalized(item.subject),
            preview: nil,
            body: item.content,
            receivedAt: parseDate(item.timestamp),
            unread: item.isUnread,
            starred: item.isStarred,
            attachments: []
        )
    }

    static func map(_ detail: MessagesMessageFeedDetail) -> Message {
        let origin = mapOrigin(detail.origin)
        let disciplineCode = detail.disciplineCode
        return Message(
            id: detail.id,
            origin: origin,
            disciplineCode: origin == .discipline ? disciplineCode : nil,
            moduleId: nil,
            sender: MessageSender(
                name: detail.senderName,
                role: roleFor(origin: origin, disciplineName: detail.disciplineName)
            ),
            subject: normalized(detail.subject),
            preview: nil,
            body: detail.content,
            receivedAt: parseDate(detail.timestamp),
            unread: detail.isUnread,
            starred: detail.isStarred,
            attachments: detail.attachments.map(mapAttachment)
        )
    }

    // MARK: - Helpers

    private static func mapOrigin(_ raw: MessagesMessageFeedOrigin) -> MessageOrigin {
        switch raw {
        case .discipline:  return .discipline
        case .secretariat: return .secretariat
        case .campus:      return .campus
        case .app:         return .app
        case .direct:      return .direct
        }
    }

    private static func mapAttachment(_ raw: MessagesMessageFeedAttachment) -> MessageAttachment {
        let kind = mapKind(raw.kind)
        switch kind {
        case .link:
            let host = hostFor(url: raw.url)
            return MessageAttachment(
                kind: .link,
                title: raw.name ?? host ?? raw.url,
                host: host,
                url: raw.url
            )
        case .image:
            return MessageAttachment(kind: .image, url: raw.url)
        case .pdf, .slides, .other:
            let name = raw.name ?? URL(string: raw.url)?.lastPathComponent ?? raw.url
            return MessageAttachment(kind: kind, name: name, url: raw.url)
        }
    }

    private static func mapKind(_ raw: MessagesMessageFeedAttachmentKind) -> MessageAttachmentKind {
        switch raw {
        case .image: return .image
        case .link:  return .link
        case .pdf:   return .pdf
        // Fixtures carry a `.slides` kind that upstream/DB never emits. Video
        // is rare; fold into the generic file tile.
        case .video: return .other
        case .other: return .other
        }
    }

    private static func roleFor(origin: MessageOrigin, disciplineName: String?) -> String {
        switch origin {
        case .discipline:
            return disciplineName?.isEmpty == false ? disciplineName! : "Disciplina"
        case .secretariat: return "Secretaria Acadêmica"
        case .campus:      return "Comunicado oficial"
        case .app:         return "Equipe UNES"
        case .module:      return "Módulo"
        case .direct:      return "Mensagem pessoal"
        }
    }

    private static func normalized(_ subject: String?) -> String? {
        guard let s = subject?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else {
            return nil
        }
        return s
    }

    private static func hostFor(url: String) -> String? {
        guard let host = URLComponents(string: url)?.host, !host.isEmpty else { return nil }
        return host.hasPrefix("www.") ? String(host.dropFirst(4)) : host
    }

    // ISO-8601 from Postgres/Ktor comes through in two flavors: with fractional
    // seconds ("2025-11-03T14:22:18.123Z") and without ("2025-11-03T14:22:18Z").
    // The default formatter rejects the first flavor, so fall through.
    private static func parseDate(_ iso: String) -> Date {
        if let d = isoFractional.date(from: iso) { return d }
        if let d = isoPlain.date(from: iso) { return d }
        return Date()
    }

    private static let isoFractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static let isoPlain: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()
}
