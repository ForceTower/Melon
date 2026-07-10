#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// One message on the watch: the sender hero on the origin mesh, then the
/// subject, the full pushed body, and its attachments.
struct WatchMessageDetailView: View {
    let store: StoreOf<WatchAppFeature>
    var messageId: String

    var body: some View {
        if let message = store.snapshot?.messages.first(where: { $0.id == messageId }) {
            content(message.item)
        } else {
            Text(.watchMessagesEmpty)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
        }
    }

    private func content(_ message: MessageItem) -> some View {
        let accent = message.accentColor
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                senderHero(message)
                    .padding(.bottom, 12)

                if let subject = message.subject {
                    Text(subject)
                        .font(.system(size: 16, weight: .bold))
                        .tracking(-0.3)
                        .foregroundStyle(UNESColor.ink)
                        .padding(EdgeInsets(top: 0, leading: 2, bottom: 10, trailing: 2))
                }

                Text(MessageBodyText.linkified(message.body, accent: accent))
                    .font(.system(size: 14))
                    .lineSpacing(3)
                    .tint(accent)
                    .foregroundStyle(UNESColor.ink2)
                    .padding(.horizontal, 2)

                if !message.attachments.isEmpty {
                    VStack(spacing: 6) {
                        ForEach(message.attachments) { attachment in
                            attachmentTile(attachment, accent: accent)
                        }
                    }
                    .padding(.top, 14)
                }
            }
        }
        .navigationTitle(title(for: message))
    }

    private func title(for message: MessageItem) -> String {
        let badge = MessagesFormat.badgeLabel(message)
        return badge.isEmpty ? MessagesFormat.kindLabel(message.origin) : badge
    }

    private func senderHero(_ message: MessageItem) -> some View {
        WatchMeshCard(variant: message.meshVariant, wash: message.accentColor) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 6) {
                    let badge = MessagesFormat.badgeLabel(message)
                    if !badge.isEmpty {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .tracking(0.3)
                            .foregroundStyle(.white)
                            .padding(EdgeInsets(top: 2, leading: 6, bottom: 2, trailing: 6))
                            .background(.white.opacity(0.2), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
                    }
                    Text(MessagesFormat.kindLabel(message.origin))
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.82))
                        .lineLimit(1)
                }
                Text(message.senderName)
                    .font(.system(size: 17, weight: .bold))
                    .tracking(-0.4)
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .padding(.top, 8)
                Text(WatchFormat.messageTimestamp(message.receivedAt, now: .now))
                    .font(.system(size: 11.5, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(.white.opacity(0.66))
                    .padding(.top, 3)
            }
        }
    }

    /// Attachments open in the watch's web viewer when the URL parses; the
    /// phone keeps the richer preview/download version.
    @ViewBuilder
    private func attachmentTile(_ attachment: MessageAttachment, accent: Color) -> some View {
        if let url = URL(string: attachment.url) {
            Link(destination: url) {
                attachmentTileContent(attachment, accent: accent)
            }
            .buttonStyle(.plain)
        } else {
            attachmentTileContent(attachment, accent: accent)
        }
    }

    private func attachmentTileContent(_ attachment: MessageAttachment, accent: Color) -> some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(accent.opacity(0.16))
                .frame(width: 28, height: 28)
                .overlay {
                    Image(systemName: symbolName(attachment.kind))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(accent)
                }
            VStack(alignment: .leading, spacing: 1) {
                Text(attachment.name ?? attachment.url)
                    .font(.system(size: 12.5, weight: .semibold))
                    .tracking(-0.1)
                    .foregroundStyle(UNESColor.ink)
                    .lineLimit(1)
                Text(subtitle(attachment))
                    .font(.system(size: 10.5, weight: .medium))
                    .foregroundStyle(UNESColor.ink4)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(EdgeInsets(top: 8, leading: 9, bottom: 8, trailing: 9))
        .background(Color.white.opacity(0.065), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .strokeBorder(Color.white.opacity(0.09))
        }
    }

    private func symbolName(_ kind: MessageAttachment.Kind) -> String {
        switch kind {
        case .link: "link"
        case .pdf: "doc.text"
        case .video: "play.rectangle"
        case .image: "photo"
        case .other: "doc"
        }
    }

    private func subtitle(_ attachment: MessageAttachment) -> String {
        switch attachment.kind {
        case .link: attachment.host ?? attachment.url
        default: attachment.kind.rawValue.uppercased()
        }
    }
}

#Preview {
    NavigationStack {
        WatchMessageDetailView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            },
            messageId: "wm2"
        )
    }
}

#Preview("Pessoal") {
    NavigationStack {
        WatchMessageDetailView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            },
            messageId: "wm3"
        )
    }
}
#endif
