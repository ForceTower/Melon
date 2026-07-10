import SwiftUI

// MARK: - Tone mapping

extension ParadoxoTier {
    var tone: Color {
        switch self {
        case .angel: UNESColor.successGreen
        case .fair: UNESColor.teal
        case .balanced: UNESColor.tangerine
        case .demanding: UNESColor.caution
        case .relentless: UNESColor.coral
        }
    }

    var label: LocalizedStringResource {
        switch self {
        case .angel: .paradoxoTierAngel
        case .fair: .paradoxoTierFair
        case .balanced: .paradoxoTierBalanced
        case .demanding: .paradoxoTierDemanding
        case .relentless: .paradoxoTierRelentless
        }
    }
}

extension ParadoxoShapeKind {
    var label: LocalizedStringResource {
        switch self {
        case .bimodal: .paradoxoShapeBimodal
        case .strict: .paradoxoShapeStrict
        case .lenient: .paradoxoShapeLenient
        case .balanced: .paradoxoShapeBalanced
        case .regular: .paradoxoShapeRegular
        }
    }
}

extension ParadoxoPulseFact.Kind {
    var label: LocalizedStringResource {
        switch self {
        case .brutal: .paradoxoPulseBrutal
        case .kind: .paradoxoPulseKind
        case .trend: .paradoxoPulseTrend
        case .gap: .paradoxoPulseGap
        case .rising: .paradoxoPulseRising
        case .surprise: .paradoxoPulseSurprise
        case .signature: .paradoxoPulseSignature
        }
    }

    var tone: Color {
        switch self {
        case .brutal: UNESColor.coral
        case .kind: UNESColor.successGreen
        case .trend: UNESColor.caution
        case .gap, .signature: UNESColor.magenta
        case .rising: UNESColor.teal
        case .surprise: UNESColor.amber
        }
    }

    var mesh: MeshView.Variant {
        switch self {
        case .brutal, .gap, .signature: .rose
        case .kind: .fresh
        case .trend, .surprise: .sun
        case .rising: .cool
        }
    }
}

/// Grade-severity tone, shared by tiles, charts and chips.
func paradoxoTone(_ mean: Double) -> Color {
    ParadoxoTier(mean: mean).tone
}

// MARK: - Card chrome

extension View {
    /// The standard v2 card: white/dark card fill, hairline border, soft
    /// shadow.
    func paradoxoCard() -> some View {
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
struct ParadoxoSectionHeader: View {
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

// MARK: - Score tile

/// Rounded square carrying the mean, tinted by severity.
struct ParadoxoScoreTile: View {
    var score: Double
    var size: CGFloat = 46

    var body: some View {
        Text(formatGrade(score))
            .font(.system(size: size * 0.4, weight: .bold))
            .monospacedDigit()
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(
                paradoxoTone(score),
                in: RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
            )
            .shadow(color: paradoxoTone(score).opacity(0.27), radius: 6, y: 4)
    }
}

// MARK: - Tier chip

struct ParadoxoTierChip: View {
    var tier: ParadoxoTier

    init(mean: Double) {
        tier = ParadoxoTier(mean: mean)
    }

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(tier.tone)
                .frame(width: 6, height: 6)
            Text(tier.label)
                .font(.system(size: 12, weight: .bold))
                .tracking(-0.12)
        }
        .foregroundStyle(tier.tone)
        .padding(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 10))
        .background(tier.tone.opacity(0.12), in: Capsule())
    }
}

// MARK: - Outcomes (approval / failure / abandonment)

struct ParadoxoOutcomesBar: View {
    var approved: Int
    var failed: Int
    var quit: Int
    var showsLegend = true

    private var total: Int { max(approved + failed + quit, 1) }

