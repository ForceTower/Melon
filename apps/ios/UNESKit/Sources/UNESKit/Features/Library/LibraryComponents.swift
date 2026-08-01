import SwiftUI

// MARK: - Type metadata

extension LibraryWorkType {
    var label: LocalizedStringResource {
        switch self {
        case .book: .libraryTypeBook
        case .pamphlet: .libraryTypePamphlet
        case .cordel: .libraryTypeCordel
        case .educationalProduct: .libraryTypeProduct
        case .dissertation: .libraryTypeDissertation
        case .thesis: .libraryTypeThesis
        case .article: .libraryTypeArticle
        case .periodical: .libraryTypePeriodical
        }
    }

    var pluralLabel: LocalizedStringResource {
        switch self {
        case .book: .libraryTypeBookPlural
        case .pamphlet: .libraryTypePamphletPlural
        case .cordel: .libraryTypeCordelPlural
        case .educationalProduct: .libraryTypeProductPlural
        case .dissertation: .libraryTypeDissertationPlural
        case .thesis: .libraryTypeThesisPlural
        case .article: .libraryTypeArticlePlural
        case .periodical: .libraryTypePeriodicalPlural
        }
    }

    var icon: String {
        switch self {
        case .book: "book"
        case .pamphlet: "doc.text"
        case .cordel: "scroll"
        case .educationalProduct: "shippingbox"
        case .dissertation, .thesis: "graduationcap"
        case .article: "doc.richtext"
        case .periodical: "newspaper"
        }
    }

    /// Books are the overwhelming majority, so they read neutral; the
    /// regional/local tail gets the accents so it stands out in a mixed list.
    var accent: Color? {
        switch self {
        case .book: nil
        case .pamphlet: UNESColor.readable(0xF4A23C)
        case .cordel: UNESColor.readable(0xE85D4E)
        case .educationalProduct: UNESColor.readable(0x8A3F9E)
        case .dissertation, .thesis: UNESColor.readable(0xB23A7A)
        case .article: UNESColor.readable(0x2F7A8C)
        case .periodical: UNESColor.readable(0x4A5A8C)
        }
    }

    var tone: Color { accent ?? UNESColor.ink3 }
}

// MARK: - Availability tones

enum LibraryTone {
    static let available = UNESColor.readable(0x2F9E5E)
    static let loan = UNESColor.readable(0xD9852E)
    static let none = UNESColor.readable(0xE85D4E)
    static let localUse = UNESColor.readable(0x4A5A8C)
    static let unknown = UNESColor.ink4
}

extension LibraryAvailability {
    var tone: Color {
        switch verdict {
        case .available: LibraryTone.available
        case .allOnLoan: LibraryTone.none
        case .localUseOnly: LibraryTone.localUse
        }
    }

    /// The one-line verdict — "3 de 9 disponíveis" / "Nenhum disponível" /
    /// "Só consulta local".
    var headline: String {
        switch verdict {
        case .available:
            .localized(total == 1
                ? .libraryAvailabilitySomeOne(available, total)
                : .libraryAvailabilitySomeOther(available, total))
        case .allOnLoan:
            .localized(.libraryAvailabilityNone)
        case .localUseOnly:
            .localized(.libraryAvailabilityLocalOnly)
        }
    }
}

// MARK: - Work mark

/// The leading tile: a typographic block carrying the call number — which is
/// what the student actually needs at the shelf — plus the type glyph and
/// the collection prefix (C = cordel, T = teses, P = periódicos…).
struct LibraryWorkMark: View {
    var work: LibraryWork
    var width: CGFloat = 54
    var height: CGFloat = 78

    private var prefixAndClass: (prefix: String?, components: [String]) {
        let parts = work.callNumber.split(separator: " ").map(String.init)
        if let first = parts.first, first.count <= 2, first.allSatisfy({ $0.isUppercase }) {
            return (first, Array(parts.dropFirst()))
        }
        return (nil, parts)
    }

    var body: some View {
        let (prefix, components) = prefixAndClass
        let tone = work.type.tone
        return VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 4) {
                if let prefix {
                    Text(prefix)
                        .font(.system(size: 9, weight: .bold))
                        .tracking(0.5)
                        .foregroundStyle(tone)
                        .padding(EdgeInsets(top: 1, leading: 3.5, bottom: 1, trailing: 3.5))
                        .background(UNESColor.surface3, in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                }
                Spacer(minLength: 0)
                Image(systemName: work.type.icon)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(tone)
            }
            Spacer(minLength: 0)
            Text(components.first ?? "")
                .font(.system(size: 13, weight: .bold))
                .tracking(-0.39)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink2)
                // The class number is the point of the tile — shrink it to
                // fit rather than ellipsize the shelf number away.
                .lineLimit(1)
                .minimumScaleFactor(0.55)
            Text(components.dropFirst().joined(separator: " "))
                .font(.system(size: 9.5, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
                .lineLimit(1)
                .padding(.top, 2)
        }
        .padding(EdgeInsets(top: 7, leading: 8, bottom: 6, trailing: 6))
        .frame(width: width, height: height, alignment: .leading)
        .background(UNESColor.surface2)
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(tone)
                .opacity(work.type.accent == nil ? 0.28 : 0.9)
                .frame(width: 3.5)
        }
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }
}

