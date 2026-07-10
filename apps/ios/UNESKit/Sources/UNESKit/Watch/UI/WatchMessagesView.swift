#if os(watchOS)
import ComposableArchitecture
import SwiftUI

/// The watch "Recados" list: the pushed inbox slice, newest first.
struct WatchMessagesView: View {
    let store: StoreOf<WatchAppFeature>

    var body: some View {
        let messages = store.snapshot?.messages ?? []
        TimelineView(.everyMinute) { context in
            let now = context.date
            List {
                if messages.isEmpty {
                    Text(.watchMessagesEmpty)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                } else {
                    Section {
                        ForEach(messages) { message in
                            Button {
                                store.send(.messageTapped(message.id))
                            } label: {
                                WatchMessageRow(message: message.item, now: now)
                            }
                            .listRowInsets(EdgeInsets(top: 8, leading: 4, bottom: 8, trailing: 8))
                        }
                    } header: {
                        unreadLine(messages)
                    }
                }
            }
        }
        .navigationTitle(Text(.watchMessagesTitle))
    }

    private func unreadLine(_ messages: [WatchSnapshot.Message]) -> some View {
        let unread = messages.count(where: \.unread)
        return Text(WatchFormat.unreadCount(unread))
            .font(.system(size: 12, weight: .bold))
            .tracking(0.4)
            .textCase(.uppercase)
            .foregroundStyle(unread > 0 ? UNESColor.coral : UNESColor.ink3)
    }
}

/// One inbox row: unread dot, origin color bar, badge + sender + time, then
/// the subject-led preview and the first attachment's name.
struct WatchMessageRow: View {
    var message: MessageItem
    var now: Date

    var body: some View {
        HStack(alignment: .top, spacing: 7) {
            Capsule()
                .fill(message.accentColor)
                .frame(width: 4)
                .frame(maxHeight: .infinity)
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 5) {
                    if message.unread {
                        Circle()
                            .fill(UNESColor.coral)
                            .frame(width: 6, height: 6)
                    }
                    let badge = MessagesFormat.badgeLabel(message)
                    if !badge.isEmpty {
                        WatchCodeChip(text: badge, color: message.accentColor)
                    }
                    Text(message.senderName)
                        .font(.system(size: 13.5, weight: message.unread ? .bold : .semibold))
                        .tracking(-0.2)
                        .foregroundStyle(message.unread ? UNESColor.ink : UNESColor.ink2)
                        .lineLimit(1)
                    Spacer(minLength: 2)
                    Text(MessagesFormat.relativeTime(for: message.receivedAt, now: now))
                        .font(.system(size: 11, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(UNESColor.ink4)
                        .lineLimit(1)
                }
                WatchMessagePreviewText(message: message)
                    .font(.system(size: 12.5, weight: message.unread ? .semibold : .medium))
                    .foregroundStyle(message.unread ? UNESColor.ink2 : UNESColor.ink3)
                    .lineLimit(2)
                if let attachment = message.attachments.first {
                    HStack(spacing: 4) {
                        Image(systemName: attachment.kind == .link ? "link" : "paperclip")
                            .font(.system(size: 10, weight: .semibold))
                        Text(attachment.name ?? attachment.url)
                            .font(.system(size: 11, weight: .semibold))
                            .lineLimit(1)
                    }
                    .foregroundStyle(UNESColor.ink3)
                }
            }
        }
    }
}

/// "Subject · preview" with the subject bolded, or just the preview.
struct WatchMessagePreviewText: View {
    var message: MessageItem

    var body: some View {
        if let subject = message.subject {
            Text("\(Text(subject).fontWeight(.bold)) · \(message.preview)")
        } else {
            Text(message.preview)
        }
    }
}

/// The latest-message hero on "Hoje": the origin mesh washed in its accent,
/// with the same badge + sender + preview line the list rows carry.
struct WatchMessageHeroCard: View {
    var message: MessageItem
    var now: Date

    var body: some View {
        WatchMeshCard(variant: message.meshVariant, wash: message.accentColor) {
            VStack(alignment: .leading, spacing: 7) {
                HStack(spacing: 6) {
                    if message.unread {
                        Circle()
                            .fill(.white)
                            .frame(width: 6, height: 6)
                    }
                    let badge = MessagesFormat.badgeLabel(message)
                    if !badge.isEmpty {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .tracking(0.3)
                            .foregroundStyle(.white)
                            .padding(EdgeInsets(top: 2, leading: 5, bottom: 2, trailing: 5))
                            .background(.white.opacity(0.22), in: RoundedRectangle(cornerRadius: 5, style: .continuous))
                    }
                    Text(message.senderName)
                        .font(.system(size: 13.5, weight: .bold))
                        .tracking(-0.2)
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Spacer(minLength: 2)
                    Text(MessagesFormat.relativeTime(for: message.receivedAt, now: now))
                        .font(.system(size: 11, weight: .medium))
                        .monospacedDigit()
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
                WatchMessagePreviewText(message: message)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(.white.opacity(0.86))
                    .lineLimit(2)
            }
        }
    }
}

#Preview {
    NavigationStack {
        WatchMessagesView(
            store: Store(initialState: WatchAppFeature.State(snapshot: .preview(), hasLoaded: true)) {
                WatchAppFeature()
            }
        )
    }
}
#endif
