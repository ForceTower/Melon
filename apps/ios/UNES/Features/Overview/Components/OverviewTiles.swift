import SwiftUI

struct OverviewTileGrid: View {
    var body: some View {
        VStack(spacing: 10) {
            HStack(spacing: 10) {
                GradeTile()
                MessagesTile()
            }
            HStack(spacing: 10) {
                TestsTile()
                StreakTile()
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
    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "coeficiente")
                Spacer(minLength: 0)
                Text("\(Text("8").foregroundStyle(UNESColor.ink))\(Text(",").foregroundStyle(UNESColor.ink4))\(Text("5").foregroundStyle(UNESColor.ink))")
                    .font(UNESFont.serif(52))
                    .tracking(-1.56)
                HStack(spacing: 4) {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 8, weight: .bold))
                        .foregroundStyle(OverviewFixtures.successIcon)
                    Text("+0,3")
                        .font(UNESFont.sans(11, weight: .medium))
                        .foregroundStyle(OverviewFixtures.success)
                    Text("vs 2025.2")
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.ink3)
                }
                .padding(.top, 4)
            }
        }
    }
}

// MARK: - Messages (always-dark mesh card)

private struct MessagesTile: View {
    var body: some View {
        ZStack(alignment: .topLeading) {
            Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
            MeshGradientView(variant: .rose, intensity: 0.75)

            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "recados", tint: Color.white.opacity(0.7))
                Spacer(minLength: 0)
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("2")
                        .font(UNESFont.serif(48))
                        .tracking(-1.44)
                        .foregroundStyle(UNESColor.surfaceLight)
                    Text("não lidos")
                        .font(UNESFont.sans(12))
                        .foregroundStyle(Color.white.opacity(0.7))
                }
                Text("Prof. Adriana · Gabarito da P1")
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
}

// MARK: - Tests (próxima prova)

private struct TestsTile: View {
    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "próxima prova")
                Spacer(minLength: 0)
                HStack(alignment: .firstTextBaseline, spacing: 4) {
                    Text("5")
                        .font(UNESFont.serif(48))
                        .foregroundStyle(UNESColor.ink)
                    Text("dias")
                        .font(UNESFont.serif(18))
                        .foregroundStyle(UNESColor.ink3)
                }
                .tracking(-0.96)
                Text("P2 · Algoritmos I")
                    .font(UNESFont.sans(12, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
                    .padding(.top, 4)
                Text("22 abr · 08:00")
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 1)
            }
        }
    }
}

// MARK: - Streak (frequência)

private struct StreakTile: View {
    private let days: [Bool] = (0..<14).map { $0 < 12 }

    var body: some View {
        TileShell {
            VStack(alignment: .leading, spacing: 0) {
                TileEyebrow(label: "frequência")
                Spacer(minLength: 0)
                Text("\(Text("96").font(UNESFont.serif(32)).foregroundStyle(UNESColor.ink))\(Text("%").font(UNESFont.serif(18)).foregroundStyle(UNESColor.ink3))")
                    .tracking(-0.64)

                HStack(spacing: 2) {
                    ForEach(Array(days.enumerated()), id: \.offset) { i, present in
                        let base: Color = present ? UNESColor.amber : UNESColor.surface3
                        let opacity: Double = present
                            ? (0.4 + Double(i) / Double(days.count) * 0.6)
                            : 1.0
                        RoundedRectangle(cornerRadius: 3, style: .continuous)
                            .fill(base.opacity(opacity))
                            .frame(height: 16)
                    }
                }
                .padding(.top, 10)

                Text("14 dias · 2 faltas permitidas")
                    .font(UNESFont.sans(11))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 4)
            }
        }
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        OverviewTileGrid().padding(14)
    }
}
