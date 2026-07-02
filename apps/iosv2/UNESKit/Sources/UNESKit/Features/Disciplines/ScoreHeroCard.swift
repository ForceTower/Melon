import SwiftUI

/// The semester scorecard atop Turmas: partial mean, discipline count, one
/// equalizer bar per discipline, and an attention line when something needs
/// care. Bars grow in with a stagger on first appearance.
struct ScoreHeroCard: View {
    let semester: SemesterDisciplines

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Média parcial")
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink3)

            HStack(alignment: .bottom, spacing: 8) {
                HStack(alignment: .bottom, spacing: 10) {
                    Text(formatGrade(semester.partialMean))
                        .font(.system(size: 52, weight: .bold))
                        .tracking(-2.34)
                        .monospacedDigit()
                        .lineSpacing(0)
                        .foregroundStyle(semester.partialMean == nil ? UNESColor.ink4 : UNESColor.ink)

                    if let delta = deltaLabel {
                        HStack(spacing: 3) {
                            Image(systemName: delta.rising ? "chevron.up" : "chevron.down")
                                .font(.system(size: 10, weight: .bold))
                            Text(delta.text)
                                .font(.system(size: 13, weight: .semibold))
                                .monospacedDigit()
                        }
                        .foregroundStyle(delta.rising ? UNESColor.successGreen : UNESColor.alertRed)
                        .padding(.bottom, 8)
                    }
                }

                Spacer(minLength: 8)

                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(semester.disciplines.count)")
                        .font(.system(size: 26, weight: .bold))
                        .tracking(-0.78)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    Text(semester.disciplines.count == 1 ? "disciplina" : "disciplinas")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
                .padding(.bottom, 2)
            }
            .padding(.top, 4)

            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 1)
                .padding(.top, 16)

            equalizer
                .padding(.top, 12)

            if semester.attentionCount > 0 {
                attentionRow(count: semester.attentionCount)
                    .padding(.top, 14)
            }
        }
        .padding(EdgeInsets(top: 18, leading: 18, bottom: 16, trailing: 18))
        .background(UNESColor.card)
        .overlay(alignment: .topTrailing) { accentWash }
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.07), radius: 12, y: 8)
    }

    private var deltaLabel: (text: String, rising: Bool)? {
        guard let delta = semester.lastGradeDelta, abs(delta) >= 0.1 else { return nil }
        return (DisciplinesFormat.signedDelta(delta), delta >= 0)
    }

    /// Soft accent glow bleeding in from the top-right corner.
    private var accentWash: some View {
        EllipticalGradient(
            stops: [
                .init(color: UNESColor.accent, location: 0),
                .init(color: .clear, location: 0.46),
            ],
            center: UnitPoint(x: 1, y: 0),
            startRadiusFraction: 0,
            endRadiusFraction: 1.1
        )
        .opacity(0.12)
        .allowsHitTesting(false)
    }

    private var equalizer: some View {
        HStack(alignment: .bottom, spacing: 8) {
            ForEach(Array(semester.disciplines.enumerated()), id: \.element.id) { index, discipline in
                EqualizerBar(
                    average: discipline.partialAverage,
                    code: discipline.code,
                    color: UNESColor.disciplineColor(discipline.colorIndex),
                    delay: Double(index) * 0.05
                )
            }
        }
    }

    private func attentionRow(count: Int) -> some View {
        HStack(spacing: 7) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(UNESColor.caution)
            Text(count == 1 ? "1 disciplina pede atenção" : "\(count) disciplinas pedem atenção")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink2)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 12)
        .overlay(alignment: .top) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }
}

/// One discipline's bar: value on top, tinted bar, code underneath. Released
/// averages grow from the 6pt stub; unreleased render as a dashed ghost.
private struct EqualizerBar: View {
    var average: Double?
    var code: String
    var color: Color
    var delay: Double

    @State private var grown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var targetHeight: CGFloat {
        guard let average else { return 50 }
        return 6 + CGFloat(min(max(average, 0), 10) / 10) * 52
    }

    var body: some View {
        VStack(spacing: 6) {
            VStack(spacing: 4) {
                Text(formatGrade(average))
                    .font(.system(size: 9, weight: .bold))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)

                bar
                    .frame(maxWidth: 26)
                    .frame(height: grown ? targetHeight : 6)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 76, alignment: .bottom)

            Text(code)
                .font(.system(size: 9, weight: .semibold))
                .tracking(0.2)
                .lineLimit(1)
                .foregroundStyle(UNESColor.ink3)
        }
        .onAppear {
            guard !grown else { return }
            if reduceMotion {
                grown = true
            } else {
                withAnimation(UNESMotion.ease(0.7).delay(delay)) {
                    grown = true
                }
            }
        }
    }

    @ViewBuilder
    private var bar: some View {
        if average == nil {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .strokeBorder(UNESColor.ink4, style: StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
                .opacity(0.4)
        } else {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(color)
        }
    }
}

#Preview {
    ScrollView {
        ScoreHeroCard(semester: DisciplinesOverview.preview().current!)
            .padding(16)
    }
    .background(UNESColor.surface)
}
