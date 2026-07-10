import SwiftUI

/// The mesh-tinted performance card: partial mean + released count, a grade
/// ring, one chip per evaluation, and the "precisa de X" line while the
/// period can still close at 7.
struct DisciplineDetailHero: View {
    let detail: DisciplineDetail
    let color: Color
    var selectedGroup: String?

    private var grades: [DisciplineDetailGrade] { detail.grades(forGroup: selectedGroup) }
    private var average: Double? { DisciplineDetail.partialAverage(of: grades) }
    private var releasedCount: Int { grades.count { $0.value != nil } }

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .warm)
            LinearGradient.css(
                stops: [
                    .init(color: color.opacity(0.33), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.72), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                topRow
                averageRow
                    .padding(.top, 18)

                if !grades.isEmpty {
                    chips
                        .padding(.top, 16)
                }

                if let needed = DisciplineDetail.neededOnPending(of: grades) {
                    neededRow(needed, pendingCount: grades.count { $0.value == nil })
                        .padding(.top, 16)
                }
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    // MARK: Rows

    private var topRow: some View {
        HStack {
            Text(detail.code)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.6)
                .foregroundStyle(.white.opacity(0.92))
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 7))

            Spacer()

            Text(detail.status.label)
                .font(.system(size: 11.5, weight: .semibold))
                .tracking(0.2)
                .foregroundStyle(.white)
                .padding(.horizontal, 9)
                .padding(.vertical, 3)
                .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 8))
        }
    }

    private var averageRow: some View {
        HStack(alignment: .bottom, spacing: 16) {
            VStack(alignment: .leading, spacing: 3) {
                (Text(.disciplinesPartialAverage) + Text(verbatim: selectedGroup.map { " · \($0)" } ?? ""))
                    .textCase(.uppercase)
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(0.5)
                    .foregroundStyle(.white.opacity(0.7))

                HStack(alignment: .lastTextBaseline, spacing: 9) {
                    Text(formatGrade(average))
                        .font(.system(size: 52, weight: .bold))
                        .tracking(-2.34)
                        .monospacedDigit()
                        .foregroundStyle(.white)

                    Text(.disciplinesDetailReleasedTally(releasedCount, grades.count))
                        .font(.system(size: 14, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.6))
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            GradeRing(
                score: average,
                color: .white,
                size: 72,
                stroke: 6,
                trackColor: .white.opacity(0.22),
                textColor: .white
            )
        }
    }

    private var chips: some View {
        FlowLayout(spacing: 7, lineSpacing: 7) {
            ForEach(grades) { grade in
                HeroEvalChip(grade: grade)
            }
        }
    }

    /// "Precisa de 7,2 nas 2 restantes para fechar em 7,0." — the needed
    /// value rounds *up*; unreachable targets (>10) render as "—".
    private func neededRow(_ needed: Double, pendingCount: Int) -> some View {
        let reachable = needed <= 10
        return HStack(alignment: .center, spacing: 9) {
            Image(systemName: reachable ? "flame" : "exclamationmark.triangle")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(
                    !reachable ? UNESColor.dangerOnDark
                        : needed > 7 ? UNESColor.amber
                        : UNESColor.successOnDark
                )

            (
                Text(.disciplinesDetailNeedsPrefix)
                    + Text(reachable ? DisciplinesFormat.neededGrade(needed) : "—")
                    .fontWeight(.bold)
                    .foregroundStyle(reachable ? .white : UNESColor.dangerSoftOnDark)
                    + Text(.disciplinesDetailNeedsMiddle(pendingCount))
                    + Text(formatGrade(7)).fontWeight(.bold)
                    + Text(verbatim: ".")
            )
            .font(.system(size: 13))
            .lineSpacing(2)
            .foregroundStyle(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 14)
        .overlay(alignment: .top) {
            Rectangle().fill(.white.opacity(0.14)).frame(height: 1)
        }
    }
}

/// "AV1 8,3" over frosted white — pending evaluations show "·" until their
/// date passes, then "—".
private struct HeroEvalChip: View {
    let grade: DisciplineDetailGrade

    var body: some View {
        HStack(spacing: 6) {
            Text(grade.label)
                .font(.system(size: 9.5, weight: .bold))
                .tracking(0.2)
                .foregroundStyle(.white)
                .padding(.horizontal, 5)
                .padding(.vertical, 2)
                .background(.white.opacity(0.2), in: RoundedRectangle(cornerRadius: 5))

            Text(valueLabel)
                .font(.system(size: 14, weight: .bold))
                .tracking(-0.28)
                .monospacedDigit()
                .foregroundStyle(grade.value != nil ? .white : .white.opacity(0.5))
        }
        .padding(EdgeInsets(top: 5, leading: 6, bottom: 5, trailing: 10))
        .background(.white.opacity(0.12), in: RoundedRectangle(cornerRadius: 11))
    }

    private var valueLabel: String {
        if let value = grade.value { return formatGrade(value) }
        let past = grade.date != nil && grade.daysUntil == nil
        return past ? "—" : "·"
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 16) {
            DisciplineDetailHero(detail: .preview(), color: UNESColor.coral)
            DisciplineDetailHero(detail: .previewMultiGroup(), color: UNESColor.violet)
            DisciplineDetailHero(detail: .previewMultiGroup(), color: UNESColor.violet, selectedGroup: "T01P01")
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
