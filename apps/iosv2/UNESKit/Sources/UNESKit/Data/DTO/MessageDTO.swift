import Foundation

// MARK: - api/sync/messages (subset — newest first on the wire)

struct MessageListDTO: Decodable {
    let messages: [ItemDTO]

    struct ItemDTO: Decodable {
        let id: String
        let subject: String?
        let content: String?
        let senderName: String?
        /// ISO8601 with fractional seconds — kept as a string on purpose:
        /// `JSONDecoder`'s `.iso8601` cannot parse it, and the UTC string
        /// sorts lexicographically anyway.
        let timestamp: String?
        let read: Bool?
    }
}

extension MessageListDTO {
    var records: [MessageRecord] {
        messages.map {
            MessageRecord(
                id: $0.id,
                subject: $0.subject,
                content: $0.content,
                senderName: $0.senderName,
                timestamp: $0.timestamp,
                read: $0.read
            )
        }
    }
}
