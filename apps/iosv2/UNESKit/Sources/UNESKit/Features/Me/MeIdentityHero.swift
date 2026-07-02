import SwiftUI

/// The identity card: avatar, name, course, and the three-stat row over a
/// drifting rose mesh.
struct MeIdentityHero: View {
    var name: String
    var course: String?
    var campus: String?
    var coefficient: CoefficientSummary?
    var attendancePercent: Int?
    var progress: SemesterProgress?

    /// The mesh backdrop, matching the design's `#160E1F`.
    private static let backdrop = Color(hex: 0x160E1F)

    var body: some View {
        ZStack {
            Self.backdrop
            MeshView(variant: .rose)
            LinearGradient.css(
                stops: [
                    .init(color: Color(hex: 0x0C0814, opacity: 0.15), location: 0),
                    .init(color: Color(hex: 0x0C0814, opacity: 0.62), location: 1),
                ],
                angle: 160
            )

            VStack(spacing: 0) {
                identityRow
                statsRow
                    .padding(.top, 16)
                    .overlay(alignment: .top) {
                        Rectangle()
                            .fill(.white.opacity(0.15))
                            .frame(height: 1)
                    }
                    .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 18, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    private var identityRow: some View {
        HStack(spacing: 15) {
            avatar
            VStack(alignment: .leading, spacing: 3) {
                Text(name)
                    .font(.system(size: 23, weight: .bold))
                    .tracking(-0.69)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                if let course {
                    Text(course)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.white.opacity(0.82))
                        .lineLimit(1)
                }
                if let campus {
                    Text(campus)
                        .font(.system(size: 11.5, weight: .medium))
                        .foregroundStyle(.white.opacity(0.55))
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.top, 2)
    }

    private var avatar: some View {
        Text(initial)
            .font(.system(size: 28, weight: .bold))
            .tracking(-0.84)
            .foregroundStyle(.white)
            .frame(width: 62, height: 62)
            .background(
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.amber, location: 0),
                        .init(color: UNESColor.coral, location: 0.55),
                        .init(color: UNESColor.magenta, location: 1),
                    ],
                    angle: 135
                ),
                in: Circle()
            )
            .shadow(color: UNESColor.coral.opacity(0.4), radius: 11, y: 8)
            .overlay(alignment: .bottomTrailing) {
                Circle()
                    .fill(UNESColor.liveGreen)
                    .stroke(Self.backdrop, lineWidth: 2.5)
                    .frame(width: 14, height: 14)
                    .offset(x: -1, y: -1)
            }
    }

    private var initial: String {
        name.first.map { String($0).uppercased() } ?? "•"
    }

    private var statsRow: some View {
        HStack(alignment: .top, spacing: 4) {
            stat(label: "CR atual", value: formatGrade(coefficient?.value), delta: delta)
            stat(label: "Frequência", value: attendancePercent.map { "\($0)" } ?? "—",
                 sub: attendancePercent != nil ? "%" : nil)
            stat(label: "Semestre", value: progress.map { "\($0.week)" } ?? "—",
                 sub: progress.map { "/ \($0.totalWeeks) sem" })
        }
    }

    private func stat(
        label: String,
        value: String,
        sub: String? = nil,
        delta: (text: String, rising: Bool)? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 10.5, weight: .semibold))
                .tracking(0.3)
                .foregroundStyle(.white.opacity(0.55))
                .padding(.bottom, 6)
            HStack(alignment: .lastTextBaseline, spacing: 3) {
                Text(value)
                    .font(.system(size: 24, weight: .bold))
                    .tracking(-0.72)
                    .monospacedDigit()
                    .foregroundStyle(.white)
                if let sub {
                    Text(sub)
                        .font(.system(size: 11.5, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.55))
                }
            }
            if let delta {
                HStack(spacing: 2) {
                    Image(systemName: delta.rising ? "arrow.up" : "arrow.down")
                        .font(.system(size: 8.5, weight: .bold))
                    Text(delta.text)
                        .font(.system(size: 11, weight: .semibold))
                        .monospacedDigit()
                }
                .foregroundStyle(delta.rising ? Color(hex: 0x7FE0A8) : UNESColor.dangerSoftOnDark)
                .padding(.top, 3)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var delta: (text: String, rising: Bool)? {
        guard let delta = coefficient?.delta, abs(delta) >= 0.1 else { return nil }
        let rising = delta >= 0
        return ("\(rising ? "+" : "−")\(formatGrade(abs(delta)))", rising)
    }
}

#Preview {
    MeIdentityHero(
        name: "Mariana Nogueira",
        course: "Engenharia de Computação",
        campus: "UEFS · Módulo 5",
        coefficient: MeOverview.preview.coefficient,
        attendancePercent: 96,
        progress: MeOverview.preview.progress
    )
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
