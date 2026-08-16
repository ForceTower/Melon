import SwiftUI

// MARK: - Palette

/// The palette baked into an exported card. Locked on purpose: the image
/// leaves the app, so it must not follow the *recipient's* appearance — only
/// the theme the sender was looking at when they shared.
struct MessageSharePalette {
    var background: Color
    var ink: Color
    var ink2: Color
    var ink3: Color
    var ink4: Color
    var line: Color

    static let light = MessageSharePalette(
        background: Color(hex: 0xFBF7F2),
        ink: Color(hex: 0x1A1420),
        ink2: Color(hex: 0x3A2F42),
        ink3: Color(hex: 0x6B5E70),
        ink4: Color(hex: 0x9C8FA0),
        line: Color(hex: 0x1A1420, opacity: 0.10)
    )

    static let dark = MessageSharePalette(
        background: Color(hex: 0x15101A),
        ink: Color(hex: 0xF5EFE6),
        ink2: Color(hex: 0xD6CEC2),
        ink3: Color(hex: 0x9F9386),
        ink4: Color(hex: 0x7B7066),
        line: Color(hex: 0xF5EFE6, opacity: 0.12)
    )

    static func of(_ scheme: ColorScheme) -> MessageSharePalette {
        scheme == .dark ? .dark : .light
    }
}

// MARK: - Card

/// The render that leaves the app: fixed width, height growing with the
/// message so the whole body ships inside the image, never truncated.
///
/// Laid out on the design's 1080-unit grid and scaled by `width`, so the same
/// view backs both the in-sheet preview and the 3× export.
struct MessageShareCard: View {
    var message: MessageItem
    var width: CGFloat = MessageShareCard.exportWidth
    var scheme: ColorScheme
    var rounded = true

    /// Rendered at 3×, this is a 1080pt-wide image — the design's grid.
    static let exportWidth: CGFloat = 360

    private var palette: MessageSharePalette { .of(scheme) }
    private func px(_ value: CGFloat) -> CGFloat { value * (width / 1080) }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle()
                .fill(message.accentColor)
                .frame(height: px(10))

            content(accent: message.accentColor)
        }
        .frame(width: width)
        .background(palette.background)
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
                    .foregroundStyle(palette.ink)
                    .padding(.bottom, px(18))
            }

            Text(MessagesFormat.fullTimestamp(for: message.receivedAt))
                .font(.system(size: px(24), weight: .semibold))
                .monospacedDigit()
                .foregroundStyle(palette.ink4)
                .padding(.bottom, px(34))

            Text(message.body.trimmingCharacters(in: .whitespacesAndNewlines))
                .font(.system(size: px(31)))
                .lineSpacing(px(31 * 0.42))
                .foregroundStyle(palette.ink2)

            footer
                .padding(.top, px(48))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: px(52), leading: px(64), bottom: px(48), trailing: px(64)))
        .background(alignment: .top) { wash(accent) }
    }

    // MARK: Blocks

    /// Wordmark only. The design also carried the origin kind here, but the
    /// sender block below already names it — and the `.direct` label reads
    /// "para você", which is addressed to the wrong person once the card is
    /// out of the app.
    private var header: some View {
        HStack(alignment: .firstTextBaseline, spacing: px(6)) {
            Text(verbatim: "unes")
                .font(.system(size: px(34), weight: .heavy))
                .tracking(px(-34 * 0.05))
                .foregroundStyle(palette.ink)
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
                    .foregroundStyle(palette.ink)
                Text(MessagesFormat.roleLine(message))
                    .font(.system(size: px(25), weight: .medium))
                    .foregroundStyle(palette.ink3)
            }
        }
    }

    private var footer: some View {
        VStack(alignment: .leading, spacing: 0) {
            Rectangle()
                .fill(palette.line)
                .frame(height: max(1, px(1.5)))
                .padding(.bottom, px(26))

            HStack(spacing: px(7)) {
                Text(.messagesShareReceivedIn)
                    .font(.system(size: px(21), weight: .medium))
                    .foregroundStyle(palette.ink4)
                Text(verbatim: "UNES")
                    .font(.system(size: px(21), weight: .bold))
                    .foregroundStyle(palette.ink3)
            }
        }
    }

    /// The origin accent bleeding out of the top-left corner, under the header.
    ///
    /// Elliptical, not radial: the radii have to scale with the frame on both
    /// axes, or the wash is still opaque when the frame clips it and the card
    /// gets a hard horizontal seam. It reaches clear ~180 units down, well
    /// inside the 290 it is given.
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

/// The plain-text form of a message — what a recipient without the app can
/// read, search and quote. Same content as the card, no chrome.
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
