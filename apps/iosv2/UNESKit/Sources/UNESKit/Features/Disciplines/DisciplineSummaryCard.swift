import SwiftUI

/// One current-semester discipline: code + status, grade ring, one chip per
/// evaluation, absence bar, and the next-evaluation countdown.
struct DisciplineSummaryCard: View {
    let discipline: DisciplineSummary

    private var color: Color { UNESColor.disciplineColor(discipline.colorIndex) }

    var body: some View {
        NavigationLink(value: DisciplinesRoute.discipline(id: discipline.id, name: discipline.name)) {
            VStack(alignment: .leading, spacing: 14) {
                header
                chips
                footer
            }
            .padding(.leading, 10)
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(UNESColor.card)
            .overlay(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(color)
                    .frame(width: 4)
                    .padding(.vertical, 16)
            }
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(.pressableCard)
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 5) {
                HStack(spacing: 7) {
                    Text(discipline.code)
                        .font(.system(size: 10.5, weight: .bold))
                        .tracking(0.4)
                        .foregroundStyle(color)
                        .padding(.horizontal, 7)
                        .padding(.vertical, 2)
                        .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 6))
                    StatusPill(status: discipline.status)
                }

                Text(discipline.name)
                    .font(.system(size: 18, weight: .bold))
                    .tracking(-0.45)
                    .lineSpacing(1)
                    .multilineTextAlignment(.leading)
                    .foregroundStyle(UNESColor.ink)

