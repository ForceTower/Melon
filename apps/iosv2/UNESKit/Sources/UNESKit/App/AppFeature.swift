import ComposableArchitecture

@Reducer
struct AppFeature {
    @ObservableState
    struct State: Equatable {
        var tab: Tab = .overview
        var overview = OverviewFeature.State()
        var schedule = ScheduleFeature.State()
        var disciplines = DisciplinesFeature.State()
        var messages = MessagesFeature.State()
        var me = MeFeature.State()
        @Shared(.appStorage("theme")) var theme: AppTheme = .system
    }

    enum Tab: String, CaseIterable, Hashable, Sendable {
        case overview, schedule, classes, messages, me
    }

    enum Action: Equatable {
        case tabChanged(Tab)
        case overview(OverviewFeature.Action)
        case schedule(ScheduleFeature.Action)
        case disciplines(DisciplinesFeature.Action)
        case messages(MessagesFeature.Action)
        case me(MeFeature.Action)
    }

    var body: some ReducerOf<Self> {
        Scope(state: \.overview, action: \.overview) { OverviewFeature() }
        Scope(state: \.schedule, action: \.schedule) { ScheduleFeature() }
        Scope(state: \.disciplines, action: \.disciplines) { DisciplinesFeature() }
        Scope(state: \.messages, action: \.messages) { MessagesFeature() }
        Scope(state: \.me, action: \.me) { MeFeature() }

        Reduce { state, action in
            switch action {
            case let .tabChanged(tab):
                state.tab = tab
                return .none

            case .overview(.delegate(.openMessages)):
                state.tab = .messages
                return .none

            case .overview(.delegate(.openSchedule)):
                state.tab = .schedule
                return .none

            case .overview, .schedule, .disciplines, .messages, .me:
                return .none
            }
        }
    }
}
