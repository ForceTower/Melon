import ComposableArchitecture
import SwiftUI

struct CurriculumFlowView: View {
    @Bindable var store: StoreOf<CurriculumFlowFeature>

    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()
            content
        }
        .navigationTitle(Text(.courseProgressFlowchartTitle))
        .inlineNavigationBar()
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .task { await store.send(.task).finish() }
        .sheet(isPresented: sheetBinding) {
            if let entry = store.presentedEntry {
                CurriculumEntrySheet(progress: store.progress, entry: entry) { code in
                    store.send(.trailRequested(code))
                }
            }
        }
    }

    private var sheetBinding: Binding<Bool> {
        Binding(
            get: { store.presentedEntryCode != nil },
            set: { if !$0 { store.send(.entrySheetDismissed) } }
        )
    }

    private var lensBinding: Binding<CurriculumFlowFeature.Lens> {
        Binding(get: { store.lens }, set: { store.send(.lensChanged($0)) })
    }

    private var periodBinding: Binding<Int> {
        Binding(get: { store.selectedPeriod }, set: { store.send(.periodSelected($0)) })
    }

    // MARK: Pinned header — lens picker, período rail, trail banner

    private var header: some View {
        VStack(spacing: 10) {
            Picker(String.localized(.courseProgressLensPicker), selection: lensBinding) {
                ForEach(CurriculumFlowFeature.Lens.allCases, id: \.self) { lens in
                    Text(lens.label).tag(lens)
                }
            }
            .segmentedPickerCompat()
            .padding(.horizontal, 16)

            if store.lens == .periods {
                CurriculumPeriodRail(
                    periods: store.periods,
                    currentPeriod: store.progress.currentPeriod,
                    selected: periodBinding
                )
            }

            if let trail = store.trail {
                trailBanner(trail)
                    .padding(.horizontal, 16)
            }
        }
        .padding(.vertical, 10)
        .background(UNESColor.surface)
        .overlay(alignment: .bottom) {
            CardRowDivider()
        }
        .animation(UNESMotion.ease(0.25), value: store.lens)
        .animation(UNESMotion.ease(0.25), value: store.trail == nil)
    }

    private func trailBanner(_ trail: CurriculumFlowFeature.Trail) -> some View {
        Button {
            store.send(.trailCleared)
        } label: {
            HStack(spacing: 8) {
                Image(systemName: "point.3.connected.trianglepath.dotted")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.tealReadable)
                Text(.courseProgressTrailBanner(trail.focus, trail.codes.count))
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink2)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Text(.courseProgressTrailClear)
                    .font(.system(size: 11.5, weight: .semibold))
                    .foregroundStyle(UNESColor.tealReadable)
            }
            .padding(EdgeInsets(top: 8, leading: 11, bottom: 8, trailing: 11))
            .background(UNESColor.tealReadable.opacity(0.1), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(UNESColor.tealReadable.opacity(0.3))
            }
        }
        .buttonStyle(.plain)
    }

    // MARK: Lenses

    @ViewBuilder
    private var content: some View {
        switch store.lens {
        case .periods:
            periodsLens
        case .map:
            mapLens
        case .grid:
            gridLens
        }
    }

    /// One page per período; the rail and the swipe share the selection.
    private var periodsLens: some View {
        TabView(selection: periodBinding) {
            ForEach(store.periods) { period in
                if let number = period.period {
                    ScrollView {
                        CurriculumPeriodPage(
                            period: period,
                            next: store.progress.period(number + 1),
                            isCurrent: number == store.progress.currentPeriod,
                            trail: store.trail?.codes,
                            onOpen: { store.send(.entryTapped($0)) },
                            onNext: { store.send(.periodSelected(number + 1)) }
                        )
                        .padding(EdgeInsets(top: 14, leading: 16, bottom: 28, trailing: 16))
                    }
                    .scrollIndicators(.hidden)
                    .tag(number)
                }
            }
        }
        .pagedTabViewStyle()
    }

    private var mapLens: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                lensTitle(
                    .courseProgressMapTitle,
                    subtitle: .localized(.courseProgressMapSubtitle(
                        store.progress.summary.disciplinesTotal,
                        store.progress.summary.disciplinesCompleted,
                        store.periods.count
                    ))
                )
                CurriculumMap(
                    progress: store.progress,
                    trail: store.trail,
                    onOpen: { store.send(.entryTapped($0)) },
                    onPeriod: { store.send(.periodSelected($0)) }
                )
                .padding(EdgeInsets(top: 13, leading: 12, bottom: 11, trailing: 12))
                .courseProgressCard()

                Text(.courseProgressLegend)
                    .textCase(.uppercase)
                    .font(.system(size: 11.5, weight: .semibold))
                    .tracking(0.5)
                    .foregroundStyle(UNESColor.ink4)
                    .padding(EdgeInsets(top: 14, leading: 3, bottom: 9, trailing: 3))
                CurriculumLegend()

                Text(.courseProgressMapHint)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink4)
                    .padding(EdgeInsets(top: 14, leading: 3, bottom: 2, trailing: 3))
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 28, trailing: 16))
        }
        .scrollIndicators(.hidden)
    }

    private var gridLens: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                lensTitle(.courseProgressGridTitle, subtitle: .localized(.courseProgressGridSubtitle))
                    .padding(.horizontal, 19)
                ScrollView(.horizontal) {
                    HStack(alignment: .top, spacing: 8) {
                        ForEach(store.periods) { period in
                            CurriculumGridColumn(
                                period: period,
                                isCurrent: period.period == store.progress.currentPeriod,
                                trail: store.trail?.codes,
                                onOpen: { store.send(.entryTapped($0)) }
                            )
                        }
                    }
                    .padding(EdgeInsets(top: 0, leading: 16, bottom: 6, trailing: 16))
                }
                .scrollIndicators(.hidden)
                CurriculumLegend()
                    .padding(EdgeInsets(top: 10, leading: 19, bottom: 2, trailing: 19))
            }
            .padding(EdgeInsets(top: 14, leading: 0, bottom: 28, trailing: 0))
        }
        .scrollIndicators(.hidden)
    }

    private func lensTitle(_ title: LocalizedStringResource, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(.system(size: 24, weight: .bold))
                .tracking(-0.84)
                .foregroundStyle(UNESColor.ink)
            Text(subtitle)
                .font(.system(size: 12.5, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 2, leading: 3, bottom: 12, trailing: 3))
    }
}

