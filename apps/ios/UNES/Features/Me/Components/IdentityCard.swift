import SwiftUI

/// Hero card at the top of the Me screen — rose mesh background, avatar,
/// student name/course/campus, and a three-column stat rail (CR, credits,
/// semester week).
///
/// On iOS 26+ the dark base swaps from a flat `MeColors.heroBg` fill to a
/// Liquid Glass surface tinted with the same color — the rose mesh sitting
/// on top is only partially opaque (each blob renders at ~0.77 alpha), so
/// the glass's specular highlights and environmental refraction read
/// through the gaps between blobs and give the card a living feel. On
/// older runtimes the solid color renders as before.
struct IdentityCard: View {
    let identity: ProfileIdentity

    var body: some View {
        ZStack(alignment: .topLeading) {
            baseSurface
            MeshGradientView(variant: .rose, intensity: 0.9)
            LinearGradient(
                colors: [MeColors.heroBg.opacity(0.15), MeColors.heroBg.opacity(0.6)],
                startPoint: .top,
                endPoint: .bottom
            )

            VStack(alignment: .leading, spacing: 0) {
                eyebrowRow
                    .padding(.bottom, 18)

                avatarRow
                    .padding(.bottom, 18)

                statsRail
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 22)
        }
        .clipShape(RoundedRectangle(cornerRadius: 28, style: .continuous))
        .shadow(color: Color.black.opacity(0.15), radius: 20, x: 0, y: 16)
    }

    @ViewBuilder
    private var baseSurface: some View {
        if #available(iOS 26.0, *) {
            Rectangle()
                .fill(Color.clear)
                .glassEffect(
                    .regular.tint(MeColors.heroBg),
                    in: RoundedRectangle(cornerRadius: 28, style: .continuous)
                )
        } else {
            MeColors.heroBg
        }
    }

    private var eyebrowRow: some View {
        HStack {
            Text("usuário · \(identity.username)")
                .font(UNESFont.mono(10))
                .tracking(1.8)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.surfaceLight.opacity(0.65))

            Spacer(minLength: 12)

            studentCardPill
        }
    }

    private var studentCardPill: some View {
        HStack(spacing: 5) {
            Image(systemName: "qrcode")
                .font(.system(size: 10, weight: .medium))
            Text("carteirinha")
                .font(UNESFont.mono(9.5))
                .tracking(0.95)
                .textCase(.uppercase)
        }
        .foregroundStyle(UNESColor.surfaceLight)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(
            Capsule(style: .continuous)
                .fill(UNESColor.surfaceLight.opacity(0.12))
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(UNESColor.surfaceLight.opacity(0.18), lineWidth: 1)
                )
        )
    }

    private var avatarRow: some View {
        HStack(alignment: .center, spacing: 14) {
            avatar

            VStack(alignment: .leading, spacing: 3) {
                Text(identity.name)
                    .font(UNESFont.serif(24))
                    .tracking(-0.36)
                    .foregroundStyle(UNESColor.surfaceLight)
                    .lineLimit(1)
                    .truncationMode(.tail)

                Text(identity.course)
                    .font(UNESFont.sans(12))
                    .foregroundStyle(UNESColor.surfaceLight.opacity(0.8))
                    .lineLimit(1)

                Text(identity.campus)
                    .font(UNESFont.mono(9))
                    .tracking(0.36)
                    .foregroundStyle(UNESColor.surfaceLight.opacity(0.55))
                    .lineLimit(1)
            }
        }
    }

    private var avatar: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        stops: [
                            .init(color: UNESColor.amber,   location: 0.0),
                            .init(color: UNESColor.coral,   location: 0.55),
                            .init(color: UNESColor.magenta, location: 1.0),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )

            Text(identity.avatarInitial)
                .font(UNESFont.serif(32))
                .tracking(-0.64)
                .foregroundStyle(UNESColor.surfaceLight)

            // Active indicator dot.
            Circle()
                .fill(MeColors.okGreen)
                .frame(width: 14, height: 14)
                .overlay(
                    Circle().strokeBorder(MeColors.heroBg, lineWidth: 2.5)
                )
                .offset(x: 22, y: 22)
        }
        .frame(width: 64, height: 64)
        .shadow(color: UNESColor.coral.opacity(0.4), radius: 12, x: 0, y: 8)
    }

    private var statsRail: some View {
        VStack(spacing: 16) {
            Rectangle()
                .fill(UNESColor.surfaceLight.opacity(0.15))
                .frame(height: 1)

            HStack(alignment: .top, spacing: 4) {
                IdentityStat(
                    label: "CR atual",
                    value: String(format: "%.1f", identity.cr).replacingOccurrences(of: ".", with: ","),
                    accent: identity.crDelta
                )
                Spacer(minLength: 0)
                IdentityStat(
                    label: "créditos",
                    value: "\(identity.creditsDone)",
                    sub: "/ \(identity.creditsRequired)"
                )
                Spacer(minLength: 0)
                IdentityStat(
                    label: "semestre",
                    value: "\(identity.semesterWeek)",
                    sub: "/ \(identity.semesterTotalWeeks) sem"
                )
            }
        }
    }
}

private struct IdentityStat: View {
    let label: String
    let value: String
    var sub: String? = nil
    var accent: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(UNESFont.mono(9))
                .tracking(1.26)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.surfaceLight.opacity(0.55))

            HStack(alignment: .lastTextBaseline, spacing: 3) {
                Text(value)
                    .font(UNESFont.serif(24))
                    .tracking(-0.48)
                    .foregroundStyle(UNESColor.surfaceLight)
                if let sub {
                    Text(sub)
                        .font(UNESFont.sans(11))
                        .foregroundStyle(UNESColor.surfaceLight.opacity(0.55))
                }
            }

            if let accent, !accent.isEmpty {
                HStack(spacing: 2) {
                    Image(systemName: "arrow.up")
                        .font(.system(size: 8, weight: .semibold))
                    Text(accent)
                }
                .font(UNESFont.mono(10))
                .foregroundStyle(MeColors.successFg)
            }
        }
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        IdentityCard(identity: MeFixtures.identity)
            .padding(14)
    }
}
