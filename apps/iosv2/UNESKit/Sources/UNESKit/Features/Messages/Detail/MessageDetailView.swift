import ComposableArchitecture
import SwiftUI

/// One message, end to end: sender card, subject + full timestamp, the
/// linkified body, and attachment tiles — everything washed in the origin's
/// accent bleeding down from the top.
struct MessageDetailView: View {
    @Bindable var store: StoreOf<MessageDetailFeature>

    var body: some View {
        let message = store.message
        let accent = message.accentColor

        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientTint(accent)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    senderCard(message, accent: accent)
                        .fadeUp(delay: 0.04)
                        .padding(.bottom, 18)

                    VStack(alignment: .leading, spacing: 10) {
                        if let subject = message.subject {
                            Text(subject)
                                .font(.system(size: 27, weight: .bold))
                                .tracking(-0.94)
                                .foregroundStyle(UNESColor.ink)
                        }
                        Text(MessagesFormat.fullTimestamp(for: message.receivedAt))
                            .font(.system(size: 13, weight: .medium))
                            .monospacedDigit()
                            .foregroundStyle(UNESColor.ink4)
                    }
                    .fadeUp(delay: 0.1)
                    .padding(EdgeInsets(top: 0, leading: 4, bottom: 16, trailing: 4))

                    Text(MessageBodyText.linkified(message.body, accent: accent))
                        .font(.system(size: 16))
                        .lineSpacing(5)
                        .tint(accent)
                        .foregroundStyle(UNESColor.ink2)
                        .textSelection(.enabled)
                        .fadeUp(delay: 0.18)
                        .padding(EdgeInsets(top: 0, leading: 4, bottom: 22, trailing: 4))

                    if !message.imageAttachments.isEmpty {
                        imagesGrid(message.imageAttachments, accent: accent)
                            .fadeUp(delay: 0.26)
                            .padding(.bottom, 18)
                    }

                    if !message.fileAttachments.isEmpty {
                        attachmentsBlock(message.fileAttachments, accent: accent)
                            .fadeUp(delay: 0.32)
                            .padding(.bottom, 20)
                    }
                }
                .padding(EdgeInsets(top: 8, leading: 16, bottom: 24, trailing: 16))
            }
        }
        .toolbar {
            ToolbarItem(placement: .trailingCompat) {
                Button {
                    store.send(.starTapped)
                } label: {
                    Image(systemName: message.starred ? "star.fill" : "star")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(message.starred ? UNESColor.caution : UNESColor.ink3)
                }
            }
        }
        .inlineNavigationBar()
    }

    // MARK: Sender

    private func senderCard(_ message: MessageItem, accent: Color) -> some View {
        HStack(spacing: 13) {
            MessageOriginAvatar(message: message, size: 48)

            VStack(alignment: .leading, spacing: 3) {
                Text(MessagesFormat.kindLabel(message.origin))
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.22)
                    .foregroundStyle(accent)
                Text(message.senderName)
                    .font(.system(size: 17, weight: .bold))
                    .tracking(-0.34)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(2)
                Text(MessagesFormat.roleLine(message))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
                    .lineLimit(1)
            }
        }
        .padding(15)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
        .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
    }

    // MARK: Attachments

    private func imagesGrid(_ images: [MessageAttachment], accent: Color) -> some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: images.count == 1 ? 1 : 2),
            spacing: 8
        ) {
            ForEach(images) { image in
                MessageImageTile(attachment: image, accent: accent)
            }
        }
    }

    private func attachmentsBlock(_ files: [MessageAttachment], accent: Color) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(.messagesDetailAttachmentsCount(files.count))
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .bold))
                .tracking(0.44)
                .monospacedDigit()
                .foregroundStyle(UNESColor.ink4)
                .padding(EdgeInsets(top: 0, leading: 4, bottom: 10, trailing: 0))

            VStack(spacing: 8) {
                ForEach(files) { file in
                    MessageAttachmentTile(attachment: file, accent: accent)
                }
            }
        }
    }

    /// The origin accent washing down from behind the navigation bar.
    private func ambientTint(_ accent: Color) -> some View {
        EllipticalGradient(
            stops: [
                .init(color: accent, location: 0),
                .init(color: .clear, location: 0.72),
            ],
            center: .top
        )
        .frame(height: 240)
        .padding(.horizontal, -30)
        .opacity(0.18)
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

// MARK: - Tiles

private struct MessageImageTile: View {
    var attachment: MessageAttachment
    var accent: Color

    var body: some View {
        AsyncImage(url: URL(string: attachment.url)) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                LinearGradient.css(
                    stops: [
                        .init(color: accent.opacity(0.19), location: 0),
                        .init(color: accent.opacity(0.06), location: 1),
                    ],
                    angle: 135
                )
                Image(systemName: "photo")
                    .font(.system(size: 26, weight: .medium))
                    .foregroundStyle(accent)
            }
        }
        .aspectRatio(16 / 10, contentMode: .fill)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }
}

private struct MessageAttachmentTile: View {
    var attachment: MessageAttachment
    var accent: Color

    @Environment(\.openURL) private var openURL

    var body: some View {
        Button {
            if let url = URL(string: attachment.url) {
                openURL(url)
            }
        } label: {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(accent.opacity(0.13))
                    .frame(width: 42, height: 42)
                    .overlay {
                        Image(systemName: symbolName)
                            .font(.system(size: 17, weight: .medium))
                            .foregroundStyle(accent)
                    }

                VStack(alignment: .leading, spacing: 2) {
                    Text(attachment.name ?? attachment.url)
                        .font(.system(size: 14, weight: .semibold))
                        .tracking(-0.14)
                        .foregroundStyle(UNESColor.ink)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                Image(systemName: attachment.kind == .link ? "chevron.right" : "arrow.down")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
            }
            .padding(12)
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.04), radius: 6, y: 4)
        }
        .buttonStyle(.pressableCard)
    }

    private var symbolName: String {
        switch attachment.kind {
        case .link: "link"
        case .pdf: "doc.text"
        case .video: "play.rectangle"
        case .image, .other: "doc"
        }
    }

    private var subtitle: String {
        switch attachment.kind {
        case .link: attachment.host ?? attachment.url
        default: attachment.kind.rawValue.uppercased()
        }
    }
}

// MARK: - Linkify

enum MessageBodyText {
    /// Detected links tinted in the origin accent — `NSDataDetector` instead
    /// of the old hand-rolled TLD regex, so bare hosts like "uefs.br/…" still
    /// resolve without an allowlist.
    static func linkified(_ text: String, accent: Color) -> AttributedString {
        var attributed = AttributedString(text)
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else {
            return attributed
        }
        for match in detector.matches(in: text, range: NSRange(text.startIndex..., in: text)) {
            guard let url = match.url,
                  let range = Range(match.range, in: text),
                  let attributedRange = Range(range, in: attributed)
            else { continue }
            attributed[attributedRange].link = url
            attributed[attributedRange].foregroundColor = accent
            attributed[attributedRange].underlineStyle = .single
        }
        return attributed
    }
}

#Preview {
    NavigationStack {
        MessageDetailView(
            store: Store(
                initialState: MessageDetailFeature.State(message: MessagesOverview.preview().messages[1])
            ) {
                MessageDetailFeature()
            }
        )
    }
}

#Preview("Comunicado") {
    NavigationStack {
        MessageDetailView(
            store: Store(
                initialState: MessageDetailFeature.State(message: MessagesOverview.preview().messages[0])
            ) {
                MessageDetailFeature()
            }
        )
    }
}