extension CurriculumFlowFeature.Lens {
    var label: LocalizedStringResource {
        switch self {
        case .periods: .courseProgressLensPeriods
        case .map: .courseProgressLensMap
        case .grid: .courseProgressLensGrid
        }
    }
}

// MARK: - Período rail (index + panorama)

struct CurriculumPeriodRail: View {
    var periods: [CurriculumPeriod]
    var currentPeriod: Int?
    @Binding var selected: Int

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal) {
                HStack(spacing: 7) {
                    ForEach(periods) { period in
                        if let number = period.period {
                            chip(period, number: number)
                                .id(number)
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
            .scrollIndicators(.hidden)
            .onAppear { proxy.scrollTo(selected, anchor: .center) }
            .onChange(of: selected) { _, number in
                withAnimation(UNESMotion.ease(0.3)) {
                    proxy.scrollTo(number, anchor: .center)
                }
            }
        }
    }

    private func chip(_ period: CurriculumPeriod, number: Int) -> some View {
        let on = number == selected
        let done = period.entries.isEmpty ? 0 : Double(period.completedCount) / Double(period.entries.count)
        return Button {
            selected = number
        } label: {
            VStack(spacing: 5) {
                Text(CourseProgressFormat.ordinal(number))
                    .font(.system(size: 15, weight: .bold))
                    .monospacedDigit()
                    .tracking(-0.45)
                    .foregroundStyle(on ? UNESColor.surface : UNESColor.ink)
                GeometryReader { geo in
                    Capsule()
                        .fill(on ? UNESColor.surface.opacity(0.35) : UNESColor.surface3)
                        .overlay(alignment: .leading) {
                            Capsule()
                                .fill(on ? UNESColor.surface : UNESBannerTone.ok)
                                .frame(width: geo.size.width * done)
                        }
                }
                .frame(height: 3)
                Text(number == currentPeriod ? .courseProgressRailNow : .courseProgressRailCount(period.completedCount, period.entries.count))
                    .font(.system(size: 9.5, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(on ? UNESColor.surface.opacity(0.72) : number == currentPeriod ? UNESColor.accent : UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 7, leading: 5, bottom: 6, trailing: 5))
            .frame(width: 52)
            .background(on ? UNESColor.ink : UNESColor.card, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .strokeBorder(on ? UNESColor.ink : UNESColor.cardLine)
            }
        }
        .buttonStyle(.plain)
        .animation(UNESMotion.ease(0.18), value: on)
    }
}

// MARK: - Lens 1 · one page per período

struct CurriculumPeriodPage: View {
    var period: CurriculumPeriod
    var next: CurriculumPeriod?
    var isCurrent: Bool
    var trail: Set<String>?
    var onOpen: (String) -> Void
    var onNext: () -> Void

    private var ordered: [CurriculumEntry] {
        period.entries.sorted { lhs, rhs in
            lhs.status.displayRank != rhs.status.displayRank
                ? lhs.status.displayRank < rhs.status.displayRank
                : false
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 5) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(CourseProgressFormat.semester(period.period ?? 0))
                        .font(.system(size: 24, weight: .bold))
                        .tracking(-0.84)
                        .foregroundStyle(UNESColor.ink)
                    if isCurrent {
                        Text(.courseProgressPeriodYours)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(UNESColor.accent)
                    }
                }
                Text(.courseProgressPeriodSubtitle(
                    period.entries.count,
                    CourseProgressFormat.hours(period.hours),
                    period.completedCount
                ))
                .font(.system(size: 12.5, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 2, leading: 3, bottom: 11, trailing: 3))

            VStack(spacing: 0) {
                ForEach(Array(ordered.enumerated()), id: \.element.id) { index, entry in
                    CurriculumEntryRow(
                        entry: entry,
                        dimmed: trail.map { !$0.contains(entry.code) } ?? false,
                        highlighted: trail?.contains(entry.code) ?? false
                    ) {
                        onOpen(entry.code)
                    }
                    if index < ordered.count - 1 {
                        CardRowDivider()
                    }
                }
            }
            .courseProgressCard()

            if let next, let number = next.period {
                Button(action: onNext) {
                    HStack(spacing: 9) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(CourseProgressFormat.semester(number))
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(UNESColor.ink)
                            Text(next.count(.available) > 0
                                ? .courseProgressNextAvailable(next.count(.available))
                                : .courseProgressNextNoneAvailable)
                                .font(.system(size: 11.5, weight: .medium))
                                .foregroundStyle(UNESColor.ink4)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                    }
                    .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
                    .overlay {
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(UNESColor.cardLine)
                    }
                }
                .buttonStyle(.plain)
                .padding(.top, 10)
            }

            Text(.courseProgressSwipeHint)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(maxWidth: .infinity)
                .padding(EdgeInsets(top: 13, leading: 0, bottom: 2, trailing: 0))
        }
    }
}

