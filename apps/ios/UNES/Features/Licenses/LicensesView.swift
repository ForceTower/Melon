import SwiftUI

/// "Licenças open source" — full list of bundled third-party licenses.
///
/// Data comes from `com.mono0926.LicensePlist.plist`, generated at build time
/// by the `license-plist` Run Script phase. The plist gives us a title, an
/// SPDX-ish identifier, and the full license body — no version, author,
/// category, or homepage. The handoff design (`UNES Licenses.html`) was drawn
/// against a richer dataset, so we render the editorial chrome it specifies
/// but scope the row content down to what the plist actually carries:
/// title + license chip + chevron, tap to push the full text in
/// `LicenseDetailView`.
///
/// Layout follows `screens-licenses.jsx` end to end: a warm mesh wash behind
/// the header, the editorial title, a distribution summary computed from the
/// real entries, the tribute card, search + license-family filter chips, then
/// the rows grouped by license, then a closing signature.
struct LicensesView: View {
    /// Snapshot of the bundled licenses, taken once on view creation. The
    /// plist is a build artifact — it can't change between view appearances
    /// inside a single launch — so there's no flow/observer to subscribe to.
    private let entries: [LicenseEntry]

    @State private var query: String = ""
    @State private var filter: LicenseFilter = .all
    @FocusState private var searchFocused: Bool

    init(entries: [LicenseEntry]? = nil) {
        self.entries = entries ?? LicensePlistLoader.load()
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()

            // Same warm mesh treatment Settings/FinalCountdown use — pinned
            // behind the header, fading into the surface so the editorial
            // type reads cleanly below.
            VStack(spacing: 0) {
                ZStack {
                    MeshGradientView(variant: .warm, intensity: 0.5)
                        .opacity(0.22)
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0.5),
                            .init(color: UNESColor.surface, location: 1),
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                }
                .frame(height: 300)
                Spacer(minLength: 0)
            }
            .ignoresSafeArea(edges: .top)
            .allowsHitTesting(false)

            ScrollView(showsIndicators: false) {
                VStack(spacing: 14) {
                    LicensesEditorialHeader(totalPackages: entries.count)
                        .fadeUpOnAppear(delay: 0.02, distance: 12, duration: 0.55)

                    LicensesSummaryCard(breakdown: breakdown)
                        .fadeUpOnAppear(delay: 0.08, distance: 12, duration: 0.55)

                    LicensesTributeCard()
                        .fadeUpOnAppear(delay: 0.14, distance: 12, duration: 0.55)

                    LicensesSearchBar(
                        query: $query,
                        focused: $searchFocused
                    )
                    .fadeUpOnAppear(delay: 0.18, distance: 12, duration: 0.55)

                    LicensesFilterChipsRow(
                        breakdown: breakdown,
                        filter: $filter
                    )
                    .fadeUpOnAppear(delay: 0.22, distance: 12, duration: 0.55)

                    groupedSection
                        .fadeUpOnAppear(delay: 0.26, distance: 12, duration: 0.55)

                    LicensesSignature()
                        .fadeUpOnAppear(delay: 0.32, distance: 12, duration: 0.55)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        // Match Settings: keep the system back chevron and pop gesture, only
        // hide the bar's background so the warm mesh continues behind the
        // header.
        .toolbarBackground(.hidden, for: .navigationBar)
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(for: LicenseEntry.self) { entry in
            LicenseDetailView(entry: entry)
        }
    }

    // MARK: - Derived data

    /// Per-family counts ordered most-common first. Drives the stacked bar,
    /// the legend grid, the filter chips, and the row grouping — keeping
    /// every bit of license chrome on the screen in the same order.
    private var breakdown: [LicenseBreakdown] {
        var counts: [LicenseFamily: Int] = [:]
        for entry in entries {
            counts[LicenseFamily(identifier: entry.identifier), default: 0] += 1
        }
        return counts
            .map { LicenseBreakdown(family: $0.key, count: $0.value) }
            .sorted { lhs, rhs in
                if lhs.count != rhs.count { return lhs.count > rhs.count }
                return lhs.family.displayName.localizedCaseInsensitiveCompare(rhs.family.displayName) == .orderedAscending
            }
    }

    private var filteredEntries: [LicenseEntry] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return entries.filter { entry in
            if case .family(let family) = filter,
               LicenseFamily(identifier: entry.identifier) != family {
                return false
            }
            guard !needle.isEmpty else { return true }
            if entry.title.lowercased().contains(needle) { return true }
            if let id = entry.identifier?.lowercased(), id.contains(needle) { return true }
            return false
        }
    }

