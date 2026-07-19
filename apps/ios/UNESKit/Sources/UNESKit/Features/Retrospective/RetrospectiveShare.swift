import SwiftUI

/// One exportable 9:16 story frame per card: wordmark header, one hero
/// stat (or the closing recap composite), signed footer. 360×640pt,
/// rendered at 3× for a 1080×1920 story image.
struct RetroStoryRender: View {
    var deck: RetrospectiveDeck
    var card: RetroCard
    var firstName: String

    var body: some View {
        ZStack {
            Color(hex: 0x0B0712)
            StaticMeshView(variant: mesh)
            RadialGradient(
                colors: [.clear, Color(hex: 0x06040C, opacity: 0.8)],
                center: .top,
                startRadius: 100,
                endRadius: 760
            )

            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    RetroUnesMark(size: 20)
                    Spacer()
                    Text(String.localized(.retroStoryTag).uppercased())
                        .font(.system(size: 11, weight: .bold))
                        .tracking(1.4)
                        .foregroundStyle(.white.opacity(0.66))
                }

                Spacer()
                slide
                Spacer()

                HStack {
                    Text(firstName)
                        .font(.system(size: 13, weight: .bold))
                        .tracking(-0.1)
                    Spacer()
                    Text(deck.semesterLabel)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(.white.opacity(0.62))
                }
                .foregroundStyle(.white)
                .padding(.top, 16)
                .overlay(alignment: .top) {
                    Rectangle().fill(.white.opacity(0.16)).frame(height: 1)
                }
            }
            .padding(EdgeInsets(top: 30, leading: 26, bottom: 30, trailing: 26))
        }
        .frame(width: 360, height: 640)
        .environment(\.colorScheme, .dark)
    }

    private var mesh: MeshView.Variant {
        switch card {
        case .abertura, .conquista, .encerramento: .warm
        case .notas: .sun
        case .frequencia: .cool
        case .score: deck.score?.isDown == true ? .sun : .fresh
        case .turma: .sun
        }
    }

    @ViewBuilder
    private var slide: some View {
        switch card {
        case .abertura:
            hero(
                kicker: .localized(.retroStoryGlanceKicker),
                big: String(deck.glance.disciplines),
                unit: .localized(.retroStoryGlanceUnit),
                sub: .localized(.retroStoryGlanceSub(deck.glance.classHours))
            )
        case .notas:
            if let grades = deck.grades {
                hero(
                    kicker: .localized(.retroStoryNotasKicker),
                    big: formatGrade(grades.media),
                    sub: .localized(.retroStoryNotasSub(formatGrade(grades.bestGrade), grades.bestDiscipline))
                )
            }
        case .frequencia:
            if let attendance = deck.attendance {
                hero(
                    kicker: .localized(.retroStoryFreqKicker),
                    big: String(attendance.percent),
                    unit: "%",
                    sub: freqSub(missedHours: attendance.missedHours)
                )
            }
        case .conquista:
            if let victory = deck.victory {
                word(
                    kicker: .localized(.retroStoryConquistaKicker),
                    word: victory.discipline,
                    sub: victory.viaFinal
                        ? .localized(.retroStoryConquistaSubFinal(formatGrade(victory.grade)))
                        : .localized(.retroStoryConquistaSub(formatGrade(victory.grade)))
                )
            }
        case .score:
            if let score = deck.score {
                hero(
                    kicker: .localized(.retroStoryScoreKicker),
                    big: formatGrade(score.value),
                    sub: score.isFirst
                        ? .localized(.retroStoryScoreSubFirst)
                        : .localized(.retroScoreDeltaChip(retroSigned(score.delta ?? 0)))
                )
            }
        case .turma:
            if let turma = deck.turma {
                hero(
                    kicker: .localized(.retroTurmaEyebrow),
                    big: String(turma.percentile),
                    unit: "%",
                    sub: .localized(.retroStoryTurmaSub(turma.discipline))
                )
            }
        case .encerramento:
            recap
        }
    }

    private func freqSub(missedHours: Int) -> String {
        switch missedHours {
        case 0: .localized(.retroStoryFreqSubZero)
        case 1: .localized(.retroStoryFreqSubOne)
        default: .localized(.retroStoryFreqSub(missedHours))
        }
    }

    private func hero(kicker: String, big: String, unit: String? = nil, sub: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(kicker)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white.opacity(0.82))
                .padding(.bottom, 10)
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Text(big)
                    .font(.system(size: 96, weight: .heavy))
                    .tracking(-5)
                    .monospacedDigit()
                if let unit {
                    Text(unit)
                        .font(.system(size: 34, weight: .heavy))
                        .opacity(0.82)
                }
            }
            .foregroundStyle(.white)
            Text(sub)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white.opacity(0.86))
                .padding(.top, 16)
        }
    }

    private func word(kicker: String, word: String, sub: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(kicker)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white.opacity(0.82))
                .padding(.bottom, 10)
            Text(word)
                .font(.system(size: 40, weight: .heavy))
                .tracking(-1.6)
                .foregroundStyle(.white)
            Text(sub)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white.opacity(0.86))
                .padding(.top, 16)
        }
    }

    private var recap: some View {
        VStack(alignment: .leading, spacing: 22) {
            Text(deck.semesterLabel)
                .font(.system(size: 58, weight: .heavy))
                .tracking(-2.4)
                .foregroundStyle(.white)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 9), GridItem(.flexible())], spacing: 9) {
                if let grades = deck.grades {
                    recapCell(.localized(.retroFechoRecapMedia), formatGrade(grades.media))
                }
                if let score = deck.score {
                    recapCell("Score", formatGrade(score.value))
                }
                recapCell(.localized(.retroFechoRecapDisciplines), String(deck.glance.disciplines))
                if let attendance = deck.attendance {
                    recapCell(.localized(.retroFechoRecapPresence), "\(attendance.percent)%")
                }
            }
        }
    }

    private func recapCell(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased())
                .font(.system(size: 10, weight: .bold))
                .tracking(0.5)
                .foregroundStyle(.white.opacity(0.6))
            Text(value)
                .font(.system(size: 24, weight: .heavy))
                .tracking(-1)
                .monospacedDigit()
                .foregroundStyle(.white)
        }
        .padding(EdgeInsets(top: 11, leading: 13, bottom: 11, trailing: 13))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(.white.opacity(0.15))
        }
    }
}

