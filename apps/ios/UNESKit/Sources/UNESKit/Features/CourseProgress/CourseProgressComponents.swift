import SwiftUI

// MARK: - Status vocabulary

/// Every status carries glyph + label + border treatment — the color is the
/// last reinforcement, never the information (the university's own PDF
/// encodes everything by fill color; we don't).
extension CurriculumEntryStatus {
    var label: LocalizedStringResource {
        switch self {
        case .completed: .courseProgressStatusCompleted
        case .inProgress: .courseProgressStatusInProgress
        case .available: .courseProgressStatusAvailable
        case .withdrawn: .courseProgressStatusWithdrawn
        case .failed: .courseProgressStatusFailed
        case .blocked: .courseProgressStatusBlocked
        case .notTaken: .courseProgressStatusNotTaken
        }
    }

    /// Narrow-row variant.
    var shortLabel: LocalizedStringResource {
        switch self {
        case .completed: .courseProgressStatusCompleted
        case .inProgress: .courseProgressStatusInProgressShort
        case .available: .courseProgressStatusAvailableShort
        case .withdrawn: .courseProgressStatusWithdrawn
        case .failed: .courseProgressStatusFailed
        case .blocked: .courseProgressStatusBlockedShort
        case .notTaken: .courseProgressStatusNotTaken
        }
    }

    var symbol: String {
        switch self {
        case .completed: "checkmark"
        case .inProgress: "circle.inset.filled"
        case .available: "plus"
        case .withdrawn: "pause"
        case .failed: "xmark"
        case .blocked: "lock"
        case .notTaken: "circle.dotted"
        }
    }

    /// Done → success green; happening → teal (the app's "live/info"
    /// accent); pickable → violet; paused → tangerine; failed → coral;
    /// stuck / untouched → neutrals.
    var tone: Color {
        switch self {
        case .completed: UNESBannerTone.ok
        case .inProgress: UNESColor.tealReadable
        case .available: UNESColor.violet
        case .withdrawn: UNESColor.tangerine
        case .failed: UNESBannerTone.danger
        case .blocked: UNESColor.ink3
        case .notTaken: UNESColor.ink4
        }
    }

    var isNeutral: Bool {
        self == .notTaken
    }

    /// Fill alpha behind the glyph for the tinted states.
    var fillOpacity: Double {
        switch self {
        case .completed: 0.12
        case .inProgress, .failed: 0.10
        case .available, .withdrawn, .blocked, .notTaken: 0
        }
    }
}

// MARK: - Border treatments

/// The status-coded outline: solid, thick, dashed, hatched, or barely there.
struct CurriculumStatusBorder: ViewModifier {
    var status: CurriculumEntryStatus
    var cornerRadius: CGFloat

    func body(content: Content) -> some View {
        content.overlay {
            let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            switch status {
            case .inProgress:
                shape.strokeBorder(status.tone, lineWidth: 2)
            case .blocked:
                shape.strokeBorder(status.tone.opacity(0.75), style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
            case .withdrawn:
                shape.strokeBorder(status.tone.opacity(0.75), lineWidth: 1)
            case .notTaken:
                shape.strokeBorder(UNESColor.line, lineWidth: 1)
            case .completed, .available, .failed:
                shape.strokeBorder(status.tone.opacity(0.55), lineWidth: 1)
            }
        }
    }
}

/// Diagonal stripes — the "not counted" / "trancada" texture.
struct DiagonalHatch: View {
    var color: Color
    var lineWidth: CGFloat = 4
    var gap: CGFloat = 5

    var body: some View {
        Canvas { context, size in
            let step = lineWidth + gap
            let span = size.width + size.height
            var offset: CGFloat = -size.height
            while offset < span {
                var path = Path()
                path.move(to: CGPoint(x: offset, y: size.height))
                path.addLine(to: CGPoint(x: offset + size.height, y: 0))
                context.stroke(path, with: .color(color), lineWidth: lineWidth)
                offset += step
            }
        }
        .allowsHitTesting(false)
    }
}

// MARK: - Badge (glyph in a square)

struct CurriculumStatusBadge: View {
    var status: CurriculumEntryStatus
    var size: CGFloat = 26
    var cornerRadius: CGFloat = 8

