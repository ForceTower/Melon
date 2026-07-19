import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct AppFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)
    @Test
    func selectingATabUpdatesState() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.tabChanged(.messages)) {
            $0.tab = .messages
        }
    }

    @Test
    func homeShortcutSwitchesTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.home(.seeScheduleTapped))
        await store.receive(.home(.delegate(.openSchedule))) {
            $0.tab = .schedule
        }
    }

    @Test
    func intentRouteSwitchesTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.intentRoute(.messages)) {
            $0.tab = .messages
        }
    }

    @Test
    func taskForwardsIntentRoutesIntoTabSwitches() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.push.requestAuthorization = {}
            $0.push.dataEvents = { .finished }
            $0.syncRepository.ping = {}
            $0.syncRepository.backfillMirror = {}
            $0.intentRouter.routes = {
                AsyncStream { continuation in
                    continuation.yield(.tab(.messages))
                    continuation.finish()
                }
            }
        }
        // Only the intent-route subscription matters here; the rest of
        // .task's fan-out is covered elsewhere.
        store.exhaustivity = .off

        await store.send(.task)
        await store.receive(.intentRoute(.messages)) {
            $0.tab = .messages
        }
    }

    @Test
    func disciplineEntityRouteOpensTheDetail() async {
        let discipline = DisciplineSummary(
            id: "d1", code: "ALGI", name: "Algoritmos I", hours: 60, missedHours: 0
        )
        let overview = DisciplinesOverview(
            current: SemesterDisciplines(id: "sem1", code: "20261", disciplines: [discipline])
        )
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in overview }
        }

        await store.send(.intentOpenDiscipline(semesterId: "sem1", disciplineId: "d1"))
        await store.receive(.intentRoute(.classes)) {
            $0.tab = .classes
        }
        await store.receive(.disciplines(.disciplineTapped(semesterId: "sem1", discipline: discipline))) {
            $0.disciplines.path[id: 0] =
                .detail(DisciplineDetailFeature.State(summary: discipline, semesterId: "sem1"))
        }
    }

    @Test
    func staleDisciplineRouteFallsBackToTheTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.disciplinesRepository.cached = { _ in nil }
        }

        await store.send(.intentOpenDiscipline(semesterId: "sem1", disciplineId: "gone"))
        await store.receive(.intentRoute(.classes)) {
            $0.tab = .classes
        }
    }

    @Test
    func messageEntityRouteOpensTheDetailAndMarksItRead() async {
        let message = MessageItem(
            id: "m1", origin: .campus, disciplineCode: nil, disciplineName: nil,
            disciplineColorIndex: nil, subject: "Prazo de matrícula", body: "O prazo termina sexta.",
            senderName: "Colegiado", receivedAt: Self.referenceDate, unread: true, starred: false
        )
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.cached = { _ in MessagesOverview(messages: [message]) }
            $0.messagesRepository.markRead = { _, _ in }
        }

        await store.send(.intentOpenMessage(id: "m1"))
        await store.receive(.intentRoute(.messages)) {
            $0.tab = .messages
        }
        var opened = message
        opened.unread = false
        await store.receive(.messages(.messageTapped(message))) {
            $0.messages.path[id: 0] = .detail(MessageDetailFeature.State(message: opened))
        }
        await store.receive(.messages(.delegate(.unreadChanged(0))))
    }

    @Test
    func staleMessageRouteFallsBackToTheTab() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.cached = { _ in nil }
            $0.messagesRepository.refresh = { _ in }
        }

        await store.send(.intentOpenMessage(id: "gone"))
        await store.receive(.intentRoute(.messages)) {
            $0.tab = .messages
        }
    }

    @Test
    func messageRouteRefreshesWhenTheTapOutrunsTheMirror() async {
        let message = MessageItem(
            id: "m1", origin: .campus, disciplineCode: nil, disciplineName: nil,
            disciplineColorIndex: nil, subject: "Prazo de matrícula", body: "O prazo termina sexta.",
            senderName: "Colegiado", receivedAt: Self.referenceDate, unread: false, starred: false
        )
        // Empty until the refresh "writes" the row — the push-tap race.
        let synced = LockIsolated(false)
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.messagesRepository.cached = { _ in
                synced.value ? MessagesOverview(messages: [message]) : nil
            }
            $0.messagesRepository.refresh = { _ in synced.setValue(true) }
        }

        await store.send(.intentOpenMessage(id: "m1"))
        await store.receive(.intentRoute(.messages)) {
            $0.tab = .messages
        }
        await store.receive(.messages(.messageTapped(message))) {
            $0.messages.path[id: 0] = .detail(MessageDetailFeature.State(message: message))
        }
    }

    @Test
    func materialRouteFetchesTheDetailAndLandsOnIt() async {
        let material = Material.preview()[0]
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.materialsRepository.material = { _ in material }
        }

        await store.send(.intentOpenMaterial(id: material.id))
        await store.receive(.intentRoute(.me)) {
            $0.tab = .me
        }
        await store.receive(.me(.deeplinkOpened(.material(material)))) {
            $0.me.path[id: 0] = .materials(MaterialsFeature.State())
            $0.me.path[id: 1] = .materialsDetail(MaterialsDetailFeature.State(material: material))
        }
    }

    @Test
    func unfetchableMaterialRouteFallsBackToTheHub() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.materialsRepository.material = { _ in throw APIError.emptyEnvelope }
        }

        await store.send(.intentOpenMaterial(id: "gone"))
        await store.receive(.intentRoute(.me)) {
            $0.tab = .me
        }
        await store.receive(.me(.deeplinkOpened(.materialsHub))) {
            $0.me.path[id: 0] = .materials(MaterialsFeature.State())
        }
    }

    @Test
    func materialsDisciplineRouteResolvesTheShelfFromTheOverview() async {
        let overview = MaterialsOverview.preview()
        let discipline = overview.disciplines[1]
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        } withDependencies: {
            $0.materialsRepository.overview = { overview }
        }

        await store.send(.intentOpenMaterialsDiscipline(disciplineId: discipline.id))
        await store.receive(.intentRoute(.me)) {
            $0.tab = .me
        }
        await store.receive(.me(.deeplinkOpened(.materialsDiscipline(discipline)))) {
            $0.me.path[id: 0] = .materials(MaterialsFeature.State())
            $0.me.path[id: 1] = .materialsList(MaterialsListFeature.State(discipline: discipline))
        }
    }

    @Test
    func unreadCountFeedsTheMessagesBadge() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        await store.send(.home(.delegate(.unreadMessagesChanged(3)))) {
            $0.unreadMessages = 3
        }
    }

    @Test
    func resumingFromBackgroundRehydratesEveryTab() async {
        // A week apart, so the two overviews carry disjoint dayStamps — the
        // exact shape of a suspension that crossed into a new week.
        let staleWeek = ScheduleOverview.preview(now: Self.referenceDate)
        let freshWeek = ScheduleOverview.preview(now: Self.referenceDate.addingTimeInterval(7 * 86_400))

        // Every tab already hydrated, so re-sent .task actions only restart
        // the mirror observations.
        var initial = AppFeature.State()
        initial.home.overview = .empty
        initial.schedule.overview = staleWeek
        initial.disciplines.overview = DisciplinesOverview()
        initial.messages.overview = .preview(now: Self.referenceDate)
        initial.me.overview = .empty
        initial.me.userName = "Ana"

        let store = TestStore(initialState: initial) {
            AppFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.syncRepository.ping = {}
            $0.homeRepository.observe = { .finished }
            $0.scheduleRepository.observe = {
                AsyncStream { continuation in
                    continuation.yield(freshWeek)
                    continuation.finish()
                }
            }
            $0.disciplinesRepository.observe = { .finished }
            $0.messagesRepository.observe = { .finished }
            $0.meRepository.observe = { .finished }
            $0.evaluationReminders.reconcile = {}
        }
        // Each tab's own hydration flow is covered by its feature tests; here
        // only the resume broadcast and the schedule replay matter.
        store.exhaustivity = .off

        await store.send(.sceneBackgrounded) {
            $0.wasBackgrounded = true
        }
        await store.send(.sceneActivated) {
            $0.wasBackgrounded = false
        }
        await store.receive(.home(.task))
        await store.receive(.schedule(.task))
        await store.receive(.disciplines(.task))
        await store.receive(.messages(.task))
        await store.receive(.me(.task))
        // The restarted observation replays the mirror recomputed with the
        // current date — the stale week gives way to the fresh one.
        await store.receive(.schedule(.overviewUpdated(freshWeek))) {
            $0.schedule.overview = freshWeek
        }
    }

    @Test
    func transientDeactivationDoesNotRehydrate() async {
        let store = TestStore(initialState: AppFeature.State()) {
            AppFeature()
        }

        // Control center, call banners, and app-switcher peeks bounce through
        // .inactive without reaching .background — no rehydration should run.
        await store.send(.sceneActivated)
    }

    @Test
    func unannouncedDiscoveryPopsTheCelebrationAndAnnouncesOnDismiss() async {
        let patched = LockIsolated<[SettingsChange]>([])
        var initialState = AppFeature.State()
        initialState.$unlockedSecretIcons.withLock { $0.insert(.paper) }

        let store = TestStore(initialState: initialState) {
            AppFeature()
        } withDependencies: {
            $0.settingsRepository.update = { change in
                patched.withValue { $0.append(change) }
                var settings = UserSettings()
                settings.apply(change)
                return settings
            }
        }

        await store.send(.secretIconsChanged) {
            $0.iconCelebration = [.paper]
        }
        await store.send(.iconCelebrationDismissed) {
            $0.iconCelebration = nil
            $0.$announcedSecretIcons.withLock { $0.insert(.paper) }
        }

        // Already celebrated — the next pass stays quiet.
        await store.send(.secretIconsChanged)
        await store.finish()

        #expect(patched.value == [.unlockedIcons([.paper])])
    }

    @Test
    func usingACelebratedIconAppliesItThroughTheClient() async {
        let applied = LockIsolated<[AppIcon]>([])
        var initialState = AppFeature.State()
        initialState.$unlockedSecretIcons.withLock { $0.insert(.nowShip) }
        initialState.iconCelebration = [.nowShip]

        let store = TestStore(initialState: initialState) {
            AppFeature()
        } withDependencies: {
            $0.appIconClient.set = { icon in applied.withValue { $0.append(icon) } }
        }

        await store.send(.iconCelebrationUsed(.nowShip)) {
            $0.iconCelebration = nil
            $0.$announcedSecretIcons.withLock { $0.insert(.nowShip) }
        }
        await store.finish()

        #expect(applied.value == [.nowShip])
    }
}