// MARK: - Type tag

/// Uppercase glyph + label — "LIVRO", "CORDEL".
struct LibraryTypeTag: View {
    var type: LibraryWorkType
    var size: CGFloat = 10.5

    private var tone: Color { type.accent ?? UNESColor.ink4 }

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: type.icon)
                .font(.system(size: size, weight: .semibold))
            Text(type.label)
                .textCase(.uppercase)
                .font(.system(size: size, weight: .bold))
                .tracking(0.42)
                // "Produto educacional" must stay a single line even in the
                // narrow novidade cards.
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .foregroundStyle(tone)
    }
}

// MARK: - Availability line (result rows)

/// One-line availability readout under a result row: skeleton while the
/// reading is in flight, then the verdict — degrading honestly when the
/// circulation system is stale or down.
struct LibraryAvailabilityLine: View {
    var work: LibraryWork
    var reading: LibraryReading?
    var now: Date

    var body: some View {
        switch reading {
        case nil:
            HStack(spacing: 7) {
                Circle()
                    .fill(UNESColor.surface3)
                    .frame(width: 8, height: 8)
                Capsule()
                    .fill(UNESColor.surface3)
                    .frame(width: 104, height: 9)
            }
            .frame(height: 17)
        case .unavailable:
            HStack(spacing: 6) {
                Image(systemName: "wifi.slash")
                    .font(.system(size: 11, weight: .medium))
                Text(.libraryAvailabilityDown)
                    .font(.system(size: 12.5, weight: .semibold))
            }
            .foregroundStyle(UNESColor.ink4)
        case .fresh, .stale:
            verdictLine(isStale: {
                if case .stale = reading { true } else { false }
            }())
        }
    }