    private var groupedFiltered: [LicenseGroup] {
        let filtered = filteredEntries
        var grouped: [LicenseFamily: [LicenseEntry]] = [:]
        for entry in filtered {
            grouped[LicenseFamily(identifier: entry.identifier), default: []].append(entry)
        }
        for family in grouped.keys {
            grouped[family]?.sort {
                $0.title.localizedCaseInsensitiveCompare($1.title) == .orderedAscending
            }
        }
        return breakdown
            .compactMap { row in
                guard let items = grouped[row.family], !items.isEmpty else { return nil }
                return LicenseGroup(family: row.family, items: items)
            }
    }

    @ViewBuilder
    private var groupedSection: some View {
        if entries.isEmpty {
            emptyBuildArtifactCard
        } else if groupedFiltered.isEmpty {
            emptySearchCard
        } else {
            VStack(spacing: 18) {
                ForEach(groupedFiltered, id: \.family) { group in
                    LicensesGroupCard(family: group.family, items: group.items)
                }
            }
        }
    }

    /// Card shown before the build artifact has been generated (e.g. a fresh
    /// checkout that hasn't run the Run Script phase yet). Keeps the screen
    /// from looking broken in that window.
    private var emptyBuildArtifactCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Sem licenças bundladas")
                .font(UNESFont.serif(18))
                .tracking(-0.18)
                .foregroundStyle(UNESColor.ink)

