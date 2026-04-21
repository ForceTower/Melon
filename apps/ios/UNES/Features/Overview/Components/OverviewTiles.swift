import SwiftUI

struct OverviewTileGrid: View {
    var grade: OverviewGradeTileData? = nil
    var messages: OverviewMessagesTileData? = nil
    var nextTest: OverviewNextTestTileData? = nil
    var attendance: OverviewAttendanceTileData? = nil

    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                GradeTile(data: grade)
                MessagesTile(data: messages)
            }
            HStack(spacing: 10) {
                TestsTile(data: nextTest)
                StreakTile(data: attendance)
            }
        }
    }
}

private struct TileShell<Content: View>: View {
    var background: Color = UNESColor.card
    var foreground: Color = UNESColor.ink
    var border: Color = UNESColor.cardLine
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .padding(14)
            .frame(maxWidth: .infinity, minHeight: 150, alignment: .topLeading)
            .cardSurface(
                RoundedRectangle(cornerRadius: 22, style: .continuous),
                fill: background,
                stroke: border
            )
            .foregroundStyle(foreground)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

private struct TileEyebrow: View {
    let label: String
    var tint: Color = UNESColor.ink3
    var body: some View {
        Text("◦ \(label)")
            .font(UNESFont.mono(9, weight: .medium))
            .tracking(1.33)
            .textCase(.uppercase)
            .foregroundStyle(tint)
    }
}

// MARK: - Grade (coeficiente)

private struct GradeTile: View {
    let data: OverviewGradeTileData?

    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "coeficiente")
                Spacer(minLength: 0)
                if let data {
                    GradeValue(value: data.value)
                    if let delta = data.delta, let comparison = data.comparisonSemester {
                        GradeDelta(delta: delta, comparisonSemester: comparison)
                            .padding(.top, 4)
                    }
                } else {
                    Text("—")
                        .font(UNESFont.serif(52))
                        .tracking(-1.56)
                        .foregroundStyle(UNESColor.ink4)
                    Text("em breve")
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 4)
                }
            }
        }
    }
}

private struct GradeValue: View {
    let value: Double
    var body: some View {
        let scaled = Int((value * 10).rounded())
        let whole = String(scaled / 10)
        let tenth = String(scaled % 10)
        Text("\(Text(whole).foregroundStyle(UNESColor.ink))\(Text(",").foregroundStyle(UNESColor.ink4))\(Text(tenth).foregroundStyle(UNESColor.ink))")
            .font(UNESFont.serif(52))
            .tracking(-1.56)
    }
}

private struct GradeDelta: View {
    let delta: Double
    let comparisonSemester: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: delta >= 0 ? "arrow.up.right" : "arrow.down.right")
                .font(.system(size: 8, weight: .bold))
                .foregroundStyle(OverviewFixtures.successIcon)
            Text(formatDelta(delta))
                .font(UNESFont.sans(11, weight: .medium))
                .foregroundStyle(OverviewFixtures.success)
            Text("vs \(comparisonSemester)")
                .font(UNESFont.sans(11))
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private func formatDelta(_ v: Double) -> String {
        let sign = v >= 0 ? "+" : "-"
        let abs = Swift.abs(v)
        let scaled = Int((abs * 10).rounded())
        return "\(sign)\(scaled / 10),\(scaled % 10)"
    }
}

// MARK: - Messages (always-dark mesh card)

private struct MessagesTile: View {
    let data: OverviewMessagesTileData?

    var body: some View {
        ZStack(alignment: .topLeading) {
            Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
            MeshGradientView(variant: .rose, intensity: 0.75)

            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "recados", tint: Color.white.opacity(0.7))
                Spacer(minLength: 0)
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("\(data?.unreadCount ?? 0)")
                        .font(UNESFont.serif(48))
                        .tracking(-1.44)
                        .foregroundStyle(UNESColor.surfaceLight)
                    Text("não lidos")
                        .font(UNESFont.sans(12))
                        .foregroundStyle(Color.white.opacity(0.7))
                }
                Text(previewLine)
                    .font(UNESFont.sans(11))
                    .foregroundStyle(Color.white.opacity(0.75))
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .padding(.top, 6)
            }
            .padding(14)
        }
        .frame(maxWidth: .infinity, minHeight: 150)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private var previewLine: String {
        guard let data else { return "sem mensagens" }
        switch (data.lastSender, data.lastPreview) {
        case let (sender?, preview?): return "\(sender) · \(preview)"
        case let (sender?, nil): return sender
        case let (nil, preview?): return preview
        default: return "caixa de entrada em dia"
        }
    }
}