                meta
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 5) {
                GradeRing(score: discipline.partialAverage, color: color)
                Text("média")
                    .textCase(.uppercase)
                    .font(.system(size: 10, weight: .semibold))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
    }

    private var meta: some View {
        HStack(spacing: 6) {
            if let teacherName = discipline.teacherName {
                Text(teacherName)
                    .lineLimit(1)
                Text("·").opacity(0.4)
            }
            Text("\(discipline.hours)h")
                .monospacedDigit()
                .layoutPriority(1)
            if let groupsLabel = discipline.groupsLabel {
                Text("·").opacity(0.4)
                Text(groupsLabel)
                    .fontWeight(.semibold)
                    .foregroundStyle(color)
                    .layoutPriority(1)
            }
        }
        .font(.system(size: 13, weight: .medium))
        .foregroundStyle(UNESColor.ink3)
    }

    // MARK: Evaluation chips

    private var chips: some View {
        FlowLayout(spacing: 7, lineSpacing: 7) {
            ForEach(discipline.grades) { grade in
                EvalChip(grade: grade, accent: color)
            }
        }
    }

    // MARK: Footer

    private var footer: some View {
        HStack(alignment: .bottom, spacing: 14) {
            VStack(alignment: .leading, spacing: 7) {
                Text("Faltas")
                    .textCase(.uppercase)
                    .font(.system(size: 10.5, weight: .semibold))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
                AbsenceBar(
                    used: discipline.missedHours,
                    allowed: discipline.allowedMissedHours,
                    risk: discipline.absenceRisk
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if let next = discipline.nextEvaluation {
                VStack(alignment: .trailing, spacing: 5) {
                    HStack(spacing: 4) {
                        Image(systemName: "clock")
                            .font(.system(size: 10, weight: .semibold))
                        Text("Próxima")
                            .textCase(.uppercase)
                            .font(.system(size: 10.5, weight: .semibold))
                            .tracking(0.4)
                    }
                    .foregroundStyle(UNESColor.ink4)

                    HStack(alignment: .lastTextBaseline, spacing: 5) {
                        Text(DisciplinesFormat.countdownLabel(daysUntil: next.daysUntil))
                            .font(.system(size: 19, weight: .bold))
                            .tracking(-0.57)
                            .monospacedDigit()
                            .foregroundStyle(next.daysUntil <= 3 ? UNESColor.caution : UNESColor.ink)
                        Text(next.label)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                    }
                }
            } else {
                VStack(alignment: .trailing, spacing: 5) {
                    Text("Avaliações")
                        .textCase(.uppercase)
                        .font(.system(size: 10.5, weight: .semibold))
                        .tracking(0.4)
                        .foregroundStyle(UNESColor.ink4)
                    (
                        Text("\(discipline.releasedCount)")
                            .foregroundStyle(UNESColor.ink)
                            + Text("/\(discipline.grades.count)")
                            .foregroundStyle(UNESColor.ink4)
                    )
                    .font(.system(size: 19, weight: .bold))
                    .tracking(-0.57)
                    .monospacedDigit()
                }
            }
        }
        .padding(.top, 12)
        .overlay(alignment: .top) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }
}

// MARK: - Status pill

struct StatusPill: View {
    let status: DisciplineStatus

    var body: some View {
        Text(label)
            .font(.system(size: 10.5, weight: .semibold))
            .tracking(0.1)
            .lineLimit(1)
            .padding(.horizontal, 8)
            .padding(.vertical, 2)
            .foregroundStyle(foreground)
            .background(background, in: RoundedRectangle(cornerRadius: 7))
    }

    private var label: String {
        switch status {
        case .approved: "aprovado"
        case .failed: "reprovado"
        case .finals: "prova final"
        case .lowGrade: "nota baixa"
        case .noGrades: "sem notas"
        case .ongoing: "em andamento"
        }
    }

    private var foreground: Color {
        switch status {
        case .approved: UNESColor.teal
        case .failed, .lowGrade: UNESColor.coral
        case .finals: UNESColor.caution
        case .noGrades: UNESColor.ink4
        case .ongoing: UNESColor.ink3
        }
    }

    private var background: Color {
        switch status {
        case .approved: UNESColor.teal.opacity(0.16)
        case .failed: UNESColor.coral.opacity(0.2)
        case .lowGrade: UNESColor.coral.opacity(0.16)
        case .finals: UNESColor.caution.opacity(0.16)
        case .noGrades, .ongoing: UNESColor.surface2
        }
    }
}

// MARK: - Evaluation chip

/// "AV1 8,3" — released chips sit on a filled pill; pending ones on a
/// hairline outline with "—" once the date has passed and "·" before it.
struct EvalChip: View {
    let grade: DisciplineGrade
    let accent: Color

    var body: some View {
        let released = grade.value != nil
        HStack(spacing: 6) {
            Text(grade.label)
                .font(.system(size: 9.5, weight: .bold))
                .tracking(0.2)
                .lineLimit(1)
                .padding(.horizontal, 5)
                .padding(.vertical, 2)
                .foregroundStyle(released ? accent : UNESColor.ink4)
                .background(
                    released ? accent.opacity(0.15) : UNESColor.surface3,
                    in: RoundedRectangle(cornerRadius: 5)
                )

            Text(valueLabel)
                .font(.system(size: 14, weight: .bold))
                .tracking(-0.28)
                .monospacedDigit()
                .foregroundStyle(released ? UNESColor.score(grade.value) : UNESColor.ink4)
        }
        .padding(EdgeInsets(top: 5, leading: 5, bottom: 5, trailing: 9))
        .background(released ? UNESColor.surface2 : .clear, in: RoundedRectangle(cornerRadius: 10))
        .overlay {
            if !released {
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(UNESColor.line)
            }
        }
    }

    private var valueLabel: String {
        if let value = grade.value { return formatGrade(value) }
        let past = grade.date.map { $0 < Date.now.dayStamp } ?? false
        return past ? "—" : "·"
    }
}

// MARK: - Absence bar

/// Segmented meter of the allowed absences, capped at 12 ticks, with the
/// remaining class-hours alongside.
struct AbsenceBar: View {
    let used: Int
    let allowed: Int
    let risk: AbsenceRisk

    var body: some View {
        let ticks = min(allowed, 12)
        let filled = allowed > 0 ? min(ticks, Int((Double(used) / Double(allowed) * Double(ticks)).rounded())) : ticks

        HStack(spacing: 8) {
            HStack(spacing: 3) {
                ForEach(0..<max(ticks, 1), id: \.self) { index in
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(index < filled ? tone : UNESColor.surface3)
                        .frame(height: 6)
                        .frame(maxWidth: .infinity)
                }
            }

            Text(remainingLabel)
                .font(.system(size: 12, weight: .semibold))
                .monospacedDigit()
                .lineLimit(1)
                .foregroundStyle(tone)
        }
    }

    private var tone: Color {
        switch risk {
        case .critical: UNESColor.coral
        case .warning: UNESColor.caution
        case .ok: UNESColor.ink3
        }
    }

    private var remainingLabel: String {
        let remaining = max(0, allowed - used)
        return remaining == 1 ? "1 livre" : "\(remaining) livres"
    }
}

// MARK: - Grade ring

/// Circular partial-average gauge; the arc sweeps in on first appearance.
struct GradeRing: View {
    var score: Double?
    var color: Color
    var size: CGFloat = 56
    var stroke: CGFloat = 4.5

    @State private var swept = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var fraction: Double {
        guard let score else { return 0 }
        return min(max(score / 10, 0), 1)
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(UNESColor.surface3, lineWidth: stroke)
            Circle()
                .trim(from: 0, to: swept ? fraction : 0)
                .stroke(color, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))

            Text(formatGrade(score))
                .font(.system(size: size * 0.34, weight: .bold))
                .tracking(-size * 0.01)
                .monospacedDigit()
                .foregroundStyle(score == nil ? UNESColor.ink4 : UNESColor.ink)
        }
        .padding(stroke / 2)
        .frame(width: size, height: size)
        .onAppear {
            guard !swept else { return }
            if reduceMotion {
                swept = true
            } else {
                withAnimation(UNESMotion.ease(0.8)) {
                    swept = true
                }
            }
        }
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 12) {
            ForEach(DisciplinesOverview.preview().current!.disciplines) { discipline in
                DisciplineSummaryCard(discipline: discipline)
            }
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
