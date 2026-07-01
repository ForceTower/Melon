import ComposableArchitecture
import SwiftUI

struct DisciplinesView: View {
    let store: StoreOf<DisciplinesFeature>

    var body: some View {
        NavigationStack {
            PlaceholderScreen(title: "Disciplinas", systemImage: "square.stack.3d.up")
                .navigationTitle("Disciplinas")
        }
        .task { store.send(.onAppear) }
    }
}

#Preview {
    DisciplinesView(store: Store(initialState: DisciplinesFeature.State()) { DisciplinesFeature() })
}
