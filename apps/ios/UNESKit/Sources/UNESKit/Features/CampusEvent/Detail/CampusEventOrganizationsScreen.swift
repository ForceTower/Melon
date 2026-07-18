import ComposableArchitecture
import SwiftUI

/// The student groups behind (and around) the event.
@Reducer
struct CampusEventOrganizationsFeature {
    @ObservableState
    struct State: Equatable {
        let organizations: [CampusEventOrganization]
    }

    enum Action: Equatable {
        case task
    }

    @Dependency(\.analytics) var analytics

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .task:
                analytics.screen(Screens.campusEventOrganizations)
                return .none
            }
        }
    }
}

struct CampusEventOrganizationsView: View {
    let store: StoreOf<CampusEventOrganizationsFeature>

    private static let meshes: [MeshView.Variant] = [.warm, .rose, .cool, .sun, .fresh]

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            CampusEventDetailWash(tone: UNESColor.magenta)

            ScrollView {
                VStack(spacing: 12) {
                    Text(.campusEventOrganizationsSubtitle)
                        .font(.system(size: 14.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(EdgeInsets(top: 0, leading: 4, bottom: 6, trailing: 4))
                        .fadeUp(delay: 0.02)

                    ForEach(Array(store.organizations.enumerated()), id: \.element.id) { index, organization in
                        card(organization, index: index)
                            .fadeUp(delay: 0.04 + Double(index) * 0.03)
                    }
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
            .scrollIndicators(.hidden)
        }
        .navigationTitle(Text(.campusEventOrganizationsTitle))
        .task { await store.send(.task).finish() }
    }

    private func card(_ organization: CampusEventOrganization, index: Int) -> some View {
        let tone = campusEventPalette[index % campusEventPalette.count]
        return HStack(spacing: 0) {
            ZStack {
                UNESColor.darkBg
                StaticMeshView(variant: Self.meshes[index % Self.meshes.count])
                Image(systemName: "person.3")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.92))
            }
            .frame(width: 76)
            .clipped()

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Text(organization.name)
                        .font(.system(size: 16, weight: .bold))
                        .tracking(-0.32)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    if let tag = organization.tag {
                        Text(tag)
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(tone)
                            .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                            .background(tone.opacity(0.1), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                    }
                }
                if let fullName = organization.fullName {
                    Text(fullName)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .padding(.top, 1)
                }
                if let details = organization.details {
                    Text(details)
                        .font(.system(size: 13, weight: .medium))
                        .lineSpacing(4)
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 8)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        }
        .fixedSize(horizontal: false, vertical: true)
        .campusEventCard()
    }
}

#Preview("Entidades") {
    NavigationStack {
        CampusEventOrganizationsView(
            store: Store(
                initialState: CampusEventOrganizationsFeature.State(
                    organizations: CampusEvent.preview().organizations
                )
            ) {
                CampusEventOrganizationsFeature()
            }
        )
    }
}