            Text("Rode o build uma vez para gerar com.mono0926.LicensePlist.plist em UNES/Resources/Licenses/.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var emptySearchCard: some View {
        VStack(spacing: 6) {
            Text("nada por aqui")
                .font(UNESFont.serif(20, italic: true))
                .tracking(-0.2)
                .foregroundStyle(UNESColor.ink)

            Text("TENTE OUTRA BUSCA OU FILTRO")
                .font(UNESFont.mono(10))
                .tracking(1.0)
                .foregroundStyle(UNESColor.ink4)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
        .padding(.horizontal, 16)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

// MARK: - Filter selection

/// Either "todos" or one specific license family. Modeled as a value type so
/// the filter chip row can drive `@State` directly without sentinel strings.
private enum LicenseFilter: Hashable {
    case all
    case family(LicenseFamily)
}

// MARK: - License families

/// Canonical license family. The plist's `License` field is loosely SPDX-ish
/// — variants like "Apache 2.0", "BSD 3-Clause", or just "MIT License" all
/// show up in the wild — so we collapse them into the small palette the
/// design speaks (`MIT`, `Apache-2.0`, …) plus an `other` bucket.
private enum LicenseFamily: Hashable {
    case mit
    case apache2
    case bsd3
    case bsd2
    case isc
    case mpl2
    case ccBy4
    case unlicense
    /// Anything we couldn't normalise — keeps the original string so the chip
    /// still shows whatever the upstream wrote.
    case other(String)

    init(identifier: String?) {
        guard let raw = identifier?.uppercased(), !raw.isEmpty else {
            self = .other("OUTRAS")
            return
        }
        let normalized = raw.replacingOccurrences(of: " ", with: "-")
        if normalized.contains("APACHE") {
            self = .apache2
        } else if normalized.contains("MIT") {
            self = .mit
        } else if normalized.contains("BSD") && normalized.contains("3") {
            self = .bsd3
        } else if normalized.contains("BSD") {
            self = .bsd2
        } else if normalized.contains("ISC") {
            self = .isc
        } else if normalized.contains("MPL") || normalized.contains("MOZILLA") {
            self = .mpl2
        } else if normalized.contains("CC-BY") || normalized.contains("CREATIVE-COMMONS") {
            self = .ccBy4
        } else if normalized.contains("UNLICENSE") || normalized == "PUBLIC-DOMAIN" {
            self = .unlicense
        } else {
            self = .other(raw)
        }
    }

    var displayName: String {
        switch self {
        case .mit:        return "MIT"
        case .apache2:    return "Apache-2.0"
        case .bsd3:       return "BSD-3-Clause"
        case .bsd2:       return "BSD-2-Clause"
        case .isc:        return "ISC"
        case .mpl2:       return "MPL-2.0"
        case .ccBy4:      return "CC-BY-4.0"
        case .unlicense:  return "Unlicense"
        case .other(let raw): return raw
        }
    }

    /// Editorial blurb used under the group header chip. Mirrors the labels
    /// in `screens-licenses.jsx` (`LIC_META[*].blurb`).
    var blurb: String {
        switch self {
        case .mit:        return "permissiva · atribuição · sem garantia"
        case .apache2:    return "permissiva · patentes · atribuição"
        case .bsd3:       return "permissiva · atribuição · sem endosso"
        case .bsd2:       return "permissiva · atribuição"
        case .isc:        return "permissiva · simplificada"
        case .mpl2:       return "copyleft fraco · arquivo a arquivo"
        case .ccBy4:      return "creative commons · atribuição"
        case .unlicense:  return "domínio público · sem reserva"
        case .other:      return "licença"
        }
    }

    var tone: LicenseTone {
        switch self {
        case .mit:        return .amber
        case .apache2:    return .teal
        case .bsd3:       return .plum
        case .bsd2:       return .plum
        case .isc:        return .sage
        case .mpl2:       return .magenta
        case .ccBy4:      return .coral
        case .unlicense:  return .sage
        case .other:      return .plum
        }
    }
}

private struct LicenseBreakdown: Hashable {
    let family: LicenseFamily
    let count: Int
}

private struct LicenseGroup: Hashable {
    let family: LicenseFamily
    let items: [LicenseEntry]
}

// MARK: - Tone palette

/// The six-color palette `screens-licenses.jsx` uses for license families.
/// Foreground/background pairs are picked to read on both the light cream
/// surface and the dark plum surface without retuning per appearance.
private enum LicenseTone {
    case plum, magenta, teal, coral, amber, sage

    var background: Color {
        switch self {
        case .plum:    return Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
        case .magenta: return Color(red: 0xB2 / 255, green: 0x3A / 255, blue: 0x7A / 255)
        case .teal:    return Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255)
        case .coral:   return Color(red: 0xE8 / 255, green: 0x5D / 255, blue: 0x4E / 255)
        case .amber:   return Color(red: 0xF4 / 255, green: 0xA2 / 255, blue: 0x3C / 255)
        case .sage:    return Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255)
        }
    }

    var foreground: Color {
        switch self {
        case .plum:    return Color(red: 0xFB / 255, green: 0xD9 / 255, blue: 0xA8 / 255)
        case .amber:   return Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
        default:       return Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
        }
    }
}

// MARK: - Header

/// Top chrome: build/version meta on the right, eyebrow + serif title with
/// the second half italicised in the accent color, then a paragraph blurb.
/// Mirrors `LicHeader` in `screens-licenses.jsx`, with the fictional "build
/// 1842 · sbom v3" replaced by the real bundle build number — fake numbers
/// would rot the moment the next archive ships.
private struct LicensesEditorialHeader: View {
    let totalPackages: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Spacer()

                Text("◦ BUILD \(Bundle.main.buildNumber) · V\(Bundle.main.appVersion)")
                    .font(UNESFont.mono(9.5))
                    .tracking(1.33)
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(.bottom, 22)

            Text("◦ CRÉDITOS · \(totalPackages) PACOTES")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.44)
                .foregroundStyle(UNESColor.ink3)
                .padding(.bottom, 8)

