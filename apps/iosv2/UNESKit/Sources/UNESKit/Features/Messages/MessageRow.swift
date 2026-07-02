import SwiftUI

// MARK: - Origin colors

extension MessageItem {
    /// The origin accent driving the avatar, role line, and detail tint.
    var accentColor: Color {
        switch origin {
        case .discipline:
            disciplineColorIndex.map(UNESColor.disciplineReadableColor) ?? UNESColor.slate
        case .secretariat: UNESColor.slate
        case .campus: UNESColor.caution
        case .app: UNESColor.tealReadable
        case .direct: UNESColor.coral
        }
    }
}

extension MessageCategory {
    var color: Color {
        switch self {
        case .disciplines: UNESColor.tealReadable
        case .university: UNESColor.caution
        case .app: UNESColor.deepViolet
        }
    }
}

// MARK: - Row

/// One inbox row: origin avatar, sender + time line, role line, optional
/// subject, preview, and the attachment/star footer. Unread rows carry the
/// accent tint and rail.
struct MessageRow: View {
    var message: MessageItem
    var relativeTime: String
    var isLast: Bool
    var onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                MessageOriginAvatar(message: message)

                VStack(alignment: .leading, spacing: 0) {
                    headline
                    Text(MessagesFormat.roleLine(message))
                        .textCase(.uppercase)
                        .font(.system(size: 11, weight: .bold))
                        .tracking(0.22)
                        .foregroundStyle(message.accentColor)
                        .lineLimit(1)
                        .padding(.top, 2)

                    if let subject = message.subject {
                        Text(subject)
                            .font(.system(size: 14, weight: message.unread ? .semibold : .medium))
                            .tracking(-0.14)
                            .foregroundStyle(UNESColor.ink)
                            .lineLimit(1)
                            .padding(.top, 4)
                    }

                    Text(message.preview)
                        .font(.system(size: 13.5))
                        .lineSpacing(3)
                        .foregroundStyle(message.subject == nil ? UNESColor.ink2 : UNESColor.ink3)
                        .lineLimit(message.subject == nil ? 3 : 2)
                        .multilineTextAlignment(.leading)
                        .padding(.top, message.subject == nil ? 4 : 2)

                    if !message.attachments.isEmpty || message.starred {
                        HStack(spacing: 12) {
                            MessageAttachmentHint(attachments: message.attachments)
                            if message.starred {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundStyle(UNESColor.caution)
                            }
                        }
                        .padding(.top, 7)
                    }
                }
            }
            .padding(EdgeInsets(top: 13, leading: 14, bottom: 13, trailing: 14))
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(MessageRowPressStyle(unread: message.unread))
        .overlay(alignment: .leading) {
            if message.unread {
                RoundedRectangle(cornerRadius: 2)
                    .fill(UNESColor.accent)
                    .frame(width: 3)
                    .padding(.vertical, 12)
            }
        }
        .overlay(alignment: .bottom) {
            if !isLast {
                Rectangle()
                    .fill(UNESColor.line)
                    .frame(height: 0.5)
                    .padding(.leading, 70)
            }
        }
    }

    private var headline: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(message.senderName)
                .font(.system(size: 15.5, weight: message.unread ? .bold : .semibold))
                .tracking(-0.31)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)

            Text(relativeTime)
                .font(.system(size: 12.5, weight: message.unread ? .bold : .medium))
                .monospacedDigit()
                .foregroundStyle(message.unread ? UNESColor.accent : UNESColor.ink4)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(UNESColor.ink4)
        }
    }
}

/// Rows tint accent-6% while unread and highlight like a table cell on press.
private struct MessageRowPressStyle: ButtonStyle {
    var unread: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background {
                if configuration.isPressed {
                    UNESColor.surface2
                } else if unread {
                    UNESColor.accent.opacity(0.06)
                }
            }
            .animation(.easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

// MARK: - Avatar

/// The 44pt origin swatch: disciplines get their tinted code block, direct
/// messages a person ring, and the rest a tinted symbol tile.
struct MessageOriginAvatar: View {
    var message: MessageItem
    var size: CGFloat = 44

    var body: some View {
        let color = message.accentColor
        switch message.origin {
        case .discipline:
            Text(MessagesFormat.badgeLabel(message))
                .font(.system(size: size * 0.24, weight: .bold))
                .tracking(0.2)
                .monospacedDigit()
                .minimumScaleFactor(0.6)
                .lineLimit(1)
                .foregroundStyle(.white)
                .padding(.horizontal, 4)
                .frame(width: size, height: size)
                .background(color, in: RoundedRectangle(cornerRadius: size * 0.29, style: .continuous))
                .shadow(color: color.opacity(0.27), radius: 6, y: 4)

        case .direct:
            Circle()
                .fill(color.opacity(0.11))
                .frame(width: size, height: size)
                .overlay {
                    Circle().strokeBorder(color.opacity(0.36), lineWidth: 1.5)
                }
                .overlay {
                    Image(systemName: "person")
                        .font(.system(size: size * 0.4, weight: .medium))
                        .foregroundStyle(color)
                }

        case .secretariat, .campus, .app:
            RoundedRectangle(cornerRadius: size * 0.29, style: .continuous)
                .fill(color.opacity(0.12))
                .frame(width: size, height: size)
                .overlay {
                    Image(systemName: symbolName)
                        .font(.system(size: size * 0.38, weight: .medium))
                        .foregroundStyle(color)
                }
        }
    }

    private var symbolName: String {
        switch message.origin {
        case .secretariat: "envelope"
        case .campus: "graduationcap"
        default: "bookmark"
        }
    }
}

// MARK: - Attachment hint

/// "📎 2 · 🖼 1" — compact counts under the preview.
struct MessageAttachmentHint: View {
    var attachments: [MessageAttachment]

    var body: some View {
        let images = attachments.count { $0.kind == .image }
        let files = attachments.count - images
        if files > 0 || images > 0 {
            HStack(spacing: 5) {
                if files > 0 {
                    Image(systemName: "paperclip")
                    Text("\(files)")
                }
                if images > 0 {
                    if files > 0 {
                        Text("·").foregroundStyle(UNESColor.ink4.opacity(0.4))
                    }
                    Image(systemName: "photo")
                    Text("\(images)")
                }
            }
            .font(.system(size: 12, weight: .semibold))
            .monospacedDigit()
            .foregroundStyle(UNESColor.ink4)
        }
    }
}

#Preview {
    let overview = MessagesOverview.preview()
    ScrollView {
        VStack(spacing: 0) {
            ForEach(Array(overview.messages.prefix(5).enumerated()), id: \.element.id) { index, message in
                MessageRow(
                    message: message,
                    relativeTime: MessagesFormat.relativeTime(for: message.receivedAt, now: .now),
                    isLast: index == 4,
                    onTap: {}
                )
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .padding(16)
    }
    .background(UNESColor.surface)
}
