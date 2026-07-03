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
    }

    static let shortcutTileColor: ShortcutTileColor = .orange
}
