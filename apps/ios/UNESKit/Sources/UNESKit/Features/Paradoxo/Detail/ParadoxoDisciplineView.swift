import ComposableArchitecture
import SwiftUI

struct ParadoxoDisciplineView: View {
    @Bindable var store: StoreOf<ParadoxoDisciplineFeature>

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            if let details = store.details {
                ParadoxoToneWash(tone: paradoxoTone(details.mean))
            }
            content
        }
        .navigationTitle(store.details?.code ?? store.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .task { await store.send(.task).finish() }
    }

    @ViewBuilder
    private var content: some View {
        if let details = store.details {
            loaded(details)
        } else if store.loadFailed {
            ParadoxoFailureView { store.send(.retryTapped) }
        } else {
            ParadoxoLoadingView()
        }
    }

    private func loaded(_ details: ParadoxoDisciplineDetails) -> some View {
        let tone = paradoxoTone(details.mean)
        return ScrollView {
            VStack(spacing: 18) {
                hero(details, tone: tone)
                    .scaleIn(delay: 0.02, duration: 0.62)

                chartCard(details, tone: tone)
                    .fadeUp(delay: 0.1)

                if !details.distribution.isEmpty {
                    distributionCard(details, tone: tone)
                        .fadeUp(delay: 0.16)
                }

                if !details.history.isEmpty {
                    insights(details)
                        .fadeUp(delay: 0.22)
                }

                if !details.teachers.isEmpty {
                    teachers(details)
                        .fadeUp(delay: 0.28)
                }
            }
            .padding(EdgeInsets(top: 8, leading: 16, bottom: 32, trailing: 16))
        }
        .scrollIndicators(.hidden)
    }

    // MARK: Hero

    private func hero(_ details: ParadoxoDisciplineDetails, tone: Color) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                Text(details.code)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.44)
                    .foregroundStyle(tone)
                    .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                    .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                if let department = details.department {
                    // Upstream already sends the full "Departamento de …" name.
                    Text(department)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
            }

            Text(details.name)
                .font(.system(size: 25, weight: .bold))
                .tracking(-0.75)
                .foregroundStyle(UNESColor.ink)
                .lineSpacing(1)
                .padding(.top, 12)

            HStack(alignment: .bottom, spacing: 14) {
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(formatGrade(details.mean))
                        .font(.system(size: 60, weight: .bold))
                        .monospacedDigit()
                        .tracking(-3)
                        .foregroundStyle(tone)
                    Text(verbatim: "/10")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(UNESColor.ink4)
                }
                ParadoxoTierChip(mean: details.mean)
                    .padding(.bottom, 10)
            }
            .padding(.top, 10)

            Text(.paradoxoDetailCalculatedWith(ParadoxoFormat.count(details.studentCount)))
                .font(.system(size: 12.5, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 6)

            ParadoxoOutcomesBar(approved: details.approved, failed: details.failed, quit: details.quit)
                .padding(.top, 16)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        .background {
            LinearGradient.css(
                stops: [
                    .init(color: tone.opacity(0.12), location: 0),
                    .init(color: .clear, location: 0.62),
                ],
                angle: 160
            )
            .background(UNESColor.card)
        }
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.08), radius: 16, y: 8)
    }

    // MARK: Chart

    private func chartCard(_ details: ParadoxoDisciplineDetails, tone: Color) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(.paradoxoChartTitle)
                .font(.system(size: 16, weight: .bold))
                .tracking(-0.32)
                .foregroundStyle(UNESColor.ink)
                .frame(maxWidth: .infinity, alignment: .leading)

            ParadoxoHistoryChart(history: details.history, tone: tone)

            if let peak = details.history.max(by: { $0.mean < $1.mean }),
               let trough = details.history.min(by: { $0.mean < $1.mean }) {
                HStack(spacing: 8) {
                    miniFact(
                        icon: "arrow.up",
                        tone: UNESColor.successGreen,
                        label: .paradoxoChartPeak,
                        value: "\(formatGrade(peak.mean)) · \(peak.semester)"
                    )
                    miniFact(
                        icon: "arrow.down",
                        tone: UNESColor.coral,
                        label: .paradoxoChartTrough,
                        value: "\(formatGrade(trough.mean)) · \(trough.semester)"
                    )
                }
            }
        }
        .padding(16)
        .paradoxoCard()
    }

    private func miniFact(icon: String, tone: Color, label: LocalizedStringResource, value: String) -> some View {
        HStack(spacing: 9) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(tone)
                .frame(width: 24, height: 24)
                .background(tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            VStack(alignment: .leading, spacing: 1) {
                Text(label)
                    .textCase(.uppercase)
                    .font(.system(size: 10.5, weight: .semibold))
                    .tracking(0.42)
                    .foregroundStyle(UNESColor.ink4)
                Text(value)
                    .font(.system(size: 13, weight: .semibold))
                    .monospacedDigit()
                    .tracking(-0.13)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 9, leading: 11, bottom: 9, trailing: 11))
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    // MARK: Distribution

    private func distributionCard(_ details: ParadoxoDisciplineDetails, tone: Color) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(.paradoxoDistTitle)
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .foregroundStyle(UNESColor.ink)
                Spacer()
                Text(ParadoxoStats.shapeKind(of: details.distribution).label)
                    .font(.system(size: 11.5, weight: .bold))
                    .foregroundStyle(tone)
                    .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                    .background(tone.opacity(0.1), in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            }

            ParadoxoDistributionChart(
                distribution: details.distribution,
                tone: tone,
                myGrade: details.myGrade
            )

            if let myGrade = details.myGrade,
               let percentile = ParadoxoStats.percentile(of: myGrade, in: details.distribution) {
                Text(.paradoxoDistTopPercent(ParadoxoFormat.percent(100 - percentile)))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(16)
        .paradoxoCard()
    }

    // MARK: Insights

    private func insights(_ details: ParadoxoDisciplineDetails) -> some View {
        let peak = details.history.max { $0.mean < $1.mean }
        let trough = details.history.min { $0.mean < $1.mean }
        let means = details.teachers.map(\.mean)
        let spread = (means.max() ?? 0) - (means.min() ?? 0)

        return VStack(spacing: 0) {
            ParadoxoSectionHeader(.paradoxoDetailInsights)
            VStack(spacing: 0) {
                if let peak {
                    ParadoxoInsightRow(
                        icon: "arrow.up",
                        tone: UNESColor.successGreen,
                        text: .paradoxoInsightPeak(peak.semester, formatGrade(peak.mean))
                    )
                }
                if let trough {
                    insetDivider
                    ParadoxoInsightRow(
                        icon: "arrow.down",
                        tone: UNESColor.coral,
                        text: .paradoxoInsightTrough(trough.semester, formatGrade(trough.mean))
                    )
                }
                if details.teachers.count > 1, spread > 1.5 {
                    insetDivider
                    ParadoxoInsightRow(
                        icon: "circle.righthalf.filled",
                        tone: UNESColor.magenta,
                        text: .paradoxoInsightGap(formatGrade(spread))
                    )
                }
            }
            .paradoxoCard()
        }
    }

    private var insetDivider: some View {
        Divider()
            .overlay(UNESColor.line)
            .padding(.leading, 56)
    }

    // MARK: Teachers

    private func teachers(_ details: ParadoxoDisciplineDetails) -> some View {
        VStack(spacing: 0) {
            ParadoxoSectionHeader(.paradoxoDetailTeachers, note: .paradoxoDetailTeachersNote)
            VStack(spacing: 8) {
                ForEach(details.teachers) { teacher in
                    teacherCard(teacher)
                }
            }
        }
    }

    private func teacherCard(_ teacher: ParadoxoDisciplineTeacher) -> some View {
        let expanded = store.expandedTeacherId == teacher.id
        let tier = ParadoxoTier(mean: teacher.mean)
        return VStack(spacing: 0) {
            Button {
                if teacher.history.count > 1 {
                    store.send(.teacherExpansionToggled(teacher.id), animation: UNESMotion.ease(0.35))
                } else {
                    store.send(.teacherPageTapped(teacher))
                }
            } label: {
                HStack(spacing: 13) {
                    ParadoxoScoreTile(score: teacher.mean)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(teacher.name)
                            .font(.system(size: 15.5, weight: .semibold))
                            .tracking(-0.31)
                            .foregroundStyle(UNESColor.ink)
                            .lineLimit(1)
                        Text(.paradoxoDetailTeacherMeta(
                            ParadoxoFormat.count(teacher.sampleCount),
                            teacher.lastSemester ?? "—"
                        ))
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    Button {
                        store.send(.teacherPageTapped(teacher))
                    } label: {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(UNESColor.ink2)
                            .frame(width: 30, height: 30)
                            .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 9, style: .continuous)
                                    .strokeBorder(UNESColor.cardLine)
                            }
                    }
                    .buttonStyle(.plain)

                    if teacher.history.count > 1 {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(UNESColor.ink4)
                            .rotationEffect(.degrees(expanded ? 180 : 0))
                    }
                }
                .padding(EdgeInsets(top: 10, leading: 14, bottom: 10, trailing: 12))
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if expanded, teacher.history.count > 1 {
                Divider()
                    .overlay(UNESColor.line)
                ParadoxoHistoryChart(
                    history: teacher.history,
                    tone: tier.tone,
                    height: 130
                )
                .padding(EdgeInsets(top: 10, leading: 12, bottom: 12, trailing: 12))
            }
        }
        .paradoxoCard()
    }
}

/// Faint severity-toned radial wash bleeding from behind the navigation bar.
struct ParadoxoToneWash: View {
    var tone: Color

    var body: some View {
        RadialGradient(
            colors: [tone.opacity(0.22), .clear],
            center: .top,
            startRadius: 0,
            endRadius: 300
        )
        .frame(height: 280)
        .padding(.horizontal, -50)
        .offset(y: -60)
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

#Preview("Disciplina") {
    NavigationStack {
        ParadoxoDisciplineView(
            store: Store(initialState: ParadoxoDisciplineFeature.State(disciplineId: "d1")) {
                ParadoxoDisciplineFeature()
            }
        )
    }
}

#Preview("Falha") {
    NavigationStack {
        ParadoxoDisciplineView(
            store: Store(
                initialState: ParadoxoDisciplineFeature.State(disciplineId: "d1", name: "Cálculo I")
            ) {
                ParadoxoDisciplineFeature()
            } withDependencies: {
                $0.paradoxoRepository.discipline = { _ in throw APIError.emptyEnvelope }
            }
        )
    }
}
