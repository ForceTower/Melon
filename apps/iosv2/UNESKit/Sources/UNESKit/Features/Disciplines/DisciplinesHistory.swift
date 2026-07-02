import SwiftUI

// MARK: - Past semester (collapsible)

/// One closed semester in the Histórico: header with the mean, approval
/// tally, and per-discipline dots; expands into one row per discipline.
struct PastSemesterCard: View {
    let semester: SemesterDisciplines
    var initiallyExpanded = false
    var onDisciplineTap: (DisciplineSummary) -> Void = { _ in }

    @State private var isExpanded: Bool

    init(
        semester: SemesterDisciplines,
        initiallyExpanded: Bool = false,
        onDisciplineTap: @escaping (DisciplineSummary) -> Void = { _ in }
    ) {
        self.semester = semester
        self.initiallyExpanded = initiallyExpanded
        self.onDisciplineTap = onDisciplineTap
        _isExpanded = State(initialValue: initiallyExpanded)
    }

    var body: some View {
        VStack(spacing: 8) {
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    isExpanded.toggle()
                }
            } label: {
                header
            }
            .buttonStyle(.pressableCard)

            if isExpanded {
                VStack(spacing: 8) {
                    ForEach(semester.disciplines) { discipline in
                        PastDisciplineRow(discipline: discipline) {
                            onDisciplineTap(discipline)
                        }
                    }
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                HStack(alignment: .lastTextBaseline, spacing: 8) {
                    Text(DisciplinesFormat.semesterLabel(semester.code))
                        .font(.system(size: 17, weight: .bold))
                        .tracking(-0.34)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    Text(semester.disciplines.count == 1 ? "1 disc." : "\(semester.disciplines.count) disc.")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                }

                summaryLine
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            HStack(spacing: 3) {
                ForEach(semester.disciplines.prefix(5)) { discipline in
                    Circle()
                        .fill(UNESColor.disciplineColor(discipline.colorIndex))
                        .frame(width: 7, height: 7)
                        .opacity(discipline.passed == true ? 1 : 0.35)
                }
            }

            Image(systemName: "chevron.down")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
                .rotationEffect(.degrees(isExpanded ? 180 : 0))
        }
        .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.04), radius: 6, y: 4)
    }

    private var summaryLine: some View {
        HStack(spacing: 8) {
            if let mean = semester.finalMean {
                (
                    Text("média ")
                        + Text(formatGrade(mean))
                        .fontWeight(.bold)
                        .foregroundStyle(UNESColor.score(mean))
                )
                .monospacedDigit()
                Text("·").opacity(0.4)
            }
            Text("\(semester.approvedCount)/\(semester.disciplines.count) aprovadas")
                .monospacedDigit()
        }
        .font(.system(size: 13, weight: .medium))
        .foregroundStyle(UNESColor.ink3)
        .lineLimit(1)
    }
}

/// One discipline of a closed semester: final grade + verdict. Tapping
/// pushes the discipline detail.
struct PastDisciplineRow: View {
    let discipline: DisciplineSummary
    var onTap: () -> Void = {}

    private var color: Color { UNESColor.disciplineColor(discipline.colorIndex) }

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(discipline.code)
                        .font(.system(size: 10, weight: .bold))
                        .tracking(0.3)
                        .foregroundStyle(color)
                    Text(discipline.name)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.3)
                        .lineLimit(1)
                        .foregroundStyle(UNESColor.ink)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 2) {
                    Text(formatGrade(discipline.finalGrade))
                        .font(.system(size: 22, weight: .bold))
                        .tracking(-0.66)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.score(discipline.finalGrade))
                    if let passed = discipline.passed {
                        Text(passed ? "aprovado" : "reprovado")
                            .textCase(.uppercase)
                            .font(.system(size: 9.5, weight: .bold))
                            .tracking(0.2)
                            .foregroundStyle(passed ? UNESColor.teal : UNESColor.coral)
                    }
                }
            }
            .padding(.leading, 8)
            .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
            .background(UNESColor.card)
            .overlay(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(color)
                    .opacity(discipline.passed == false ? 0.4 : 1)
                    .frame(width: 4)
                    .padding(.vertical, 10)
            }
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
        }
        .buttonStyle(.pressableCard)
    }
}

// MARK: - Pending semester (downloadable)

/// A semester known upstream but not mirrored yet — tapping pulls its
/// payload and folds it into the Histórico.
struct DownloadSemesterCard: View {
    let semester: PendingSemester
    var isDownloading: Bool
    var onDownload: () -> Void

    var body: some View {
        Button(action: onDownload) {
            HStack(spacing: 14) {
                icon

                VStack(alignment: .leading, spacing: 2) {
                    HStack(alignment: .lastTextBaseline, spacing: 8) {
                        Text(DisciplinesFormat.semesterLabel(semester.code))
                            .font(.system(size: 17, weight: .bold))
                            .tracking(-0.34)
                            .monospacedDigit()
                            .foregroundStyle(isDownloading ? UNESColor.ink3 : UNESColor.ink)
                        if isDownloading {
                            Text("baixando…")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(UNESColor.ink4)
                        } else if let count = semester.disciplineCount {
                            Text("~\(DisciplinesFormat.disciplineCountLabel(count))")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundStyle(UNESColor.ink4)
                        }
                    }
                    Text(isDownloading ? "Buscando notas e faltas…" : "Toque para baixar o histórico.")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                if !isDownloading {
                    Text("Baixar")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(UNESColor.accent)
                }
            }
            .padding(EdgeInsets(top: 14, leading: 16, bottom: 14, trailing: 16))
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(UNESColor.cardLine, style: StrokeStyle(lineWidth: 1, dash: [5, 4]))
            }
        }
        .buttonStyle(.pressableCard)
        .disabled(isDownloading)
    }

    private var icon: some View {
        ZStack {
            if isDownloading {
                SpinnerRing(size: 16, color: UNESColor.ink3, trackColor: UNESColor.surface3)
            } else {
                Image(systemName: "arrow.down.to.line")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
        .frame(width: 38, height: 38)
        .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 10) {
            PastSemesterCard(semester: DisciplinesOverview.preview().past[0], initiallyExpanded: true)
            ForEach(DisciplinesOverview.preview().pending) { pending in
                DownloadSemesterCard(
                    semester: pending,
                    isDownloading: pending.id.hasSuffix("2024-1"),
                    onDownload: {}
                )
            }
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
