import SwiftUI

/// Rich list card for a current-semester discipline. Code chip + status pill,
/// title, professor/hours meta, a grade ring, eval chips, and a footer with
/// absences + the appropriate projection (next eval / needed average / progress).
struct ActiveDisciplineCard: View {
    let discipline: Discipline

    private var grades: [GradeEntry] { discipline.allGrades }

    /// The most relevant right-side footer module — chosen from (in order):
    ///   1. A countdown to the next scheduled evaluation,
    ///   2. The score required on remaining evals to close at 7.0,
    ///   3. A raw completed/total progress when nothing is scheduled.
    private enum Footer {
        case countdown(days: Int, label: String)
        case needed(NeededProjection)
        case progress(completed: Int, total: Int)
    }

    private var footer: Footer {
        if let next = discipline.nextEvaluation,
           let days = DisciplineDate.daysUntil(next.date),
           days >= 0 {
            return .countdown(days: days, label: next.label)
        }
        if let needed = discipline.needed(target: 7.0) {
            return .needed(needed)
        }
        return .progress(completed: discipline.completedCount, total: discipline.totalEvaluations)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            topRow
            evalChips
            footerRow
        }
        .padding(16)
        .background(cardBackground)
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(discipline.color)
                .frame(width: 3)
                .clipShape(RoundedRectangle(cornerRadius: 2, style: .continuous))
                .padding(.vertical, 14)
        }
    }

    // MARK: - Top row

    private var topRow: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(discipline.fullCode)
                        .font(UNESFont.mono(10, weight: .bold))
                        .tracking(1)
                        .foregroundStyle(discipline.color)
                    StatusPill(status: discipline.status)
                }

                Text(discipline.title)
                    .font(UNESFont.serif(20))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)

                metaRow
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            VStack(spacing: 4) {
                GradeRing(score: discipline.partialAverage,
                          size: 56, stroke: 4,
                          color: discipline.color)
                Text("MÉDIA")
                    .font(UNESFont.mono(9))
                    .tracking(0.72)
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .padding(.leading, 8)
    }

    private var metaRow: some View {
        HStack(spacing: 6) {
            Text(discipline.prof)
                .font(UNESFont.sans(12))
                .foregroundStyle(UNESColor.ink3)
                .lineLimit(1)
                .truncationMode(.tail)
            Text("·").foregroundStyle(UNESColor.ink3.opacity(0.4))
            Text("\(discipline.hours)h")
                .font(UNESFont.mono(10))
                .foregroundStyle(UNESColor.ink3)
            if let label = discipline.groupsShortLabel {
                Text("·").foregroundStyle(UNESColor.ink3.opacity(0.4))
                HStack(spacing: 4) {
                    Image(systemName: "rectangle.split.2x1")
                        .font(.system(size: 8, weight: .semibold))
                    Text(label)
                        .font(UNESFont.mono(9.5, weight: .semibold))
                        .tracking(0.38)
                }
                .foregroundStyle(discipline.color)
                .padding(.horizontal, 6)
                .padding(.vertical, 1)
                .background(
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(discipline.color.opacity(0.10))
                )
            }
            Spacer(minLength: 0)
        }
    }

    // MARK: - Eval chips

    private var evalChips: some View {
        FlowLayout(spacing: 6, rowSpacing: 6) {
            ForEach(grades) { g in
                EvalChip(grade: g, accent: discipline.color)
            }
        }
        .padding(.leading, 8)
    }

    // MARK: - Footer

    @ViewBuilder
    private var footerRow: some View {
        HStack(alignment: .top, spacing: 10) {
            footerCell(title: "Faltas") {
                AbsenceBar(used: discipline.absences, allowed: discipline.allowedAbsences)
            }
            footerCell(title: footerTitle) {
                footerValue
            }
        }
        .padding(.leading, 8)
        .padding(.top, 10)
        .overlay(alignment: .top) {
            DashedSeparator()
                .padding(.leading, 8)
        }
    }

    private var footerTitle: String {
        switch footer {
        case .countdown: return "Próxima avaliação"
        case .needed:    return "Para média 7"
        case .progress:  return "Progresso"
        }
    }

    @ViewBuilder
    private var footerValue: some View {
        switch footer {
        case let .countdown(days, label):
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(days == 0 ? "hoje" : "\(days)d")
                    .font(UNESFont.serif(18))
                    .tracking(-0.36)
                    .foregroundStyle(days <= 3 ? DisciplineScoreColor.caution : UNESColor.ink)
                Text(label)
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
            }
        case let .needed(n):
            HStack(alignment: .firstTextBaseline, spacing: 5) {
                Text(n.required > 10 ? "—" : String(format: "%.1f", n.required))
                    .font(UNESFont.serif(18))
                    .tracking(-0.36)
                    .foregroundStyle(requiredColor(for: n.required))
                Text(n.required > 10
                     ? "inatingível"
                     : "\(n.pending) restante\(n.pending > 1 ? "s" : "")")
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
            }
        case let .progress(completed, total):
            HStack(alignment: .firstTextBaseline, spacing: 5) {
                Text("\(completed)/\(total)")
                    .font(UNESFont.serif(18))
                    .tracking(-0.36)
                    .foregroundStyle(UNESColor.ink)
                Text("avaliações")
                    .font(UNESFont.sans(12))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
    }

    private func requiredColor(for required: Double) -> Color {
        if required > 10 { return DisciplineScoreColor.danger }
        if required > 7  { return DisciplineScoreColor.caution }
        return DisciplineScoreColor.excellent
    }

    @ViewBuilder
    private func footerCell<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(UNESFont.mono(9, weight: .semibold))
                .tracking(0.9)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink4)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var cardBackground: some View {
        RoundedRectangle(cornerRadius: 22, style: .continuous)
            .fill(UNESColor.card)
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(UNESColor.cardLine, lineWidth: 1)
            )
            .shadow(color: Color.black.opacity(0.04), radius: 1, x: 0, y: 1)
    }
}

