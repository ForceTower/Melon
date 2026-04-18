import SwiftUI

/// Message origin. Each kind gets distinct swatch treatment on the list row
/// and an accent color that tints the detail screen's ambient glow.
enum MessageOrigin: String, Hashable {
    case discipline   // from a specific enrolled discipline (sender is the prof)
    case secretariat  // from Secretaria Acadêmica
    case campus       // campus-wide announcement
    case app          // from the UNES team
    case module       // from an extra module (intercâmbio, biblioteca, RU)
    case direct       // direct message addressed to THIS student personally
}

enum MessageAttachmentKind: String, Hashable {
    case pdf, slides, image, link, other

    /// Uppercase label used in the meta row of a file tile.
    var label: String {
        switch self {
        case .pdf:    return "PDF"
        case .slides: return "SLIDES"
        case .image:  return "IMAGE"
        case .link:   return "LINK"
        case .other:  return "FILE"
        }
    }
}

struct MessageAttachment: Identifiable, Hashable {
    let id = UUID()
    let kind: MessageAttachmentKind
    /// File name for pdf/slides/image/other.
    let name: String?
    /// Human file size like "1.4 MB" for files, nil for images/links.
    let size: String?
    /// Link title (for .link).
    let title: String?
    /// Link host like "drive.google.com" (for .link).
    let host: String?
    /// Link URL (for .link).
    let url: String?

    init(
        kind: MessageAttachmentKind,
        name: String? = nil,
        size: String? = nil,
        title: String? = nil,
        host: String? = nil,
        url: String? = nil
    ) {
        self.kind = kind
        self.name = name
        self.size = size
        self.title = title
        self.host = host
        self.url = url
    }
}

struct MessageSender: Hashable {
    let name: String
    /// Secondary line under the name on the list row and sender card — usually
    /// the discipline title, "Comunicado oficial", or the role.
    let role: String
}

struct Message: Identifiable, Hashable {
    let id: String
    let origin: MessageOrigin
    /// Discipline short code, only for `.discipline`.
    let disciplineCode: String?
    /// Module id (intercambio/biblioteca/ru), only for `.module`.
    let moduleId: String?
    let sender: MessageSender
    /// Optional — app & module messages have subjects, prof/direct don't.
    let subject: String?
    /// Optional teaser used on the list row when the body is long.
    let preview: String?
    let body: String
    let receivedAt: Date
    var unread: Bool
    var starred: Bool
    let attachments: [MessageAttachment]

    init(
        id: String,
        origin: MessageOrigin,
        disciplineCode: String? = nil,
        moduleId: String? = nil,
        sender: MessageSender,
        subject: String? = nil,
        preview: String? = nil,
        body: String,
        receivedAt: Date,
        unread: Bool = false,
        starred: Bool = false,
        attachments: [MessageAttachment] = []
    ) {
        self.id = id
        self.origin = origin
        self.disciplineCode = disciplineCode
        self.moduleId = moduleId
        self.sender = sender
        self.subject = subject
        self.preview = preview
        self.body = body
        self.receivedAt = receivedAt
        self.unread = unread
        self.starred = starred
        self.attachments = attachments
    }
}

// MARK: - Origin metadata

/// Visual metadata attached to a message based on its origin. Mirrors
/// `originMeta()` in `screens-messages-data.jsx`.
struct MessageOriginMeta {
    let label: String
    let color: Color
    let kind: String
}

extension Message {
    var meta: MessageOriginMeta {
        switch origin {
        case .discipline:
            let color = Self.disciplineColor(code: disciplineCode)
            return .init(label: disciplineCode ?? "?", color: color, kind: "Disciplina")
        case .secretariat:
            return .init(label: "SEC",  color: Color(red: 0x6B/255, green: 0x5E/255, blue: 0x70/255), kind: "Secretaria")
        case .campus:
            return .init(label: "UEFS", color: Color(red: 0xD9/255, green: 0x85/255, blue: 0x2E/255), kind: "Universidade")
        case .app:
            return .init(label: "UNES", color: Color(red: 0x3B/255, green: 0x9E/255, blue: 0xAE/255), kind: "App")
        case .module:
            let (label, color) = Self.moduleMeta(moduleId: moduleId)
            return .init(label: label, color: color, kind: "Módulo")
        case .direct:
            return .init(label: "∙", color: Color(red: 0xE8/255, green: 0x5D/255, blue: 0x4E/255), kind: "Pessoal")
        }
    }

