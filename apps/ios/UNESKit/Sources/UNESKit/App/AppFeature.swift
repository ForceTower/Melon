import Combine
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
        /// Secret icons discovered outside Configurações (Folio Runner, the
        /// watch) being celebrated with the app-level unlock sheet.
        var iconCelebration: [AppIcon]?
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
        @Shared(.unlockedSecretIcons) var unlockedSecretIcons
        @Shared(.announcedSecretIcons) var announcedSecretIcons
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
        case intentOpenDiscipline(semesterId: String, disciplineId: String)
        case intentOpenMessage(id: String)
        case intentOpenMaterial(id: String)
        case intentOpenMaterialsDiscipline(disciplineId: String)
        case secretIconsChanged
        case iconCelebrationDismissed
        case iconCelebrationUsed(AppIcon)
        case home(HomeFeature.Action)
        case schedule(ScheduleFeature.Action)
        case disciplines(DisciplinesFeature.Action)
        case messages(MessagesFeature.Action)
        case me(MeFeature.Action)
    }

    @Dependency(\.push) var push
    @Dependency(\.analytics) var analytics
    @Dependency(\.syncRepository) var syncRepository
    @Dependency(\.homeRepository) var homeRepository
    @Dependency(\.disciplinesRepository) var disciplinesRepository
    @Dependency(\.messagesRepository) var messagesRepository
    @Dependency(\.materialsRepository) var materialsRepository
    @Dependency(\.intentRouter) var intentRouter
    @Dependency(\.appIconClient) var appIconClient
    @Dependency(\.settingsRepository) var settingsRepository
    @Dependency(\.continuousClock) var clock
    @Dependency(\.date) var date
    private let log = Log.scoped("AppFeature")

    private enum CancelID { case ping, backfill, pushEvents, pushRefresh, intentRoutes, secretIcons, secretIconSync }

    var body: some ReducerOf<Self> {
        Scope(state: \.home, action: \.home) { HomeFeature() }
        Scope(state: \.schedule, action: \.schedule) { ScheduleFeature() }
        Scope(state: \.disciplines, action: \.disciplines) { DisciplinesFeature() }
        Scope(state: \.messages, action: \.messages) { MessagesFeature() }
        Scope(state: \.me, action: \.me) { MeFeature() }

        Reduce { state, action in
            switch action {
            case .task:
                reportTabScreen(state.tab)
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
                            case let .tab(tab):
                                await send(.intentRoute(tab))
                            case let .discipline(semesterId, disciplineId):
                                await send(.intentOpenDiscipline(semesterId: semesterId, disciplineId: disciplineId))
                            case let .message(id):
                                await send(.intentOpenMessage(id: id))
                            case let .material(id):
                                await send(.intentOpenMaterial(id: id))
                            case let .materialsDiscipline(disciplineId):
                                await send(.intentOpenMaterialsDiscipline(disciplineId: disciplineId))
                            }
                        }
                    }
                    .cancellable(id: CancelID.intentRoutes, cancelInFlight: true),
                    .send(.secretIconsChanged),
                    .run { [unlocked = state.$unlockedSecretIcons] send in
                        for await _ in unlocked.publisher.values {
                            await send(.secretIconsChanged)
                        }
                    }
                    .cancellable(id: CancelID.secretIcons, cancelInFlight: true)
                )

            case let .tabChanged(tab):
                reportTabScreen(tab, previous: state.tab)
                state.tab = tab
                return .none

            case let .intentRoute(tab):
                log.info("intent route consumed tab=\(tab.rawValue)")
                reportTabScreen(tab, previous: state.tab)
                state.tab = tab
                return .none

            case let .intentOpenDiscipline(semesterId, disciplineId):
                // Consume-time resolution: the mirror is readable before any
                // tab has loaded, so a buffered cold-launch route lands on
                // fresh data; a stale index row falls back to the tab alone.
                return .run { [log, now = date.now] send in
                    let overview = try? await disciplinesRepository.cached(now)
                    let groups = [overview?.current].compactMap { $0 } + (overview?.past ?? [])
                    guard let group = groups.first(where: { $0.id == semesterId }),
                          let discipline = group.disciplines.first(where: { $0.id == disciplineId })
                    else {
                        log.info("entity route fallback kind=discipline")
                        await send(.intentRoute(.classes))
                        return
                    }
                    await send(.intentRoute(.classes))
                    await send(.disciplines(.disciplineTapped(semesterId: semesterId, discipline: discipline)))
                }

            case let .intentOpenMessage(id):
                return .run { [log, now = date.now] send in
                    var overview = try? await messagesRepository.cached(now)
                    if overview?.messages.first(where: { $0.id == id }) == nil {
                        // A notification tap can outrun the mirror — the
                        // message it announced may not be local yet. One
                        // refresh before giving up to the inbox.
                        try? await messagesRepository.refresh(now)
                        overview = try? await messagesRepository.cached(now)
                    }
                    guard let message = overview?.messages.first(where: { $0.id == id }) else {
                        log.info("entity route fallback kind=message")
                        await send(.intentRoute(.messages))
                        return
                    }
                    await send(.intentRoute(.messages))
                    await send(.messages(.messageTapped(message)))
                }

            case let .intentOpenMaterial(id):
                // Materiais is online-only, so the deeplink resolves with a
                // fetch; the hub is the floor when it can't (offline,
                // deleted, audience mismatch) — never an error dialog.
                return .run { [log] send in
                    guard let material = try? await materialsRepository.material(id) else {
                        log.info("entity route fallback kind=material")
                        await send(.intentRoute(.me))
                        await send(.me(.deeplinkOpened(.materialsHub)))
                        return
                    }
                    await send(.intentRoute(.me))
                    await send(.me(.deeplinkOpened(.material(material))))
                }

            case let .intentOpenMaterialsDiscipline(disciplineId):
                return .run { [log] send in
                    let overview = try? await materialsRepository.overview()
                    guard let discipline = overview?.disciplines.first(where: { $0.id == disciplineId }) else {
                        log.info("entity route fallback kind=materialsDiscipline")
                        await send(.intentRoute(.me))
                        await send(.me(.deeplinkOpened(.materialsHub)))
                        return
                    }
                    await send(.intentRoute(.me))
                    await send(.me(.deeplinkOpened(.materialsDiscipline(discipline))))
                }

            case .secretIconsChanged:
                // Announced is only marked on dismissal, so a celebration the
                // app never got to show (killed mid-game, sheet blocked by a
                // cover) fires again on the next pass.
                let fresh = AppIcon.allCases.filter {
                    state.unlockedSecretIcons.contains($0) && !state.announcedSecretIcons.contains($0)
                }
                guard !fresh.isEmpty, state.iconCelebration == nil else { return .none }
                log.info("celebrating secret icons \(fresh.map(\.rawValue))")
                state.iconCelebration = fresh
                // Record the discovery on the account; version-tap unlocks
                // arrive pre-announced and push from SettingsFeature instead.
                return .run { [icons = state.unlockedSecretIcons.icons] _ in
                    _ = try await settingsRepository.update(.unlockedIcons(icons))
                } catch: { [log] error, _ in
                    log.warn("unlocked icons sync failed; retried on next settings load", error: error)
                }
                .cancellable(id: CancelID.secretIconSync, cancelInFlight: true)

            case .iconCelebrationDismissed:
                markCelebrationAnnounced(&state)
                return .none

            case let .iconCelebrationUsed(icon):
                markCelebrationAnnounced(&state)
                log.info("set app icon \(icon.rawValue) source=celebration")
                return .run { _ in
                    try await appIconClient.set(icon)
                } catch: { [log] error, _ in
                    log.warn("app icon change failed", error: error)
                }

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
                    reportTabScreen(.schedule, previous: state.tab)
                    state.tab = .schedule
                case .openClasses:
                    reportTabScreen(.classes, previous: state.tab)
                    state.tab = .classes
                case .openMessages:
                    reportTabScreen(.messages, previous: state.tab)
                    state.tab = .messages
                case .openMe:
                    reportTabScreen(.me, previous: state.tab)
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

    /// Fires `screen_view` for the five tab roots — initial mount and every
    /// selection change, deduped against the previous tab so an unrelated
    /// re-entry into the same tab (e.g. a delegate action) can't double-fire.
    private func reportTabScreen(_ tab: Tab, previous: Tab? = nil) {
        guard tab != previous else { return }
        analytics.screen(screenName(for: tab))
    }

    private func screenName(for tab: Tab) -> String {
        switch tab {
        case .home: Screens.overview
        case .schedule: Screens.schedule
        case .classes: Screens.disciplines
        case .messages: Screens.messages
        case .me: Screens.me
        }
    }

    private func markCelebrationAnnounced(_ state: inout State) {
        guard let icons = state.iconCelebration else { return }
        state.$announcedSecretIcons.withLock { $0.formUnion(icons) }
        state.iconCelebration = nil
    }
}
