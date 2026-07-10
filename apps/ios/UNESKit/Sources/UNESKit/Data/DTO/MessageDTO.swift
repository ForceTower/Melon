import Foundation

// MARK: - api/sync/messages (one page — newest first on the wire)

struct MessageListDTO: Decodable {
    let messages: [ItemDTO]
    /// Opaque cursor for the next page; nil on the last one.
    var nextCursor: String? = nil

    struct ItemDTO: Decodable {
        let id: String
        /// "upstream" (SAGRES) or "app".
        var source: String? = nil
        let subject: String?
        let content: String?
        let senderName: String?
        var senderType: Int? = nil
        /// ISO8601 with fractional seconds — kept as a string on purpose:
        /// `JSONDecoder`'s `.iso8601` cannot parse it, and the UTC string
        /// sorts lexicographically anyway.
        let timestamp: String?
        let read: Bool?
        var starred: Bool? = nil
        var scopes: [ScopeDTO]? = nil
        var attachments: [AttachmentDTO]? = nil
    }

    struct ScopeDTO: Decodable {
        let id: String
        let scope: String
        var classId: String? = nil
        var disciplineCode: String? = nil
        var disciplineName: String? = nil
    }

    struct AttachmentDTO: Decodable {
        let id: String
        let kind: String
        let url: String
        var name: String? = nil
        var position: Int? = nil
    }
}

extension MessageListDTO {
    var page: MessageMirrorPage {
        MessageMirrorPage(
            messages: messages.map {
                MessageRecord(
                    id: $0.id,
                    subject: $0.subject,
                    content: $0.content,
                    senderName: $0.senderName,
                    timestamp: $0.timestamp,
                    read: $0.read,
                    source: $0.source,
                    senderType: $0.senderType,
                    starred: $0.starred
                )
            },
            scopes: messages.flatMap { message in
                (message.scopes ?? []).map {
                    MessageScopeRecord(
                        id: $0.id,
                        messageId: message.id,
                        scope: $0.scope,
                        classId: $0.classId,
                        disciplineCode: $0.disciplineCode,
                        disciplineName: $0.disciplineName
                    )
                }
            },
            attachments: messages.flatMap { message in
                (message.attachments ?? []).map {
                    MessageAttachmentRecord(
                        id: $0.id,
                        messageId: message.id,
                        kind: $0.kind,
                        name: $0.name,
                        url: $0.url,
                        position: $0.position
                    )
                }
            }
        )
    }
}
