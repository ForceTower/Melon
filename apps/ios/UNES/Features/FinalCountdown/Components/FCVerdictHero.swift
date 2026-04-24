import SwiftUI

/// The dramatic gradient card at the top of the calculator. Collates the
/// status pill, the radial dial, the title lines, and the big headline row
/// (needed grade + sub + message). When the verdict is `final`, it also
/// draws the horizontal difficulty meter at the bottom.
struct FCVerdictHero: View {
    let verdict: FCVerdict
    let weighted: Bool

    private var copy: FCVerdictCopy { FinalCountdownCopy.copy(for: verdict) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            headerRow
                .padding(.bottom, 14)

            FCMeter(avg: verdict.avg, tone: copy.tone)

            titleBlock
                .padding(.top, 4)

            headlineBlock
                .padding(.top, 16)

            if verdict.kind == .final, let need = verdict.need {
                difficultyMeter(need: need)
                    .padding(.top, 14)
            }
        }
        .padding(18)
        .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255))
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(backgroundGradient)
        )
        .overlay {
            // Decorative corner glow — picks up the verdict's tone. Matches
            // the JS `radial-gradient(circle, tone 0%, transparent 70%)` on
            // a 160×160 div sitting at `top: -30, right: -30` with
            // `border-radius: 50%`: the glow's center lands 50 px inside
            // from the card's trailing edge and 50 px down from the top,
            // half of its circle spilling past the corner (clipped by the
            // card's rounded-rect mask applied below).
            GeometryReader { geo in
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [copy.tone.bg.opacity(0.33), .clear],
                            center: .center,
                            startRadius: 0,
                            endRadius: 80
                        )
                    )
                    .frame(width: 160, height: 160)
                    .position(x: geo.size.width - 50, y: 50)
            }
            .allowsHitTesting(false)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .shadow(color: .black.opacity(0.18), radius: 20, x: 0, y: 16)
    }

    // MARK: - Pieces

    private var headerRow: some View {
        HStack {
            HStack(spacing: 8) {
                ZStack {
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .fill(copy.tone.bg)
                        .frame(width: 22, height: 22)
                    Image(systemName: copy.icon)
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(copy.tone.fg)
                }
                Text(copy.eyebrow.uppercased())
                    .font(UNESFont.mono(9))
                    .tracking(1.44)
                    .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255))
            }
            .padding(.leading, 8)
            .padding(.trailing, 10)
            .padding(.vertical, 5)
            .background(
                Capsule()
                    .fill(Color.white.opacity(0.08))
                    .overlay(Capsule().strokeBorder(Color.white.opacity(0.12), lineWidth: 1))
            )

            Spacer(minLength: 0)

            Text((weighted ? "PONDERADO" : "MÉDIA SIMPLES"))
                .font(UNESFont.mono(9))
                .tracking(0.9)
                .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.55))
        }
    }

    private var titleBlock: some View {
        VStack(spacing: 2) {
            let lines = copy.titleLines.enumerated().filter { !$0.element.isEmpty }
            ForEach(Array(lines), id: \.offset) { offset, line in
                Text(line)
                    .font(UNESFont.serif(34, italic: offset == 1))
                    .tracking(-0.68)
                    .foregroundStyle(offset == 1 ? italicLineColor : Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255))
                    .opacity(offset == 1 ? 0.9 : 1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .multilineTextAlignment(.center)
    }

    private var headlineBlock: some View {
        HStack(alignment: .center, spacing: 14) {
            Text(copy.headline)
                .font(UNESFont.serif(56))
                .tracking(-1.68)
                .foregroundStyle(headlineColor)
                .lineLimit(1)
                .minimumScaleFactor(0.5)

            VStack(alignment: .leading, spacing: 3) {
                Text(copy.sub.uppercased())
                    .font(UNESFont.mono(9.5))
                    .tracking(0.95)
                    .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.55))
                Text(copy.message)
                    .font(UNESFont.sans(12))
                    .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.85))
                    .lineSpacing(1.4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(14)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color.white.opacity(0.06))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(Color.white.opacity(0.1), lineWidth: 1)
                )
        )
    }

    private func difficultyMeter(need: Double) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("DIFÍCIL · 0").font(UNESFont.mono(8.5)).tracking(1.19)
                Spacer()
                Text("CRUEL · 5").font(UNESFont.mono(8.5)).tracking(1.19)
                Spacer()
                Text("BRUTAL · 10").font(UNESFont.mono(8.5)).tracking(1.19)
            }
            .foregroundStyle(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255).opacity(0.5))

            GeometryReader { geo in
                let clamped = max(0, min(10, need)) / 10
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(LinearGradient(
                            colors: [
                                FCTone.green.bg.opacity(0.5),
                                FCTone.amber.bg.opacity(0.7),
                                FCTone.coral.bg.opacity(0.85),
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        ))
                        .frame(height: 6)

                    Capsule()
                        .fill(Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255))
                        .frame(width: 3, height: 14)
                        .shadow(color: .white.opacity(0.3), radius: 5)
                        .offset(x: geo.size.width * CGFloat(clamped) - 1.5)
                }
                .frame(height: 14)
            }
            .frame(height: 14)
        }
    }

    // MARK: - Colors

    private var backgroundGradient: LinearGradient {
        let stops: (Color, Color)
        switch verdict.kind {
        case .passed:
            stops = (Color(red: 0x1A/255, green: 0x3A/255, blue: 0x28/255),
                     Color(red: 0x0F/255, green: 0x24/255, blue: 0x18/255))
        case .failed, .impossible:
            stops = (Color(red: 0x2A/255, green: 0x16/255, blue: 0x24/255),
                     Color(red: 0x18/255, green: 0x0D/255, blue: 0x1A/255))
        case .final, .borderlineFinal, .failingTrack:
            stops = (Color(red: 0x3A/255, green: 0x1E/255, blue: 0x1A/255),
                     Color(red: 0x20/255, green: 0x11/255, blue: 0x10/255))
        case .borderline:
            stops = (Color(red: 0x3A/255, green: 0x2A/255, blue: 0x12/255),
                     Color(red: 0x20/255, green: 0x16/255, blue: 0x08/255))
        case .ontrack, .empty:
            stops = (Color(red: 0x1A/255, green: 0x2A/255, blue: 0x2F/255),
                     Color(red: 0x0E/255, green: 0x16/255, blue: 0x18/255))
        }
        return LinearGradient(
            colors: [stops.0, stops.1],
            startPoint: UnitPoint(x: 0.15, y: 0),
            endPoint: UnitPoint(x: 1, y: 1)
        )
    }

    private var italicLineColor: Color {
        // For amber tones the italic line uses the peach stop; otherwise the
        // tone's own foreground.
        copy.tone == .amber ? Color(red: 0xFB/255, green: 0xD9/255, blue: 0xA8/255) : copy.tone.fg
    }

    private var headlineColor: Color {
        // Amber-toned verdicts (borderline) use peach for readability; tones
        // whose `fg` is the surface invert to the tone itself so the number
        // pops against the dark panel.
        if copy.tone == .amber {
            return Color(red: 0xFB/255, green: 0xD9/255, blue: 0xA8/255)
        }
        let surfaceLight = Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255)
        if copy.tone.fg == surfaceLight {
            return copy.tone.bg
        }
        return surfaceLight
    }
}
