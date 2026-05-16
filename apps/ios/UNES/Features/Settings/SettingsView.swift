import SwiftUI

/// "Configurações" screen — editorial settings hub. Three stacked sections:
/// account (credential vault), display (grade spoiler + lock-screen preview),
/// and notifications (three grouped cards with per-row toggles). Sync cadence,
/// wifi gates, and frequency moved server-side in this rewrite, so the
/// screen only exposes what the client still decides. Mirrors
/// `SettingsScreen` in `screens-settings.jsx`.
struct SettingsView: View {
    @State private var viewModel: SettingsViewModel
    // Reveal stays UI-only: every screen visit re-presents Face ID. Persisting
    // it would let a backgrounded app surface the password without a fresh
    // unlock, which is the wrong default for a credential vault.
    @State private var credentialsRevealed = false
    @State private var spoilerOpen = false

    init(factory: SettingsFactory) {
        _viewModel = State(initialValue: factory.makeViewModel())
    }

    // Factory-less init — retained so `#Preview` keeps rendering against
    // `SettingsFixtures` without a live graph.
    init() {
        _viewModel = State(initialValue: SettingsViewModel())
    }

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
                    SettingsHeader(lastSyncLabel: viewModel.lastSyncLabel)
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
        .task { await viewModel.observe() }
    }

    // MARK: - Sections

    private var accountSection: some View {
        VStack(spacing: 0) {
            SettingsSectionHeader(eyebrow: "capítulo 1", title: "Conta", meta: "UEFS")
            CredentialCard(
                // Real credentials win once the flow emits; the loading
                // placeholder keeps the card laid out so the section doesn't
                // collapse on first paint.
                credentials: viewModel.credentials ?? Self.loadingPlaceholder,
                revealed: $credentialsRevealed
            )
        }
    }

    // Empty pair shown for the brief window between view appearance and the
    // first credentials emission. The card renders blanks rather than fixture
    // data so a release build never leaks the design-time username/password.
    private static let loadingPlaceholder = SettingsCredentials(username: "", password: "")

    private var displaySection: some View {
        VStack(spacing: 10) {
            SettingsSectionHeader(eyebrow: "capítulo 2", title: "Exibição", meta: "spoiler")
            NotificationPreview(spoiler: viewModel.state.spoiler)
            SpoilerPickerRow(value: viewModel.spoilerBinding(), isOpen: $spoilerOpen)
        }
    }

    private var notificationsSection: some View {
        let state = viewModel.state
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
        let state = viewModel.state
        return NotificationGroupCard(
            kicker: "§ 1",
            title: "Mensagens",
            activeCount: state.messageKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "megaphone", tone: .amber,
                label: "Broadcasts", hint: "Avisos da universidade",
                isOn: viewModel.toggleBinding(\.notifMsgBroadcast)
            )
            NotificationToggleRow(
                icon: "person.2", tone: .teal,
                label: "Da turma", hint: "Mensagens enviadas à classe",
                isOn: viewModel.toggleBinding(\.notifMsgClass)
            )
            NotificationToggleRow(
                icon: "envelope", tone: .plum,
                label: "Diretas", hint: "Professor ou secretaria · você",
                isOn: viewModel.toggleBinding(\.notifMsgDirect),
                showSeparator: false
            )
        }
    }

    private var gradesGroup: some View {
        let state = viewModel.state
        return NotificationGroupCard(
            kicker: "§ 2",
            title: "Notas",
            activeCount: state.gradeKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "sparkles", tone: .coral,
                label: "Publicada", hint: "Uma nota nova apareceu",
                isOn: viewModel.toggleBinding(\.notifGradePosted)
            )
            NotificationToggleRow(
                icon: "pencil", tone: .magenta,
                label: "Alterada", hint: "O valor foi corrigido",
                isOn: viewModel.toggleBinding(\.notifGradeChanged)
            )
            NotificationToggleRow(
                icon: "calendar", tone: .plum,
                label: "Data alterada", hint: "Prazo ou marco da avaliação",
                isOn: viewModel.toggleBinding(\.notifGradeDateChanged),
                showSeparator: false
            )
        }
    }

    private var classesGroup: some View {
        let state = viewModel.state
        return NotificationGroupCard(
            kicker: "§ 3",
            title: "Aulas",
            activeCount: state.classKeyPaths.filter { state[keyPath: $0] }.count,
            total: 3
        ) {
            NotificationToggleRow(
                icon: "mappin.and.ellipse", tone: .teal,
                label: "Sala alterada", hint: "Mudança de localização",
                isOn: viewModel.toggleBinding(\.notifClassLocation)
            )
            NotificationToggleRow(
                icon: "book", tone: .amber,
                label: "Material publicado", hint: "Slides, enunciados, lista",
                isOn: viewModel.toggleBinding(\.notifClassMaterial)
            )
            NotificationToggleRow(
                icon: "tag", tone: .coral,
                label: "Assunto da aula", hint: "O tópico previsto mudou",
                isOn: viewModel.toggleBinding(\.notifClassSubject),
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