    var body: some View {
        VStack(spacing: 12) {
            GeometryReader { proxy in
                HStack(spacing: 2) {
                    segment(approved, color: UNESColor.successGreen, width: proxy.size.width)
                    segment(failed, color: UNESColor.coral, width: proxy.size.width)
                    segment(quit, color: UNESColor.ink4, width: proxy.size.width)
                }
            }
            .frame(height: 10)
            .background(UNESColor.surface3)
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))

            if showsLegend {
                HStack(spacing: 0) {
                    legend(approved, color: UNESColor.successGreen, label: .paradoxoOutcomeApproved)
                    legend(failed, color: UNESColor.coral, label: .paradoxoOutcomeFailed)
                    legend(quit, color: UNESColor.ink4, label: .paradoxoOutcomeQuit)
                }
            }
        }
    }

    private func segment(_ value: Int, color: Color, width: CGFloat) -> some View {
        color.frame(width: max(0, width * CGFloat(value) / CGFloat(total) - 2))
    }

    private func legend(_ value: Int, color: Color, label: LocalizedStringResource) -> some View {
        VStack(spacing: 2) {
            HStack(spacing: 5) {
                Circle()
                    .fill(color)
                    .frame(width: 7, height: 7)
                Text(ParadoxoFormat.count(value))
                    .font(.system(size: 17, weight: .bold))
                    .monospacedDigit()
                    .tracking(-0.34)
                    .foregroundStyle(UNESColor.ink)
            }
            Text(label)
                .font(.system(size: 11.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Insight row ("Curiosidades")

struct ParadoxoInsightRow: View {
    var icon: String
    var tone: Color
    var text: LocalizedStringResource

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(tone)
                .frame(width: 30, height: 30)
                .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            Text(text)
                .font(.system(size: 13.5, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
                .lineSpacing(2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
    }
}

// MARK: - Small stat tile (teacher detail)

struct ParadoxoStatTile<Extra: View>: View {
    var label: LocalizedStringResource
    var value: String
    var tone: Color
    @ViewBuilder var extra: Extra

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.53)
                .foregroundStyle(UNESColor.ink4)
            Text(value)
                .font(.system(size: 24, weight: .bold))
                .monospacedDigit()
                .tracking(-0.72)
                .foregroundStyle(tone)
            extra
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .padding(EdgeInsets(top: 12, leading: 13, bottom: 12, trailing: 13))
        .paradoxoCard()
    }
}

extension ParadoxoStatTile where Extra == EmptyView {
    init(label: LocalizedStringResource, value: String, tone: Color) {
        self.init(label: label, value: value, tone: tone) { EmptyView() }
    }
}

// MARK: - Loading / failure states

struct ParadoxoLoadingView: View {
    var body: some View {
        ProgressView()
            .controlSize(.large)
            .tint(UNESColor.ink3)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 80)
    }
}

struct ParadoxoFailureView: View {
    var onRetry: () -> Void

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: 22, weight: .medium))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 52, height: 52)
                .background(UNESColor.surface2, in: Circle())
            Text(.paradoxoErrorTitle)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            Button {
                onRetry()
            } label: {
                Text(.paradoxoErrorRetry)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(UNESColor.accent)
            }
            .buttonStyle(.plain)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 64)
    }
}

#Preview("Componentes") {
    ScrollView {
        VStack(alignment: .leading, spacing: 20) {
            HStack(spacing: 12) {
                ParadoxoScoreTile(score: 3.5)
                ParadoxoScoreTile(score: 5.7)
                ParadoxoScoreTile(score: 7.2)
                ParadoxoScoreTile(score: 9.1)
            }
            HStack(spacing: 8) {
                ParadoxoTierChip(mean: 2.1)
                ParadoxoTierChip(mean: 6.0)
                ParadoxoTierChip(mean: 9.0)
            }
            ParadoxoOutcomesBar(approved: 2477, failed: 3743, quit: 648)
                .padding(16)
                .paradoxoCard()
            ParadoxoStatTile(label: .paradoxoTeacherApproval, value: "41%", tone: UNESColor.coral)
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
