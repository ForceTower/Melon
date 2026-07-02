import ComposableArchitecture
import Foundation

/// The pinned shortcuts. Matrícula and Countdown push their flows; the
/// calendar opens its teaser sheet until the feature lands.
enum MeShortcut: String, Equatable, Sendable, Identifiable, CaseIterable {
    case enrollment, calendar, countdown

    var id: String { rawValue }
}

/// The "Definições" rows. Settings and Licenses push their screens, About
/// opens its sheet; the remaining destinations land with their features.
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
        /// Presents the about sheet while non-nil.
        var aboutInfo: AppInfo?
        /// Drives the sheet's transient "copiado" feedback.
        var isAboutCopied = false
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
        case enrollment(EnrollmentFeature)
        case enrollmentOffers(EnrollmentOffersFeature)
        case enrollmentDiscipline(EnrollmentDisciplineFeature)
        case enrollmentTimetable(EnrollmentTimetableFeature)
        case enrollmentReview(EnrollmentReviewFeature)
        case enrollmentSuccess(EnrollmentSuccessFeature)
    }

    enum Action: Equatable {
        case task
        case overviewUpdated(CachedMeOverview)
        case profileLoaded(Profile)
        case eventsLoaded([AcademicEvent])
        case shortcutTapped(MeShortcut)
        case shortcutDismissed
        case settingsRowTapped(MeSettingsRow)
        case aboutInfoResolved(AppInfo)
        case aboutCopyTapped
        case aboutCopyFeedbackExpired
        case aboutDismissed
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
    @Dependency(\.appInfo) var appInfo
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.continuousClock) var clock
    @Dependency(\.date.now) var now

    private enum CancelID { case observation, aboutResolve, aboutCopyFeedback }

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
                case .enrollment:
                    state.path.append(.enrollment(EnrollmentFeature.State(profile: state.profile)))
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
                case .about:
                    // The sheet opens with the synchronous snapshot; StoreKit
                    // refines the channel label right behind it.
                    state.aboutInfo = appInfo.current()
                    return .run { send in
                        await send(.aboutInfoResolved(appInfo.resolved()))
                    }
                    .cancellable(id: CancelID.aboutResolve, cancelInFlight: true)
                case .sync, .feedback:
                    // The remaining destinations land with their features.
                    break
                }
                return .none

            case let .aboutInfoResolved(info):
                state.aboutInfo = info
                return .none

            case .aboutCopyTapped:
                guard let info = state.aboutInfo else { return .none }
                state.isAboutCopied = true
                return .run { send in
                    await pasteboard.copy(info.debugText)
                    try await clock.sleep(for: .milliseconds(1800))
                    await send(.aboutCopyFeedbackExpired)
                }
                .cancellable(id: CancelID.aboutCopyFeedback, cancelInFlight: true)

            case .aboutCopyFeedbackExpired:
                state.isAboutCopied = false
                return .none

            case .aboutDismissed:
                state.aboutInfo = nil
                state.isAboutCopied = false
                return .merge(
                    .cancel(id: CancelID.aboutResolve),
                    .cancel(id: CancelID.aboutCopyFeedback)
                )

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

            case let .path(.element(id: _, action: pathAction)):
                return routeEnrollment(pathAction, state: &state)

            case .path, .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// The matrícula steps are sibling path elements sharing one in-memory
    /// session; their routing delegates all land here.
    private func routeEnrollment(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case .enrollment(.delegate(.openOffers)):
            state.path.append(.enrollmentOffers(EnrollmentOffersFeature.State()))

        case let .enrollment(.delegate(.openDiscipline(id))),
             let .enrollmentOffers(.delegate(.openDiscipline(id))):
            state.path.append(.enrollmentDiscipline(EnrollmentDisciplineFeature.State(disciplineId: id)))

        case .enrollment(.delegate(.openReview)),
             .enrollmentOffers(.delegate(.openReview)),
             .enrollmentTimetable(.delegate(.openReview)):
            state.path.append(.enrollmentReview(EnrollmentReviewFeature.State()))

        case .enrollmentOffers(.delegate(.openTimetable)),
             .enrollmentDiscipline(.delegate(.openTimetable)),
             .enrollmentReview(.delegate(.openTimetable)):
            state.path.append(.enrollmentTimetable(EnrollmentTimetableFeature.State()))

        case .enrollmentReview(.delegate(.submitted)):
            // Collapse the flow to its entry screen underneath the
            // confirmation, so back navigation can't reenter the review.
            // Bounded to enrollment steps so it can never strip screens
            // beneath the flow.
            while let last = state.path.last, last.isEnrollmentStep {
                state.path.removeLast()
            }
            state.path.append(.enrollmentSuccess(EnrollmentSuccessFeature.State()))

        case .enrollmentSuccess(.delegate(.finished)):
            // Guarded so a duplicate delivery can't pop the entry screen too.
            if case .enrollmentSuccess = state.path.last {
                state.path.removeLast()
            }

        default:
            break
        }
        return .none
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

extension MeFeature.Path.State {
    /// An intermediate step of the matrícula flow — not the entry screen,
    /// not the confirmation.
    fileprivate var isEnrollmentStep: Bool {
        switch self {
        case .enrollmentOffers, .enrollmentDiscipline, .enrollmentTimetable, .enrollmentReview:
            true
        default:
            false
        }
    }
}
