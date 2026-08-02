import ComposableArchitecture
import SwiftUI

struct LibraryResultsView: View {
    @Bindable var store: StoreOf<LibraryResultsFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            content
        }
        .navigationTitle(store.query)
        .inlineNavigationBar()
        .task { await store.send(.task).finish() }
        .toolbar {
            if !store.isTooBroad, !store.isEmpty {
                ToolbarItem(placement: .primaryAction) {
                    refineButton
                }
            }
        }
        .sheet(isPresented: $store.isRefinePresented) {
            LibraryRefineSheet(store: store)
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.isTooBroad {
            ScrollView { broadState.padding(16) }
                .scrollIndicators(.hidden)
        } else if store.isLoading {
            MaterialsLoadingView()
        } else if store.isEmpty {
            ScrollView { emptyState.padding(16) }
                .scrollIndicators(.hidden)
        } else {
            results
        }
    }

    private var refineButton: some View {
        Button {
            store.send(.binding(.set(\.isRefinePresented, true)))
        } label: {
            Image(systemName: "line.3.horizontal.decrease")
                .fontWeight(.semibold)
                .overlay(alignment: .topTrailing) {
                    if store.activeFacetCount > 0 {
                        Text(String(store.activeFacetCount))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 4)
                            .frame(minWidth: 15, minHeight: 15)
                            .background(UNESColor.accent, in: Capsule())
                            .offset(x: 9, y: -7)
                    }
                }
        }
        .tint(store.activeFacetCount > 0 ? UNESColor.accent : UNESColor.ink2)
    }

    // MARK: Results

    /// Rows are individual lazy items — the card chrome is painted per row so
    /// a hundred-work result set only materializes what is on screen, and each
    /// row's appearance is what triggers its availability consultation.
    private var results: some View {
        ScrollView {
            LazyVStack(spacing: 0) {
                header
                    .fadeUp(delay: 0.02)
                    .padding(.bottom, 12)

                if let banner = degradationBanner {
                    banner
                        .fadeUp(delay: 0.06)
                        .padding(.bottom, 12)
                }

                chipRow
                    .fadeUp(delay: 0.08)
                    .padding(.bottom, 14)

                if store.filtered.isEmpty {
                    filteredEmpty
                        .fadeUp(delay: 0.12)
                } else {
                    groupRows

                    if store.hasMore {
                        loadMoreRow
                    } else {
                        endLine
                    }
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 32, trailing: 16))
        }
        .scrollIndicators(.hidden)
    }

    @ViewBuilder
    private var groupRows: some View {
        ForEach(Array(store.groups.enumerated()), id: \.offset) { groupIndex, group in
            if let type = group.type {
                groupHeader(type: type, count: group.works.count)
                    .padding(.top, groupIndex == 0 ? 0 : 18)
            }
            ForEach(Array(group.works.enumerated()), id: \.element.id) { index, work in
                LibraryResultRow(
                    work: work,
                    reading: store.readings[work.id],
                    now: store.now
                ) {
                    store.send(.workTapped(work))
                }
                .cardSegment(isFirst: index == 0, isLast: index == group.works.count - 1)
                .onAppear { store.send(.rowAppeared(work.id)) }
            }
        }
    }

    private func groupHeader(type: LibraryWorkType, count: Int) -> some View {
        HStack(spacing: 8) {
            Text(type.pluralLabel)
                .textCase(.uppercase)
                .font(.system(size: 13, weight: .bold))
                .tracking(0.26)
                .foregroundStyle(UNESColor.ink3)
            Text(String(count))
                .font(.system(size: 12.5, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 0.5)
        }
        .padding(EdgeInsets(top: 0, leading: 2, bottom: 8, trailing: 0))
    }

    /// The next-page sentinel: materializing it is the "reached the bottom"
    /// signal.
    private var loadMoreRow: some View {
        ProgressView()
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .onAppear { store.send(.loadMoreReached) }
    }

    private var endLine: some View {
        Text(store.displayCount == 1
            ? .libraryResultsEndOne(store.displayCount)
            : .libraryResultsEndOther(store.displayCount))
            .font(.system(size: 12, weight: .medium))
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink4)
            .frame(maxWidth: .infinity)
            .padding(.top, 16)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 9) {
            HStack(alignment: .firstTextBaseline, spacing: 8) {
                Text(LibraryFormat.count(store.displayCount))
                    .font(.system(size: 26, weight: .bold))
                    .tracking(-1.04)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                Text(headerSuffix)
                    .font(.system(size: 15, weight: .medium))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
            LibraryFreshnessStamp(reading: store.aggregatedReading, now: store.now) {
                store.send(.refreshTapped)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
    }

    private var headerSuffix: String {
        let works = String.localized(store.displayCount == 1 ? .libraryResultsWorkOne : .libraryResultsWorkOther)
        let context = String.localized(store.activeFacetCount > 0
            ? .libraryResultsSuffixFiltered
            : .libraryResultsSuffixQuery(store.query))
        return "\(works) \(context)"
    }

    /// The availability degradation banner — catalogue stays trustworthy,
    /// the copy counts don't.
    private var degradationBanner: LibraryNote? {
        switch store.aggregatedReading {
        case .unavailable:
            LibraryNote(icon: "wifi.slash", tone: LibraryTone.none, .libraryResultsDownBanner)
        case let .stale(checkedAt):
            LibraryNote(
                icon: "hourglass",
                tone: LibraryTone.loan,
                text: Text(.libraryResultsStaleBanner(LibraryFormat.ago(from: checkedAt, to: store.now)))
            )
        default:
            nil
        }
    }

    // MARK: Sort + facet chips

    private var chipRow: some View {
        LibraryChipRow {
            sortChip
            ForEach(activeFacetChips, id: \.self) { chip in
                LibraryChip(isActive: true, tone: chip.tone, onTap: {
                    store.send(.facetToggled(chip.group, chip.key))
                }) {
                    Text(chip.label)
                    Image(systemName: "xmark")
                        .font(.system(size: 9, weight: .bold))
                }
            }
            if store.activeFacetCount > 1 {
                Button {
                    store.send(.clearFacetsTapped)
                } label: {
                    Text(.libraryRefineClear)
                        .font(.system(size: 13.5, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                        .frame(height: 34)
                        .padding(.horizontal, 8)
                }
                .buttonStyle(.plain)
            }
        }
    }

    /// `Menu` doesn't exist on watchOS; the screen is never mounted there,
    /// so the fallback just cycles through the sorts.
    @ViewBuilder
    private var sortChip: some View {
        #if os(watchOS)
        Button {
            let all = LibrarySort.allCases
            let index = all.firstIndex(of: store.sort) ?? 0
            store.send(.binding(.set(\.sort, all[(index + 1) % all.count])))
        } label: {
            sortChipLabel
        }
        .buttonStyle(.plain)
        #else
        Menu {
            Picker(selection: $store.sort) {
                ForEach(LibrarySort.allCases, id: \.self) { sort in
                    Text(sort.label).tag(sort)
                }
            } label: {
                EmptyView()
            }
        } label: {
            sortChipLabel
        }
        #endif
    }

    private var sortChipLabel: some View {
        HStack(spacing: 6) {
            Image(systemName: "arrow.up.arrow.down")
                .font(.system(size: 11, weight: .semibold))
            Text(store.sort.label)
        }
        .font(.system(size: 13.5, weight: .semibold))
        .tracking(-0.14)
        .foregroundStyle(UNESColor.ink2)
        .padding(.horizontal, 14)
        .frame(height: 34)
        .background(UNESColor.card, in: Capsule())
        .overlay {
            Capsule().strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.04), radius: 4, y: 2)
    }

    private struct FacetChip: Hashable {
        var group: LibraryFacetGroup
        var key: String
        var label: String
        var toneHex: UInt32?

        var tone: Color { toneHex.map { UNESColor.readable($0) } ?? UNESColor.ink }
    }

    private var activeFacetChips: [FacetChip] {
        LibraryFacetGroup.allCases.flatMap { group -> [FacetChip] in
            let values = store.state.facetValues(for: group)
            return (store.facets[group] ?? []).sorted().map { key in
                FacetChip(
                    group: group,
                    key: key,
                    label: values.first { $0.key == key }?.label ?? key,
                    toneHex: nil
                )
            }
        }
    }

    private var filteredEmpty: some View {
        VStack(spacing: 10) {
            Text(.libraryResultsFilteredEmpty)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button {
                store.send(.clearFacetsTapped)
            } label: {
                Text(.libraryRefineClear)
                    .font(.system(size: 14.5, weight: .semibold))
                    .foregroundStyle(UNESColor.accent)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(26)
        .materialsCard()
    }

    // MARK: Failure states

    /// Pergamum times out on queries it would have to scan the whole
    /// catalogue for — narrow instead of waiting 30 s for nothing.
    private var broadState: some View {
        LibraryStateCard(
            icon: "magnifyingglass",
            tone: LibraryTone.loan,
            title: Text(.libraryResultsBroadTitle),
            body_: Text(.libraryResultsBroadBody)
        ) {
            LibrarySuggestionRow(
                icon: "person",
                label: .libraryResultsSuggestAuthor,
                hint: .libraryResultsSuggestAuthorHint
            ) {
                store.send(.editQueryTapped)
            }
            LibrarySuggestionRow(
                icon: "text.magnifyingglass",
                label: .libraryResultsSuggestSubject,
                hint: .libraryResultsSuggestSubjectHint
            ) {
                store.send(.editQueryTapped)
            }
        }
    }

    private var emptyState: some View {
        LibraryStateCard(
            icon: "magnifyingglass",
            tone: UNESColor.ink3,
            title: Text(.libraryResultsEmptyTitle(store.query)),
            body_: Text(.libraryResultsEmptyBody)
        ) {
            if store.canBroadenScope {
                LibrarySuggestionRow(
                    icon: "square.grid.2x2",
                    label: .libraryResultsSuggestAllFields,
                    hint: .libraryResultsSuggestAllFieldsHint
                ) {
                    store.send(.broadenScopeTapped)
                }
            }
            LibrarySuggestionRow(
                icon: "books.vertical",
                label: .libraryResultsSuggestCallNumber,
                hint: .libraryResultsSuggestCallNumberHint
            ) {
                store.send(.editQueryTapped)
            }
        }
    }
}

extension LibrarySort {
    var label: LocalizedStringResource {
        switch self {
        case .relevance: .librarySortRelevance
        case .newest: .librarySortNewest
        case .oldest: .librarySortOldest
        case .titleAZ: .librarySortTitleAZ
        }
    }
}

// MARK: - Card segments

extension View {
    /// One row of a results card. The card chrome (fill, hairline, corners)
    /// is painted per row so rows can be independent lazy items instead of
    /// children of one giant card view.
    func cardSegment(isFirst: Bool, isLast: Bool) -> some View {
        background(UNESColor.card)
            .clipShape(CardSegmentShape(isFirst: isFirst, isLast: isLast))
            .overlay(alignment: .top) {
                if !isFirst {
                    Divider()
                        .overlay(UNESColor.line)
                        .padding(.leading, 82)
                }
            }
            .overlay {
                CardSegmentBorder(isFirst: isFirst, isLast: isLast)
                    .stroke(UNESColor.cardLine, lineWidth: 1)
            }
    }
}

private let cardSegmentRadius: CGFloat = 20

/// The slice of the card a single row owns — corners only on the outer rows.
private struct CardSegmentShape: Shape {
    var isFirst: Bool
    var isLast: Bool

    func path(in rect: CGRect) -> Path {
        UnevenRoundedRectangle(
            cornerRadii: RectangleCornerRadii(
                topLeading: isFirst ? cardSegmentRadius : 0,
                bottomLeading: isLast ? cardSegmentRadius : 0,
                bottomTrailing: isLast ? cardSegmentRadius : 0,
                topTrailing: isFirst ? cardSegmentRadius : 0
            ),
            style: .continuous
        ).path(in: rect)
    }
}

/// The card hairline as one row sees it: the sides, plus the rounded top or
/// bottom cap on the outer rows — never a line where two rows meet.
private struct CardSegmentBorder: Shape {
    var isFirst: Bool
    var isLast: Bool

    func path(in rect: CGRect) -> Path {
        let radius = cardSegmentRadius
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.minY + (isFirst ? radius : 0)))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY - (isLast ? radius : 0)))
        if isLast {
            path.addQuadCurve(
                to: CGPoint(x: rect.minX + radius, y: rect.maxY),
                control: CGPoint(x: rect.minX, y: rect.maxY)
            )
            path.addLine(to: CGPoint(x: rect.maxX - radius, y: rect.maxY))
            path.addQuadCurve(
                to: CGPoint(x: rect.maxX, y: rect.maxY - radius),
                control: CGPoint(x: rect.maxX, y: rect.maxY)
            )
        } else {
            path.move(to: CGPoint(x: rect.maxX, y: rect.maxY))
        }
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY + (isFirst ? radius : 0)))
        if isFirst {
            path.addQuadCurve(
                to: CGPoint(x: rect.maxX - radius, y: rect.minY),
                control: CGPoint(x: rect.maxX, y: rect.minY)
            )
            path.addLine(to: CGPoint(x: rect.minX + radius, y: rect.minY))
            path.addQuadCurve(
                to: CGPoint(x: rect.minX, y: rect.minY + radius),
                control: CGPoint(x: rect.minX, y: rect.minY)
            )
        }
        return path
    }
}

