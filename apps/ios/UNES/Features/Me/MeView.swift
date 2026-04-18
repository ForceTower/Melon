import SwiftUI

/// "Eu" tab — personal hub that replaces the old menu list. Composed of:
/// identity hero, semester progress strip, pinned shortcut grid, services
/// list, sign-out pill, and a version footer. Mirrors `MeScreen` in
/// `screens-me.jsx`; the tweak-panel toggles live in Settings instead of an
/// in-screen overlay.
struct MeView: View {
    private let identity = MeFixtures.identity
    private let pinned = MeFixtures.pinned(from: MeFixtures.defaultPinned)
    private let settings = MeFixtures.settingsRows

    var body: some View {
        NavigationStack {
            screenBody
                .navigationTitle("Eu")
                .toolbar(.hidden, for: .navigationBar)
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
                            ShortcutGrid(shortcuts: pinned)
                        }
                        .fadeUpOnAppear(delay: 0.3, distance: 12, duration: 0.55)

                        VStack(spacing: 0) {
                            MeSectionLabel(label: "definições")
                            SettingsCard(rows: settings)
                        }
                        .fadeUpOnAppear(delay: 0.38, distance: 12, duration: 0.55)

                        SignOutButton()
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

    private var footer: some View {
        Text("◦ UNES V4.2.1 · FEITO COM ♥ EM FEIRA DE SANTANA ◦")
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