            (
                Text("Licenças ").foregroundStyle(UNESColor.ink)
                + Text("open source").font(UNESFont.serif(40, italic: true)).foregroundStyle(UNESColor.accent)
            )
            .font(UNESFont.serif(40))
            .tracking(-0.8)
            .lineLimit(2)
            .minimumScaleFactor(0.7)

            Text("UNES é construído sobre o trabalho de centenas de pessoas que compartilham seu código abertamente. Aqui está quem.")
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink3)
                .lineSpacing(2)
                .padding(.top, 10)
                .frame(maxWidth: 300, alignment: .leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .padding(.top, 4)
    }
}

// MARK: - Summary card (distribution)

/// Distribution-by-license card — stacked bar on top, a two-column legend
/// underneath. Mirrors `LicSummary` in the JSX. Total and slices come from
/// the real plist; we drop the fictional "auditado · 22 abr" stamp since
/// there's no real audit date to surface.
private struct LicensesSummaryCard: View {
    let breakdown: [LicenseBreakdown]

    private var total: Int { breakdown.reduce(0) { $0 + $1.count } }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("◦ DISTRIBUIÇÃO")
                        .font(UNESFont.sans(10, weight: .medium))
                        .tracking(1.2)
                        .foregroundStyle(UNESColor.ink3)

                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text("\(total)")
                            .font(UNESFont.serif(22))
                            .tracking(-0.33)
                            .foregroundStyle(UNESColor.ink)
                        Text("pacotes diretos")
                            .font(UNESFont.serif(18, italic: true))
                            .tracking(-0.18)
                            .foregroundStyle(UNESColor.ink3)
                    }
                }

                Spacer()

                Text("◦ \(breakdown.count) FAMÍLIAS")
                    .font(UNESFont.mono(9.5))
                    .tracking(0.76)
                    .foregroundStyle(UNESColor.ink4)
            }

            stackedBar

            legend
        }
        .padding(.horizontal, 16)
        .padding(.top, 14)
        .padding(.bottom, 16)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var stackedBar: some View {
        GeometryReader { geo in
            HStack(spacing: 0) {
                ForEach(Array(breakdown.enumerated()), id: \.offset) { index, row in
                    let width = geo.size.width * CGFloat(row.count) / CGFloat(max(total, 1))
                    Rectangle()
                        .fill(row.family.tone.background)
                        .frame(width: max(width, 0))
                        .overlay(alignment: .trailing) {
                            if index < breakdown.count - 1 {
                                Rectangle()
                                    .fill(Color.white.opacity(0.08))
                                    .frame(width: 1)
                            }
                        }
                }
            }
        }
        .frame(height: 10)
        .background(UNESColor.surface2)
        .clipShape(RoundedRectangle(cornerRadius: 5, style: .continuous))
    }

    private var legend: some View {
        let columns = [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)]
        return LazyVGrid(columns: columns, alignment: .leading, spacing: 7) {
            ForEach(Array(breakdown.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 2, style: .continuous)
                        .fill(row.family.tone.background)
                        .frame(width: 7, height: 7)

                    Text(row.family.displayName)
                        .font(UNESFont.mono(10.5))
                        .foregroundStyle(UNESColor.ink2)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Text("\(row.count)")
                        .font(UNESFont.serif(14))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink)
                }
            }
        }
    }
}

// MARK: - Tribute card

/// Dark mesh card with the heart icon and the "obrigado coletivo" line.
/// Mirrors `LicTribute` — fixed dark surface in both appearances since the
/// rose mesh + amber accent only read on the deep plum background.
private struct LicensesTributeCard: View {
    var body: some View {
        ZStack {
            Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)

            MeshGradientView(variant: .rose, intensity: 0.6)
                .opacity(0.5)

            LinearGradient(
                colors: [
                    Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.4),
                    Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.85),
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(UNESColor.amber.opacity(0.2))
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(UNESColor.amber.opacity(0.3), lineWidth: 1)
                    Image(systemName: "heart")
                        .font(.system(size: 15, weight: .regular))
                        .foregroundStyle(UNESColor.amber)
                }
                .frame(width: 32, height: 32)

