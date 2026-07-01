import ComposableArchitecture
import SwiftUI

struct MessagesView: View {
    let store: StoreOf<MessagesFeature>

    var body: some View {
        NavigationStack {
            PlaceholderScreen(title: "Mensagens", systemImage: "bubble.left")
                .navigationTitle("Mensagens")
        }
        .task { store.send(.onAppear) }
    }
}

#Preview {
    MessagesView(store: Store(initialState: MessagesFeature.State()) { MessagesFeature() })
}