    private func verdictLine(isStale: Bool) -> some View {
        let availability = work.availability(now: now)
        return HStack(spacing: 7) {
            Circle()
                .fill(isStale ? LibraryTone.unknown : availability.tone)
                .frame(width: 8, height: 8)
            Text(availability.headline)
                .font(.system(size: 12.5, weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(isStale ? UNESColor.ink3 : availability.tone)
                .lineLimit(1)
                .layoutPriority(1)
            Text(context(availability, isStale: isStale))
                .font(.system(size: 12.5, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .lineLimit(1)
        }
    }

    private func context(_ availability: LibraryAvailability, isStale: Bool) -> String {
        if isStale, case let .stale(checkedAt) = reading {
            return "· \(String.localized(.libraryAvailabilityLastReading(LibraryFormat.ago(from: checkedAt, to: now))))"
        }
        if availability.branches.count == 1, let branch = availability.branches.first {
            return "· \(branch.branch.sigla)"
        }
        return "· \(String.localized(.libraryAvailabilityBranches(availability.branches.count)))"
    }
}

// MARK: - Freshness stamp

/// Always says when the circulation reading was taken, and degrades to
/// "last known" wording instead of silently lying.
struct LibraryFreshnessStamp: View {
    var reading: LibraryReading?
    var now: Date
    var onRefresh: (() -> Void)?

    var body: some View {
        HStack(spacing: 7) {
            icon
            Text(label)
                .font(.system(size: 12.5, weight: .medium))
                .tracking(-0.13)
                .foregroundStyle(tone)
            if let onRefresh, reading != nil {
                Button {
                    onRefresh()
                } label: {
                    Text(.libraryFreshRefresh)
                        .font(.system(size: 12.5, weight: .semibold))
                        .foregroundStyle(UNESColor.accent)
                }
                .buttonStyle(.plain)
            }
        }
    }

    @ViewBuilder
    private var icon: some View {
        switch reading {
        case nil:
            ProgressView()
                .controlSize(.mini)
                .tint(UNESColor.ink4)
        case .unavailable:
            Image(systemName: "wifi.slash")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(tone)
        case .stale:
            Image(systemName: "hourglass")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(tone)
        case .fresh:
            Image(systemName: "clock")
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(tone)
        }
    }

    private var tone: Color {
        switch reading {
        case nil, .fresh: UNESColor.ink4
        case .stale: LibraryTone.loan
        case .unavailable: LibraryTone.none
        }
    }

    private var label: String {
        switch reading {
        case nil:
            .localized(.libraryFreshChecking)
        case .unavailable:
            .localized(.libraryFreshDown)
        case let .stale(checkedAt):
            .localized(.libraryFreshStale(LibraryFormat.ago(from: checkedAt, to: now)))
        case let .fresh(checkedAt):
            .localized(.libraryFreshChecked(LibraryFormat.ago(from: checkedAt, to: now)))
        }
    }
}

// MARK: - Note box

/// Soft informational note — the honest small print about what the
/// catalogue does and doesn't provide.
struct LibraryNote: View {
    var icon: String = "info.circle"
    var tone: Color?
    var text: Text

    init(icon: String = "info.circle", tone: Color? = nil, _ resource: LocalizedStringResource) {
        self.icon = icon
        self.tone = tone
        self.text = Text(resource)
    }

    init(icon: String = "info.circle", tone: Color? = nil, text: Text) {
        self.icon = icon
        self.tone = tone
        self.text = text
    }

    var body: some View {
        HStack(alignment: .top, spacing: 9) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(tone ?? UNESColor.ink4)
                .padding(.top, 1)
            text
                .font(.system(size: 12.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(tone ?? UNESColor.ink3)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .background(
            tone.map { AnyShapeStyle($0.opacity(0.09)) } ?? AnyShapeStyle(UNESColor.surface2),
            in: RoundedRectangle(cornerRadius: 14, style: .continuous)
        )
    }
}

// MARK: - Filter chip

/// The results/refine chip — capsule, inverse fill while active.
struct LibraryChip<Label: View>: View {
    var isActive = false
    var tone: Color = UNESColor.ink
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

extension View {
    /// The design's scroll-strip mask: content fades out over the last
    /// points before each edge instead of clipping mid-chip.
    func chipRowFade(leading: CGFloat = 20, trailing: CGFloat = 26) -> some View {
        mask {
            HStack(spacing: 0) {
                LinearGradient(colors: [.clear, .black], startPoint: .leading, endPoint: .trailing)
                    .frame(width: leading)
                Rectangle().fill(.black)
                LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing)
                    .frame(width: trailing)
            }
        }
    }
}

/// Edge-to-edge horizontally scrolling chip strip: bleeds through the screen
/// inset so chips reach the display edge, fading out at both ends. The
/// vertical padding pair keeps chip shadows inside the masked bounds.
struct LibraryChipRow<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        ScrollView(.horizontal) {
            HStack(spacing: 8) {
                content
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 14)
        }
        .scrollIndicators(.hidden)
        .chipRowFade()
        .padding(.horizontal, -16)
        .padding(.vertical, -14)
    }
}

// MARK: - State card (broad / empty)

/// Full-width state card for the search failure modes, with actionable
/// suggestions instead of a dead end.
struct LibraryStateCard<Content: View>: View {
    var icon: String
    var tone: Color
    var title: Text
    var body_: Text
    @ViewBuilder var suggestions: Content

    var body: some View {
        VStack(spacing: 0) {
            Image(systemName: icon)
                .font(.system(size: 24, weight: .medium))
                .foregroundStyle(tone)
                .frame(width: 54, height: 54)
                .background(tone.opacity(0.1), in: RoundedRectangle(cornerRadius: 17, style: .continuous))
            title
                .font(.system(size: 19, weight: .bold))
                .tracking(-0.57)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.center)
                .padding(.top, 15)
            body_
                .font(.system(size: 13.5, weight: .medium))
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            VStack(spacing: 8) {
                suggestions
            }
            .padding(.top, 18)
        }
        .padding(EdgeInsets(top: 24, leading: 20, bottom: 20, trailing: 20))
        .frame(maxWidth: .infinity)
        .materialsCard()
    }
}

struct LibrarySuggestionRow: View {
    var icon: String
    var label: LocalizedStringResource
    var hint: LocalizedStringResource?
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 11) {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 20)
                VStack(alignment: .leading, spacing: 1) {
                    Text(label)
                        .font(.system(size: 14, weight: .semibold))
                        .tracking(-0.28)
                        .foregroundStyle(UNESColor.ink)
                    if let hint {
                        Text(hint)
                            .font(.system(size: 11.5, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                    }
                }
                .multilineTextAlignment(.leading)
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
            .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .contentShape(Rectangle())
        }
        .buttonStyle(CardPressStyle())
    }
}

#Preview("Componentes") {
    let now = Date()
    let works = LibraryFixtures.works(now: now)
    return ScrollView {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 10) {
                ForEach(works.prefix(4)) { work in
                    LibraryWorkMark(work: work)
                }
            }
            LibraryTypeTag(type: .cordel)
            VStack(alignment: .leading, spacing: 8) {
                LibraryAvailabilityLine(work: works[0], reading: .fresh(checkedAt: now), now: now)
                LibraryAvailabilityLine(work: works[2], reading: .fresh(checkedAt: now), now: now)
                LibraryAvailabilityLine(work: works[6], reading: .stale(checkedAt: now.addingTimeInterval(-16 * 3600)), now: now)
                LibraryAvailabilityLine(work: works[1], reading: .unavailable, now: now)
                LibraryAvailabilityLine(work: works[3], reading: nil, now: now)
            }
            LibraryFreshnessStamp(reading: .fresh(checkedAt: now), now: now, onRefresh: {})
            LibraryFreshnessStamp(reading: .stale(checkedAt: now.addingTimeInterval(-970 * 60)), now: now, onRefresh: {})
            LibraryNote(icon: "book", .libraryHubCatalogueNote)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
