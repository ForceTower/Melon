import ComposableArchitecture
import SwiftUI

/// Licenças, end to end: the always-dark distribution hero, search + family
/// filter, the grouped dependency cards with expandable rows, and the SBOM
/// footer. The inline nav title fades in as the large header scrolls away.
struct LicensesView: View {
    let store: StoreOf<LicensesFeature>
    @State private var titleProgress: CGFloat = 0

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("Licenças")
                    .font(.system(size: 16, weight: .semibold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                    .opacity(titleProgress)
                    .offset(y: (1 - titleProgress) * 6)
            }
        }
        .inlineNavigationBar()
    }

    private var content: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)

                VStack(spacing: 0) {
                    LicensesHero()
                        .scaleIn(delay: 0.1, duration: 0.62)
                        .padding(.bottom, 22)

                    VStack(spacing: 0) {
                        sectionHeader("Dependências")
                        SearchField(placeholder: "Buscar pacote ou autor", query: store.query) {
                            store.send(.queryChanged($0))
                        }
                        .padding(.bottom, 11)
                    }
                    .fadeUp(delay: 0.2)
                }
                .padding(.horizontal, 16)

                LicensesFilterChips(active: store.family) {
                    store.send(.familySelected($0))
                }
                .fadeUp(delay: 0.2)
                .padding(.bottom, 16)

                VStack(spacing: 0) {
                    groups
                        .fadeUp(delay: 0.28)

                    LicensesFooter()
                        .fadeUp(delay: 0.36)
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 8)
            .padding(.bottom, 12)
        }
        .onScrollGeometryChange(for: CGFloat.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top
        } action: { _, offset in
            titleProgress = min(max((offset - 40) / 44, 0), 1)
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(.system(size: 22, weight: .bold))
            .tracking(-0.66)
            .foregroundStyle(UNESColor.ink)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Licenças")
                .font(.system(size: 40, weight: .bold))
                .tracking(-1.6)
                .foregroundStyle(UNESColor.ink)
            Text("UNES é construído sobre o trabalho de pessoas que compartilham seu código abertamente. Aqui está quem.")
                .font(.system(size: 14, weight: .medium))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink3)
                .frame(maxWidth: 300, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 20, bottom: 18, trailing: 20))
    }

    @ViewBuilder
    private var groups: some View {
        if store.groups.isEmpty {
            emptyState
                .padding(.bottom, 14)
        } else {
            ForEach(store.groups) { group in
                LicenseGroupCard(
                    group: group,
                    expandedID: store.expandedID,
                    copiedID: store.copiedID,
                    onToggle: { store.send(.packageToggled($0), animation: UNESMotion.ease(0.28)) },
                    onHomepage: { store.send(.homepageTapped($0)) },
                    onCopy: { store.send(.copyTapped($0)) },
                    onLicenseText: { store.send(.licenseTextTapped($0)) }
                )
                .padding(.bottom, 14)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 4) {
            Text("Nada por aqui")
                .font(.system(size: 17, weight: .bold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text("Tente outra busca ou filtro.")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity)
        .padding(EdgeInsets(top: 40, leading: 16, bottom: 40, trailing: 16))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 320)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.28)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

#Preview {
    NavigationStack {
        LicensesView(
            store: Store(initialState: LicensesFeature.State()) {
                LicensesFeature()
            }
        )
    }
}
