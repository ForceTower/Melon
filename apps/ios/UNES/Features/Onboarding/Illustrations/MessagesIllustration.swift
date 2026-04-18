import SwiftUI

struct MessagesIllustration: View {
    private struct Msg: Identifiable {
        let id = UUID()
        let from: String
        let preview: String
        let color: Color
        let time: String
        let unread: Bool
    }

    private let msgs: [Msg] = [
        .init(from: "Prof. Adriana", preview: "Gabarito da P1 liberado", color: UNESColor.magenta, time: "ag.",    unread: true),
        .init(from: "Coordenação CC", preview: "Matrícula em optativas", color: UNESColor.coral,   time: "09:14", unread: false),
        .init(from: "DCE UEFS",       preview: "Assembleia geral quinta…", color: UNESColor.amber, time: "ont.",  unread: false),
    ]

    @State private var appeared = false

    var body: some View {
        VStack(spacing: 10) {
            ForEach(Array(msgs.enumerated()), id: \.element.id) { i, m in
                HStack(spacing: 12) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(m.color)
                        Text(String(m.from.prefix(1)))
                            .font(UNESFont.serif(18))
                            .foregroundStyle(UNESColor.surface)
                    }
                    .frame(width: 40, height: 40)

                    VStack(alignment: .leading, spacing: 2) {
                        HStack {
                            Text(m.from)
                                .font(UNESFont.sans(13, weight: .semibold))
                                .foregroundStyle(UNESColor.ink)
                            Spacer()
                            Text(m.time)
                                .font(UNESFont.mono(10))
                                .foregroundStyle(UNESColor.ink3)
                        }
                        Text(m.preview)
                            .font(UNESFont.sans(13))
                            .foregroundStyle(UNESColor.ink3)
                            .lineLimit(1)
                    }

                    if m.unread {
                        Circle()
                            .fill(UNESColor.accent)
                            .frame(width: 8, height: 8)
                            .pulseForever()
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(UNESColor.surface)
                        .shadow(color: UNESColor.ink.opacity(0.05), radius: 8, y: 4)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18, style: .continuous)
                                .strokeBorder(UNESColor.ink.opacity(0.06), lineWidth: 1)
                        )
                )
                .rotationEffect(.degrees(i.isMultiple(of: 2) ? -1.5 : 1.5))
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 16)
                .animation(
                    .spring(response: 0.6, dampingFraction: 0.75).delay(0.1 + Double(i) * 0.15),
                    value: appeared
                )
            }
        }
        .padding(.top, 16)
        .frame(width: 260, height: 260)
        .onAppear { appeared = true }
    }
}