// MARK: - Tests (próxima prova)

private struct TestsTile: View {
    let data: OverviewNextTestTileData?

    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "próxima prova")
                Spacer(minLength: 0)
                if let data {
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text("\(data.daysUntil)")
                            .font(UNESFont.serif(48))
                            .foregroundStyle(UNESColor.ink)
                        Text(data.daysUntil == 1 ? "dia" : "dias")
                            .font(UNESFont.serif(18))
                            .foregroundStyle(UNESColor.ink3)
                    }
                    .tracking(-0.96)
                    Text("\(data.disciplineName)")
                        .font(UNESFont.sans(12, weight: .medium))
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .padding(.top, 4)
                    Text(data.dateLabel)
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 1)
                } else {
                    Text("—")
                        .font(UNESFont.serif(48))
                        .foregroundStyle(UNESColor.ink4)
                        .tracking(-0.96)
                    Text("sem provas agendadas")
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 4)
                }
            }
        }
    }
}

// MARK: - Streak (frequência)

private struct StreakTile: View {
    let data: OverviewAttendanceTileData?

    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "frequência")
                Spacer(minLength: 0)
                percentageLabel

                HStack(spacing: 2) {
                    let days = paddedDays
                    ForEach(Array(days.enumerated()), id: \.offset) { i, present in
                        let base: Color = present ? UNESColor.amber : UNESColor.surface3
                        let opacity: Double = present
                            ? (0.4 + Double(i) / Double(max(days.count, 1)) * 0.6)
                            : 1.0
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(base.opacity(opacity))
                            .frame(height: 16)
                    }
                }
                .padding(.top, 10)

                Text(footer)
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 4)
            }
        }
    }

    @ViewBuilder
    private var percentageLabel: some View {
        if let pct = data?.percentage {
            Text("\(Text("\(pct)").font(UNESFont.serif(32)).foregroundStyle(UNESColor.ink))\(Text("%").font(UNESFont.serif(18)).foregroundStyle(UNESColor.ink3))")
                .tracking(-0.64)
        } else {
            Text("—")
                .font(UNESFont.serif(32))
                .foregroundStyle(UNESColor.ink4)
                .tracking(-0.64)
        }
    }

    private var paddedDays: [Bool] {
        let period = data?.periodDays ?? 14
        let days = data?.days ?? []
        if days.count >= period { return Array(days.prefix(period)) }
        return days + Array(repeating: false, count: period - days.count)
    }

    private var footer: String {
        let period = data?.periodDays ?? 14
        let allowed = data?.allowedAbsences ?? 0
        let faltasLabel = allowed == 1 ? "1 falta permitida" : "\(allowed) faltas permitidas"
        return "\(period) dias · \(faltasLabel)"
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        OverviewTileGrid(
            grade: OverviewGradeTileData(value: 8.5, delta: 0.3, comparisonSemester: "2025.2"),
            messages: OverviewMessagesTileData(
                unreadCount: 2,
                lastSender: "Prof. Adriana",
                lastPreview: "Gabarito da P1"
            ),
            nextTest: OverviewNextTestTileData(
                label: "P2",
                disciplineName: "Algoritmos I",
                daysUntil: 5,
                dateLabel: "22 abr"
            ),
            attendance: OverviewAttendanceTileData(
                percentage: 96,
                days: (0..<14).map { $0 < 12 },
                allowedAbsences: 2,
                periodDays: 14
            )
        )
        .padding(14)
    }
}
