import ComposableArchitecture
import SwiftUI

struct MeView: View {
    let store: StoreOf<MeFeature>

    var body: some View {
        NavigationStack {
            Group {
                if let profile = store.profile {
                    List {
                        Section {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(profile.name).font(.headline)
                                if let course = profile.course {
                                    Text(course).font(.subheadline).foregroundStyle(.secondary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                } else if store.isLoading {
                    ProgressView()
                } else {
                    PlaceholderScreen(
                        title: "Eu",
                        systemImage: "person",
                        message: store.errorMessage ?? "Sem perfil carregado"
                    )
                }
            }
            .navigationTitle("Eu")
        }
        .task { store.send(.onAppear) }
    }
}

#Preview {
    MeView(
        store: Store(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.profileRepository = .previewValue
        }
    )
}
