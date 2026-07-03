import ComposableArchitecture

@Reducer
struct AppFeature {
    @ObservableState
    struct State: Equatable {
        var tab: Tab = .home
        var home = HomeFeature.State()
        var schedule = ScheduleFeature.State()
        var disciplines = DisciplinesFeature.State()
        var messages = MessagesFeature.State()
        var me = MeFeature.State()
        var unreadMessages = 0
        /// Whether the scene truly reached .background — transient .inactive
        /// blips (control center, app-switcher peek) never set it.
        var wasBackgrounded = false
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
    }

    enum Tab: String, CaseIterable, Hashable, Sendable {
        case home, schedule, classes, messages, me
    }

    enum Action: Equatable {
        case task
        case tabChanged(Tab)
        case sceneBackgrounded
        case sceneActivated
        case pushDataReceived(PushDataEvent)
        case intentRoute(Tab)
        case home(HomeFeature.Action)
        case schedule(ScheduleFeature.Action)
        case disciplines(DisciplinesFeature.Action)
        case messages(MessagesFeature.Action)
        case me(MeFeature.Action)
    }

    @Dependency(\.push) var push
    @Dependency(\.syncRepository) var syncRepository
    @Dependency(\.homeRepository) var homeRepository
    @Dependency(\.intentRouter) var intentRouter
    @Dependency(\.continuousClock) var clock
    @Dependency(\.date) var date
    private let log = Log.scoped("AppFeature")

    private enum CancelID { case ping, backfill, pushEvents, pushRefresh, intentRoutes }

    var body: some ReducerOf<Self> {
        Scope(state: \.home, action: \.home) { HomeFeature() }
        Scope(state: \.schedule, action: \.schedule) { ScheduleFeature() }
        Scope(state: \.disciplines, action: \.disciplines) { DisciplinesFeature() }
        Scope(state: \.messages, action: \.messages) { MessagesFeature() }
        Scope(state: \.me, action: \.me) { MeFeature() }

        Reduce { state, action in
            switch action {
            case .task:
                // Safety net for accounts that never saw the intro's prompt
                // (skip path, reinstalls) — a no-op once iOS has asked.
                return .merge(
                    .run { _ in await push.requestAuthorization() },
                    pingEffect(),
                    .run { _ in try? await syncRepository.backfillMirror() }
                        .cancellable(id: CancelID.backfill, cancelInFlight: true),
                    .run { send in
                        for await event in push.dataEvents() {
                            await send(.pushDataReceived(event))
                        }
                    }
                    .cancellable(id: CancelID.pushEvents, cancelInFlight: true),
                    .run { send in
                        for await route in intentRouter.routes() {
                            switch route {
                            case let .tab(tab): await send(.intentRoute(tab))
                            }
                        }
                    }
                    .cancellable(id: CancelID.intentRoutes, cancelInFlight: true)
                )

            case let .tabChanged(tab):
                state.tab = tab
                return .none

            case let .intentRoute(tab):
                log.info("intent route consumed tab=\(tab.rawValue)")
                state.tab = tab
                return .none

            case .sceneBackgrounded:
                state.wasBackgrounded = true
                return .none

            case .sceneActivated:
                // Resume arrives as .background → .inactive → .active, so the
                // latch — not the previous phase — decides whether this is a
                // real return from background.
                guard state.wasBackgrounded else { return .none }
                state.wasBackgrounded = false
                log.debug("scene returned from background -> refreshing all tabs")
                // Overviews bake "today" in at compute time and the mirror
                // observations only re-emit on writes, so a suspension that
                // crosses midnight leaves every tab rendering the old day.
                // Re-sending .task restarts each observation (cancelInFlight)
                // and its initial replay recomputes with the current date.
                return .merge(
                    pingEffect(),
                    .concatenate(
                        .send(.home(.task)),
                        .send(.schedule(.task)),
                        .send(.disciplines(.task)),
                        .send(.messages(.task)),
                        .send(.me(.task))
                    )
                )

            case let .pushDataReceived(event):
                log.info("data push kind=\(event.kind) -> scheduling mirror refresh")
                // The backend sends one push per upstream change, so a sync
                // that lands several grades arrives as a burst — debounce it
                // into a single refresh. The fresh data reaches every tab
                // through its mirror observation.
                return .run { _ in
                    try await clock.sleep(for: .seconds(2))
                    try? await homeRepository.refresh(now: date.now)
                }
                .cancellable(id: CancelID.pushRefresh, cancelInFlight: true)

            case let .home(.delegate(delegate)):
                switch delegate {
                case .openSchedule:
                    state.tab = .schedule
                case .openClasses:
                    state.tab = .classes
                case .openMessages:
                    state.tab = .messages
                case .openMe:
                    state.tab = .me
                case let .unreadMessagesChanged(count):
                    state.unreadMessages = count
                }
                return .none

            case let .messages(.delegate(.unreadChanged(count))):
                state.unreadMessages = count
                return .none

            case .home, .schedule, .disciplines, .messages, .me:
                return .none
            }
        }
    }

    private func pingEffect() -> Effect<Action> {
        .run { _ in try? await syncRepository.ping() }
            .cancellable(id: CancelID.ping, cancelInFlight: true)
    }
}
