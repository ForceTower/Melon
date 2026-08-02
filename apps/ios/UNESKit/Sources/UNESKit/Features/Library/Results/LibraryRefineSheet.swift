import ComposableArchitecture
import SwiftUI

/// The refine sheet: display toggles plus the facet groups with their
/// counts over the current result set.
struct LibraryRefineSheet: View {
    @Bindable var store: StoreOf<LibraryResultsFeature>

    /// Which facet groups are expanded — presentation-only state.
    @State private var expanded: Set<LibraryFacetGroup> = [.type, .branch]
    /// Groups showing every value instead of the top five.
    @State private var showingAll: Set<LibraryFacetGroup> = []

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    togglesCard
                        .padding(.bottom, 16)

                    VStack(spacing: 0) {
                        ForEach(LibraryFacetGroup.allCases, id: \.self) { group in
                            facetGroup(group)
                        }
                    }

                    LibraryNote(.libraryRefineNote)
                        .padding(.top, 16)
                }
                .padding(EdgeInsets(top: 4, leading: 18, bottom: 20, trailing: 18))
            }
            .scrollIndicators(.hidden)
            .background(UNESColor.surface)
            .navigationTitle(Text(.libraryRefineTitle))
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        store.send(.clearFacetsTapped)
                    } label: {
                        Text(.libraryRefineClear)
                    }
                    .tint(store.activeFacetCount > 0 ? UNESColor.accent : UNESColor.ink4)
                    .disabled(store.activeFacetCount == 0)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        store.send(.binding(.set(\.isRefinePresented, false)))
                    } label: {
                        Text(.libraryRefineDone)
                            .fontWeight(.semibold)
                    }
                    .tint(UNESColor.accent)
                }
            }
            .safeAreaInset(edge: .bottom) {
                applyBar
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.hidden)
    }

    private var togglesCard: some View {
        VStack(spacing: 0) {
            toggleRow(
                title: .libraryRefineOnlyAvailable,
                hint: .libraryRefineOnlyAvailableHint,
                isOn: $store.onlyAvailable
            )
            Divider()
                .overlay(UNESColor.line)
                .padding(.leading, 15)
            toggleRow(
                title: .libraryRefineGroupByType,
                hint: .libraryRefineGroupByTypeHint,
                isOn: $store.groupByType
            )
        }
        .materialsCard()
    }

    private func toggleRow(
        title: LocalizedStringResource,
        hint: LocalizedStringResource,
        isOn: Binding<Bool>
    ) -> some View {
        Toggle(isOn: isOn) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.29)
                    .foregroundStyle(UNESColor.ink)
                Text(hint)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .tint(UNESColor.successGreen)
        .padding(EdgeInsets(top: 11, leading: 15, bottom: 11, trailing: 15))
    }

    // MARK: Facet groups

    @ViewBuilder
    private func facetGroup(_ group: LibraryFacetGroup) -> some View {
        let values = store.state.facetValues(for: group)
        if !values.isEmpty {
            let isExpanded = expanded.contains(group)
            let selectedCount = store.facets[group]?.count ?? 0
            VStack(spacing: 0) {
                Button {
                    withAnimation(UNESMotion.ease(0.25)) {
                        if isExpanded { expanded.remove(group) } else { expanded.insert(group) }
                    }
                } label: {
                    HStack(spacing: 9) {
                        Text(group.label)
                            .font(.system(size: 15.5, weight: .semibold))
                            .tracking(-0.31)
                            .foregroundStyle(UNESColor.ink)
                        if selectedCount > 0 {
                            Text(String(selectedCount))
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(.white)
                                .frame(minWidth: 18, minHeight: 18)
                                .background(UNESColor.accent, in: Capsule())
                        }
                        Spacer(minLength: 8)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                            .rotationEffect(.degrees(isExpanded ? 90 : 0))
                    }
                    .padding(.vertical, 14)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                if isExpanded {
                    VStack(spacing: 0) {
                        let shown = showingAll.contains(group) ? values : Array(values.prefix(5))
                        ForEach(shown) { value in
                            facetRow(group: group, value: value)
                        }
                        if !showingAll.contains(group), values.count > 5 {
                            Button {
                                showingAll.insert(group)
                            } label: {
                                Text(.libraryRefineShowAll(values.count))
                                    .font(.system(size: 13.5, weight: .semibold))
                                    .foregroundStyle(UNESColor.accent)
                                    .padding(.vertical, 7)
                            }
                            .buttonStyle(.plain)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                    .padding(.bottom, 12)
                }
                Divider()
                    .overlay(UNESColor.line)
            }
        }
    }

    private func facetRow(group: LibraryFacetGroup, value: LibraryFacetValue) -> some View {
        let isOn = store.facets[group]?.contains(value.key) ?? false
        return Button {
            store.send(.facetToggled(group, value.key))
        } label: {
            HStack(spacing: 11) {
                Image(systemName: isOn ? "checkmark.square.fill" : "square")
                    .font(.system(size: 19, weight: .medium))
                    .foregroundStyle(isOn ? UNESColor.ink : UNESColor.surface3)
                Text(value.label)
                    .font(.system(size: 14.5, weight: isOn ? .semibold : .medium))
                    .tracking(-0.29)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text(LibraryFormat.count(value.count))
                    .font(.system(size: 12.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.vertical, 9)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private var applyBar: some View {
        Button {
            store.send(.binding(.set(\.isRefinePresented, false)))
        } label: {
            Text(store.displayCount == 1
                ? .libraryRefineApplyOne(store.displayCount)
                : .libraryRefineApplyOther(store.displayCount))
                .tracking(-0.17)
        }
        .buttonStyle(.unesAccent)
        .padding(EdgeInsets(top: 10, leading: 20, bottom: 8, trailing: 20))
        .background(UNESColor.surface)
    }
}

extension LibraryFacetGroup {
    var label: LocalizedStringResource {
        switch self {
        case .type: .libraryFacetType
        case .branch: .libraryFacetBranch
        case .subject: .libraryFacetSubject
        case .author: .libraryFacetAuthor
        case .language: .libraryFacetLanguage
        case .year: .libraryFacetYear
        }
    }
}

#Preview("Refinar") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            LibraryRefineSheet(
                store: Store(
                    initialState: {
                        var state = LibraryResultsFeature.State(query: "cálculo", scope: .all)
                        let works = LibraryFixtures.works(now: Date())
                        state.works = works
                        state.total = works.count
                        state.serverFacets = LibrarySearchPage.facetValues(over: works)
                        state.isLoading = false
                        return state
                    }()
                ) {
                    LibraryResultsFeature()
                }
            )
        }
}
