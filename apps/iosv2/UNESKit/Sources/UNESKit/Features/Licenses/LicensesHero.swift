import SwiftUI

/// The always-dark "obrigado coletivo" hero: package count, tribute line,
/// and the license distribution as a stacked bar with legend.
struct LicensesHero: View {
    private let breakdown = LicenseCatalog.breakdown
    private var total: Int { breakdown.reduce(0) { $0 + $1.count } }

    var body: some View {
        ZStack {
            UNESColor.darkBg
            MeshView(variant: .rose)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.15), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.68), location: 1),
                ],
                angle: 155
            )

            VStack(alignment: .leading, spacing: 0) {
                eyebrow
                countRow
                    .padding(.top, 16)
                tribute
                    .padding(.top, 8)
                distributionBar
                    .padding(.top, 18)
                legend
                    .padding(.top, 13)
            }
            .padding(EdgeInsets(top: 18, leading: 20, bottom: 20, trailing: 20))
        }
        .environment(\.colorScheme, .dark)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .shadow(color: Color(hex: 0x141020, opacity: 0.28), radius: 20, y: 18)
    }

    private var eyebrow: some View {
        HStack(spacing: 7) {
            LiveDot()
            Text(.licensesHeroEyebrow)
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.2)
        }
        .foregroundStyle(.white.opacity(0.9))
    }

    private var countRow: some View {
        HStack(alignment: .lastTextBaseline, spacing: 8) {
            Text("\(total)")
                .font(.system(size: 52, weight: .bold))
                .tracking(-2.08)
                .monospacedDigit()
                .foregroundStyle(.white)
            Text(total == 1 ? .licensesHeroPackageUnitOne : .licensesHeroPackageUnitOther)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white.opacity(0.62))
        }
    }

    private var tribute: some View {
        Text(.licensesHeroTribute)
            .font(.system(size: 14, weight: .medium))
            .lineSpacing(3)
            .foregroundStyle(.white.opacity(0.82))
            .frame(maxWidth: 250, alignment: .leading)
    }

    /// Family shares as proportional segments over a faint track.
    private var distributionBar: some View {
        GeometryReader { geometry in
            let gaps = CGFloat(max(0, breakdown.count - 1)) * 1.5
            HStack(spacing: 1.5) {
                ForEach(breakdown) { share in
                    Rectangle()
                        .fill(share.family.heroTone)
                        .frame(width: (geometry.size.width - gaps) * CGFloat(share.count) / CGFloat(max(1, total)))
                }
            }
        }
        .frame(height: 12)
        .background(.white.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }

    private var legend: some View {
        FlowLayout(spacing: 14, lineSpacing: 8) {
            ForEach(breakdown) { share in
                HStack(spacing: 6) {
                    RoundedRectangle(cornerRadius: 3, style: .continuous)
                        .fill(share.family.heroTone)
                        .frame(width: 8, height: 8)
                    Text(share.family.rawValue)
                        .font(.system(size: 11.5, weight: .semibold, design: .monospaced))
                        .foregroundStyle(.white.opacity(0.72))
                    Text("\(share.count)")
                        .font(.system(size: 11.5, weight: .bold))
                        .monospacedDigit()
                        .foregroundStyle(.white)
                }
            }
        }
    }
}

#Preview {
    LicensesHero()
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
