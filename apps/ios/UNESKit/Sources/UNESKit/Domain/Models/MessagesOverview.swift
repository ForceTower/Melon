import Foundation

/// The Mensagens inbox, newest first, computed from the mirrored messages
/// plus the local read/star overlay.
struct MessagesOverview: Equatable, Sendable {
    var messages: [MessageItem] = []
    var syncedAt: Date?

    static let empty = MessagesOverview()

    var unreadCount: Int {
        messages.count(where: \.unread)
    }

    func count(in category: MessageCategory) -> Int {
        messages.count { $0.category == category }
    }
}

/// Where a message came from, resolved from its source + scope rows the same
/// way the KMP `MessageOriginResolver` does: app-authored messages are always
/// `.app`; otherwise the most specific scope wins (class → discipline,
/// personal → direct, coordination → secretariat, course/university → campus,
/// list → app) and scopeless upstream messages fall back to `.campus`.
enum MessageOrigin: String, Codable, Equatable, Sendable {
    case discipline
    case secretariat
    case campus
    case app
    case direct
}

/// The digest-hero categories: each origin folds into one composition slice.
enum MessageCategory: Equatable, Sendable, CaseIterable {
    case disciplines
    case university
    case app
}

struct MessageItem: Equatable, Sendable, Identifiable {
    var id: String
    var origin: MessageOrigin
    /// SAGRES discipline code from the class scope, e.g. "EXA801".
    var disciplineCode: String?
    var disciplineName: String?
    /// Tint index of the matching active-semester discipline card, so the
    /// inbox swatch matches Turmas; nil when the discipline isn't mirrored.
    var disciplineColorIndex: Int?
    var subject: String?
    var body: String
    var senderName: String
    var receivedAt: Date
    var unread: Bool
    var starred: Bool
    var attachments: [MessageAttachment] = []

    var category: MessageCategory {
        switch origin {
        case .discipline, .direct: .disciplines
        case .secretariat, .campus: .university
        case .app: .app
        }
    }

    /// The body flattened to one paragraph for list rows.
    var preview: String {
        body
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .joined(separator: " ")
    }

    var imageAttachments: [MessageAttachment] {
        attachments.filter { $0.kind == .image }
    }

    var fileAttachments: [MessageAttachment] {
        attachments.filter { $0.kind != .image }
    }
}

struct MessageAttachment: Equatable, Sendable, Identifiable {
    enum Kind: String, Codable, Sendable {
        case image, link, pdf, video, other
    }

    var id: String
    var kind: Kind
    var name: String?
    var url: String

    /// "drive.google.com" — the subtitle for link tiles.
    var host: String? {
        URL(string: url)?.host() ?? url.split(separator: "/").first.map(String.init)
    }
}

