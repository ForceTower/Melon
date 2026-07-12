import ComposableArchitecture
import Foundation

/// The pinned shortcuts — most push their flow; the document ones
/// (comprovante / histórico) open the request sheet instead.
enum MeShortcut: String, Equatable, Sendable, Identifiable, CaseIterable {
    case enrollment, calendar, countdown, certificate, history, paradoxo, materials

    var id: String { rawValue }
}

/// The "Definições" rows. Settings and Licenses push their screens, About
/// opens its sheet, Feedback deep-links into Messages.
enum MeSettingsRow: String, Equatable, Sendable, CaseIterable {
    case settings, about, feedback, licenses
}

@Reducer
struct MeFeature {
    @ObservableState
    struct State: Equatable {
        /// Session name shown until the profile fetch lands.
        var userName: String?
        var profile: Profile?
        var overview: MeOverview?
        /// Presents the about sheet while non-nil.
        var aboutInfo: AppInfo?
        /// Drives the sheet's transient "copiado" feedback.
        var isAboutCopied = false
        var isLogoutPromptPresented = false
        var path = StackState<Path.State>()
        @Presents var document: MeDocumentFeature.State?
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
        @Shared(.appStorage(FeatureFlags.enrollmentEnabledKey)) var isEnrollmentEnabled = false
        @Shared(.appStorage(FeatureFlags.certificateEnabledKey)) var isCertificateEnabled = false
        @Shared(.appStorage(FeatureFlags.historyEnabledKey)) var isHistoryEnabled = false
        @Shared(.appStorage(FeatureFlags.paradoxoEnabledKey)) var isParadoxoEnabled = false
        @Shared(.appStorage(FeatureFlags.materialsEnabledKey)) var isMaterialsEnabled = false

        var displayName: String? { profile?.name ?? userName }

        /// The tiles the grid shows — the gated ones only while their flag is
        /// on. Debug builds skip the gating so every flow stays reachable.
        var shortcuts: [MeShortcut] {
            #if DEBUG
            MeShortcut.allCases
            #else
            MeShortcut.allCases.filter { shortcut in
                switch shortcut {
                case .enrollment: isEnrollmentEnabled
                case .certificate: isCertificateEnabled
                case .history: isHistoryEnabled
                case .paradoxo: isParadoxoEnabled
                case .materials: isMaterialsEnabled
                case .calendar, .countdown: true
                }
            }
            #endif
        }
    }

    @Reducer
    enum Path {
        case settings(SettingsFeature)
        case passkeys(PasskeysFeature)
        case calendar(CalendarFeature)
        case countdown(FinalCountdownFeature)
        case licenses(LicensesFeature)
        case enrollment(EnrollmentFeature)
        case enrollmentOffers(EnrollmentOffersFeature)
        case enrollmentDiscipline(EnrollmentDisciplineFeature)
        case enrollmentTimetable(EnrollmentTimetableFeature)
        case enrollmentReview(EnrollmentReviewFeature)
        case enrollmentSuccess(EnrollmentSuccessFeature)
        case paradoxo(ParadoxoFeature)
        case paradoxoExplore(ParadoxoExploreFeature)
        case paradoxoDiscipline(ParadoxoDisciplineFeature)
        case paradoxoTeacher(ParadoxoTeacherFeature)
        case materials(MaterialsFeature)
        case materialsList(MaterialsListFeature)
        case materialsDetail(MaterialsDetailFeature)
        case materialsSaved(MaterialsSavedFeature)
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
        case logoutConfirmed
        case document(PresentationAction<MeDocumentFeature.Action>)
        case path(StackActionOf<Path>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case loggedOut(firstName: String?)
        }
    }

