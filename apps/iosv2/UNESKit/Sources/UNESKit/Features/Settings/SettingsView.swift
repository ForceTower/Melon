import ComposableArchitecture
import SwiftUI

/// Configurações, end to end: the credential vault, the theme picker, grade
/// privacy with its live lock-screen mock, and the nine notification
/// switches. The inline nav title fades in as the large header scrolls away.
struct SettingsView: View {
    let store: StoreOf<SettingsFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .toolbar {
            ToolbarItem(placement: .principalCompat) {
                Text(.settingsTitle)
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    SettingsCredentialsHero(
                        name: store.displayName ?? String.localized(.settingsDefaultName),
                        email: store.profile?.email,
                        credentials: store.credentials,
                        isRevealed: store.isPasswordRevealed,
                        copied: store.copied,
                        onReveal: { store.send(.revealTapped) },
                        onCopy: { store.send(.copyTapped($0)) }
                    )
                    .scaleIn(delay: 0.1, duration: 0.62)
                    .padding(.bottom, 22)

                    VStack(spacing: 0) {
                        sectionHeader(String.localized(.settingsAppearance))
                        SettingsAppearanceCard(theme: store.theme) {
                            store.send(.themeSelected($0))
                        }
                    }
                    .fadeUp(delay: 0.2)
                    .padding(.bottom, 12)

                    VStack(spacing: 0) {
                        sectionHeader(String.localized(.settingsGrades), meta: String.localized(.settingsGradesPrivacyMeta))
                        SettingsSpoilerSection(spoiler: store.settings.gradeSpoiler) {
                            store.send(.spoilerSelected($0))
                        }
                    }
                    .fadeUp(delay: 0.28)
                    .padding(.bottom, 12)

                    VStack(spacing: 0) {
                        sectionHeader(
                            String.localized(.settingsNotifications),
                            meta: String.localized(.settingsNotificationsActiveCount(
                                store.settings.activeNotificationCount,
                                NotificationToggle.allCases.count
                            ))
                        )
                        VStack(spacing: 12) {
                            ForEach(SettingsNotificationGroup.all) { group in
                                SettingsNotificationGroupCard(group: group, settings: store.settings) {
                                    store.send(.notificationToggled($0))
                                }
                            }
                        }
                    }
                    .fadeUp(delay: 0.36)

                    footer
                        .fadeUp(delay: 0.44)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .scrollIndicators(.hidden)
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(.settingsTitle)
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text(.settingsSubtitle)
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 18, trailing: 20))
    }

    private func sectionHeader(_ title: String, meta: String? = nil) -> some View {
        HStack(alignment: .lastTextBaseline) {
            Text(title)
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)
            Spacer()
            if let meta {
                Text(meta)
                    .font(.system(size: 13, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }

    private var footer: some View {
        VStack(spacing: 3) {
            Text(MeFormat.versionBuildLabel)
            Text(.settingsFooterNote)
        }
        .font(.system(size: 11.5, weight: .medium))
        .foregroundStyle(UNESColor.ink4)
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 10, leading: 16, bottom: 4, trailing: 16))
    }

    /// Faint cool mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .cool, intensity: 0.5)
            .frame(height: 320)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.3)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

#Preview {
    NavigationStack {
        SettingsView(
            store: Store(initialState: SettingsFeature.State()) {
                SettingsFeature()
            }
        )
    }
}
