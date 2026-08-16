import SwiftUI

// MARK: - Card

/// Fixed width, height growing with the message so the whole body ships
/// inside the image. Laid out on the design's 1080-unit grid and scaled by
/// `width`, so one view backs both the sheet preview and the export.
struct MessageShareCard: View {
    var message: MessageItem
    var width: CGFloat = MessageShareCard.exportWidth
    var scheme: ColorScheme
    var rounded = true

    /// 1080px wide once rendered at 3×.
    static let exportWidth: CGFloat = 360

    private func px(_ value: CGFloat) -> CGFloat { value * (width / 1080) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle()
                .fill(message.accentColor)
                .frame(height: px(10))

            content(accent: message.accentColor)
        }
        .frame(width: width)
        .background(UNESColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: rounded ? px(40) : 0, style: .continuous))
        .environment(\.colorScheme, scheme)
    }

    private func content(accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(.bottom, px(44))

            sender
                .padding(.bottom, px(40))

            if let subject = message.subject, !subject.isEmpty {
                Text(subject)
                    .font(.system(size: px(54), weight: .bold))
                    .tracking(px(-54 * 0.035))
                    .foregroundStyle(UNESColor.ink)
                    .padding(.bottom, px(18))
            }

            Text(MessagesFormat.fullTimestamp(for: message.receivedAt))
                .font(.system(size: px(24), weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
                .padding(.bottom, px(34))

            Text(message.body.trimmingCharacters(in: .whitespacesAndNewlines))
                .font(.system(size: px(31)))
                .lineSpacing(px(31 * 0.42))
                .foregroundStyle(UNESColor.ink2)

            footer
                .padding(.top, px(48))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: px(52), leading: px(64), bottom: px(48), trailing: px(64)))
        .background(alignment: .top) { wash(accent) }
    }

    // MARK: Blocks

    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: px(6)) {
            Text(verbatim: "unes")
                .font(.system(size: px(34), weight: .heavy))
                .tracking(px(-34 * 0.05))
                .foregroundStyle(UNESColor.ink)
            Circle()
                .fill(
                    LinearGradient(
                        stops: [
                            .init(color: UNESColor.amber, location: 0),
                            .init(color: UNESColor.coral, location: 0.55),
                            .init(color: UNESColor.magenta, location: 1),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: px(9), height: px(9))
        }
    }

    private var sender: some View {
        HStack(spacing: px(22)) {
            MessageOriginAvatar(message: message, size: px(96))

            VStack(alignment: .leading, spacing: px(6)) {
                Text(message.senderName)
                    .font(.system(size: px(36), weight: .bold))
                    .tracking(px(-36 * 0.025))
                    .foregroundStyle(UNESColor.ink)
                Text(MessagesFormat.roleLine(message))
                    .font(.system(size: px(25), weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle()
                .fill(UNESColor.line)
                .frame(height: max(1, px(1.5)))
                .padding(.bottom, px(26))

            HStack(spacing: px(7)) {
                Text(.messagesShareReceivedIn)
                    .font(.system(size: px(21), weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                Text(verbatim: "UNES")
                    .font(.system(size: px(21), weight: .bold))
                    .foregroundStyle(UNESColor.ink3)
            }
        }
    }

    /// Elliptical so the falloff scales with the frame on both axes — a
    /// width-derived radius is still opaque where the frame clips it.
    private func wash(_ accent: Color) -> some View {
        EllipticalGradient(
            stops: [
                .init(color: accent.opacity(scheme == .dark ? 0.18 : 0.14), location: 0),
                .init(color: .clear, location: 0.72),
            ],
            center: UnitPoint(x: 0.12, y: 0),
            endRadiusFraction: 0.875
        )
        .frame(height: px(290))
        .allowsHitTesting(false)
    }
}

// MARK: - Text form

/// Same content as the card, for recipients who need to read, search and
/// quote it.
enum MessageShareText {
    static func build(for message: MessageItem) -> String {
        var lines = ["\(message.senderName) — \(MessagesFormat.roleLine(message))"]
        if let subject = message.subject, !subject.isEmpty {
            lines.append(subject)
        }
        lines.append(MessagesFormat.fullTimestamp(for: message.receivedAt))
        lines.append("")
        lines.append(message.body.trimmingCharacters(in: .whitespacesAndNewlines))
        lines.append("")
        lines.append(.localized(.messagesShareTextFooter))
        return lines.joined(separator: "\n")
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 24) {
            MessageShareCard(message: MessagesOverview.preview().messages[1], scheme: .light)
            MessageShareCard(message: MessagesOverview.preview().messages[3], scheme: .dark)
        }
        .padding(24)
    }
    .background(UNESColor.surface2)
}