                VStack(alignment: .leading, spacing: 6) {
                    Text("◦ UM OBRIGADO COLETIVO")
                        .font(UNESFont.mono(9.5))
                        .tracking(1.33)
                        .foregroundStyle(Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.55))

                    (
                        Text("Nada disso aqui existiria sem quem ").foregroundStyle(Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255))
                        + Text("publicou seu código").font(UNESFont.serif(17, italic: true)).foregroundStyle(UNESColor.amber)
                        + Text(" de graça.").foregroundStyle(Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255))
                    )
                    .font(UNESFont.serif(17))
                    .tracking(-0.17)
                    .lineSpacing(3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 16)
        }
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.18), radius: 16, x: 0, y: 12)
    }
}

// MARK: - Search bar

private struct LicensesSearchBar: View {
    @Binding var query: String
    var focused: FocusState<Bool>.Binding

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 14, weight: .regular))
                .foregroundStyle(UNESColor.ink3)

            TextField("buscar por pacote ou licença", text: $query)
                .font(UNESFont.sans(13))
                .foregroundStyle(UNESColor.ink)
                .tint(UNESColor.accent)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused(focused)

            if !query.isEmpty {
                Button {
                    query = ""
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(4)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Limpar busca")
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
        .cardSurface(
            RoundedRectangle(cornerRadius: 14, style: .continuous),
            stroke: focused.wrappedValue ? UNESColor.ink : UNESColor.cardLine
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .animation(.easeOut(duration: 0.15), value: focused.wrappedValue)
    }
}

// MARK: - Filter chips

private struct LicensesFilterChipsRow: View {
    let breakdown: [LicenseBreakdown]
    @Binding var filter: LicenseFilter

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                LicensesFilterChip(
                    label: "todos",
                    isActive: filter == .all,
                    activeBackground: UNESColor.ink,
                    activeForeground: UNESColor.surface
                ) {
                    filter = .all
                }

                ForEach(breakdown, id: \.family) { row in
                    let active = filter == .family(row.family)
                    LicensesFilterChip(
                        label: row.family.displayName,
                        isActive: active,
                        activeBackground: row.family.tone.background,
                        activeForeground: row.family.tone.foreground
                    ) {
                        filter = active ? .all : .family(row.family)
                    }
                }
            }
            .padding(.horizontal, 2)
            .padding(.vertical, 2)
        }
    }
}

private struct LicensesFilterChip: View {
    let label: String
    let isActive: Bool
    let activeBackground: Color
    let activeForeground: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(UNESFont.mono(10, weight: .medium))
                .tracking(0.4)
                .foregroundStyle(isActive ? activeForeground : UNESColor.ink2)
                .padding(.horizontal, 11)
                .padding(.vertical, 6)
                .background(
                    Capsule().fill(isActive ? activeBackground : UNESColor.surface2)
                )
                .overlay(
                    Capsule().strokeBorder(
                        isActive ? activeBackground : UNESColor.line,
                        lineWidth: 1
                    )
                )
        }
        .buttonStyle(.plain)
        .animation(.easeOut(duration: 0.15), value: isActive)
    }
}

// MARK: - Group card

private struct LicensesGroupCard: View {
    let family: LicenseFamily
    let items: [LicenseEntry]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.horizontal, 4)
                .padding(.bottom, 10)

            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, entry in
                    NavigationLink(value: entry) {
                        LicensesListRow(entry: entry, family: family)
                    }
                    .buttonStyle(LicensesRowPressStyle())

                    if index < items.count - 1 {
                        Rectangle()
                            .fill(UNESColor.line)
                            .frame(height: 1)
                            .padding(.leading, 32)
                    }
                }
            }
            .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            LicensesFamilyChip(family: family, size: .medium)

            Text(family.blurb)
                .font(UNESFont.mono(9.5))
                .tracking(0.95)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text("\(items.count)")
                .font(UNESFont.serif(16))
                .tracking(-0.16)
                .foregroundStyle(UNESColor.ink)
        }
    }
}

// MARK: - Family chip

