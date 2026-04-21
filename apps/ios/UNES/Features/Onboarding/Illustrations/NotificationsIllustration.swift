import SwiftUI

struct NotificationsIllustration: View {
    private struct Push: Identifiable {
        let id = UUID()
        let app: String
        let title: String
        let body: String
        let chip: Color
        let glyph: Glyph
    }

    private enum Glyph {
        case grade, message, schedule, material
    }

    private let pushes: [Push] = [
        .init(app: "Nota publicada",     title: "CÁLCULO II · P2",  body: "8,7 lançado por Prof. Ribamar",   chip: UNESColor.amber,   glyph: .grade),
        .init(app: "Novo recado",        title: "Prof. Adriana Souza", body: "Gabarito da P1 está disponível no mural.", chip: UNESColor.magenta, glyph: .message),
        .init(app: "Mudança de horário", title: "ALGI II · quinta", body: "Remanejada: PAT12 → PAT70", chip: UNESColor.coral,   glyph: .schedule),
        .init(app: "Material novo",      title: "FÍSICA II",        body: "Lista de exercícios · cap. 7",    chip: UNESColor.plum,    glyph: .material),
    ]

    @State private var appeared = false

    var body: some View {
        VStack(spacing: 14) {
            lockscreenHeader

            VStack(spacing: 7) {
                ForEach(Array(pushes.enumerated()), id: \.element.id) { i, p in
                    pushCard(p)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 18)
                        .animation(
                            .spring(response: 0.55, dampingFraction: 0.72)
                                .delay(0.15 + Double(i) * 0.11),
                            value: appeared
                        )
                }
            }
        }
        .padding(.top, 4)
        .frame(width: 260, height: 260)
        .onAppear { appeared = true }
    }

    private var lockscreenHeader: some View {
        VStack(spacing: 3) {
            HStack(spacing: 0) {
                Text("9")
                Text(":").opacity(0.5)
                Text("14")
            }
            .font(UNESFont.serif(22))
            .tracking(-0.4)
            .foregroundStyle(UNESColor.ink)

            Text("QUI · 23 ABR")
                .font(UNESFont.mono(8))
                .tracking(1.6)
                .foregroundStyle(UNESColor.ink3)
        }
        .fadeInOnAppear(delay: 0.05)
    }

    private func pushCard(_ p: Push) -> some View {
        HStack(spacing: 10) {
            ZStack {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(p.chip)
                glyph(p.glyph)
            }
            .frame(width: 28, height: 28)

            VStack(alignment: .leading, spacing: 1) {
                HStack(alignment: .firstTextBaseline) {
                    Text(p.app.uppercased())
                        .font(UNESFont.mono(8.5))
                        .tracking(1.2)
                        .foregroundStyle(UNESColor.ink3)
                    Spacer(minLength: 6)
                    Text("agora")
                        .font(UNESFont.mono(8.5))
                        .foregroundStyle(UNESColor.ink4)
                }
                Text(p.title)
                    .font(UNESFont.sans(12.5, weight: .semibold))
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text(p.body)
                    .font(UNESFont.sans(11.5))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, 11)
        .padding(.vertical, 9)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(UNESColor.card)
                .shadow(color: .black.opacity(0.08), radius: 10, y: 6)
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(UNESColor.cardLine, lineWidth: 1)
                )
        )
    }

    @ViewBuilder
    private func glyph(_ g: Glyph) -> some View {
        switch g {
        case .grade:
            Text("9")
                .font(UNESFont.serif(15, italic: true))
                .foregroundStyle(UNESColor.surfaceLight)
        case .message:
            Image(systemName: "bubble.left")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.surfaceLight)
        case .schedule:
            Image(systemName: "clock")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.surfaceLight)
        case .material:
            Image(systemName: "doc")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.surfaceLight)
        }
    }
}