    @Dependency(\.meRepository) var meRepository
    @Dependency(\.localDocuments) var localDocuments
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.appInfo) var appInfo
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.openURL) var openURL
    @Dependency(\.locale) var locale
    @Dependency(\.continuousClock) var clock

    private let log = Log.scoped("MeFeature")

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
                case .certificate:
                    state.document = documentState(.enrollmentCertificate, from: state)
                case .history:
                    state.document = documentState(.academicHistory, from: state)
                case .paradoxo:
                    state.path.append(.paradoxo(ParadoxoFeature.State()))
                case .materials:
                    state.path.append(.materials(MaterialsFeature.State()))
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
                log.info("begin logout")
                state.isLogoutPromptPresented = true
                return .none

            case .logoutPromptDismissed:
                state.isLogoutPromptPresented = false
                return .none

            case .logoutConfirmed:
                log.info("confirm logout")
                state.isLogoutPromptPresented = false
                state.$theme.withLock { $0 = .system }
                let firstName = firstName(of: state.displayName)
                return .run { [log] send in
                    try? await meRepository.wipeLocalData()
                    do {
                        try sessionStore.clear()
                        log.info("session logout ok")
                    } catch {
                        log.warn("session logout failed; continuing", error: error)
                    }
                    await send(.delegate(.loggedOut(firstName: firstName)))
                }

            case let .path(.element(id: _, action: pathAction)):
                return .merge(
                    routeSettings(pathAction, state: &state),
                    routeEnrollment(pathAction, state: &state),
                    routeParadoxo(pathAction, state: &state),
                    routeMaterials(pathAction, state: &state)
                )

            case .document, .path, .delegate:
                return .none
            }
        }
        .ifLet(\.$document, action: \.document) {
            MeDocumentFeature()
        }
        .forEach(\.path, action: \.path)
    }

    private func documentState(_ document: AcademicDocument, from state: State) -> MeDocumentFeature.State {
        MeDocumentFeature.State(
            document: document,
            studentName: state.displayName,
            course: state.profile?.course,
            score: state.overview?.coefficient?.value,
            stored: localDocuments.load(document)
        )
    }

    /// Settings pushes the passkeys manager onto the host stack.
    private func routeSettings(_ action: Path.Action, state: inout State) -> Effect<Action> {
        if case .settings(.delegate(.openPasskeys)) = action {
            state.path.append(.passkeys(PasskeysFeature.State(accountName: state.displayName)))
        }
        return .none
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

    /// Paradoxo screens push each other freely (discipline ↔ teacher), so
    /// their open delegates all land here on the host stack.
    private func routeParadoxo(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case let .paradoxo(.delegate(.openDiscipline(id, name))),
             let .paradoxoTeacher(.delegate(.openDiscipline(id, name))):
            state.path.append(.paradoxoDiscipline(ParadoxoDisciplineFeature.State(disciplineId: id, name: name)))

        case let .paradoxo(.delegate(.openTeacher(id, name))),
             let .paradoxoDiscipline(.delegate(.openTeacher(id, name))):
            state.path.append(.paradoxoTeacher(ParadoxoTeacherFeature.State(teacherId: id, name: name)))

        case let .paradoxo(.delegate(.openExplore(ranking))):
            state.path.append(.paradoxoExplore(ParadoxoExploreFeature.State(ranking: ranking)))

        case let .paradoxoExplore(.delegate(.open(ref, name))):
            switch ref.kind {
            case .discipline:
                state.path.append(.paradoxoDiscipline(ParadoxoDisciplineFeature.State(disciplineId: ref.id, name: name)))
            case .teacher:
                state.path.append(.paradoxoTeacher(ParadoxoTeacherFeature.State(teacherId: ref.id, name: name)))
            }

        default:
            break
        }
        return .none
    }

    /// Materiais screens all push onto the host stack: hub → discipline
    /// shelf → material, plus the saved shelf.
    private func routeMaterials(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case let .materials(.delegate(.openDiscipline(discipline))):
            state.path.append(.materialsList(MaterialsListFeature.State(discipline: discipline)))

        case .materials(.delegate(.openSaved)):
            state.path.append(.materialsSaved(MaterialsSavedFeature.State()))

        case let .materialsList(.delegate(.openMaterial(material, _))),
             let .materialsSaved(.delegate(.openMaterial(material))):
            state.path.append(.materialsDetail(MaterialsDetailFeature.State(material: material)))

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
        .run { [log] send in
            // Only feeds the course line — ignore failures.
            do {
                let profile = try await profileRepository.current()
                await send(.profileLoaded(profile))
            } catch {
                log.debug("me profile load failed")
            }
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
        \(String.localized(.meFeedbackSmsFooter))
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
