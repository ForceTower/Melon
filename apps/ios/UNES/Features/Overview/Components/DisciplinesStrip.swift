import SwiftUI

struct DisciplinesStrip: View {
    let items: [OverviewDiscipline]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("◦ semestre 2026.1")
                        .font(UNESFont.sans(12, weight: .medium))
                        .tracking(1.44)
                        .textCase(.uppercase)
                        .foregroundStyle(UNESColor.ink3)
                    Text("Suas turmas")
                        .font(UNESFont.serif(22))
                        .tracking(-0.22)
                        .foregroundStyle(UNESColor.ink)
                }
            }
            .padding(.horizontal, 18)
            .padding(.bottom, 10)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(items) { item in
                        DisciplineCard(item: item)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.top, 4)
                .padding(.bottom, 12)
            }
        }
    }
}

private struct DisciplineCard: View {
    let item: OverviewDiscipline

    var body: some View {
        ZStack(alignment: .topLeading) {
            // Color glow
            Circle()
                .fill(item.color.opacity(0.12))
                .frame(width: 90, height: 90)
                .offset(x: 82, y: -30)

            VStack(alignment: .leading, spacing: 0) {
                Text(item.code)
                    .font(UNESFont.mono(9, weight: .semibold))
                    .tracking(0.9)
                    .foregroundStyle(item.color)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(item.color.opacity(0.1))
                    )

                Text(item.title)
                    .font(UNESFont.serif(17))
                    .tracking(-0.17)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)
                    .padding(.top, 10)

                Spacer(minLength: 0)

                Text("PARCIAL")
                    .font(UNESFont.mono(9))
                    .tracking(1.08)
                    .foregroundStyle(UNESColor.ink3)

                Text(item.grade)
                    .font(UNESFont.serif(26))
                    .tracking(-0.52)
                    .foregroundStyle(item.grade == "—" ? UNESColor.ink4 : UNESColor.ink)
                    .padding(.top, 2)
            }
            .padding(14)
        }
        .frame(width: 142, height: 168, alignment: .topLeading)
        .cardSurface(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
    }
}

#Preview {
    ZStack {
        UNESColor.surface.ignoresSafeArea()
        DisciplinesStrip(items: OverviewFixtures.disciplines)
    }
}
