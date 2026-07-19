import SwiftUI

// The story cards, one view per beat. Each re-runs its entrance
// choreography when it becomes current — the player keys the card view
// by index.

// MARK: - 1 · Abertura

struct RetroCardAbertura: View {
    var deck: RetrospectiveDeck

    var body: some View {
        RetroCardShell(mesh: .warm, dim: 0.66) {
            RetroEyebrow(text: .localized(.retroAberturaEyebrow))
            Spacer()
            Text(deck.semesterLabel)
                .font(.system(size: 96, weight: .heavy))
                .tracking(-5)
                .foregroundStyle(.white)
                .fadeUp(delay: 0.18)
            Text(.retroAberturaSubtitle)
                .font(.system(size: 22, weight: .semibold))
                .tracking(-0.4)
                .foregroundStyle(.white.opacity(0.9))
                .padding(.top, 14)
                .fadeUp(delay: 0.42)
            Spacer()
            HStack(spacing: 30) {
                glanceStat(value: deck.glance.disciplines, label: .localized(.retroAberturaDisciplines), delay: 0.5)
                glanceStat(value: deck.glance.classHours, label: .localized(.retroAberturaHours), delay: 0.62)
            }
            .padding(.top, 20)
            .overlay(alignment: .top) {
                Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
            }
            .fadeUp(delay: 0.6)
        }
    }

    private func glanceStat(value: Int, label: String, delay: Double) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            RetroCountUp(target: Double(value), delay: delay, duration: 1) {
                Text($0)
                    .font(.system(size: 40, weight: .heavy))
                    .tracking(-1.6)
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            Text(label)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.white.opacity(0.66))
        }
    }
}

// MARK: - 2 · Notas

struct RetroCardNotas: View {
    var grades: RetrospectiveDeck.Grades

    var body: some View {
        RetroCardShell(mesh: .sun) {
            RetroEyebrow(text: .localized(.retroNotasEyebrow))
            Spacer()
            Text(.retroNotasLead)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(.white.opacity(0.82))
                .padding(.bottom, 8)
                .fadeUp(delay: 0.16)
            RetroCountUp(target: grades.media, decimals: 1, delay: 0.34) {
                Text($0)
                    .font(.system(size: 112, weight: .heavy))
                    .tracking(-6)
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            .popIn(delay: 0.2, from: 0.92)
            Spacer()
            HStack(spacing: 14) {
                Image(systemName: "star")
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(Color(hex: 0xFFDFA0))
                    .frame(width: 46, height: 46)
                    .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text(String.localized(.retroNotasBestLabel).uppercased())
                        .font(.system(size: 12.5, weight: .bold))
                        .tracking(0.6)
                        .foregroundStyle(.white.opacity(0.6))
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        Text(formatGrade(grades.bestGrade))
                            .font(.system(size: 26, weight: .heavy))
                            .tracking(-0.8)
                            .monospacedDigit()
                        Text(.retroNotasBestIn(grades.bestDiscipline))
                            .font(.system(size: 15.5, weight: .semibold))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                    .foregroundStyle(.white)
                }
            }
            .padding(EdgeInsets(top: 15, leading: 18, bottom: 15, trailing: 18))
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(.white.opacity(0.14))
            }
            .fadeUp(delay: 0.56)
        }
    }
}

// MARK: - 3 · Frequência

struct RetroCardFrequencia: View {
    var attendance: RetrospectiveDeck.Attendance

    var body: some View {
        RetroCardShell(mesh: .cool) {
            RetroEyebrow(text: .localized(.retroFreqEyebrow))
            Spacer()
            HStack {
                Spacer()
                RetroProgressRing(percent: attendance.percent) {
                    VStack(spacing: 4) {
                        RetroCountUp(target: Double(attendance.percent), delay: 0.38, duration: 1.25) {
                            Text($0 + "%")
                                .font(.system(size: 52, weight: .heavy))
                                .tracking(-2)
                                .monospacedDigit()
                                .foregroundStyle(.white)
                        }
                        Text(String.localized(.retroFreqRingLabel).uppercased())
                            .font(.system(size: 12.5, weight: .semibold))
                            .tracking(0.5)
                            .foregroundStyle(.white.opacity(0.6))
                    }
                }
                Spacer()
            }
            .popIn(delay: 0.16, from: 0.92)
            Spacer()
            Text(headline)
                .font(.system(size: 30, weight: .heavy))
                .tracking(-0.9)
                .lineSpacing(2)
                .foregroundStyle(.white)
                .fadeUp(delay: 0.62)
            RetroCaption(text: note, delay: 0.78)
                .padding(.top, 12)
        }
    }

