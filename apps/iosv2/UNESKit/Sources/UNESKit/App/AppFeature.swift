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
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
    }

    enum Tab: String, CaseIterable, Hashable, Sendable {
        case home, schedule, classes, messages, me
    }

    enum Action: Equatable {
        case tabChanged(Tab)
        case home(HomeFeature.Action)
        case schedule(ScheduleFeature.Action)
        case disciplines(DisciplinesFeature.Action)
        case messages(MessagesFeature.Action)
        case me(MeFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Scope(state: \.home, action: \.home) { HomeFeature() }
        Scope(state: \.schedule, action: \.schedule) { ScheduleFeature() }
        Scope(state: \.disciplines, action: \.disciplines) { DisciplinesFeature() }
        Scope(state: \.messages, action: \.messages) { MessagesFeature() }
        Scope(state: \.me, action: \.me) { MeFeature() }

        Reduce { state, action in
            switch action {
            case let .tabChanged(tab):
                state.tab = tab
                return .none

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

            case .home, .schedule, .disciplines, .messages, .me:
                return .none
            }
        }
    }
}
