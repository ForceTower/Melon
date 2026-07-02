import SwiftUI

/// The widget-style inbox digest: unread headline (or the all-clear check),
/// total count, a category composition bar with legend, and the
/// mark-all-read action while anything is unread.
struct MessagesDigestHero: View {
    var messages: [MessageItem]
    var onMarkAllRead: () -> Void

    private var unreadCount: Int { messages.count(where: \.unread) }

    private var slices: [(category: MessageCategory, count: Int)] {
        MessageCategory.allCases.map { category in
            (category, messages.count { $0.category == category })
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(unreadCount == 0 ? "Caixa de entrada" : "Não lidas")
                .textCase(.uppercase)
                .font(.system(size: 12, weight: .semibold))
                .tracking(0.4)
                .foregroundStyle(UNESColor.ink3)

            HStack(alignment: .lastTextBaseline) {
                headline
                Spacer(minLength: 12)
                VStack(alignment: .trailing, spacing: 2) {
                    Text("\(messages.count)")
                        .font(.system(size: 26, weight: .bold))
                        .tracking(-0.78)
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink)
                    Text("no total")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                }
            }
            .padding(.top, 4)

            compositionBar
                .padding(.top, 16)

            legend
                .padding(.top, 12)

            if unreadCount > 0 {
                Button(action: onMarkAllRead) {
                    HStack(spacing: 7) {
                        Image(systemName: "checkmark")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(UNESColor.accent)
                        Text("Marcar todas como lidas")
                            .font(.system(size: 14, weight: .semibold))
                            .tracking(-0.14)
                            .foregroundStyle(UNESColor.ink)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 41)
                }
                .buttonStyle(DigestActionStyle())
                .padding(.top, 15)
            }
        }
        .padding(EdgeInsets(top: 18, leading: 18, bottom: 16, trailing: 18))
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .background(alignment: .topTrailing) { accentWash }
        .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.07), radius: 12, y: 8)
    }

    @ViewBuilder
    private var headline: some View {
        if unreadCount == 0 {
            HStack(spacing: 10) {
                Circle()
                    .fill(UNESColor.tealReadable.opacity(0.16))
                    .frame(width: 42, height: 42)
                    .overlay {
                        Image(systemName: "checkmark")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(UNESColor.tealReadable)
                    }
                Text("Tudo em dia")
                    .font(.system(size: 24, weight: .bold))
                    .tracking(-0.72)
                    .foregroundStyle(UNESColor.ink)
            }
        } else {
            HStack(alignment: .lastTextBaseline, spacing: 10) {
                Text("\(unreadCount)")
                    .font(.system(size: 52, weight: .bold))
                    .tracking(-2.34)
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink)
                Text(unreadCount == 1 ? "mensagem nova" : "mensagens novas")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
    }

    /// Category shares as proportional rounded segments.
    private var compositionBar: some View {
        GeometryReader { geometry in
            let visible = slices.filter { $0.count > 0 }
            let total = max(1, visible.reduce(0) { $0 + $1.count })
            let gaps = CGFloat(max(0, visible.count - 1)) * 3
            HStack(spacing: 3) {
                ForEach(visible, id: \.category) { slice in
                    RoundedRectangle(cornerRadius: 4)
                        .fill(slice.category.color)
                        .frame(width: (geometry.size.width - gaps) * CGFloat(slice.count) / CGFloat(total))
                }
            }
        }
        .frame(height: 8)
    }

    private var legend: some View {
        FlowLayout(spacing: 16, lineSpacing: 8) {
            ForEach(slices, id: \.category) { slice in
                HStack(spacing: 6) {
                    Circle()
                        .fill(slice.category.color)
                        .frame(width: 8, height: 8)
                    Text(slice.category.label)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.ink2)
                    Text("\(slice.count)")
                        .font(.system(size: 12.5, weight: .semibold))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                }
            }
        }
    }

    /// The faint accent glow bleeding from the top-right corner.
    private var accentWash: some View {
        EllipticalGradient(
            stops: [
                .init(color: UNESColor.accent, location: 0),
                .init(color: .clear, location: 0.46),
            ],
            center: .topTrailing,
            endRadiusFraction: 1.2
        )
        .opacity(0.12)
        .allowsHitTesting(false)
    }
}

/// The v2 `.m-press` treatment: settles to 98% on a filled neutral pill.
private struct DigestActionStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(
                configuration.isPressed ? UNESColor.surface3 : UNESColor.surface2,
                in: RoundedRectangle(cornerRadius: 13, style: .continuous)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 20) {
            MessagesDigestHero(messages: MessagesOverview.preview().messages, onMarkAllRead: {})
            MessagesDigestHero(
                messages: MessagesOverview.preview().messages.map {
                    var message = $0
                    message.unread = false
                    return message
                },
                onMarkAllRead: {}
            )
        }
        .padding(16)
    }
    .background(UNESColor.surface)
}