    private var headline: String {
        switch attendance.missedHours {
        case 0: .localized(.retroFreqHeadlineZero)
        case 1: .localized(.retroFreqHeadlineOne)
        default: .localized(.retroFreqHeadline(attendance.missedHours))
        }
    }

    private var note: String {
        switch attendance.percent {
        case 95...: .localized(.retroFreqNoteHigh)
        case 90..<95: .localized(.retroFreqNoteSteady)
        default: .localized(.retroFreqNoteTough)
        }
    }
}

// MARK: - 4 · Maior virada

struct RetroCardConquista: View {
    var victory: RetrospectiveDeck.Victory

    var body: some View {
        RetroCardShell(mesh: .warm, dim: 0.78) {
            RetroEyebrow(text: .localized(.retroConquistaEyebrow))
            Spacer()
            HStack(spacing: 8) {
                Image(systemName: "checkmark")
                    .font(.system(size: 13, weight: .heavy))
                Text(victory.viaFinal ? .retroConquistaChipFinal : .retroConquistaChip)
                    .font(.system(size: 14.5, weight: .heavy))
            }
            .foregroundStyle(Color(hex: 0x7BEE99))
            .padding(.horizontal, 15)
            .padding(.vertical, 8)
            .background(Color(hex: 0x5CE07A, opacity: 0.2), in: Capsule())
            .padding(.bottom, 20)
            .fadeUp(delay: 0.16)
            Text(victory.discipline)
                .font(.system(size: 46, weight: .heavy))
                .tracking(-1.8)
                .lineSpacing(0)
                .foregroundStyle(.white)
                .fadeUp(delay: 0.28)
            HStack(alignment: .lastTextBaseline, spacing: 10) {
                Text(String.localized(.retroConquistaGradeLabel).uppercased())
                    .font(.system(size: 13, weight: .bold))
                    .tracking(0.6)
                    .foregroundStyle(.white.opacity(0.55))
                Text(formatGrade(victory.grade))
                    .font(.system(size: 30, weight: .heavy))
                    .tracking(-0.9)
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            .padding(.top, 16)
            .fadeUp(delay: 0.5)
            Spacer()
            RetroCaption(
                text: victory.viaFinal
                    ? .localized(.retroConquistaLineFinal)
                    : .localized(.retroConquistaLine),
                delay: 0.66
            )
        }
    }
}

// MARK: - 5 · Score

struct RetroCardScore: View {
    var score: RetrospectiveDeck.ScoreCard

    var body: some View {
        RetroCardShell(mesh: score.isDown ? .sun : .fresh, dim: 0.72) {
            RetroEyebrow(
                text: .localized(score.isFirst ? .retroScoreEyebrowFirst : .retroScoreEyebrow)
            )
            Spacer()
            VStack(spacing: 18) {
                RetroCountUp(target: score.value, decimals: 1, delay: 0.34) {
                    Text($0)
                        .font(.system(size: 108, weight: .heavy))
                        .tracking(-5.5)
                        .monospacedDigit()
                        .foregroundStyle(.white)
                }
                .popIn(delay: 0.16, from: 0.92)
                RetroTrendChip(score: score)
                    .fadeUp(delay: 0.62)
                Group {
                    if score.isFirst {
                        VStack(spacing: 12) {
                            Circle()
                                .fill(Color(hex: 0x5CE07A))
                                .frame(width: 16, height: 16)
                                .shadow(color: Color(hex: 0x5CE07A, opacity: 0.4), radius: 8)
                            Text(.retroScoreFirstFooter)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.6))
                        }
                        .padding(.top, 20)
                    } else {
                        RetroScoreChart(points: score.series, up: !score.isDown)
                    }
                }
                .frame(minHeight: 104)
                .fadeUp(delay: 0.8)
            }
            .frame(maxWidth: .infinity)
            Spacer()
            RetroCaption(text: caption, delay: 0.9)
        }
    }

    private var caption: String {
        if score.isFirst { return .localized(.retroScoreCaptionFirst) }
        return score.isDown ? .localized(.retroScoreCaptionDown) : .localized(.retroScoreCaptionUp)
    }
}

// MARK: - 6 · Turma (percentile — ours, in the design's language)

struct RetroCardTurma: View {
    var turma: RetrospectiveDeck.Turma

