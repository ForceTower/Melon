import ComposableArchitecture
import Foundation

/// The pinned shortcuts — each pushes its flow.
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
        case calendar(CalendarFeature)
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
        case shortcutTapped(MeShortcut)
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
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.appInfo) var appInfo
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.openURL) var openURL
    @Dependency(\.locale) var locale
    @Dependency(\.continuousClock) var clock

    private enum CancelID { case observation, aboutResolve, aboutCopyFeedback }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                if state.userName == nil {
                    state.userName = sessionStore.current()?.user.name
                }
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates; the one-shot fetch only runs
                // on the first one. A pushed calendar sits outside the tab's
                // own task, so the resume wake-up is forwarded — it re-anchors
                // "hoje" and refreshes the feed.
                let effects = [observeMirror()]
                    + (state.overview == nil ? [loadProfile()] : [])
                    + wakeCalendar(in: state)
                return .merge(effects)

            case let .overviewUpdated(cached):
                state.overview = cached.overview
                state.syncedAt = cached.syncedAt
                return .none

            case let .profileLoaded(profile):
                state.profile = profile
                return .none

            case let .shortcutTapped(shortcut):
                switch shortcut {
                case .enrollment:
                    state.path.append(.enrollment(EnrollmentFeature.State(profile: state.profile)))
                case .calendar:
                    state.path.append(.calendar(CalendarFeature.State()))
                case .countdown:
                    state.path.append(.countdown(FinalCountdownFeature.State()))
                }
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
                case .feedback:
                    let url = feedbackSMSURL(for: appInfo.current())
                    return .run { _ in
                        guard let url else { return }
                        await openURL(url)
                    }
                case .sync:
                    // Sync's destination lands with its feature.
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

    private func wakeCalendar(in state: State) -> [Effect<Action>] {
        state.path.ids.compactMap { id in
            guard case .calendar = state.path[id: id] else { return nil }
            return .send(.path(.element(id: id, action: .calendar(.task))))
        }
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

    private func firstName(of name: String?) -> String? {
        name?.split(separator: " ").first.map(String.init)
    }

    /// `sms:` deep link that opens Messages addressed to the maintainer with
    /// the build/device header prefilled — same recipe as the v1 app.
    private func feedbackSMSURL(for info: AppInfo) -> URL? {
        let body = """
        UNES \(info.version)(\(info.build)) - \(info.deviceModel) \(locale.identifier)
        id: \(info.machineId)
        Os dados acima me ajudam a encontrar o erro, não apaga se puder :)
        """
        guard let encoded = body.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) else {
            return nil
        }
        return URL(string: "sms:joaopaulo761@gmail.com?&body=\(encoded)")
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
