import SwiftUI

// MARK: - Tone mapping

extension CampusEventCategory {
    var label: LocalizedStringResource {
        switch self {
        case .quest: .campusEventCategoryQuest
        case .workshop: .campusEventCategoryWorkshop
        case .lecture: .campusEventCategoryLecture
        case .presentation: .campusEventCategoryPresentation
        case .groupDynamic: .campusEventCategoryDynamic
        case .other: .campusEventCategoryOther
        }
    }

    var tone: Color {
        switch self {
        case .quest: UNESColor.amber
        case .workshop: UNESColor.teal
        case .lecture: UNESColor.violet
        case .presentation: UNESColor.magenta
        case .groupDynamic: UNESColor.coral
        case .other: UNESColor.tangerine
        }
    }

    var mesh: MeshView.Variant {
        switch self {
        case .quest: .sun
        case .workshop: .cool
        case .lecture, .presentation: .rose
        case .groupDynamic, .other: .warm
        }
    }

    var icon: String {
        switch self {
        case .quest: "sparkles"
        case .workshop: "wrench.and.screwdriver"
        case .lecture: "mic"
        case .presentation: "star"
        case .groupDynamic: "person.2"
        case .other: "square.grid.2x2"
        }
    }
}

extension CampusEventAudience {
    var label: LocalizedStringResource {
        switch self {
        case .everyone: .campusEventFilterEveryone
        case .freshmen: .campusEventFilterFreshmen
        case .veterans: .campusEventFilterVeterans
        }
    }

    var tone: Color {
        switch self {
        case .everyone: UNESColor.successGreen
        case .freshmen: UNESColor.coral
        case .veterans: UNESColor.teal
        }
    }
}

/// Deterministic per-name accent, stable across launches (unlike
/// `String.hashValue`) — drives avatar gradients and list tints.
func campusEventStableIndex(_ text: String, count: Int) -> Int {
    guard count > 0 else { return 0 }
    var hash: UInt32 = 0
    for scalar in text.unicodeScalars {
        hash = hash &* 31 &+ scalar.value
    }
    return Int(hash % UInt32(count))
}

/// The rotating tint palette for venues and organizations.
let campusEventPalette: [Color] = [
    UNESColor.violet, UNESColor.teal, UNESColor.amber,
    UNESColor.magenta, UNESColor.successGreen, UNESColor.coral,
]

func campusEventTone(for text: String) -> Color {
    campusEventPalette[campusEventStableIndex(text, count: campusEventPalette.count)]
}

// MARK: - Card chrome

extension View {
    /// The standard v2 card: white/dark card fill, hairline border, soft
    /// shadow.
    func campusEventCard() -> some View {
        background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// Section title with an optional supporting note, aligned with the cards.
struct CampusEventSectionHeader: View {
    var title: LocalizedStringResource
    var note: LocalizedStringResource?

    init(_ title: LocalizedStringResource, note: LocalizedStringResource? = nil) {
        self.title = title
        self.note = note
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.system(size: 21, weight: .bold))
                .tracking(-0.63)
                .foregroundStyle(UNESColor.ink)
            if let note {
                Text(note)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 12, trailing: 4))
    }
}

// MARK: - Chips & tiles

/// Audience capsule: tinted dot + label.
struct CampusEventAudienceChip: View {
    var audience: CampusEventAudience
    var large = false

    var body: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(audience.tone)
                .frame(width: 6, height: 6)
            Text(audience.label)
                .font(.system(size: large ? 12.5 : 11, weight: .bold))
                .tracking(-0.11)
                .lineLimit(1)
        }
        .foregroundStyle(audience.tone)
        .padding(EdgeInsets(top: large ? 5 : 3, leading: large ? 9 : 7, bottom: large ? 5 : 3, trailing: large ? 11 : 8))
        .background(audience.tone.opacity(0.12), in: Capsule())
        .fixedSize()
    }
}

/// Category badge: icon + label on a tinted rounded rect.
struct CampusEventCategoryPill: View {
    var category: CampusEventCategory

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: category.icon)
                .font(.system(size: 9.5, weight: .bold))
            Text(category.label)
                .font(.system(size: 11, weight: .bold))
                .lineLimit(1)
        }
        .foregroundStyle(category.tone)
        .padding(EdgeInsets(top: 4, leading: 7, bottom: 4, trailing: 9))
        .background(category.tone.opacity(0.1), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        .fixedSize()
    }
}

/// Rounded square glyph carrying the category color.
struct CampusEventCategoryTile: View {
    var category: CampusEventCategory
    var size: CGFloat = 44

    var body: some View {
        Image(systemName: category.icon)
            .font(.system(size: size * 0.38, weight: .semibold))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                category.tone,
                in: RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
            )
            .shadow(color: category.tone.opacity(0.27), radius: 7, y: 5)
    }
}

/// Initials over a gradient picked by a stable hash of the name.
struct CampusEventAvatar: View {
    var name: String
    var size: CGFloat = 42

    private static let gradients: [(Color, Color)] = [
        (UNESColor.coral, UNESColor.amber),
        (UNESColor.violet, UNESColor.magenta),
        (UNESColor.teal, UNESColor.successGreen),
        (UNESColor.magenta, UNESColor.coral),
        (UNESColor.teal, UNESColor.violet),
        (UNESColor.amber, UNESColor.coral),
    ]