    /// Look up a discipline's accent color by short code across all semesters
    /// in `DisciplineFixtures`. Falls back to a neutral when not found.
    private static func disciplineColor(code: String?) -> Color {
        guard let code else { return Color(red: 0x6B/255, green: 0x5E/255, blue: 0x70/255) }
        for sem in DisciplineFixtures.semesters {
            if let d = sem.disciplines.first(where: { $0.code == code }) {
                return d.color
            }
        }
        for (_, discs) in DisciplineFixtures.lazyDisciplines {
            if let d = discs.first(where: { $0.code == code }) {
                return d.color
            }
        }
        return Color(red: 0x6B/255, green: 0x5E/255, blue: 0x70/255)
    }

    private static func moduleMeta(moduleId: String?) -> (String, Color) {
        switch moduleId {
        case "intercambio": return ("INTER", Color(red: 0x6B/255, green: 0x4B/255, blue: 0x9C/255))
        case "biblioteca": return ("BIB",   Color(red: 0x5C/255, green: 0x8C/255, blue: 0x3E/255))
        case "ru":         return ("RU",    Color(red: 0xC3/255, green: 0x7A/255, blue: 0x4A/255))
        default:           return ("MOD",   Color(red: 0x6B/255, green: 0x5E/255, blue: 0x70/255))
        }
    }
}

// MARK: - Filters

enum MessageFilter: String, CaseIterable, Identifiable {
    case all, unread, starred, disc, univ, app

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all:     return "Todas"
        case .unread:  return "Não lidas"
        case .starred: return "Salvas"
        case .disc:    return "Disciplinas"
        case .univ:    return "Universidade"
        case .app:     return "App & módulos"
        }
    }

    func matches(_ m: Message) -> Bool {
        switch self {
        case .all:     return true
        case .unread:  return m.unread
        case .starred: return m.starred
        case .disc:    return m.origin == .discipline || m.origin == .direct
        case .univ:    return m.origin == .secretariat || m.origin == .campus
        case .app:     return m.origin == .app || m.origin == .module
        }
    }
}

// MARK: - Date bucketing

/// Pinned "today" used by the mock data, matching the prototype.
enum MessageDate {
    static let today = DateComponents(
        calendar: .current, year: 2026, month: 4, day: 18, hour: 13, minute: 20
    ).date!

    enum Bucket: String, CaseIterable {
        case today      = "Hoje"
        case yesterday  = "Ontem"
        case thisWeek   = "Esta semana"
        case thisMonth  = "Este mês"
        case older      = "Mais antigas"
    }

    static func bucket(for date: Date) -> Bucket {
        let secs = today.timeIntervalSince(date)
        let days = Int(floor(secs / 86_400))
        if days <= 0 { return .today }
        if days == 1 { return .yesterday }
        if days <= 7 { return .thisWeek }
        if days <= 31 { return .thisMonth }
        return .older
    }

    /// Short relative label for the list row timestamp.
    static func relativeTime(for date: Date) -> String {
        let secs = today.timeIntervalSince(date)
        let mins = Int(floor(secs / 60))
        if mins < 1  { return "agora" }
        if mins < 60 { return "\(mins) min" }
        let hrs = mins / 60
        if hrs < 24  { return "\(hrs)h" }
        let days = hrs / 24
        if days == 1 { return "ontem" }
        if days <= 6 { return "\(days) d" }
        let months = ["jan","fev","mar","abr","mai","jun","jul","ago","set","out","nov","dez"]
        let cal = Calendar.current
        let m = cal.component(.month, from: date) - 1
        let d = cal.component(.day, from: date)
        return "\(d) \(months[m])"
    }

    /// Long form used on the detail screen.
    static func fullTime(for date: Date) -> String {
        let months = ["janeiro","fevereiro","março","abril","maio","junho","julho","agosto","setembro","outubro","novembro","dezembro"]
        let cal = Calendar.current
        let d = cal.component(.day, from: date)
        let m = cal.component(.month, from: date) - 1
        let y = cal.component(.year, from: date)
        let hh = String(format: "%02d", cal.component(.hour, from: date))
        let mm = String(format: "%02d", cal.component(.minute, from: date))
        return "\(d) de \(months[m]) de \(y) · \(hh):\(mm)"
    }
}
