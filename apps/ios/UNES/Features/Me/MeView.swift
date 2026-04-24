import SwiftUI

/// "Eu" tab — personal hub that replaces the old menu list. Composed of:
/// identity hero, semester progress strip, pinned shortcut grid, services
/// list, sign-out pill, and a version footer. Mirrors `MeScreen` in
/// `screens-me.jsx`; the tweak-panel toggles live in Settings instead of an
/// in-screen overlay.
struct MeView: View {
    @State private var viewModel: MeViewModel
    // Shortcut grid + settings rows are UI affordances, not user data — they
    // keep reading from `MeFixtures`. Only the hero / semester strip / CR /
    // credits are viewmodel-driven.
    private let pinned = MeFixtures.pinned(from: MeFixtures.defaultPinned)
    private let settings = MeFixtures.settingsRows
    private let onLoggedOut: () -> Void
    // Initial guess used on first present; swapped for the measured height
    // once `LogoutConfirmationSheet` reports its intrinsic size so the
    // detent never leaves empty space below the buttons.
    @State private var logoutSheetHeight: CGFloat = 380
    // Type-erased path — the Me hub can push both `Shortcut.Kind` (from the
    // shortcut grid) and `MeSettingsRow.Kind` (from the settings list) onto
    // the same stack.
    @State private var path = NavigationPath()

    init(factory: MeFactory, onLoggedOut: @escaping () -> Void = {}) {
        _viewModel = State(initialValue: factory.makeViewModel())
        self.onLoggedOut = onLoggedOut
    }

    // Factory-less init — retained so `#Preview` keeps rendering against
    // `MeFixtures` without a live graph.
    init() {
        _viewModel = State(initialValue: MeViewModel())
        self.onLoggedOut = {}
    }

    private var identity: ProfileIdentity {
        viewModel.identity ?? MeFixtures.identity
    }

    var body: some View {
        NavigationStack(path: $path) {
            screenBody
                .navigationTitle("Eu")
                .toolbar(.hidden, for: .navigationBar)
                .navigationDestination(for: Shortcut.Kind.self) { kind in
                    switch kind {
                    case .countdown:
                        FinalCountdownView()
                    default:
                        EmptyView()
                    }
                }
                .navigationDestination(for: MeSettingsRow.Kind.self) { kind in
                    switch kind {
                    case .settings:
                        SettingsView()
                    default:
                        EmptyView()
                    }
                }
                .task { await viewModel.observe() }
                .sheet(isPresented: logoutSheetBinding) {
                    logoutSheet
                }
                .overlay {
                    if viewModel.logoutStep == .flashing {
                        LogoutFlashView()
                            .transition(.opacity)
                    }
                }
                .fullScreenCover(isPresented: loggedOutCoverBinding) {
                    LoggedOutView(firstName: viewModel.logoutName) {
                        onLoggedOut()
                    }
                }
        }
    }

    private var logoutSheetBinding: Binding<Bool> {
        Binding(
            get: { viewModel.logoutStep == .confirming },
            set: { showing in
                if !showing, viewModel.logoutStep == .confirming {
                    viewModel.cancelLogout()
                }
            }
        )
    }

    private var loggedOutCoverBinding: Binding<Bool> {
        Binding(
            get: { viewModel.logoutStep == .loggedOut },
            set: { _ in }
        )
    }

    @ViewBuilder
    private var logoutSheet: some View {
        if #available(iOS 16.4, *) {
            LogoutConfirmationSheet(
                identity: identity,
                onCancel: { viewModel.cancelLogout() },
                onConfirm: { keepData in
                    Task { await viewModel.confirmLogout(keepData: keepData) }
                },
                measuredHeight: $logoutSheetHeight
            )
            .presentationDetents([.height(logoutSheetHeight)])
            .presentationDragIndicator(.visible)
            .presentationBackground(UNESColor.surface)
            .presentationCornerRadius(28)
        } else {
            LogoutConfirmationSheet(
                identity: identity,
                onCancel: { viewModel.cancelLogout() },
                onConfirm: { keepData in
                    Task { await viewModel.confirmLogout(keepData: keepData) }
                },
                measuredHeight: $logoutSheetHeight
            )
            .presentationDetents([.height(logoutSheetHeight)])
            .presentationDragIndicator(.visible)
        }
    }

    private var screenBody: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Ambient rose mesh pinned behind the hero, fading into the
            // surface so it never reads as a hard seam. Intensity folds in
            // what the HTML prototype achieves with a wrapper `opacity: 0.4`
            // — applying opacity to the whole container separately from the
            // gradient produces a visible cream-on-cream step at the frame
            // edge, so we bake the dimness into the mesh itself instead.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .rose, intensity: 0.22)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: UNESColor.surface, location: 1.0),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 320)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    MeHeader(identity: identity)
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    VStack(spacing: 14) {
                        IdentityCard(identity: identity)
                            .fadeScaleInOnAppear(delay: 0.12, from: 0.985, duration: 0.6, anchor: .top)

                        SemesterStrip(identity: identity)
                            .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)

                        VStack(spacing: 0) {
                            MeSectionLabel(label: "atalhos fixados", actionLabel: "gerenciar")
                            ShortcutGrid(shortcuts: pinned, onTap: handleShortcutTap)
                        }
                        .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)

                        VStack(spacing: 0) {
                            MeSectionLabel(label: "definições")
                            SettingsCard(rows: settings, onTap: handleSettingsRowTap)
                        }
                        .fadeUpOnAppear(delay: 0.38, distance: 12, duration: 0.55)

                        SignOutButton(action: { viewModel.beginLogout() })
                            .fadeUpOnAppear(delay: 0.46, distance: 12, duration: 0.55)

                        footer
                            .fadeUpOnAppear(delay: 0.54, distance: 12, duration: 0.55)
                    }
                    .padding(.horizontal, 14)
                    .padding(.bottom, 24)
                }
            }
        }
    }

    private func handleShortcutTap(_ kind: Shortcut.Kind) {
        switch kind {
        case .countdown:
            path.append(kind)
        default:
            break
        }
    }

    private func handleSettingsRowTap(_ row: MeSettingsRow) {
        switch row.id {
        case .settings:
            path.append(row.id)
        default:
            break
        }
    }

    private var footer: some View {
        Text("◦ UNES V\(Bundle.main.appVersion) · FEITO COM ♥ EM FEIRA DE SANTANA ◦")
            .font(UNESFont.mono(9))
            .tracking(1.26)
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(.top, 4)
            .padding(.bottom, 8)
    }
}

#Preview {
    MeView()
}
