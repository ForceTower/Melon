import SwiftUI

/// "Configurações" screen — editorial settings hub. Three stacked sections:
/// account (credential vault), display (grade spoiler + lock-screen preview),
/// and notifications (three grouped cards with per-row toggles). Sync cadence,
/// wifi gates, and frequency moved server-side in this rewrite, so the
/// screen only exposes what the client still decides. Mirrors
/// `SettingsScreen` in `screens-settings.jsx`.
struct SettingsView: View {
    @State private var state = SettingsState()
    @State private var spoilerOpen = false

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Warm mesh wash pinned behind the header, fading into the surface
            // so the editorial type reads cleanly below. Same treatment as
            // `FinalCountdownView` — keeps the two screens tonally related.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.25)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.5),
                            .init(color: UNESColor.surface, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 300)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    SettingsHeader(lastSyncLabel: SettingsFixtures.lastSyncLabel)
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    VStack(spacing: 22) {
                        accountSection
                            .fadeUpOnAppear(delay: 0.12, distance: 12, duration: 0.55)

                        displaySection
                            .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)

                        notificationsSection
                            .fadeUpOnAppear(delay: 0.32, distance: 12, duration: 0.55)

                        SettingsFooter()
                            .fadeUpOnAppear(delay: 0.44, distance: 12, duration: 0.55)
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        // Keep the system nav bar for the native back chevron and the
        // interactive pop gesture; only hide its background so the warm
        // mesh reads continuously behind the header. Same treatment as
        // `FinalCountdownView`.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Sections

    private var accountSection: some View {
        VStack(spacing: 0) {
            SettingsSectionHeader(eyebrow: "capítulo 1", title: "Conta", meta: "UEFS")
            CredentialCard(
                credentials: SettingsFixtures.credentials,
                revealed: $state.credentialsRevealed
            )
        }
    }

    private var displaySection: some View {
        VStack(spacing: 10) {
            SettingsSectionHeader(eyebrow: "capítulo 2", title: "Exibição", meta: "spoiler")
            NotificationPreview(spoiler: state.spoiler)
            SpoilerPickerRow(value: $state.spoiler, isOpen: $spoilerOpen)
        }
    }

    private var notificationsSection: some View {
        let activeTotal = state.allNotificationKeyPaths.filter { state[keyPath: $0] }.count

        return VStack(spacing: 10) {
            SettingsSectionHeader(
                eyebrow: "capítulo 3",
                title: "Notificações",
                meta: "\(activeTotal)/9"
            )

            messagesGroup
            gradesGroup
            classesGroup
        }
    }

    private var messagesGroup: some View {
        NotificationGroupCard(
            kicker: "§ 1",
            title: "Mensagens",
            activeCount: state.messageKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "megaphone", tone: .amber,
                label: "Broadcasts", hint: "Avisos da universidade",
                isOn: $state.notifMsgBroadcast
            )
            NotificationToggleRow(
                icon: "person.2", tone: .teal,
                label: "Da turma", hint: "Mensagens enviadas à classe",
                isOn: $state.notifMsgClass
            )
            NotificationToggleRow(
                icon: "envelope", tone: .plum,
                label: "Diretas", hint: "Professor ou secretaria · você",
                isOn: $state.notifMsgDirect,
                showSeparator: false
            )
        }
    }

    private var gradesGroup: some View {
        NotificationGroupCard(
            kicker: "§ 2",
            title: "Notas",
            activeCount: state.gradeKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "sparkles", tone: .coral,
                label: "Publicada", hint: "Uma nota nova apareceu",
                isOn: $state.notifGradePosted
            )
            NotificationToggleRow(
                icon: "pencil", tone: .magenta,
                label: "Alterada", hint: "O valor foi corrigido",
                isOn: $state.notifGradeChanged
            )
            NotificationToggleRow(
                icon: "calendar", tone: .plum,
                label: "Data alterada", hint: "Prazo ou marco da avaliação",
                isOn: $state.notifGradeDateChanged,
                showSeparator: false
            )
        }
    }

    private var classesGroup: some View {
        NotificationGroupCard(
            kicker: "§ 3",
            title: "Aulas",
            activeCount: state.classKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "mappin.and.ellipse", tone: .teal,
                label: "Sala alterada", hint: "Mudança de localização",
                isOn: $state.notifClassLocation
            )
            NotificationToggleRow(
                icon: "book", tone: .amber,
                label: "Material publicado", hint: "Slides, enunciados, lista",
                isOn: $state.notifClassMaterial
            )
            NotificationToggleRow(
                icon: "tag", tone: .coral,
                label: "Assunto da aula", hint: "O tópico previsto mudou",
                isOn: $state.notifClassSubject,
                showSeparator: false
            )
        }
    }
}

#if DEBUG
    #Preview {
        NavigationStack {
            SettingsView()
        }
    }
#endif