// MARK: - Dashed separator used between the card body and footer

private struct DashedSeparator: View {
    var body: some View {
        GeometryReader { geo in
            Path { p in
                p.move(to: CGPoint(x: 0, y: 0.5))
                p.addLine(to: CGPoint(x: geo.size.width, y: 0.5))
            }
            .stroke(style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
            .foregroundStyle(UNESColor.line)
        }
        .frame(height: 1)
    }
}

// MARK: - Minimal flow layout used for eval-chip wrapping

struct FlowLayout: Layout {
    var spacing: CGFloat = 6
    var rowSpacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var rows: [[CGSize]] = [[]]
        var rowWidth: CGFloat = 0
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            let additional = rows[rows.count - 1].isEmpty ? size.width : size.width + spacing
            if rowWidth + additional > width, !rows[rows.count - 1].isEmpty {
                rows.append([size])
                rowWidth = size.width
            } else {
                rows[rows.count - 1].append(size)
                rowWidth += additional
            }
        }
        let height = rows.enumerated().reduce(CGFloat(0)) { acc, pair in
            let rowHeight = pair.element.map(\.height).max() ?? 0
            return acc + rowHeight + (pair.offset == 0 ? 0 : rowSpacing)
        }
        let usedWidth = rows.map { row -> CGFloat in
            guard !row.isEmpty else { return 0 }
            return row.map(\.width).reduce(0, +) + CGFloat(row.count - 1) * spacing
        }.max() ?? 0
        return CGSize(width: min(width, usedWidth), height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        let maxX = bounds.maxX
        for s in subviews {
            let size = s.sizeThatFits(.unspecified)
            if x + size.width > maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + rowSpacing
                rowHeight = 0
            }
            s.place(at: CGPoint(x: x, y: y),
                    anchor: .topLeading,
                    proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
