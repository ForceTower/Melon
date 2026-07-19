import ComposableArchitecture
import Foundation

@Reducer
struct HomeFeature {
    @ObservableState
    struct State: Equatable {
        var overview: HomeOverview?
        var userName: String?
        var isLoading = false
        var errorMessage: String?
        var lastRefreshed: Date?
        var campusEvent: CampusEvent?
        var path = StackState<Path.State>()

        @ObservationStateIgnored
        @Shared(.appStorage(FeatureFlags.campusEventEnabledKey)) var isCampusEventEnabled = false
        @Shared(.appStorage(FeatureFlags.retrospectiveEnabledKey)) var isRetrospectiveEnabled = false
        @Shared(.appStorage(RetrospectiveFeature.seenSemesterKey)) var retrospectiveSeenSemester = ""
        @Shared(.appStorage("retrospective_banner_dismissed_semester")) var retrospectiveDismissedSemester = ""
        /// The semester whose window the mirror says is open — auto-detected,
        /// nil outside the window (or while grades are still landing).
        var retrospectiveSemester: String?

        var isRetrospectiveSeen: Bool {
            retrospectiveSemester.map { $0 == retrospectiveSeenSemester } ?? false
        }

        /// The celebratory banner can be dismissed until seen; the slim
        /// "toque pra rever" row stays for the whole window.
        var showsRetrospectiveBanner: Bool {
            guard let semester = retrospectiveSemester else { return false }
            return !(!isRetrospectiveSeen && retrospectiveDismissedSemester == semester)
        }
    }

    @Reducer
    enum Path {
        case detail(DisciplineDetailFeature)
        case campusEvent(CampusEventFeature)
        case retrospective(RetrospectiveFeature)
        case campusEventActivity(CampusEventActivityFeature)
        case campusEventSpeakers(CampusEventSpeakersFeature)
        case campusEventWorkshops(CampusEventWorkshopsFeature)
        case campusEventVenues(CampusEventVenuesFeature)
        case campusEventOrganizations(CampusEventOrganizationsFeature)
        case materialsList(MaterialsListFeature)
        case materialsDetail(MaterialsDetailFeature)
    }

