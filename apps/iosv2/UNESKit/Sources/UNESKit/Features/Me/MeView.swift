import ComposableArchitecture
import SwiftUI

struct MeView: View {
    @Bindable var store: StoreOf<MeFeature>

    var body: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
            ZStack(alignment: .top) {
                UNESColor.surface.ignoresSafeArea()
                ambientWash
                content
            }
            .navigationTitle("Eu")
        } destination: { store in
            switch store.case {
            case let .settings(store):
                SettingsView(store: store)
            case let .calendar(store):
                CalendarView(store: store)
            case let .countdown(store):
                FinalCountdownView(store: store)
            case let .licenses(store):
                LicensesView(store: store)
            case let .enrollment(store):
                EnrollmentView(store: store)
            case let .enrollmentOffers(store):
                EnrollmentOffersView(store: store)
            case let .enrollmentDiscipline(store):
                EnrollmentDisciplineView(store: store)
            case let .enrollmentTimetable(store):
                EnrollmentTimetableView(store: store)
            case let .enrollmentReview(store):
                EnrollmentReviewView(store: store)
            case let .enrollmentSuccess(store):
                EnrollmentSuccessView(store: store)
            }
        }
        .task { await store.send(.task).finish() }
        .sheet(item: aboutBinding) { info in
            MeAboutSheet(info: info, isCopied: store.isAboutCopied) {
                store.send(.aboutCopyTapped)
            } onClose: {
                store.send(.aboutDismissed)
            }
        }
        .sheet(isPresented: logoutPromptBinding) {
            MeLogoutSheet(userName: store.displayName) {
                store.send(.logoutPromptDismissed)
            } onConfirm: { keepData in
                store.send(.logoutConfirmed(keepData: keepData))
            }
        }
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                MeIdentityHero(
                    name: store.displayName ?? "Estudante",
                    course: store.profile?.course,
                    campus: store.overview?.campus,
                    coefficient: store.overview?.coefficient,
                    attendancePercent: store.overview?.attendancePercent,
                    progress: store.overview?.progress
                )
                .scaleIn(delay: 0.1, duration: 0.62)
                .padding(.bottom, 20)

                if let progress = store.overview?.progress {
                    MeSemesterWidget(progress: progress)
                        .fadeUp(delay: 0.2)
                        .padding(.bottom, 22)
                }

                VStack(spacing: 0) {
                    sectionHeader("Atalhos")
                    MeShortcutGrid(countdown: store.overview?.countdown) { shortcut in
                        store.send(.shortcutTapped(shortcut))
                    }
                }
                .fadeUp(delay: 0.28)
                .padding(.bottom, 26)

                VStack(spacing: 0) {
                    sectionHeader("Definições")
                    MeSettingsList(syncedAt: store.syncedAt) { row in
                        store.send(.settingsRowTapped(row))
                    }
                }
                .fadeUp(delay: 0.36)
                .padding(.bottom, 22)

                logoutButton
                    .fadeUp(delay: 0.44)

                footer
                    .fadeUp(delay: 0.5)
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 12, trailing: 16))
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 22, weight: .bold))
            .tracking(-0.66)
            .foregroundStyle(UNESColor.ink)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }

    private var logoutButton: some View {
        Button {
            store.send(.logoutTapped)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.system(size: 14, weight: .semibold))
                Text("Sair da conta")
                    .font(.system(size: 15, weight: .semibold))
            }
            .foregroundStyle(Color(hex: 0xE5453A))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(TilePressStyle())
    }

    private var footer: some View {
        Text("\(MeFormat.versionLabel) · feito com ♥ em Feira de Santana")
            .font(.system(size: 11.5, weight: .medium))
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 4, trailing: 16))
    }

    /// Faint rose mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .rose, intensity: 0.55)
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
            .opacity(0.34)
            .offset(y: -80)
            .ignoresSafeArea()
    }

    // MARK: Sheet bindings

    private var aboutBinding: Binding<AppInfo?> {
        Binding(
            get: { store.aboutInfo },
            set: { value in
                if value == nil { store.send(.aboutDismissed) }
            }
        )
    }

    private var logoutPromptBinding: Binding<Bool> {
        Binding(
            get: { store.isLogoutPromptPresented },
            set: { value in
                if !value { store.send(.logoutPromptDismissed) }
            }
        )
    }
}

#Preview {
    MeView(
        store: Store(initialState: MeFeature.State(userName: "Mariana Nogueira")) {
            MeFeature()
        }
    )
}
