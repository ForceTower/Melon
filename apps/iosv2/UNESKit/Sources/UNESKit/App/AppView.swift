import ComposableArchitecture
import SwiftUI

struct AppView: View {
    @Bindable var store: StoreOf<AppFeature>
    @Environment(\.scenePhase) private var scenePhase

    private var tabBinding: Binding<AppFeature.Tab> {
        Binding(get: { store.tab }, set: { store.send(.tabChanged($0)) })
    }

    var body: some View {
        TabView(selection: tabBinding) {
            Tab(String.localized(.navToday), systemImage: "house", value: AppFeature.Tab.home) {
                HomeView(store: store.scope(state: \.home, action: \.home))
            }
            Tab(String.localized(.navSchedule), systemImage: "square.grid.2x2", value: AppFeature.Tab.schedule) {
                ScheduleView(store: store.scope(state: \.schedule, action: \.schedule))
            }
            Tab(String.localized(.navClasses), systemImage: "square.stack.3d.up", value: AppFeature.Tab.classes) {
                DisciplinesView(store: store.scope(state: \.disciplines, action: \.disciplines))
            }
            Tab(String.localized(.navMessages), systemImage: "bubble.left", value: AppFeature.Tab.messages) {
                MessagesView(store: store.scope(state: \.messages, action: \.messages))
            }
            .badgeCompat(store.unreadMessages)
            Tab(String.localized(.navMe), systemImage: "person", value: AppFeature.Tab.me) {
                MeView(store: store.scope(state: \.me, action: \.me))
            }
        }
        .preferredColorScheme(store.theme.colorScheme)
        .task { await store.send(.task).finish() }
        .onChange(of: scenePhase) { _, newPhase in
            switch newPhase {
            case .background: store.send(.sceneBackgrounded)
            case .active: store.send(.sceneActivated)
            default: break
            }
        }
    }
}

#Preview {
    AppView(store: Store(initialState: AppFeature.State()) { AppFeature() })
}
