import ComposableArchitecture
import SwiftUI

struct OverviewView: View {
    let store: StoreOf<OverviewFeature>

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Button("Ver mensagens") { store.send(.openMessagesTapped) }
                    Button("Ver horário") { store.send(.openScheduleTapped) }
                } header: {
                    Text("Atalhos")
                }
            }
            .navigationTitle("Hoje")
        }
    }
}

#Preview {
    OverviewView(store: Store(initialState: OverviewFeature.State()) { OverviewFeature() })
}
