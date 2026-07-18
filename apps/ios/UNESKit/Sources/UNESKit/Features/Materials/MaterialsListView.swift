import ComposableArchitecture
import SwiftUI

struct MaterialsListView: View {
    @Bindable var store: StoreOf<MaterialsListFeature>

    private var tone: Color { UNESColor.disciplineReadableColor(store.discipline.colorIndex) }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash
            content
        }
        .navigationTitle(store.discipline.code)
        .navigationBarTitleDisplayMode(.inline)
        .task { await store.send(.task).finish() }
        .sheet(item: $store.scope(state: \.upload, action: \.upload)) { uploadStore in
            MaterialsUploadSheet(store: uploadStore)
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.details != nil {
            shelf
        } else if store.loadFailed {
            MaterialsFailureView { store.send(.retryTapped) }
        } else {
            MaterialsLoadingView()
        }
    }

    private var shelf: some View {
        ScrollView {
            VStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 14)

                if store.isEmpty {
                    MaterialsEmptyShelf(discipline: store.discipline) {
                        store.send(.contributeTapped)
                    }
                } else {
                    filters
                        .fadeUp(delay: 0.08)
                        .padding(.bottom, 16)

                    if !store.mine.isEmpty {
                        mineSection
                            .fadeUp(delay: 0.14)
                            .padding(.bottom, 22)
                    }

                    resultsSection
                        .fadeUp(delay: 0.2)
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: store.isEmpty ? 24 : 96, trailing: 16))
        }
        .scrollIndicators(.hidden)
        .scrollDismissesKeyboard(.immediately)
        .refreshable { await store.send(.refreshPulled).finish() }
        .searchable(
            text: $store.searchQuery,
            placement: Self.searchPlacement,
            prompt: Text(.materialsListSearchPrompt)
        )
        .overlay(alignment: .bottom) {
            if !store.isEmpty {
                contributeBar
            }
        }
    }

    private static var searchPlacement: SearchFieldPlacement {
        #if os(watchOS)
        .automatic
        #else
        .navigationBarDrawer(displayMode: .automatic)
        #endif
    }

    // MARK: Header

    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(store.discipline.code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.5)
                    .foregroundStyle(tone)
                    .padding(EdgeInsets(top: 3, leading: 8, bottom: 3, trailing: 8))
                    .background(tone.opacity(0.12), in: RoundedRectangle(cornerRadius: 7, style: .continuous))
                Text(.materialsListEyebrow)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            Text(store.discipline.name)
                .font(.system(size: 28, weight: .bold))
                .tracking(-0.98)
                .foregroundStyle(UNESColor.ink)
                .lineSpacing(1)
            if !store.isEmpty {
                Text(store.published.count == 1
                    ? .materialsListCountOne(store.published.count)
                    : .materialsListCountOther(store.published.count))
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    // MARK: Filters

    private var filters: some View {
        typeChips
    }

    /// Horizontally scrollable type chips — "Tudo" plus one per type on the
    /// shelf.
    private var typeChips: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                MaterialsFilterChip(
                    isActive: store.typeFilter == nil,
                    tone: UNESColor.ink
                ) {
                    store.send(.binding(.set(\.typeFilter, nil)))
                } label: {
                    Text(.materialsListFilterAll)
                    Text(String(store.published.count))
                        .opacity(0.7)
                }
                ForEach(availableTypes, id: \.self) { type in
                    MaterialsFilterChip(
                        isActive: store.typeFilter == type,
                        tone: type.tone
                    ) {
                        store.send(.binding(.set(\.typeFilter, store.typeFilter == type ? nil : type)))
                    } label: {
                        Image(systemName: type.icon)
                            .font(.system(size: 11, weight: .semibold))
                        Text(type.pluralLabel)
                        Text(String(count(of: type)))
                            .opacity(0.75)
                    }
                }
            }
            // Bleeds through the screen inset so scrolled chips reach the
            // display edge instead of clipping at the content column.
            .padding(.horizontal, 20)
        }
        .scrollIndicators(.hidden)
        // The row is exactly chip height, so without this the chip shadows
        // clip at the scroll bounds.
        .scrollClipDisabled()
        .padding(.horizontal, -16)
    }

    private var availableTypes: [MaterialType] {
        MaterialType.allCases.filter { count(of: $0) > 0 }
    }

    private func count(of type: MaterialType) -> Int {
        store.published.count { $0.type == type }
    }

    // MARK: Sections

    private var mineSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(.materialsListSectionMine)
            VStack(spacing: 0) {
                ForEach(Array(store.mine.enumerated()), id: \.element.id) { index, material in
                    if index > 0 {
                        Divider()
                            .overlay(UNESColor.line)
                            .padding(.leading, 15)
                    }
                    MaterialMineRow(material: material) {
                        store.send(.materialTapped(material))
                    }
                }
            }
            .materialsCard()
        }
    }

    private var resultsSection: some View {
        VStack(spacing: 0) {
            MaterialsSectionHeader(
                title: store.typeFilter.map(\.pluralLabel) ?? .materialsListSectionShelf
            ) {
                Text(store.filtered.count == 1
                    ? .materialsListItemsOne(store.filtered.count)
                    : .materialsListItemsOther(store.filtered.count))
                    .font(.system(size: 13, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            }
            if store.filtered.isEmpty {
                noResults
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(store.filtered.enumerated()), id: \.element.id) { index, material in
                        if index > 0 {
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
        }
    }

    private var noResults: some View {
        VStack(spacing: 5) {
            Text(.materialsListEmptyFilteredTitle)
                .font(.system(size: 14.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink2)
            Text(.materialsListEmptyFilteredSubtitle)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(26)
        .materialsCard()
    }

    // MARK: Contribute CTA

    /// The floating bottom CTA over a surface fade, so the shelf scrolls
    /// underneath it.
    private var contributeBar: some View {
        Button {
            store.send(.contributeTapped)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "plus")
                    .font(.system(size: 15, weight: .bold))
                Text(.materialsListContributeCta)
                    .tracking(-0.17)
            }
        }
        .buttonStyle(.unesAccent)
        .padding(EdgeInsets(top: 34, leading: 20, bottom: 12, trailing: 20))
        .background {
            LinearGradient(
                stops: [
                    .init(color: UNESColor.surface.opacity(0), location: 0),
                    .init(color: UNESColor.surface, location: 0.42),
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .bottom)
            .allowsHitTesting(false)
        }
    }

    /// Faint discipline-tinted wash behind the header.
    private var ambientWash: some View {
        RadialGradient(
            colors: [tone.opacity(0.3), .clear],
            center: .top,
            startRadius: 0,
            endRadius: 260
        )
        .frame(height: 300)
        .opacity(0.6)
        .offset(y: -60)
        .ignoresSafeArea()
    }
}

// MARK: - Filter chip

private struct MaterialsFilterChip<Label: View>: View {
    var isActive: Bool
    var tone: Color
    var onTap: () -> Void
    @ViewBuilder var label: Label

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 6) {
                label
            }
            .font(.system(size: 13.5, weight: .semibold))
            .tracking(-0.14)
            .monospacedDigit()
            // The fills read dark in light mode and lifted in dark mode, so
            // the active label is the inverse surface, never a fixed white.
            .foregroundStyle(isActive ? UNESColor.surface : UNESColor.ink2)
            .padding(.horizontal, 14)
            .frame(height: 34)
            .background(isActive ? AnyShapeStyle(tone) : AnyShapeStyle(UNESColor.card), in: Capsule())
            .overlay {
                if !isActive {
                    Capsule().strokeBorder(UNESColor.cardLine)
                }
            }
            .shadow(color: Color(hex: 0x141020, opacity: isActive ? 0 : 0.04), radius: 4, y: 2)
        }
        .buttonStyle(TilePressStyle())
    }
}

// MARK: - Empty shelf

/// No materials at all — the dark mesh pitch plus the "what you can send"
/// type list.
private struct MaterialsEmptyShelf: View {
    var discipline: MaterialsDiscipline
    var onContribute: () -> Void

    var body: some View {
        VStack(spacing: 22) {
            pitch
                .scaleIn(delay: 0.08, duration: 0.62)
            VStack(spacing: 0) {
                MaterialsSectionHeader(.materialsEmptySectionTypes)
                VStack(spacing: 0) {
                    ForEach(Array(MaterialType.allCases.enumerated()), id: \.element) { index, type in
                        if index > 0 {
                            Divider()
                                .overlay(UNESColor.line)
                                .padding(.leading, 66)
                        }
                        HStack(spacing: 13) {
                            MaterialTypeBadge(type: type, size: 38)
                            VStack(alignment: .leading, spacing: 1) {
                                Text(type.pluralLabel)
                                    .font(.system(size: 14.5, weight: .semibold))
                                    .tracking(-0.22)
                                    .foregroundStyle(UNESColor.ink)
                                Text(type.hint)
                                    .font(.system(size: 12.5, weight: .medium))
                                    .foregroundStyle(UNESColor.ink4)
                            }
                            Spacer(minLength: 0)
                        }
                        .padding(EdgeInsets(top: 12, leading: 15, bottom: 12, trailing: 15))
                    }
                }
                .materialsCard()
            }
            .fadeUp(delay: 0.2)
        }
        .padding(.top, 8)
    }

    private var pitch: some View {
        ZStack {
            MeshView(variant: .warm, intensity: 0.95)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.disciplineColor(discipline.colorIndex).opacity(0.27), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.72), location: 1),
                ],
                angle: 160
            )
            VStack(spacing: 0) {
                Image(systemName: "sparkles")
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(.white)
                    .frame(width: 60, height: 60)
                    .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                Text(.materialsEmptyTitle)
                    .font(.system(size: 21, weight: .bold))
                    .tracking(-0.63)
                    .foregroundStyle(.white)
                    .padding(.top, 16)
                Text(.materialsEmptySubtitle(discipline.name))
                    .font(.system(size: 14, weight: .medium))
                    .lineSpacing(3)
                    .foregroundStyle(.white.opacity(0.76))
                    .multilineTextAlignment(.center)
                    .padding(.top, 9)
                Button(action: onContribute) {
                    HStack(spacing: 8) {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                        Text(.materialsEmptyCta)
                            .font(.system(size: 16, weight: .semibold))
                            .tracking(-0.16)
                    }
                    .foregroundStyle(UNESColor.darkBg)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(.white, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                }
                .buttonStyle(TilePressStyle())
                .padding(.top, 20)
            }
            .padding(EdgeInsets(top: 26, leading: 22, bottom: 24, trailing: 22))
        }
        .background(UNESColor.darkBg)
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.26), radius: 20, y: 9)
    }
}

#Preview("Acervo") {
    NavigationStack {
        MaterialsListView(
            store: Store(
                initialState: MaterialsListFeature.State(
                    discipline: MaterialsOverview.preview().disciplines[0]
                )
            ) {
                MaterialsListFeature()
            }
        )
    }
}

#Preview("Vazia") {
    NavigationStack {
        MaterialsListView(
            store: Store(
                initialState: MaterialsListFeature.State(
                    discipline: MaterialsOverview.preview().disciplines[4]
                )
            ) {
                MaterialsListFeature()
            }
        )
    }
}
