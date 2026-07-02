import SwiftUI

/// "Turmas" — horizontally scrolling discipline cards with partial grades.
struct HomeClassesCarousel: View {
    let disciplines: [DisciplineCard]
    var onSeeAll: () -> Void
    var onOpen: (DisciplineCard) -> Void

    var body: some View {
        VStack(spacing: 0) {
            HomeSectionHeader(title: "Turmas", action: "Ver todas", onAction: onSeeAll)
                .padding(.horizontal, 18)

            ScrollView(.horizontal) {
                HStack(spacing: 12) {
                    ForEach(disciplines) { discipline in
                        card(discipline)
                    }
                }
                .padding(EdgeInsets(top: 2, leading: 18, bottom: 6, trailing: 18))
            }
            .scrollIndicators(.hidden)
            // Card shadows spill past the content insets; let them draw
            // instead of clipping at the scroll bounds.
            .scrollClipDisabled()
        }
    }

    private func card(_ discipline: DisciplineCard) -> some View {
        let color = UNESColor.disciplineColor(discipline.colorIndex)
        return Button {
            onOpen(discipline)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                Text(discipline.code)
                    .font(.system(size: 10, weight: .bold))
                    .tracking(0.4)
                    .foregroundStyle(color)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(color.opacity(0.125), in: RoundedRectangle(cornerRadius: 7))
                    .lineLimit(1)

                Text(discipline.name)
                    .font(.system(size: 16, weight: .bold))
                    .tracking(-0.32)
                    .lineSpacing(1)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
                    .padding(.top, 12)

                Spacer(minLength: 8)

                Text("Parcial")
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.3)
                    .foregroundStyle(UNESColor.ink4)

                Text(formatGrade(discipline.partial))
                    .font(.system(size: 26, weight: .bold))
                    .tracking(-0.78)
                    .monospacedDigit()
                    .foregroundStyle(discipline.partial == nil ? UNESColor.ink4 : UNESColor.ink)
                    .padding(.top, 2)
            }
            .padding(15)
            .frame(width: 138, height: 158, alignment: .topLeading)
            .background(UNESColor.card)
            .overlay(alignment: .topTrailing) {
                Circle()
                    .fill(color)
                    .opacity(0.14)
                    .frame(width: 84, height: 84)
                    .offset(x: 28, y: -28)
            }
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    NavigationStack {
        HomeClassesCarousel(disciplines: HomeOverview.preview().disciplines, onSeeAll: {}, onOpen: { _ in })
            .frame(maxHeight: .infinity)
            .background(UNESColor.surface)
    }
}
