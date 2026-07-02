import ComposableArchitecture
import Foundation

/// The two pinned shortcuts. Countdown pushes the calculator; the calendar
/// opens its teaser sheet until the feature lands.
enum MeShortcut: String, Equatable, Sendable, Identifiable, CaseIterable {
    case calendar, countdown

    var id: String { rawValue }
}

/// The "Definições" rows. Settings and Licenses push their screens; the
/// remaining destinations land with their features.
enum MeSettingsRow: String, Equatable, Sendable, CaseIterable {
    case settings, sync, about, feedback, licenses
}

@Reducer
struct MeFeature {
    @ObservableState
    struct State: Equatable {
        /// Session name shown until the profile fetch lands.
        var userName: String?
        var profile: Profile?
        var overview: MeOverview?
        var syncedAt: Date?
        var events: [AcademicEvent] = []
        var activeShortcut: MeShortcut?
        var isLogoutPromptPresented = false
        var path = StackState<Path.State>()
        @Shared(.appStorage("theme")) var theme: AppTheme = .system

        var displayName: String? { profile?.name ?? userName }
    }

    @Reducer
    enum Path {
        case settings(SettingsFeature)
        case countdown(FinalCountdownFeature)
        case licenses(LicensesFeature)
    }

    enum Action: Equatable {
        case task
        case overviewUpdated(CachedMeOverview)
        case profileLoaded(Profile)
        case eventsLoaded([AcademicEvent])
        case shortcutTapped(MeShortcut)
        case shortcutDismissed
        case settingsRowTapped(MeSettingsRow)
        case logoutTapped
        case logoutPromptDismissed
        case logoutConfirmed(keepData: Bool)
        case path(StackActionOf<Path>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case loggedOut(firstName: String?, keptData: Bool, dataSummary: LocalDataSummary?)
        }
    }

    @Dependency(\.meRepository) var meRepository
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.eventsRepository) var eventsRepository
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.date.now) var now

    private enum CancelID { case observation }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                if state.userName == nil {
                    state.userName = sessionStore.current()?.user.name
                }
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates; the one-shot fetches only run
                // on the first one.
                guard state.overview == nil else { return observeMirror() }
                return .merge(observeMirror(), loadProfile(), loadEvents())

            case let .overviewUpdated(cached):
                state.overview = cached.overview
                state.syncedAt = cached.syncedAt
                return .none

            case let .profileLoaded(profile):
                state.profile = profile
                return .none

            case let .eventsLoaded(events):
                state.events = events
                return .none

            case let .shortcutTapped(shortcut):
                switch shortcut {
                case .calendar:
                    state.activeShortcut = shortcut
                case .countdown:
                    state.path.append(.countdown(FinalCountdownFeature.State()))
                }
                return .none

            case .shortcutDismissed:
                state.activeShortcut = nil
                return .none

            case let .settingsRowTapped(row):
                switch row {
                case .settings:
                    state.path.append(.settings(SettingsFeature.State(
                        profile: state.profile,
                        userName: state.userName
                    )))
                case .licenses:
                    state.path.append(.licenses(LicensesFeature.State()))
                case .sync, .about, .feedback:
                    // The remaining destinations land with their features.
                    break
                }
                return .none

            case .logoutTapped:
                state.isLogoutPromptPresented = true
                return .none

            case .logoutPromptDismissed:
                state.isLogoutPromptPresented = false
                return .none

            case let .logoutConfirmed(keepData):
                state.isLogoutPromptPresented = false
                if !keepData {
                    state.$theme.withLock { $0 = .system }
                }
                let firstName = firstName(of: state.displayName)
                return .run { send in
                    let summary = keepData ? (try? await meRepository.localData()) : nil
                    if !keepData {
                        try? await meRepository.wipeLocalData()
                    }
                    try? sessionStore.clear()
                    await send(.delegate(.loggedOut(
                        firstName: firstName,
                        keptData: keepData,
                        dataSummary: summary
                    )))
                }

            case .path, .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await cached in meRepository.observe() {
                await send(.overviewUpdated(cached))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    private func loadProfile() -> Effect<Action> {
        .run { send in
            // Only feeds the course line — ignore failures.
            guard let profile = try? await profileRepository.current() else { return }
            await send(.profileLoaded(profile))
        }
    }

    private func loadEvents() -> Effect<Action> {
        .run { send in
            // The calendar teaser is a garnish — ignore failures.
            guard let events = try? await eventsRepository.upcoming(now) else { return }
            await send(.eventsLoaded(events))
        }
    }

    private func firstName(of name: String?) -> String? {
        name?.split(separator: " ").first.map(String.init)
    }
}

extension MeFeature.Path.State: Equatable {}
extension MeFeature.Path.Action: Equatable {}