/// Full-width discipline row: badge, full portal name, code · hours · status.
struct CurriculumEntryRow: View {
    var entry: CurriculumEntry
    var dimmed = false
    var highlighted = false
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            HStack(alignment: .top, spacing: 12) {
                CurriculumStatusBadge(status: entry.status, size: 28)
                    .padding(.top, 1)
                VStack(alignment: .leading, spacing: 6) {
                    Text(entry.name)
                        .font(.system(size: 14.5, weight: .semibold))
                        .tracking(-0.17)
                        .foregroundStyle(UNESColor.ink)
                        .multilineTextAlignment(.leading)
                    HStack(spacing: 7) {
                        Text(entry.code)
                            .font(.system(size: 11, weight: .semibold))
                            .tracking(0.2)
                            .monospacedDigit()
                        Circle().fill(UNESColor.ink4.opacity(0.6)).frame(width: 3, height: 3)
                        Text(CourseProgressFormat.hours(entry.hours))
                            .font(.system(size: 11, weight: .semibold))
                            .monospacedDigit()
                        CurriculumStatusChip(
                            status: entry.status,
                            full: entry.status == .blocked || entry.status == .withdrawn,
                            small: true
                        )
                    }
                    .foregroundStyle(UNESColor.ink4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
                    .padding(.top, 6)
            }
            .padding(EdgeInsets(top: 12, leading: 14, bottom: 13, trailing: 14))
            .background(highlighted ? entry.status.tone.opacity(0.06) : .clear)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .opacity(dimmed ? 0.34 : 1)
        .animation(UNESMotion.ease(0.25), value: dimmed)
    }
}

// MARK: - Lens 2 · whole-course map

struct CurriculumMap: View {
    var progress: CourseProgress
    var trail: CurriculumFlowFeature.Trail?
    var onOpen: (String) -> Void
    var onPeriod: (Int) -> Void