private struct LicensesFamilyChip: View {
    enum Size { case small, medium }
    let family: LicenseFamily
    var size: Size = .small

    var body: some View {
        let compact = size == .small
        Text(family.displayName)
            .font(UNESFont.mono(compact ? 9 : 10, weight: .medium))
            .tracking(0.4)
            .foregroundStyle(family.tone.foreground)
            .padding(.horizontal, compact ? 6 : 9)
            .padding(.vertical, compact ? 2 : 4)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(family.tone.background)
            )
            .lineLimit(1)
            .fixedSize()
    }
}

// MARK: - Single row

private struct LicensesListRow: View {
    let entry: LicenseEntry
    let family: LicenseFamily

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(family.tone.background)
                .opacity(0.85)
                .frame(width: 4)
                .frame(maxHeight: .infinity)
                .padding(.vertical, 2)

            VStack(alignment: .leading, spacing: 3) {
                Text(entry.title)
                    .font(UNESFont.mono(12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                    .truncationMode(.middle)

                Text(entry.identifier ?? "◦ ARQUIVO DE LICENÇA")
                    .font(UNESFont.mono(10))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            LicensesFamilyChip(family: family, size: .small)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 13)
        .frame(minHeight: 56)
        .contentShape(Rectangle())
    }
}

private struct LicensesRowPressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed
                    ? UNESColor.surface2.opacity(0.6)
                    : Color.clear
            )
    }
}

// MARK: - Footer signature

/// Closing signature — same vocabulary as `SettingsFooter` but scoped to
/// this screen. Replaces the design's "Baixar SBOM completo" pill since we
/// don't ship an SBOM artifact for users to download.
private struct LicensesSignature: View {
    var body: some View {
        VStack(spacing: 8) {
            HStack(spacing: 6) {
                Circle().fill(UNESColor.accent).frame(width: 5, height: 5)
                Text("unes")
                    .font(UNESFont.serif(16, italic: true))
                    .tracking(-0.16)
                    .foregroundStyle(UNESColor.ink2)
                Circle().fill(UNESColor.accent).frame(width: 5, height: 5)
            }

            Text("V\(Bundle.main.appVersion) · BUILD \(Bundle.main.buildNumber)")
                .font(UNESFont.mono(9))
                .tracking(1.62)
                .foregroundStyle(UNESColor.ink4)

            Text("EM CONFORMIDADE COM OS TERMOS\nDE CADA LICENÇA REPRODUZIDA ACIMA")
                .font(UNESFont.mono(9))
                .tracking(1.08)
                .foregroundStyle(UNESColor.ink4)
                .multilineTextAlignment(.center)
                .lineSpacing(3)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 14)
        .padding(.bottom, 12)
    }
}

// MARK: - Preview

#if DEBUG
    #Preview {
        NavigationStack {
            LicensesView(entries: [
                LicenseEntry(title: "Firebase", identifier: "Apache-2.0",
                             body: "Apache License\nVersion 2.0"),
                LicenseEntry(title: "leveldb", identifier: "BSD-3-Clause",
                             body: "Copyright (c) 2011 The LevelDB Authors."),
                LicenseEntry(title: "Promises", identifier: "Apache-2.0",
                             body: "Apache License\nVersion 2.0"),
                LicenseEntry(title: "KeychainAccess", identifier: "MIT",
                             body: "MIT License"),
                LicenseEntry(title: "SwiftSoup", identifier: "MIT",
                             body: "MIT License"),
                LicenseEntry(title: "Alamofire", identifier: "MIT",
                             body: "MIT License"),
                LicenseEntry(title: "lodash", identifier: "ISC",
                             body: "ISC License"),
                LicenseEntry(title: "fontawesome", identifier: "CC-BY-4.0",
                             body: "Creative Commons Attribution"),
                LicenseEntry(title: "rn-debounce", identifier: "Unlicense",
                             body: "This is free and unencumbered software"),
                LicenseEntry(title: "abseil", identifier: nil,
                             body: "License text only."),
            ])
        }
    }
#endif
