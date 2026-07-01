import ComposableArchitecture
import SwiftUI

struct ScheduleView: View {
    let store: StoreOf<ScheduleFeature>

    var body: some View {
        NavigationStack {
            PlaceholderScreen(title: "Horário", systemImage: "square.grid.2x2")
                .navigationTitle("Horário")
        }
        .task { store.send(.onAppear) }
    }
}

#Preview {
    ScheduleView(store: Store(initialState: ScheduleFeature.State()) { ScheduleFeature() })
}