// MARK: - Result row

struct LibraryResultRow: View {
    var work: LibraryWork
    var reading: LibraryReading?
    var now: Date
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .top, spacing: 13) {
                LibraryWorkMark(work: work)
                VStack(alignment: .leading, spacing: 4) {
                    metaLine
                    Text(work.parsedTitle.title)
                        .font(.system(size: 15.5, weight: .semibold))
                        .tracking(-0.47)
                        .lineSpacing(1)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    if let subtitle = work.parsedTitle.subtitle {
                        Text(subtitle)
                            .font(.system(size: 12.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .lineLimit(1)
                    }
                    Text(authorLine)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                    LibraryAvailabilityLine(work: work, reading: reading, now: now)
                        .padding(.top, 2)
                }
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
    }

    private var metaLine: some View {
        HStack(spacing: 7) {
            LibraryTypeTag(type: work.type)
            switch work.year {
            case let .year(text):
                Text(text)
                    .font(.system(size: 11, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
            case .illegible:
                Text(.libraryResultsYearIllegible)
                    .font(.system(size: 11, weight: .semibold))
                    .italic()
                    .foregroundStyle(UNESColor.ink4)
            case .none:
                EmptyView()
            }
            if let edition = work.edition {
                Text(verbatim: "· \(edition)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
        }
    }

    private var authorLine: String {
        guard !work.authors.isEmpty else { return .localized(.libraryAuthorUnknown) }
        let shown = work.authors.prefix(2).joined(separator: " · ")
        let extra = work.authors.count - 2
        return extra > 0 ? "\(shown) +\(extra)" : shown
    }
}

#Preview("Resultados") {
    NavigationStack {
        LibraryResultsView(
            store: Store(initialState: LibraryResultsFeature.State(query: "cálculo", scope: .all)) {
                LibraryResultsFeature()
            }
        )
    }
}

#Preview("Busca ampla") {
    NavigationStack {
        LibraryResultsView(
            store: Store(initialState: LibraryResultsFeature.State(query: "a", scope: .all)) {
                LibraryResultsFeature()
            }
        )
    }
}

#Preview("Vazio") {
    NavigationStack {
        LibraryResultsView(
            store: Store(
                initialState: LibraryResultsFeature.State(query: "cálculo aplicado a jogos", scope: .title)
            ) {
                LibraryResultsFeature()
            }
        )
    }
}

#Preview("Fora do ar") {
    NavigationStack {
        LibraryResultsView(
            store: Store(initialState: LibraryResultsFeature.State(query: "cálculo", scope: .all)) {
                LibraryResultsFeature()
            } withDependencies: {
                $0.libraryRepository.checkAvailability = { _ in
                    LibraryAvailabilitySnapshot(reading: .unavailable, copies: [])
                }
            }
        )
    }
}