/// The share sheet: the current card's story render previewed, exported
/// through the system share sheet.
struct RetroShareSheet: View {
    var deck: RetrospectiveDeck
    var card: RetroCard
    var firstName: String

    @State private var rendered: Image?

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 1) {
                    Text(.retroShareTitle)
                        .font(.system(size: 20, weight: .bold))
                        .tracking(-0.4)
                        .foregroundStyle(UNESColor.ink)
                    Text(.retroShareFormat)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
                Spacer()
            }
            .padding(EdgeInsets(top: 24, leading: 20, bottom: 6, trailing: 20))

            Spacer()
            RetroStoryRender(deck: deck, card: card, firstName: firstName)
                .scaleEffect(0.55, anchor: .center)
                .frame(width: 360 * 0.55, height: 640 * 0.55)
                .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                .shadow(color: .black.opacity(0.4), radius: 25, y: 12)
            Spacer()

            if let rendered {
                ShareLink(
                    item: rendered,
                    preview: SharePreview(String.localized(.retroStoryTag), image: rendered)
                ) {
                    HStack(spacing: 9) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 15, weight: .bold))
                        Text(.retroShareAction)
                            .font(.system(size: 16, weight: .semibold))
                            .tracking(-0.2)
                    }
                    .foregroundStyle(UNESColor.surface)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(UNESColor.ink, in: Capsule())
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 16)
            }
        }
        .background(UNESColor.surface)
        .task { render() }
    }

    @MainActor
    private func render() {
        let renderer = ImageRenderer(
            content: RetroStoryRender(deck: deck, card: card, firstName: firstName)
        )
        renderer.scale = 3
        renderer.proposedSize = ProposedViewSize(width: 360, height: 640)
        guard let cgImage = renderer.cgImage else { return }
        rendered = Image(decorative: cgImage, scale: 3)
    }
}

#Preview {
    RetroStoryRender(
        deck: RetrospectiveDeck(
            semesterCode: "20261",
            semesterLabel: "2026.1",
            nextLabel: "2026.2",
            glance: RetrospectiveDeck.Glance(disciplines: 5, classHours: 270, weeks: 20),
            grades: RetrospectiveDeck.Grades(media: 7.4, bestGrade: 9.2, bestDiscipline: "Estruturas Discretas"),
            attendance: RetrospectiveDeck.Attendance(percent: 94, missedHours: 16),
            victory: nil,
            score: RetrospectiveDeck.ScoreCard(value: 6.7, previous: 6.6, series: [6.2, 6.4, 6.6, 6.7]),
            failures: []
        ),
        card: .encerramento,
        firstName: "Marina"
    )
}
