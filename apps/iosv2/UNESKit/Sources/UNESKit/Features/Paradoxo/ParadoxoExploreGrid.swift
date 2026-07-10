import SwiftUI

extension ParadoxoExploreKind {
    var title: LocalizedStringResource {
        switch self {
        case .brutal: .paradoxoExploreBrutalTitle
        case .kind: .paradoxoExploreKindTitle
        case .rising: .paradoxoExploreRisingTitle
        case .gap: .paradoxoExploreGapTitle
        }
    }

    var subtitle: LocalizedStringResource {
        switch self {
        case .brutal: .paradoxoExploreBrutalSub
        case .kind: .paradoxoExploreKindSub
        case .rising: .paradoxoExploreRisingSub
        case .gap: .paradoxoExploreGapSub
        }
    }

    var icon: String {
        switch self {
        case .brutal: "bolt.fill"
        case .kind: "sun.max.fill"
        case .rising: "chart.line.uptrend.xyaxis"
        case .gap: "circle.righthalf.filled"
        }
    }

    var tone: Color {
        switch self {
        case .brutal: UNESColor.coral
        case .kind: UNESColor.successGreen
        case .rising: UNESColor.teal
        case .gap: UNESColor.magenta
        }
    }
}

/// The 2×2 "Explorar" category tiles.
struct ParadoxoExploreGrid: View {
    var onSelect: (ParadoxoExploreKind) -> Void

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(ParadoxoExploreKind.allCases, id: \.self) { kind in
                tile(kind)
            }
        }
    }

    private func tile(_ kind: ParadoxoExploreKind) -> some View {
        Button {
            onSelect(kind)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                Image(systemName: kind.icon)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(kind.tone)
                    .frame(width: 34, height: 34)
                    .background(kind.tone.opacity(0.13), in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                    .padding(.bottom, 12)
                Text(kind.title)
                    .font(.system(size: 15, weight: .bold))
                    .tracking(-0.3)
                    .foregroundStyle(UNESColor.ink)
                Text(kind.subtitle)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .padding(.top, 1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(15)
            .background(alignment: .topTrailing) {
                Circle()
                    .fill(kind.tone.opacity(0.13))
                    .frame(width: 74, height: 74)
                    .offset(x: 24, y: -24)
            }
            .paradoxoCard()
        }
        .buttonStyle(TilePressStyle())
    }
}

#Preview("Explorar") {
    ParadoxoExploreGrid(onSelect: { _ in })
        .padding(16)
        .frame(maxHeight: .infinity)
        .background(UNESColor.surface)
}