    var body: some View {
        let solid = status == .completed
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        Image(systemName: status.symbol)
            .font(.system(size: size * 0.46, weight: .bold))
            .foregroundStyle(solid ? .white : status.isNeutral ? UNESColor.ink4 : status.tone)
            .frame(width: size, height: size)
            .background {
                if solid {
                    shape.fill(status.tone)
                } else if status.fillOpacity > 0 {
                    shape.fill(status.tone.opacity(status.fillOpacity))
                }
            }
            .overlay {
                if status == .withdrawn {
                    DiagonalHatch(color: status.tone.opacity(0.14), lineWidth: 3, gap: 4)
                        .clipShape(shape)
                }
            }
            .overlay {
                if solid {
                    shape.strokeBorder(status.tone, lineWidth: 1)
                } else {
                    Color.clear.modifier(CurriculumStatusBorder(status: status, cornerRadius: cornerRadius))
                }
            }
    }
}

// MARK: - Chip

struct CurriculumStatusChip: View {
    var status: CurriculumEntryStatus
    var full = false
    var small = false

    var body: some View {
        HStack(spacing: small ? 4 : 5) {
            Image(systemName: status.symbol)
                .font(.system(size: small ? 9 : 10, weight: .bold))
            Text(full ? status.label : status.shortLabel)
                .font(.system(size: small ? 10.5 : 11.5, weight: .semibold))
                .tracking(-0.05)
                .lineLimit(1)
        }
        .foregroundStyle(status.isNeutral ? UNESColor.ink3 : status.tone)
        .padding(EdgeInsets(
            top: small ? 2 : 3, leading: small ? 6 : 7,
            bottom: small ? 2 : 3, trailing: small ? 7 : 9
        ))
        .background(status.isNeutral ? UNESColor.surface2 : status.tone.opacity(0.11), in: Capsule())
        .overlay {
            Capsule().strokeBorder(status.isNeutral ? UNESColor.line : status.tone.opacity(0.32))
        }
    }
}

/// Every status, in display order — the map/grid legend.
struct CurriculumLegend: View {
    var body: some View {
        FlowLayout(spacing: 6, lineSpacing: 6) {
            ForEach(CurriculumEntryStatus.displayOrder, id: \.self) { status in
                CurriculumStatusChip(status: status, full: true, small: true)
            }
        }
    }
}

// MARK: - Meter

/// Hours completed over hours required. `unknown` draws no fill and a
/// hatched track (no denominator); `hatched` keeps the denominator but marks
/// the track as not observable.
struct CurriculumMeter: View {
    var value: Int
    var total: Int?
    var tone: Color = UNESBannerTone.ok
    var height: CGFloat = 7
    var unknown = false
    var hatched = false

    private var fraction: Double {
        guard !unknown, let total, total > 0 else { return 0 }
        return min(1, max(0, Double(value) / Double(total)))
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(UNESColor.surface3)
                if hatched || unknown {
                    DiagonalHatch(color: UNESColor.ink.opacity(0.09), lineWidth: 4, gap: 5)
                        .clipShape(Capsule())
                }
                if fraction > 0 {
                    Capsule()
                        .fill(tone)
                        .frame(width: max(height, proxy.size.width * fraction))
                }
            }
        }
        .frame(height: height)
        .animation(UNESMotion.ease(0.7), value: fraction)
    }
}

// MARK: - Card chrome

extension View {
    /// The standard v2 card: card fill, hairline border, soft shadow.
    func courseProgressCard(cornerRadius: CGFloat = 20) -> some View {
        background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }
}

/// Section title with a supporting hint, aligned with the cards.
struct CourseProgressSectionHeader: View {
    var title: LocalizedStringResource
    var hint: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 19, weight: .bold))
                .tracking(-0.53)
                .foregroundStyle(UNESColor.ink)
            if let hint {
                Text(hint)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 0, leading: 4, bottom: 10, trailing: 4))
    }
}

/// Hairline between stacked rows inside a card.
struct CardRowDivider: View {
    var body: some View {
        Rectangle()
            .fill(UNESColor.line)
            .frame(height: 0.5)
    }
}

// MARK: - Hour-type row ("Por natureza")

/// One requirement bucket: name, completed over required, meter, then the
/// percent (or why it's legitimately zero) and how much is left. Numbers are
/// always present — the meter is redundant on purpose.
struct CurriculumRequirementRow: View {
    var requirement: CurriculumRequirementProgress
    /// No curriculum → hours cumpridas with no denominator.
    var unknown = false

