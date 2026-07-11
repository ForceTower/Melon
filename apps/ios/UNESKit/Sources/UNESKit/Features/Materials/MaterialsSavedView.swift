import ComposableArchitecture
import SwiftUI

struct MaterialsSavedView: View {
    let store: StoreOf<MaterialsSavedFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            content
        }
        .navigationTitle(Text(.materialsSavedTitle))
        .navigationBarTitleDisplayMode(.large)
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var content: some View {
        if let materials = store.materials {
            if materials.isEmpty {
                empty
            } else {
                shelf
            }
        } else if store.loadFailed {
            MaterialsFailureView { store.send(.retryTapped) }
        } else {
            MaterialsLoadingView()
        }
    }

    private var shelf: some View {
        ScrollView {
            VStack(spacing: 0) {
                Text(.materialsSavedSubtitle)
                    .font(.system(size: 15, weight: .medium))
                    .tracking(-0.15)
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 4)
                    .padding(.bottom, 18)
                    .fadeUp(delay: 0.02)

                ForEach(Array(store.groups.enumerated()), id: \.element.discipline.id) { index, group in
                    VStack(spacing: 0) {
                        MaterialsSectionHeader(verbatim: "\(group.discipline.code) · \(group.discipline.name)")
                        VStack(spacing: 0) {
                            ForEach(Array(group.materials.enumerated()), id: \.element.id) { rowIndex, material in
                                if rowIndex > 0 {
                                    Divider()
                                        .overlay(UNESColor.line)
                                        .padding(.leading, 72)
                                }
                                MaterialRow(material: material) {
                                    store.send(.materialTapped(material))
                                }
                            }
                        }
                        .materialsCard()
                    }
                    .fadeUp(delay: 0.08 + Double(index) * 0.05)
                    .padding(.bottom, 22)
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .refreshable { await store.send(.refreshPulled).finish() }
    }

    private var empty: some View {
        VStack(spacing: 14) {
            Image(systemName: "bookmark")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 52, height: 52)
                .background(UNESColor.surface2, in: Circle())
            Text(.materialsSavedEmpty)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 72)
    }
}

#Preview("Salvos") {
    NavigationStack {
        MaterialsSavedView(
            store: Store(initialState: MaterialsSavedFeature.State()) {
                MaterialsSavedFeature()
            }
        )
    }
}

#Preview("Vazio") {
    NavigationStack {
        MaterialsSavedView(
            store: Store(initialState: MaterialsSavedFeature.State()) {
                MaterialsSavedFeature()
            } withDependencies: {
                $0.materialsRepository.saved = { [] }
            }
        )
    }
}
