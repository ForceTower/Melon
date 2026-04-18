import Foundation

enum MeFixtures {
    static let identity = ProfileIdentity(
        name: "Mariana Nogueira",
        firstName: "Mariana",
        course: "Engenharia de Computação",
        campus: "Instituto de Computação · Niterói",
        enrollment: "118.104.072",
        avatarInitial: "M",
        semester: "2026.1",
        semesterWeek: 7,
        semesterTotalWeeks: 18,
        progressPct: 38,
        cr: 8.5,
        crDelta: "+0,3",
        creditsDone: 142,
        creditsRequired: 240,
        semesterStart: "início · 24 fev",
        finalExam: "prova final · 07 jul"
    )

    /// Full catalogue of shortcuts the hub can surface. Each tweak-panel preset
    /// picks a subset of these keys.
    static let library: [Shortcut.Kind: Shortcut] = [
        .account:   Shortcut(id: .account,   label: "Conta UNES",      hint: "SSO · histórico",
                             systemImage: "person.crop.circle", tone: .plum),
        .zhonya:    Shortcut(id: .zhonya,    label: "Zhonya",          hint: "pausar notificações",
                             systemImage: "moon.zzz",           tone: .magenta),
        .flowchart: Shortcut(id: .flowchart, label: "Fluxograma",      hint: "grade curricular",
                             systemImage: "square.stack.3d.up", tone: .teal),
        .bandejao:  Shortcut(id: .bandejao,  label: "Bandejão",        hint: "cardápio de hoje",
                             systemImage: "fork.knife",         tone: .amber),
        .calendar:  Shortcut(id: .calendar,  label: "Calendário",      hint: "datas acadêmicas",
                             systemImage: "calendar",           tone: .coral),
        .countdown: Shortcut(id: .countdown, label: "Final Countdown", hint: "semestre · 11 sem",
                             systemImage: "timer",              tone: .plum),
        .request:   Shortcut(id: .request,   label: "Requerimentos",   hint: "secretaria",
                             systemImage: "doc.text",           tone: .teal),
        .theme:     Shortcut(id: .theme,     label: "Editor de tema",  hint: "cores e fontes",
                             systemImage: "paintbrush",         tone: .magenta),
        .reminders: Shortcut(id: .reminders, label: "Lembretes",       hint: "3 ativos",
                             systemImage: "bell",               tone: .coral),
        .adventure: Shortcut(id: .adventure, label: "Adventure",       hint: "easter egg",
                             systemImage: "safari",             tone: .amber),
    ]

    static let shortcutSets: [MeShortcutSet] = [
        .init(id: "default",  label: "Padrão",
              pinned: [.account, .flowchart, .bandejao, .calendar, .countdown, .reminders]),
        .init(id: "academic", label: "Acadêmico",
              pinned: [.account, .flowchart, .calendar, .countdown, .request, .reminders]),
        .init(id: "campus",   label: "Campus",
              pinned: [.bandejao, .calendar, .countdown, .account, .adventure, .theme]),
        .init(id: "minimal",  label: "Mínimo",
              pinned: [.account, .flowchart, .bandejao]),
    ]

    static let defaultPinned: [Shortcut.Kind] = shortcutSets[0].pinned

    static let settingsRows: [MeSettingsRow] = [
        .init(id: .settings, label: "Configurações",
              hint: "tema, notificações, dados",    systemImage: "gearshape"),
        .init(id: .sync,     label: "Registro de sincronização",
              hint: "última: há 2 min",              systemImage: "arrow.triangle.2.circlepath", statusOK: true),
        .init(id: .about,    label: "Sobre o aplicativo",
              hint: "versão 4.2.1 · build 1842",     systemImage: "info.circle"),
        .init(id: .feedback, label: "Erros & sugestões",
              hint: "fale com os mantenedores",      systemImage: "ladybug"),
        .init(id: .licenses, label: "Licenças open source",
              hint: "126 pacotes",                   systemImage: "c.circle"),
    ]

    /// Lookup helper for view code that holds the pinned IDs but needs the
    /// full Shortcut record.
    static func pinned(from ids: [Shortcut.Kind]) -> [Shortcut] {
        ids.compactMap { library[$0] }
    }
}
