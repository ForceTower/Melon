import AppIntents

/// The curated Siri / Spotlight / Action Button surface. Phrases are written
/// in pt-BR (the development language); `AppShortcuts.xcstrings` carries the
/// English variants.
struct UNESAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: NextClassIntent(),
            phrases: [
                "Qual minha próxima aula no \(.applicationName)",
                "Próxima aula no \(.applicationName)",
                "Que aula eu tenho agora no \(.applicationName)",
                "Qual é a próxima aula no \(.applicationName)",
            ],
            shortTitle: "shortcut.nextClass.title",
            systemImageName: "calendar.badge.clock"
        )
        AppShortcut(
            intent: TodayScheduleIntent(),
            phrases: [
                "Aulas de hoje no \(.applicationName)",
                "Meu horário de hoje no \(.applicationName)",
                "Que aulas eu tenho hoje no \(.applicationName)",
                "Como está meu dia no \(.applicationName)",
            ],
            shortTitle: "shortcut.today.title",
            systemImageName: "list.bullet.rectangle"
        )
        AppShortcut(
            intent: OpenTabIntent(),
            phrases: [
                "Abrir \(\.$tab) no \(.applicationName)",
                "Abre \(\.$tab) no \(.applicationName)",
                "Mostrar \(\.$tab) no \(.applicationName)",
            ],
            shortTitle: "shortcut.openTab.title",
            systemImageName: "arrow.up.forward.app"
        )
        AppShortcut(
            intent: ScoreIntent(),
            phrases: [
                "Qual meu score no \(.applicationName)",
                "Meu score no \(.applicationName)",
                "Como está meu score no \(.applicationName)",
                "Qual é o meu score no \(.applicationName)",
            ],
            shortTitle: "shortcut.score.title",
            systemImageName: "chart.line.uptrend.xyaxis"
        )
        AppShortcut(
            intent: UnreadMessagesIntent(),
            phrases: [
                "Tenho mensagem nova no \(.applicationName)",
                "Mensagens não lidas no \(.applicationName)",
                "Tem mensagem nova no \(.applicationName)",
                "Alguma mensagem nova no \(.applicationName)",
            ],
            shortTitle: "shortcut.unread.title",
            systemImageName: "envelope.badge"
        )
        AppShortcut(
            intent: FinalExamIntent(),
            phrases: [
                "Quanto preciso na final de \(\.$discipline) no \(.applicationName)",
                "Quanto preciso na Prova Final de \(\.$discipline) no \(.applicationName)",
                "Como estou em \(\.$discipline) no \(.applicationName)",
                "Quanto preciso na final no \(.applicationName)",
                "Tô de final no \(.applicationName)",
                "Quanto preciso tirar na prova de \(\.$discipline) no \(.applicationName)",
                "Quanto preciso tirar em \(\.$discipline) no \(.applicationName)",
            ],
            shortTitle: "shortcut.finalExam.title",
            systemImageName: "flag.checkered"
        )
        AppShortcut(
            intent: DisciplineNextClassIntent(),
            phrases: [
                "Quando é minha aula de \(\.$discipline) no \(.applicationName)",
                "Onde é minha aula de \(\.$discipline) no \(.applicationName)",
                "Qual o professor de \(\.$discipline) no \(.applicationName)",
                "Quem dá aula de \(\.$discipline) no \(.applicationName)",
            ],
            shortTitle: "shortcut.disciplineClass.title",
            systemImageName: "calendar.badge.clock"
        )
        AppShortcut(
            intent: DisciplineGradesIntent(),
            phrases: [
                "Qual minha nota em \(\.$discipline) no \(.applicationName)",
                "Minhas notas de \(\.$discipline) no \(.applicationName)",
                "Quanto tirei em \(\.$discipline) no \(.applicationName)",
            ],
            shortTitle: "shortcut.grades.title",
            systemImageName: "number.square"
        )
    }

    static let shortcutTileColor: ShortcutTileColor = .orange
}