    enum Action: Equatable {
        case task
        case refreshPulled
        case mirrorUpdated(CachedHomeOverview)
        case refreshFailed(String)
        case profileLoaded(Profile)
        case campusEventLoaded(CampusEvent?)
        case retrospectiveChecked(String?)
        case retrospectiveCardTapped
        case retrospectiveBannerDismissed
        case campusEventCardTapped
        case disciplineTapped(id: String, name: String, offerId: String?, isNowClass: Bool)
        case seeScheduleTapped
        case seeAllClassesTapped
        case messagesWidgetTapped
        case avatarTapped
        case path(StackActionOf<Path>)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openSchedule
            case openClasses
            case openMessages
            case openMe
            case unreadMessagesChanged(Int)
        }
    }

    @Dependency(\.homeRepository) var homeRepository
    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.campusEventRepository) var campusEventRepository
    @Dependency(\.database) var database
    @Dependency(\.date.now) var now
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("HomeFeature")

    private enum CancelID { case observation, refresh, heroRollover, campusEventObservation, campusEventRefresh }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                // The observation replays the mirror on subscription, so
                // every appearance rehydrates; the refresh keeps retrying on
                // each appearance until the mirror has data, so a first load
                // cancelled mid-flight (tab switch) can't wedge the spinner.
                guard state.overview == nil else {
                    return .merge(
                        observeMirror(), observeCampusEvent(state), refreshCampusEvent(state),
                        checkRetrospective(state)
                    )
                }
                state.isLoading = true
                state.errorMessage = nil
                return .merge(
                    observeMirror(), refresh(), loadProfile(),
                    observeCampusEvent(state), refreshCampusEvent(state),
                    checkRetrospective(state)
                )

            case .refreshPulled:
                guard !state.isLoading else { return .none }
                if state.overview == nil {
                    state.isLoading = true
                    state.errorMessage = nil
                }
                return .merge(refresh(), refreshCampusEvent(state))

            case let .mirrorUpdated(cached):
                state.isLoading = false
                state.errorMessage = nil
                state.overview = cached.overview
                // The mirror's own sync stamp, so "Atualizado há X min"
                // survives relaunches.
                state.lastRefreshed = cached.syncedAt
                return .merge(
                    .send(.delegate(.unreadMessagesChanged(cached.overview.messages?.unreadCount ?? 0))),
                    scheduleHeroRollover(cached.overview)
                )

            case let .refreshFailed(message):
                log.warn("home refresh failed err=\(message)")
                state.isLoading = false
                // A stale overview beats an error screen; only surface the
                // failure when there is nothing to show.
                if state.overview == nil {
                    state.errorMessage = message
                }
                return .none

            case let .profileLoaded(profile):
                state.userName = profile.name
                return .none

            case let .campusEventLoaded(event):
                state.campusEvent = event
                return .none

            case let .retrospectiveChecked(semester):
                state.retrospectiveSemester = semester
                return .none

            case .retrospectiveBannerDismissed:
                guard let code = state.retrospectiveSemester else { return .none }
                state.$retrospectiveDismissedSemester.withLock { $0 = code }
                return .none

            case .retrospectiveCardTapped:
                guard let code = state.retrospectiveSemester else { return .none }
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "retrospective")
                log.info("open retrospective semester=\(code)")
                state.path.append(.retrospective(RetrospectiveFeature.State(semesterCode: code)))
                return .none

            case .campusEventCardTapped:
                guard let event = state.campusEvent else { return .none }
                analytics.selectContent(contentType: ContentTypes.hub, itemId: "campus_event")
                log.info("open campus event id=\(event.id)")
                state.path.append(.campusEvent(CampusEventFeature.State(event: event)))
                return .none

            case let .disciplineTapped(id, name, offerId, isNowClass):
                if isNowClass {
                    analytics.selectContent(
                        contentType: ContentTypes.tile,
                        itemId: "now_class",
                        properties: offerId.map { ["offer_id": $0] } ?? [:]
                    )
                } else if let offerId {
                    // Android skips the event when the offer is unknown —
                    // mirror that rather than mixing discipline ids into the
                    // offer-id namespace.
                    analytics.selectContent(contentType: ContentTypes.discipline, itemId: offerId)
                }
                guard let overview = state.overview, let semesterId = overview.semesterId else { return .none }
                let colorIndex = overview.disciplines.first { $0.id == id }?.colorIndex ?? 0
                state.path.append(.detail(DisciplineDetailFeature.State(
                    semesterId: semesterId,
                    disciplineId: id,
                    name: name,
                    colorIndex: colorIndex
                )))
                return .none

            case .seeScheduleTapped:
                analytics.selectContent(contentType: ContentTypes.tile, itemId: "schedule")
                return .send(.delegate(.openSchedule))

            case .seeAllClassesTapped:
                return .send(.delegate(.openClasses))

            case .messagesWidgetTapped:
                analytics.selectContent(contentType: ContentTypes.tile, itemId: "unread_messages")
                return .send(.delegate(.openMessages))

            case .avatarTapped:
                return .send(.delegate(.openMe))

            case let .path(.element(id: _, action: pathAction)):
                return .merge(
                    routeCampusEvent(pathAction, state: &state),
                    routeMaterials(pathAction, state: &state)
                )

            case .path:
                return .none

            case .delegate:
                return .none
            }
        }
        .forEach(\.path, action: \.path)
    }

    /// Campus-event screens fan out from the hub (and the activity detail
    /// links back to the venues), so their pushes route here on the host
    /// stack.
    private func routeCampusEvent(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case let .campusEvent(.delegate(delegate)):
            switch delegate {
            case let .openActivity(activity, event):
                state.path.append(.campusEventActivity(CampusEventActivityFeature.State(activity: activity, event: event)))
            case let .openSpeakers(event):
                state.path.append(.campusEventSpeakers(CampusEventSpeakersFeature.State(speakers: event.speakers)))
            case let .openWorkshops(event):
                state.path.append(.campusEventWorkshops(CampusEventWorkshopsFeature.State(workshops: event.workshops)))
            case let .openVenues(event):
                state.path.append(.campusEventVenues(CampusEventVenuesFeature.State(event: event)))
            case let .openOrganizations(event):
                state.path.append(.campusEventOrganizations(CampusEventOrganizationsFeature.State(organizations: event.organizations)))
            }
        case let .campusEventActivity(.delegate(.openVenues(event))):
            state.path.append(.campusEventVenues(CampusEventVenuesFeature.State(event: event)))
        default:
            break
        }
        return .none
    }

    /// Discipline detail's Materiais entry pushes the shelf (and its
    /// material screens) on this stack.
    private func routeMaterials(_ action: Path.Action, state: inout State) -> Effect<Action> {
        switch action {
        case let .detail(.delegate(.openMaterials(discipline))):
            state.path.append(.materialsList(MaterialsListFeature.State(discipline: discipline)))
        case let .materialsList(.delegate(.openMaterial(material, _))):
            state.path.append(.materialsDetail(MaterialsDetailFeature.State(material: material)))
        default:
            break
        }
        return .none
    }

    /// The reactive backbone: every mirror write (sync refresh, message
    /// read/star, semester download — from any tab) lands here as a fresh
    /// overview.
    private func observeMirror() -> Effect<Action> {
        .run { send in
            for await cached in homeRepository.observe() {
                await send(.mirrorUpdated(cached))
            }
        }
        .cancellable(id: CancelID.observation, cancelInFlight: true)
    }

    /// Rewrites the mirror from upstream; the fresh snapshot arrives through
    /// the observation.
    private func refresh() -> Effect<Action> {
        .run { [log] send in
            do {
                try await homeRepository.refresh(now: now)
            } catch {
                // Offline with a mirror: recompute from local data so the
                // time-derived pieces (hero, "Seu dia") still advance.
                if let cached = try? await homeRepository.cached(now: now) {
                    log.warn("refresh failed, using cached overview", error: error)
                    await send(.mirrorUpdated(cached))
                } else {
                    await send(.refreshFailed(error.localizedDescription))
                }
            }
        }
        .cancellable(id: CancelID.refresh, cancelInFlight: true)
    }

    /// Once the hero class starts, its countdown is spent — reload so the
    /// hero advances to the next occurrence.
    private func scheduleHeroRollover(_ overview: HomeOverview) -> Effect<Action> {
        guard let startsAt = overview.hero?.startsAt else { return .none }
        let delay = startsAt.timeIntervalSince(now) + 1
        guard delay > 0 else { return .none }
        return .run { send in
            try await clock.sleep(for: .seconds(delay))
            await send(.refreshPulled)
        }
        .cancellable(id: CancelID.heroRollover, cancelInFlight: true)
    }

    private func loadProfile() -> Effect<Action> {
        .run { send in
            // Only feeds the avatar initial — ignore failures.
            guard let profile = try? await profileRepository.current() else { return }
            await send(.profileLoaded(profile))
        }
    }

    /// Streams the mirrored featured event behind the remote flag, so the
    /// card reacts to refreshes from any screen and to logout wipes.
    private func observeCampusEvent(_ state: State) -> Effect<Action> {
        guard state.isCampusEventEnabled else {
            return state.campusEvent == nil ? .none : .send(.campusEventLoaded(nil))
        }
        return .run { send in
            for await event in campusEventRepository.observe() {
                await send(.campusEventLoaded(event))
            }
        }
        .cancellable(id: CancelID.campusEventObservation, cancelInFlight: true)
    }

    /// Rewrites the event mirror from upstream; the result (including
    /// un-featuring) lands through the observation. Failures keep the stale
    /// payload for offline access and never surface.
    private func refreshCampusEvent(_ state: State) -> Effect<Action> {
        guard state.isCampusEventEnabled else { return .none }
        return .run { _ in
            try? await campusEventRepository.refresh()
        }
        .cancellable(id: CancelID.campusEventRefresh, cancelInFlight: true)
    }

    /// Asks the mirror which semester's window is open (if any) — the
    /// banner fronts only a story the mirror can actually tell.
    private func checkRetrospective(_ state: State) -> Effect<Action> {
        guard state.isRetrospectiveEnabled else { return .none }
        return .run { [now] send in
            let mirror = MirrorStore(writer: database)
            await send(.retrospectiveChecked(try? await mirror.retrospectiveWindowCode(now: now)))
        }
    }
}

extension HomeFeature.Path.State: Equatable {}
extension HomeFeature.Path.Action: Equatable {}
