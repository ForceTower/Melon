import SwiftUI

/// One row inside a bucket card. Shows the origin swatch, a sender/time
/// header, the origin-colored meta line, an optional subject, the preview,
/// and a footer with attachment hints and a star indicator.
///
/// Wrapped by `NavigationLink` at the call site — must NOT be a `Button`
/// itself, or the button would eat the tap before the link could push.
struct MessageRow: View {
    let message: Message

    private var preview: String {
        if let p = message.preview, !p.isEmpty { return p }
        return message.body.replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            OriginSwatch(message: message)

            VStack(alignment: .leading, spacing: 0) {
                senderRow
                contextLine
                if let subject = message.subject, !subject.isEmpty {
                    subjectLine(subject)
                }
                previewLine
                footerRow
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .topLeading) {
            if message.unread {
                Circle()
                    .fill(UNESColor.coral)
                    .frame(width: 6, height: 6)
                    .offset(x: 4, y: 22)
            }
        }
        .contentShape(Rectangle())
    }

    // MARK: - Subviews

    private var senderRow: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(message.sender.name)
                .font(UNESFont.sans(13, weight: message.unread ? .bold : .medium))
                .tracking(-0.13)
                .foregroundStyle(UNESColor.ink)
                .lineLimit(1)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, alignment: .leading)

            // Self-updating relative timestamp. Scoped to this `Text` so a long-
            // lived list doesn't freeze on "agora" but we also don't re-render
            // the entire row every minute.
            TimelineView(.periodic(from: .now, by: 60)) { context in
                Text(MessageDate.relativeTime(for: message.receivedAt, now: context.date))
                    .font(UNESFont.mono(10))
                    .tracking(0.4)
                    .foregroundStyle(UNESColor.ink4)
                    .fixedSize()
            }
        }
        .padding(.bottom, 2)
    }

    private var contextLine: some View {
        Text(message.sender.role)
            .font(UNESFont.mono(9, weight: .semibold))
            .tracking(0.9)
            .textCase(.uppercase)
            .foregroundStyle(message.meta.color)
            .lineLimit(1)
            .truncationMode(.tail)
            .padding(.bottom, 4)
    }

    private func subjectLine(_ subject: String) -> some View {
        Text(subject)
            .font(UNESFont.sans(14, weight: message.unread ? .semibold : .medium))
            .foregroundStyle(UNESColor.ink)
            .lineSpacing(14 * 0.3)
            .lineLimit(1)
            .truncationMode(.tail)
            .padding(.bottom, 3)
    }

    private var previewLine: some View {
        let hasSubject = message.subject != nil
        return Text(preview)
            .font(UNESFont.sans(hasSubject ? 12 : 13,
                                weight: !hasSubject && message.unread ? .medium : .regular))
            .foregroundStyle(hasSubject ? UNESColor.ink3 : UNESColor.ink2)
            .lineSpacing((hasSubject ? 12 : 13) * 0.45)
            .lineLimit(hasSubject ? 2 : 3)
            .truncationMode(.tail)
            .multilineTextAlignment(.leading)
    }

    @ViewBuilder
    private var footerRow: some View {
        let atts = message.attachments
        let showFooter = !atts.isEmpty || message.starred
        if showFooter {
            HStack(spacing: 10) {
                AttachHint(attachments: atts)
                if message.starred {
                    Image(systemName: "star.fill")
                        .font(.system(size: 10))
                        .foregroundStyle(Color(red: 0xD9/255, green: 0x85/255, blue: 0x2E/255))
                }
            }
            .padding(.top, 6)
        }
    }
}

/// Attachment hint shown in the row footer — mono count for files and a small
/// image icon with count for images.
struct AttachHint: View {
    let attachments: [MessageAttachment]

    var body: some View {
        let images = attachments.filter { $0.kind == .image }.count
        let files = attachments.count - images
        if files == 0 && images == 0 { EmptyView() }
        else {
            HStack(spacing: 4) {
                if files > 0 {
                    Image(systemName: "paperclip")
                        .font(.system(size: 10))
                    Text("\(files)")
                        .font(UNESFont.mono(10))
                }
                if files > 0 && images > 0 {
                    Text("·").opacity(0.4)
                }
                if images > 0 {
                    Image(systemName: "photo")
                        .font(.system(size: 10))
                    Text("\(images)")
                        .font(UNESFont.mono(10))
                }
            }
            .foregroundStyle(UNESColor.ink4)
        }
    }
}