    var body: some View {
        RetroCardShell(mesh: .sun, dim: 0.72) {
            RetroEyebrow(text: .localized(.retroTurmaEyebrow))
            Spacer()
            Text(.retroTurmaLead)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(.white.opacity(0.82))
                .padding(.bottom, 8)
                .fadeUp(delay: 0.16)
            RetroCountUp(target: Double(turma.percentile), delay: 0.34) {
                Text($0 + "%")
                    .font(.system(size: 108, weight: .heavy))
                    .tracking(-5)
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            .popIn(delay: 0.2, from: 0.92)
            Text(.retroTurmaHeadline(turma.discipline))
                .font(.system(size: 30, weight: .heavy))
                .tracking(-0.9)
                .foregroundStyle(.white)
                .padding(.top, 16)
                .fadeUp(delay: 0.56)
            Spacer()
            RetroCaption(text: .localized(.retroTurmaCaption(turma.cohortSize)), delay: 0.72)
        }
    }
}

// MARK: - 7 · Encerramento

struct RetroCardEncerramento: View {
    var deck: RetrospectiveDeck
    var onShare: () -> Void

    var body: some View {
        RetroCardShell(mesh: .warm, dim: 0.72) {
            HStack {
                RetroUnesMark(size: 22)
                Spacer()
                Text(deck.semesterLabel)
                    .font(.system(size: 12.5, weight: .bold))
                    .tracking(1.8)
                    .foregroundStyle(.white.opacity(0.6))
            }
            .fadeUp(delay: 0.05)
            Spacer()
            Text(closingHeadline)
                .font(.system(size: 38, weight: .heavy))
                .tracking(-1.3)
                .lineSpacing(1)
                .foregroundStyle(.white)
                .fadeUp(delay: 0.16)
            RetroCaption(text: closingSub, delay: 0.4)
                .padding(.top, 10)
            recapGrid
                .padding(.top, 24)
                .fadeUp(delay: 0.56)
            if !deck.failures.isEmpty {
                Text(.retroFechoRetakes(deck.failures.count, deck.nextLabel))
                    .font(.system(size: 13.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.7))
                    .padding(.top, 12)
                    .fadeUp(delay: 0.66)
            }
            Spacer()
            Button(action: onShare) {
                HStack(spacing: 9) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 15, weight: .bold))
                    Text(.retroFechoShareButton)
                        .font(.system(size: 16.5, weight: .bold))
                        .tracking(-0.2)
                }
                .foregroundStyle(Color(hex: 0x140F1C))
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(.white, in: Capsule())
                .shadow(color: .black.opacity(0.34), radius: 17, y: 14)
            }
            .padding(.top, 8)
            .fadeUp(delay: 0.8)
        }
    }

    private var closingHeadline: String {
        switch deck.profile {
        case .freshman: .localized(.retroFechoHeadlineFirst)
        case .struggled: .localized(.retroFechoHeadlineTough(deck.semesterLabel))
        case .veteran:
            deck.score?.isDown == true
                ? .localized(.retroFechoHeadlineDown(deck.semesterLabel))
                : .localized(.retroFechoHeadlineUp(deck.semesterLabel))
        }
    }

    private var closingSub: String {
        switch deck.profile {
        case .freshman: .localized(.retroFechoSubFirst)
        case .struggled: .localized(.retroFechoSubTough(deck.nextLabel))
        case .veteran:
            deck.score?.isDown == true
                ? .localized(.retroFechoSubDown(deck.nextLabel))
                : .localized(.retroFechoSubUp)
        }
    }

    private var recapGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible())], spacing: 10) {
            if let grades = deck.grades {
                recapCell(label: .localized(.retroFechoRecapMedia), value: formatGrade(grades.media))
            }
            if let score = deck.score {
                recapCell(
                    label: "Score",
                    value: formatGrade(score.value),
                    sub: score.isFirst ? .localized(.retroFechoRecapFirst) : retroSigned(score.delta ?? 0)
                )
            }
            recapCell(label: .localized(.retroFechoRecapDisciplines), value: String(deck.glance.disciplines))
            if let attendance = deck.attendance {
                recapCell(label: .localized(.retroFechoRecapPresence), value: String(attendance.percent), sub: "%")
            }
        }
    }

    private func recapCell(label: String, value: String, sub: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label.uppercased())
                .font(.system(size: 11.5, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(.white.opacity(0.58))
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Text(value)
                    .font(.system(size: 30, weight: .heavy))
                    .tracking(-1.2)
                    .monospacedDigit()
                    .foregroundStyle(.white)
                if let sub {
                    Text(sub)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
        }
        .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.09), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(.white.opacity(0.14))
        }
    }
}

#Preview {
    RetroCardScore(
        score: RetrospectiveDeck.ScoreCard(value: 7.8, previous: 7.4, series: [6.9, 7.1, 7.0, 7.4, 7.8])
    )
}
