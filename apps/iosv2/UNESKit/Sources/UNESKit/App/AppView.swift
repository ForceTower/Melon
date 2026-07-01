import ComposableArchitecture
import SwiftUI

struct AppView: View {
    @Bindable var store: StoreOf<AppFeature>

    private var tabBinding: Binding<AppFeature.Tab> {
        Binding(get: { store.tab }, set: { store.send(.tabChanged($0)) })
    }

    var body: some View {
        TabView(selection: tabBinding) {
            Tab("Hoje", systemImage: "house", value: AppFeature.Tab.home) {
                HomeView(store: store.scope(state: \.home, action: \.home))
            }
            Tab("Horário", systemImage: "square.grid.2x2", value: AppFeature.Tab.schedule) {
                ScheduleView(store: store.scope(state: \.schedule, action: \.schedule))
            }
            Tab("Turmas", systemImage: "square.stack.3d.up", value: AppFeature.Tab.classes) {
                DisciplinesView(store: store.scope(state: \.disciplines, action: \.disciplines))
            }
            Tab("Mensagens", systemImage: "bubble.left", value: AppFeature.Tab.messages) {
                MessagesView(store: store.scope(state: \.messages, action: \.messages))
            }
            .badge(store.unreadMessages)
            Tab("Eu", systemImage: "person", value: AppFeature.Tab.me) {
                MeView(store: store.scope(state: \.me, action: \.me))
            }
        }
        .preferredColorScheme(store.theme.colorScheme)
    }
}

#Preview {
    AppView(store: Store(initialState: AppFeature.State()) { AppFeature() })
}
