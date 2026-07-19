import SwiftUI

/// Home's entrance while the Retrospectiva window is open: the celebratory
/// mesh banner with confetti until the story is seen, then a slim
/// "toque pra rever" row.
struct RetrospectiveBanner: View {
    var semesterLabel: String
    var seen: Bool
    var onOpen: () -> Void
    var onDismiss: () -> Void

    var body: some View {
        if seen {
            slimRow
        } else {
            announcement
        }
    }

    // MARK: Novo

    private var announcement: some View {
        ZStack {
            Color(hex: 0x120C1C)
            MeshView(variant: .warm)
            LinearGradient(
                colors: [Color(hex: 0x0A0810, opacity: 0.08), Color(hex: 0x0A0810, opacity: 0.66)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            RetroConfetti()

            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 8) {
                    Circle()
                        .fill(Color(hex: 0x5CE07A))
                        .frame(width: 7, height: 7)
                    Text(String.localized(.retroBannerEyebrow).uppercased())
                        .font(.system(size: 11.5, weight: .bold))
                        .tracking(1.1)
                }
                .foregroundStyle(.white.opacity(0.9))

                HStack(alignment: .bottom, spacing: 14) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(.retroBannerTitle)
                            .font(.system(size: 25, weight: .heavy))
                            .tracking(-0.9)
                            .lineSpacing(0)
                        Text(.retroBannerSub(semesterLabel))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(.white.opacity(0.82))
                            .frame(maxWidth: 230, alignment: .leading)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Text(semesterLabel)
                        .font(.system(size: 40, weight: .heavy))
                        .tracking(-2)
                }
                .foregroundStyle(.white)
                .padding(.top, 18)

                Button(action: onOpen) {
                    HStack(spacing: 8) {
                        Text(.retroBannerCta)
                            .font(.system(size: 15.5, weight: .bold))
                            .tracking(-0.2)
                        Image(systemName: "arrow.right")
                            .font(.system(size: 13, weight: .bold))
                    }
                    .foregroundStyle(Color(hex: 0x140F1C))
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(.white, in: Capsule())
                }
                .buttonStyle(.pressableCard)
                .padding(.top, 18)
            }
            .padding(EdgeInsets(top: 18, leading: 19, bottom: 19, trailing: 19))
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.36), radius: 24, y: 20)
        .overlay(alignment: .topTrailing) {
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 28, height: 28)
                    .background(.white.opacity(0.18), in: Circle())
            }
            .padding(12)
        }
        .environment(\.colorScheme, .dark)
    }

    // MARK: Visto

    private var slimRow: some View {
        Button(action: onOpen) {
            HStack(spacing: 13) {
                MeshView(variant: .warm)
                    .frame(width: 46, height: 46)
                    .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                VStack(alignment: .leading, spacing: 1) {
                    Text(.retroBannerSeenTitle(semesterLabel))
                        .font(.system(size: 15, weight: .bold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                    Text(.retroBannerSeenSub)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 30, height: 30)
                    .background(UNESColor.surface2, in: Circle())
            }
            .padding(EdgeInsets(top: 13, leading: 15, bottom: 13, trailing: 15))
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.06), radius: 9, y: 6)
        }
        .buttonStyle(.pressableCard)
    }
}

/// Five brand-toned flecks drifting down the banner, looping — decorative
/// only, so it sits still under Reduce Motion.
private struct RetroConfetti: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let flecks: [(x: Double, hex: UInt32, period: Double, drift: Double)] = [
        (0.12, 0xF4A23C, 3.0, -30), (0.34, 0x5CE07A, 3.3, 18), (0.58, 0xE85D4E, 3.6, -12),
        (0.80, 0xFBD9A8, 3.9, 24), (0.92, 0xB23A7A, 4.2, -20),
    ]

    var body: some View {
        if reduceMotion {
            Color.clear
        } else {
            TimelineView(.animation) { context in
                Canvas { canvas, size in
                    let time = context.date.timeIntervalSinceReferenceDate
                    for fleck in Self.flecks {
                        let progress = (time / fleck.period).truncatingRemainder(dividingBy: 1)
                        let x = fleck.x * size.width + fleck.drift * progress
                        let y = -10 + (size.height + 20) * progress
                        var fleckContext = canvas
                        fleckContext.opacity = progress < 0.12 ? progress / 0.12 : 1 - progress
                        fleckContext.translateBy(x: x, y: y)
                        fleckContext.rotate(by: .degrees(400 * progress))
                        fleckContext.fill(
                            Path(roundedRect: CGRect(x: -3.5, y: -4.5, width: 7, height: 9), cornerRadius: 2),
                            with: .color(Color(hex: fleck.hex))
                        )
                    }
                }
            }
            .allowsHitTesting(false)
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        RetrospectiveBanner(semesterLabel: "2026.1", seen: false, onOpen: {}, onDismiss: {})
        RetrospectiveBanner(semesterLabel: "2026.1", seen: true, onOpen: {}, onDismiss: {})
    }
    .padding(16)
    .background(UNESColor.surface)
}
