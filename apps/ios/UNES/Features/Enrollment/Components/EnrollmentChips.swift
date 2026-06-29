import SwiftUI

// UNES — small enrollment primitives: section eyebrow, code chip, status badge
// and the seat meter. Ported from `screens-matricula-ui.jsx`.

/// Mono uppercase label used as a section eyebrow ("◦ carga horária").
struct EnrollmentEyebrow: View {
    let text: String
    var color: Color = UNESColor.ink3
    var size: CGFloat = 10

    var body: some View {
        Text(text.uppercased())
            .font(UNESFont.mono(size, weight: .semibold))
            .tracking(size * 0.14)
            .foregroundStyle(color)
    }
}

/// Tinted, monospaced discipline-code chip (e.g. "EXA427").
struct CodeChip: View {
    let code: String
    let tone: EnrollmentTone
    var small: Bool = false

    var body: some View {
        Text(code)
            .font(UNESFont.mono(small ? 9 : 10, weight: .semibold))
            .tracking((small ? 9 : 10) * 0.08)
            .foregroundStyle(tone.color)
            .padding(.horizontal, small ? 5 : 7)
            .padding(.vertical, small ? 2 : 3)
            .background(
                RoundedRectangle(cornerRadius: 6, style: .continuous)
                    .fill(tone.color.opacity(0.12))
            )
            .lineLimit(1)
    }
}

/// Pill badge describing a section's standing in the catalogue and proposal.
struct EnrollmentBadge: View {
    enum Kind {
        case mandatory      // obrigatória
        case optional       // optativa
        case suggested      // sugerida pelo curso
        case prereq         // pré-requisito (pendente)
        case waitlist       // na fila de espera
        case inProposal     // already in the proposal (shows the section label)
    }

    let kind: Kind
    let text: String

    var body: some View {
        HStack(spacing: 5) {
            indicator
            Text(text)
                .font(UNESFont.sans(10.5, weight: .semibold))
                .tracking(0.21)
        }
        .foregroundStyle(foreground)
        .padding(.horizontal, 8)
        .padding(.vertical, 3)
        .background(
            Capsule(style: .continuous).fill(background)
        )
        .overlay {
            if kind == .optional {
                Capsule(style: .continuous).strokeBorder(UNESColor.cardLine, lineWidth: 1)
            }
        }
    }

    @ViewBuilder
    private var indicator: some View {
        switch kind {
        case .suggested:
            Circle().fill(EnrollmentPalette.okSolid).frame(width: 5, height: 5)
        case .waitlist:
            Circle().fill(EnrollmentPalette.warnSolid).frame(width: 5, height: 5)
        case .prereq:
            Text("!")
                .font(UNESFont.sans(9, weight: .heavy))
                .foregroundStyle(UNESColor.card)
                .frame(width: 12, height: 12)
                .background(Circle().fill(EnrollmentPalette.danger))
        case .inProposal:
            Image(systemName: "checkmark")
                .font(.system(size: 8, weight: .bold))
        case .mandatory, .optional:
            EmptyView()
        }
    }

    private var foreground: Color {
        switch kind {
        case .mandatory:  return UNESColor.ink2
        case .optional:   return UNESColor.ink3
        case .suggested:  return EnrollmentPalette.ok
        case .prereq:     return EnrollmentPalette.danger
        case .waitlist:   return EnrollmentPalette.warn
        case .inProposal: return UNESColor.surface
        }
    }

    private var background: Color {
        switch kind {
        case .mandatory:  return UNESColor.surface2
        case .optional:   return .clear
        case .suggested:  return EnrollmentPalette.okSolid.opacity(0.10)
        case .prereq:     return EnrollmentPalette.danger.opacity(0.10)
        case .waitlist:   return EnrollmentPalette.warnSolid.opacity(0.10)
        case .inProposal: return UNESColor.ink
        }
    }
}

/// Compact "X / Y" seat gauge with a fill bar and "vagas / quase cheia /
/// lotada" caption colored by fill pressure.
struct SeatMeter: View {
    let section: ClassSection
    var compact: Bool = false

    private var seats: SeatState { SeatState(section) }

    private var color: Color {
        if seats.isFull { return EnrollmentPalette.danger }
        if seats.isTight { return EnrollmentPalette.warnSolid }
        return EnrollmentPalette.okSolid
    }

    private var caption: String {
        if seats.isFull { return "lotada" }
        if seats.isTight { return "quase cheia" }
        return "vagas"
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("\(seats.filled)")
                    .font(UNESFont.sans(compact ? 15 : 17, weight: .bold))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                Text("/ \(seats.total)")
                    .font(UNESFont.mono(10))
                    .foregroundStyle(UNESColor.ink4)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(UNESColor.surface3)
                    Capsule()
                        .fill(color)
                        .frame(width: max(0, min(1, seats.pct)) * geo.size.width)
                }
            }
            .frame(height: 4)
            Text(caption.uppercased())
                .font(UNESFont.mono(8.5, weight: .semibold))
                .tracking(0.68)
                .foregroundStyle(color)
        }
        .frame(width: compact ? 64 : 86, alignment: .leading)
    }
}