extension MessagesOverview {
    static func preview(now: Date = .now) -> MessagesOverview {
        func ago(_ hours: Double) -> Date { now.addingTimeInterval(-hours * 3600) }
        return MessagesOverview(
            messages: [
                MessageItem(
                    id: "m1", origin: .campus,
                    subject: nil,
                    body: "Prezadxs estudantes,\n\nConfirmamos o retorno das atividades presenciais na próxima quarta-feira, 22 de abril, após a semana de recesso.\n\nA secretaria está disponível para tirar dúvidas sobre trancamento e matrícula extemporânea até sexta-feira. Confira o calendário em uefs.br/calendario-2026-1.\n\nAtenciosamente,\nReitoria UEFS",
                    senderName: "Reitoria UEFS",
                    receivedAt: ago(4), unread: true, starred: false
                ),
                MessageItem(
                    id: "m2", origin: .discipline,
                    disciplineCode: "ALGI", disciplineName: "Algoritmos e Programação II", disciplineColorIndex: 0,
                    subject: nil,
                    body: "Pessoal,\n\nSubi o gabarito comentado da primeira avaliação no moodle. Qualquer dúvida, usem o fórum da disciplina ou me procurem no início da aula de segunda.\n\nA média da turma foi 6.2 — vamos revisar ponteiros duplos com calma na terça.\n\nAbs,\nAdriana",
                    senderName: "Adriana Lima",
                    receivedAt: ago(5.2), unread: true, starred: true,
                    attachments: [
                        MessageAttachment(id: "a1", kind: .pdf, name: "gabarito-av1-algi.pdf", url: "https://example.org/gabarito-av1-algi.pdf"),
                    ]
                ),
                MessageItem(
                    id: "m3", origin: .direct,
                    subject: nil,
                    body: "Mariana,\n\nNotei que você não compareceu ao laboratório 03 ontem. Se foi alguma questão de saúde ou imprevisto, me avise em particular para que eu possa ajustar a nota de participação.\n\nAtt,\nBeatriz",
                    senderName: "Beatriz Sampaio",
                    receivedAt: ago(19), unread: true, starred: false
                ),
                MessageItem(
                    id: "m4", origin: .app,
                    subject: "Novidade: notas da prática e teórica separadas",
                    body: "Olá,\n\nA partir desta semana, disciplinas com múltiplas turmas mostram professores, notas e anexos separados por turma.\n\nComo sempre, nos escrevam se encontrarem algo estranho — este app é feito por alunos para alunos.\n\n— UNES",
                    senderName: "UNES",
                    receivedAt: ago(28), unread: false, starred: false,
                    attachments: [
                        MessageAttachment(id: "a2", kind: .image, name: "preview.png", url: "https://example.org/preview.png"),
                    ]
                ),
                MessageItem(
                    id: "m5", origin: .discipline,
                    disciplineCode: "LPOO", disciplineName: "Linguagem de Programação OO", disciplineColorIndex: 2,
                    subject: nil,
                    body: "Pessoal,\n\nAdicionei dois capítulos do Bloch (Effective Java) e o slide atualizado sobre mixins ao repositório da disciplina. Recomendo muito a leitura antes da aula de quinta.",
                    senderName: "Carlos Mendes",
                    receivedAt: ago(3 * 24), unread: false, starred: false,
                    attachments: [
                        MessageAttachment(id: "a3", kind: .pdf, name: "effective-java-cap3.pdf", url: "https://example.org/cap3.pdf"),
                        MessageAttachment(id: "a4", kind: .pdf, name: "effective-java-cap4.pdf", url: "https://example.org/cap4.pdf"),
                        MessageAttachment(id: "a5", kind: .link, name: "Slides — mixins 2026", url: "https://drive.google.com/mixins-2026"),
                    ]
                ),
                MessageItem(
                    id: "m6", origin: .secretariat,
                    subject: nil,
                    body: "Caros estudantes,\n\nConvidamos vocês para uma roda de conversa sobre Transtorno do Espectro Autista (TEA), organizada pelo núcleo de acessibilidade.\n\nData: 24 de abril, 14h.\nLocal: Auditório Central.",
                    senderName: "Secretaria Acadêmica",
                    receivedAt: ago(4 * 24), unread: false, starred: false
                ),
                MessageItem(
                    id: "m7", origin: .campus,
                    subject: nil,
                    body: "Estudantes,\n\nDevido à greve anunciada dos transportes municipais para o dia 15/04, as aulas presenciais serão opcionais. Os professores foram orientados a não aplicar avaliações.",
                    senderName: "Comunicação UEFS",
                    receivedAt: ago(11 * 24), unread: false, starred: false
                ),
                MessageItem(
                    id: "m8", origin: .app,
                    subject: "Bem-vinda ao UNES",
                    body: "Oi, Mariana!\n\nSeja bem-vinda ao UNES. Se precisar de algo ou tiver sugestões, nos mande uma mensagem direto por aqui — respondemos rápido.\n\nBons estudos!\n— Equipe UNES",
                    senderName: "UNES",
                    receivedAt: ago(38 * 24), unread: false, starred: false
                ),
            ],
            syncedAt: now.addingTimeInterval(-120)
        )
    }
}
