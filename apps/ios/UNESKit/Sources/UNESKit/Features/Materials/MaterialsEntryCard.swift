import SwiftUI

/// The Materiais entry point on Discipline Detail: a dark mesh card carrying
/// the discipline's shelf count (or the contribution pitch when empty) that
/// pushes the discipline's materials list.
struct MaterialsEntryCard: View {
    var discipline: MaterialsDiscipline
    /// The host screen's discipline tint, washing over the mesh.
    var color: Color
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                MeshView(variant: .warm, intensity: 0.9)
                LinearGradient.css(
                    stops: [
                        .init(color: color.opacity(0.33), location: 0),
                        .init(color: UNESColor.scrim.opacity(0.74), location: 1),
                    ],
                    angle: 150
                )
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 12) {
                        Image(systemName: "books.vertical")
                            .font(.system(size: 19, weight: .medium))
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(.white.opacity(0.16), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(.materialsTitle)
                                .textCase(.uppercase)
                                .font(.system(size: 11, weight: .bold))
                                .tracking(0.55)
                                .foregroundStyle(.white.opacity(0.72))
                            Text(title)
                                .font(.system(size: 16.5, weight: .bold))
                                .tracking(-0.33)
                                .foregroundStyle(.white)
                                .lineLimit(1)
                        }
                        Spacer(minLength: 8)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(.white.opacity(0.85))
                    }
                    Text(discipline.total > 0 ? .materialsEntrySubtitle : .materialsEntryEmptySubtitle)
                        .font(.system(size: 13, weight: .medium))
                        .lineSpacing(2.5)
                        .foregroundStyle(.white.opacity(0.78))
                        .multilineTextAlignment(.leading)
                }
                .padding(EdgeInsets(top: 16, leading: 18, bottom: 16, trailing: 18))
            }
            .background(UNESColor.darkBg)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .shadow(color: Color(hex: 0x141020, opacity: 0.22), radius: 17, y: 7)
            .contentShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .buttonStyle(CardPressStyle())
    }

    private var title: LocalizedStringResource {
        if discipline.total == 0 {
            .materialsEmptyTitle
        } else if discipline.total == 1 {
            .materialsEntryCountOne(discipline.total)
        } else {
            .materialsEntryCountOther(discipline.total)
        }
    }
}

#Preview("Com acervo") {
    VStack(spacing: 16) {
        MaterialsEntryCard(
            discipline: MaterialsOverview.preview().disciplines[0],
            color: UNESColor.disciplineColor(0)
        ) {}
        MaterialsEntryCard(
            discipline: MaterialsOverview.preview().disciplines[4],
            color: UNESColor.disciplineColor(4)
        ) {}
    }
    .padding(16)
    .frame(maxHeight: .infinity)
    .background(UNESColor.surface)
}