    private var notCounted: Bool { !requirement.derivable }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text(requirement.shortLabel)
                    .font(.system(size: 14.5, weight: .semibold))
                    .tracking(-0.2)
                    .foregroundStyle(UNESColor.ink)
                    .frame(maxWidth: .infinity, alignment: .leading)
                hoursLabel
            }
            .padding(.bottom, 9)

            CurriculumMeter(
                value: requirement.hoursCompleted,
                total: requirement.hoursRequired,
                tone: notCounted ? UNESColor.tangerine : UNESBannerTone.ok,
                unknown: unknown,
                hatched: notCounted
            )

            HStack(alignment: .center, spacing: 8) {
                footnote
                    .frame(maxWidth: .infinity, alignment: .leading)
                if !unknown, !notCounted, requirement.hoursRemaining > 0 {
                    Text(.courseProgressRemainingShort(CourseProgressFormat.hours(requirement.hoursRemaining)))
                        .font(.system(size: 11.5, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            .padding(.top, 8)

            if notCounted {
                Text(.courseProgressNotCountedNote)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(EdgeInsets(top: 9, leading: 11, bottom: 9, trailing: 11))
                    .background(UNESColor.tangerine.opacity(0.08), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(UNESColor.tangerine.opacity(0.24))
                    }
                    .padding(.top, 9)
            }
        }
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 14, trailing: 15))
    }

    @ViewBuilder
    private var hoursLabel: some View {
        let completed = CourseProgressFormat.count(requirement.hoursCompleted)
        let required = CourseProgressFormat.count(requirement.hoursRequired)
        Group {
            if notCounted {
                Text(.courseProgressHoursOfRequired(completed, required))
                    .foregroundStyle(UNESColor.ink3)
            } else if unknown {
                Text(.courseProgressHoursCompletedShort(CourseProgressFormat.hours(requirement.hoursCompleted)))
                    .foregroundStyle(UNESColor.ink)
            } else {
                Text(completed)
                    .foregroundStyle(UNESColor.ink)
                    + Text(verbatim: " / ").foregroundStyle(UNESColor.ink4)
                    + Text(CourseProgressFormat.hours(requirement.hoursRequired)).foregroundStyle(UNESColor.ink4)
            }
        }
        .font(.system(size: 13.5, weight: .semibold))
        .monospacedDigit()
        .tracking(-0.1)
    }

    @ViewBuilder
    private var footnote: some View {
        Group {
            if notCounted {
                Label(String.localized(.courseProgressNotCountedYet), systemImage: "doc.text")
                    .foregroundStyle(UNESColor.tangerine)
            } else if unknown {
                Text(.courseProgressNoTotalForCurriculum)
                    .foregroundStyle(UNESColor.ink4)
            } else if requirement.hoursCompleted > 0, let percent = requirement.percent {
                (Text(CourseProgressFormat.percent(percent)).bold().foregroundStyle(UNESBannerTone.ok)
                    + Text(verbatim: " ")
                    + Text(.courseProgressCompletedSuffix))
                    .foregroundStyle(UNESColor.ink4)
            } else if let starts = requirement.startsAtPeriod {
                Label(
                    String.localized(.courseProgressStartsAt(CourseProgressFormat.semester(starts))),
                    systemImage: "clock"
                )
                .foregroundStyle(UNESColor.ink4)
            } else {
                Text(.courseProgressNotStarted)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .font(.system(size: 11.5, weight: .medium))
        .monospacedDigit()
        .labelStyle(CompactLabelStyle())
    }
}

/// Icon + text on one line, tight spacing.
struct CompactLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 5) {
            configuration.icon
                .font(.system(size: 10.5, weight: .semibold))
            configuration.title
        }
    }
}

// MARK: - Loading / failure

struct CourseProgressLoadingView: View {
    var body: some View {
        VStack(spacing: 14) {
            ProgressView()
                .controlSize(.regular)
            Text(.courseProgressLoading)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.bottom, 80)
    }
}

struct CourseProgressFailureView: View {
    var onRetry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 56, height: 56)
                .background(UNESColor.surface2, in: Circle())
            Text(.courseProgressLoadFailedTitle)
                .font(.system(size: 17, weight: .bold))
                .tracking(-0.3)
                .foregroundStyle(UNESColor.ink)
            Text(.courseProgressLoadFailedBody)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button(action: onRetry) {
                Text(.commonTryAgain)
            }
            .buttonStyle(.bordered)
            .tint(UNESColor.accent)
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(EdgeInsets(top: 0, leading: 32, bottom: 80, trailing: 32))
    }
}

#Preview("Vocabulário") {
    ScrollView {
        VStack(alignment: .leading, spacing: 20) {
            CurriculumLegend()
            HStack(spacing: 8) {
                ForEach(CurriculumEntryStatus.displayOrder, id: \.self) { status in
                    CurriculumStatusBadge(status: status, size: 34, cornerRadius: 10)
                }
            }
            VStack(spacing: 0) {
                let requirements = CourseProgress.preview().requirements
                ForEach(Array(requirements.enumerated()), id: \.element.id) { index, requirement in
                    CurriculumRequirementRow(requirement: requirement)
                    if index < requirements.count - 1 { CardRowDivider() }
                }
            }
            .courseProgressCard()
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