    private static let tile: CGFloat = 28

    private var maxRows: Int {
        progress.scheduledPeriods.map(\.entries.count).max() ?? 0
    }

    var body: some View {
        HStack(alignment: .top, spacing: 4) {
            ForEach(progress.scheduledPeriods) { period in
                VStack(spacing: 4) {
                    Button {
                        if let number = period.period { onPeriod(number) }
                    } label: {
                        Text(CourseProgressFormat.ordinal(period.period ?? 0))
                            .font(.system(size: 10.5, weight: .bold))
                            .monospacedDigit()
                            .foregroundStyle(period.period == progress.currentPeriod ? UNESColor.accent : UNESColor.ink4)
                            .frame(width: Self.tile)
                            .padding(.bottom, 2)
                    }
                    .buttonStyle(.plain)
                    ForEach(period.entries) { entry in
                        CurriculumMapTile(
                            entry: entry,
                            size: Self.tile,
                            dimmed: trail.map { !$0.codes.contains(entry.code) } ?? false,
                            ringed: trail?.focus == entry.code
                        ) {
                            onOpen(entry.code)
                        }
                    }
                    ForEach(0..<max(0, maxRows - period.entries.count), id: \.self) { _ in
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .strokeBorder(UNESColor.line, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
                            .frame(width: Self.tile, height: Self.tile)
                            .opacity(0.5)
                    }
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

/// A discipline as a single glyph tile — the map density.
struct CurriculumMapTile: View {
    var entry: CurriculumEntry
    var size: CGFloat
    var dimmed = false
    var ringed = false
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            CurriculumStatusBadge(status: entry.status, size: size, cornerRadius: 8)
                .overlay {
                    if ringed {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .strokeBorder(entry.status.tone, lineWidth: 2)
                            .padding(-3)
                    }
                }
        }
        .buttonStyle(TilePressStyle())
        .opacity(dimmed ? 0.22 : 1)
        .animation(UNESMotion.ease(0.25), value: dimmed)
        .accessibilityLabel(Text(verbatim: "\(entry.code) \(entry.name)"))
        .accessibilityValue(Text(entry.status.label))
    }
}

// MARK: - Lens 3 · side-by-side grid

struct CurriculumGridColumn: View {
    var period: CurriculumPeriod
    var isCurrent: Bool
    var trail: Set<String>?
    var onOpen: (String) -> Void

    private static let width: CGFloat = 156

    var body: some View {
        VStack(spacing: 7) {
            HStack(spacing: 8) {
                Text(CourseProgressFormat.semester(period.period ?? 0))
                    .font(.system(size: 13, weight: .bold))
                    .tracking(-0.26)
                    .monospacedDigit()
                Spacer(minLength: 0)
                Text(CourseProgressFormat.hours(period.hours))
                    .font(.system(size: 10.5, weight: .semibold))
                    .monospacedDigit()
                    .opacity(0.7)
            }
            .foregroundStyle(isCurrent ? UNESColor.surface : UNESColor.ink2)
            .padding(EdgeInsets(top: 7, leading: 10, bottom: 7, trailing: 10))
            .frame(width: Self.width)
            .background(isCurrent ? UNESColor.ink : UNESColor.surface2, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
            .padding(.bottom, 6)

            ForEach(period.entries) { entry in
                CurriculumGridCell(
                    entry: entry,
                    width: Self.width,
                    dimmed: trail.map { !$0.contains(entry.code) } ?? false
                ) {
                    onOpen(entry.code)
                }
            }
        }
    }
}

/// A discipline as a compact cell — the grid density.
struct CurriculumGridCell: View {
    var entry: CurriculumEntry
    var width: CGFloat
    var dimmed = false
    var onOpen: () -> Void

    var body: some View {
        Button(action: onOpen) {
            VStack(alignment: .leading, spacing: 7) {
                HStack(spacing: 6) {
                    Text(entry.code)
                        .font(.system(size: 10.5, weight: .bold))
                        .tracking(0.3)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                    Spacer(minLength: 0)
                    CurriculumStatusBadge(status: entry.status, size: 19, cornerRadius: 6)
                }
                Text(entry.name)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(-0.12)
                    .foregroundStyle(UNESColor.ink)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                HStack(spacing: 6) {
                    Text(CourseProgressFormat.hours(entry.hours))
                        .font(.system(size: 10.5, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                    Spacer(minLength: 0)
                    Text(entry.status.shortLabel)
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(entry.status.isNeutral ? UNESColor.ink4 : entry.status.tone)
                }
            }
            .padding(EdgeInsets(top: 9, leading: 10, bottom: 10, trailing: 10))
            .frame(width: width, alignment: .topLeading)
            .background(entry.status.fillOpacity > 0 ? entry.status.tone.opacity(entry.status.fillOpacity) : UNESColor.card)
            .overlay {
                if entry.status == .withdrawn {
                    DiagonalHatch(color: entry.status.tone.opacity(0.13))
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .modifier(CurriculumStatusBorder(status: entry.status, cornerRadius: 14))
        }
        .buttonStyle(TilePressStyle())
        .opacity(dimmed ? 0.3 : 1)
        .animation(UNESMotion.ease(0.25), value: dimmed)
    }
}

// MARK: - Previews

#Preview("Fluxograma") {
    NavigationStack {
        CurriculumFlowView(
            store: Store(initialState: CurriculumFlowFeature.State(progress: .preview())) {
                CurriculumFlowFeature()
            }
        )
    }
}

#Preview("Mapa com trilha") {
    NavigationStack {
        CurriculumFlowView(
            store: Store(initialState: {
                var state = CurriculumFlowFeature.State(progress: .preview())
                state.lens = .map
                state.trail = .init(focus: "CHF344", codes: state.progress.trail(through: "CHF344"))
                return state
            }()) {
                CurriculumFlowFeature()
            }
        )
    }
}

#Preview("Grade") {
    NavigationStack {
        CurriculumFlowView(
            store: Store(initialState: {
                var state = CurriculumFlowFeature.State(progress: .preview())
                state.lens = .grid
                return state
            }()) {
                CurriculumFlowFeature()
            }
        )
    }
}
