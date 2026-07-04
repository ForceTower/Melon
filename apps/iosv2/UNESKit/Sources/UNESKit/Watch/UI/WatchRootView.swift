#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// The watch app's entry: one store, native NavigationStack, value routing.
public struct WatchRootView: View {
    @State private var store = Store(initialState: WatchAppFeature.State()) {
        WatchAppFeature()
    }

    public init() {}

    public var body: some View {
        @Bindable var store = store
        NavigationStack(path: $store.path) {
            WatchHomeView(store: store)
                .navigationDestination(for: WatchAppFeature.Route.self) { route in
                    switch route {
                    case .week:
                        WatchWeekView(store: store)
                    case let .discipline(id):
                        WatchDisciplineView(store: store, disciplineId: id)
                    }
                }
        }
        .tint(UNESColor.coral)
        .task { await store.send(.task).finish() }
    }
}

#Preview {
    WatchRootView()
}
#endif
