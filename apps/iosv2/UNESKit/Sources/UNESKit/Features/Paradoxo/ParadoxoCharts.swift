import Charts
import SwiftUI

// MARK: - Semester history (line / area)

enum ParadoxoChartStyle: String, Equatable, Sendable, CaseIterable {
    case line, area

    var label: LocalizedStringResource {
        switch self {
        case .line: .paradoxoChartLine
        case .area: .paradoxoChartArea
        }
    }
}

/// Mean per semester across the years, on a fixed 0–10 scale. Peak and
/// trough carry value annotations; the rest of the points only render when
/// the series is short enough to breathe.
struct ParadoxoHistoryChart: View {
    var history: [ParadoxoSemesterMean]
    var tone: Color
    var style: ParadoxoChartStyle = .line
    var height: CGFloat = 178

    var body: some View {
        Chart {
            if style == .area {
                ForEach(history, id: \.semester) { point in
                    AreaMark(
                        x: .value("Semestre", point.semester),
                        y: .value("Média", point.mean)
                    )
                    .foregroundStyle(
                        LinearGradient(
                            colors: [tone.opacity(0.22), tone.opacity(0)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
            }
            ForEach(history, id: \.semester) { point in
                LineMark(
                    x: .value("Semestre", point.semester),
                    y: .value("Média", point.mean)
                )
                .foregroundStyle(tone)
                .lineStyle(StrokeStyle(lineWidth: 2.4, lineCap: .round, lineJoin: .round))
            }
            ForEach(markedPoints, id: \.semester) { point in
                PointMark(
                    x: .value("Semestre", point.semester),
                    y: .value("Média", point.mean)
                )
                .foregroundStyle(tone)
                .symbolSize(50)
                .annotation(position: .top, spacing: 4) {
                    if annotatesEveryPoint || isExtreme(point) {
                        Text(formatGrade(point.mean))
                            .font(.system(size: 10.5, weight: .bold))
                            .monospacedDigit()
                            .foregroundStyle(isExtreme(point) ? tone : UNESColor.ink3)
                    }
                }
            }
        }
        .chartYScale(domain: 0...10)
        .chartYAxis {
            AxisMarks(position: .trailing, values: [0, 5, 10]) { _ in
                AxisGridLine(stroke: StrokeStyle(lineWidth: 1, dash: [1, 4]))
                    .foregroundStyle(UNESColor.line)
                AxisValueLabel()
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .chartXAxis {
            AxisMarks(values: labeledSemesters) { _ in
                AxisValueLabel()
                    .font(.system(size: 9.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .frame(height: height)
    }

    /// Peak and trough always; every point when the series is short.
    private var markedPoints: [ParadoxoSemesterMean] {
        guard history.count > 3 else { return history }
        if history.count <= 20 { return history }
        return [peak, trough].compactMap(\.self)
    }

    private var annotatesEveryPoint: Bool { history.count <= 12 }

    private var peak: ParadoxoSemesterMean? { history.max { $0.mean < $1.mean } }
    private var trough: ParadoxoSemesterMean? { history.min { $0.mean < $1.mean } }

    private func isExtreme(_ point: ParadoxoSemesterMean) -> Bool {
        point.semester == peak?.semester || point.semester == trough?.semester
    }

    private var labeledSemesters: [String] {
        let stride = history.count > 16 ? 6 : history.count > 8 ? 3 : history.count > 4 ? 2 : 1
        return history.enumerated().compactMap { index, point in
            index % stride == 0 || index == history.count - 1 ? point.semester : nil
        }
    }
}

// MARK: - Grade distribution (0…10 histogram)

/// How the final grades spread across the integer buckets, with an optional
/// rule marking the student's own grade.
struct ParadoxoDistributionChart: View {
    var distribution: [Double]
    var tone: Color
    var myGrade: Double?
    var height: CGFloat = 140

    var body: some View {
        Chart {
            ForEach(Array(distribution.enumerated()), id: \.offset) { bucket, share in
                BarMark(
                    x: .value("Nota", Double(bucket)),
                    y: .value("Alunos", share),
                    width: .ratio(0.72)
                )
                .foregroundStyle(isMyBucket(bucket) ? tone : tone.opacity(0.25))
                .cornerRadius(3)
            }
            if let myGrade {
                RuleMark(x: .value("Nota", myGrade))
                    .foregroundStyle(tone)
                    .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [3, 3]))
                    .annotation(position: .top, spacing: 2) {
                        Text(.paradoxoDistMyGrade(formatGrade(myGrade)))
                            .font(.system(size: 10.5, weight: .bold))
                            .foregroundStyle(.white)
                            .padding(EdgeInsets(top: 3, leading: 9, bottom: 3, trailing: 9))
                            .background(tone, in: Capsule())
                    }
            }
        }
        .chartXScale(domain: -0.6...10.6)
        .chartYAxis(.hidden)
        .chartXAxis {
            AxisMarks(values: [0.0, 2, 4, 6, 8, 10]) { _ in
                AxisValueLabel()
                    .font(.system(size: 9.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
            }
        }
        .frame(height: height)
    }

    private func isMyBucket(_ bucket: Int) -> Bool {
        guard let myGrade else { return false }
        return Int(myGrade.rounded()) == bucket
    }
}

// MARK: - Donut ring (teacher hero)

struct ParadoxoDonut: View {
    var score: Double
    var tone: Color
    var size: CGFloat = 172
    var stroke: CGFloat = 13
    var label: String?

    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            Circle()
                .stroke(UNESColor.surface3, lineWidth: stroke)
            Circle()
                .trim(from: 0, to: shown ? score / 10 : 0)
                .stroke(tone, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 6) {
                Text(formatGrade(score))
                    .font(.system(size: size * 0.3, weight: .bold))
                    .monospacedDigit()
                    .tracking(-size * 0.012)
                    .foregroundStyle(UNESColor.ink)
                if let label {
                    Text(label)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
            }
        }
        .frame(width: size, height: size)
        .padding(stroke / 2)
        .onAppear {
            guard !shown else { return }
            if reduceMotion {
                shown = true
            } else {
                withAnimation(UNESMotion.ease(1.1).delay(0.15)) {
                    shown = true
                }
            }
        }
    }
}

#Preview("Gráficos") {
    let details = ParadoxoDisciplineDetails.preview()
    return ScrollView {
        VStack(spacing: 20) {
            ParadoxoHistoryChart(history: details.history, tone: UNESColor.coral, style: .area)
                .padding(16)
                .paradoxoCard()
            ParadoxoDistributionChart(
                distribution: details.distribution,
                tone: UNESColor.coral,
                myGrade: details.myGrade
            )
            .padding(16)
            .paradoxoCard()
            ParadoxoDonut(score: 3.5, tone: UNESColor.coral, label: "1.335 alunos")
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