    var body: some View {
        let pair = Self.gradients[campusEventStableIndex(name, count: Self.gradients.count)]
        Text(initials)
            .font(.system(size: size * 0.38, weight: .semibold))
            .tracking(-0.3)
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                LinearGradient.css(
                    stops: [.init(color: pair.0, location: 0), .init(color: pair.1, location: 1)],
                    angle: 135
                ),
                in: Circle()
            )
            .shadow(color: Color(hex: 0x141020, opacity: 0.18), radius: 6, y: 4)
    }

    private var initials: String {
        let parts = name.split(separator: " ")
        guard let first = parts.first else { return "•" }
        guard parts.count > 1, let last = parts.last else {
            return String(first.prefix(2)).uppercased()
        }
        return "\(first.prefix(1))\(last.prefix(1))".uppercased()
    }
}

/// Faint tinted radial washing down from behind the navigation title on
/// detail screens.
struct CampusEventDetailWash: View {
    var tone: Color

    var body: some View {
        RadialGradient(
            colors: [tone.opacity(0.26), .clear],
            center: UnitPoint(x: 0.5, y: 0),
            startRadius: 0,
            endRadius: 340
        )
        .frame(height: 280)
        .padding(.horizontal, -50)
        .offset(y: -60)
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

// MARK: - Countdown

/// One D/H/M/S cell of the big countdown.
struct CampusEventCountCell: View {
    var value: Int
    var label: LocalizedStringResource

    var body: some View {
        VStack(spacing: 4) {
            Text(CampusEventFormat.padded(value))
                .font(.system(size: 34, weight: .bold))
                .tracking(-1.7)
                .monospacedDigit()
                .foregroundStyle(.white)
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10, weight: .semibold))
                .tracking(0.8)
                .foregroundStyle(.white.opacity(0.6))
        }
        .frame(minWidth: 46)
    }
}

/// The full "DD : HH : MM : SS" row, rendered for a frozen `now` — wrap in a
/// `TimelineView` to tick.
struct CampusEventCountdownRow: View {
    var target: Date
    var now: Date

    var body: some View {
        let countdown = CampusEventFormat.countdown(until: target, now: now)
        HStack(alignment: .top, spacing: 8) {
            CampusEventCountCell(value: countdown.days, label: .campusEventCountdownDays)
            separator
            CampusEventCountCell(value: countdown.hours, label: .campusEventCountdownHours)
            separator
            CampusEventCountCell(value: countdown.minutes, label: .campusEventCountdownMinutes)
            separator
            CampusEventCountCell(value: countdown.seconds, label: .campusEventCountdownSeconds)
        }
    }

    private var separator: some View {
        Text(":")
            .font(.system(size: 28, weight: .bold))
            .foregroundStyle(.white.opacity(0.32))
            .padding(.top, 1)
    }
}

// MARK: - Hero bits (on-dark)

/// Compact activity block inside the hero footer: category tile + tagged
/// title + time/venue meta.
struct CampusEventHeroActivityBlock: View {
    var activity: CampusEventActivity
    var tag: LocalizedStringResource
    var tagTone: Color = .white.opacity(0.55)
    var timeZone: TimeZone = .autoupdatingCurrent

    var body: some View {
        HStack(spacing: 11) {
            CampusEventCategoryTile(category: activity.category, size: 40)
            VStack(alignment: .leading, spacing: 1) {
                Text(tag)
                    .textCase(.uppercase)
                    .font(.system(size: 10.5, weight: .bold))
                    .tracking(0.53)
                    .foregroundStyle(tagTone)
                Text(activity.title)
                    .font(.system(size: 15, weight: .bold))
                    .tracking(-0.3)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text("\(CampusEventFormat.weekdayShort(for: activity.startsAt, in: timeZone)) · \(CampusEventFormat.time(for: activity.startsAt, in: timeZone)) · \(activity.venueName)")
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Per-day progress segments while the event runs: done days in white, the
/// current one in live green.
struct CampusEventDayProgress: View {
    var dayCount: Int
    var currentIndex: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<max(dayCount, 1), id: \.self) { index in
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(
                        index < currentIndex
                            ? Color.white.opacity(0.9)
                            : index == currentIndex ? UNESColor.liveGreen : .white.opacity(0.22)
                    )
                    .frame(height: 5)
            }
        }
    }
}

/// Number + label stat in the ended hero.
struct CampusEventHeroStat: View {
    var value: Int
    var label: LocalizedStringResource

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("\(value)")
                .font(.system(size: 24, weight: .heavy))
                .tracking(-0.72)
                .monospacedDigit()
                .foregroundStyle(.white)
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 11.5, weight: .semibold))
                .tracking(0.46)
                .foregroundStyle(.white.opacity(0.55))
        }
    }
}

#Preview("Componentes") {
    let event = CampusEvent.preview()
    return ScrollView {
        VStack(alignment: .leading, spacing: 20) {
            HStack {
                CampusEventCategoryPill(category: .quest)
                CampusEventCategoryPill(category: .workshop)
                CampusEventCategoryPill(category: .lecture)
            }
            HStack {
                CampusEventAudienceChip(audience: .freshmen)
                CampusEventAudienceChip(audience: .veterans)
                CampusEventAudienceChip(audience: .everyone, large: true)
            }
            HStack {
                CampusEventCategoryTile(category: .presentation)
                CampusEventCategoryTile(category: .groupDynamic)
                CampusEventAvatar(name: "Claudia P. Pereira")
            }
            VStack(spacing: 16) {
                CampusEventCountdownRow(target: event.startsAt, now: .now)
                CampusEventHeroActivityBlock(activity: event.activities[0], tag: .campusEventHubOpening)
                CampusEventDayProgress(dayCount: 5, currentIndex: 2)
            }
            .padding(20)
            .background(UNESColor.darkBg, in: RoundedRectangle(cornerRadius: 24))
        }
        .padding(20)
    }
    .background(UNESColor.surface)
}
