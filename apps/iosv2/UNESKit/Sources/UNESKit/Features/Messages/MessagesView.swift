import ComposableArchitecture
import SwiftUI

struct MessagesView: View {
    @Bindable var store: StoreOf<MessagesFeature>

    var body: some View {
        NavigationStack(path: $store.scope(state: \.path, action: \.path)) {
            ZStack(alignment: .top) {
                UNESColor.surface.ignoresSafeArea()
                ambientWash

                if let overview = store.overview {
                    if overview.messages.isEmpty {
                        emptyState
                    } else {
                        loaded(overview)
                    }
                } else if let message = store.errorMessage {
                    errorState(message)
                } else {
                    SpinnerRing(size: 28, color: UNESColor.accent, trackColor: UNESColor.surface3)
                        .frame(maxHeight: .infinity)
                }
            }
            .navigationTitle("Mensagens")
        } destination: { store in
            switch store.case {
            case let .detail(store):
                MessageDetailView(store: store)
            }
        }
        .task { await store.send(.task).finish() }
    }

    // MARK: Content

    private func loaded(_ overview: MessagesOverview) -> some View {
        TimelineView(.everyMinute) { context in
            let now = context.date
            let inbox = bucketed(
                overview.messages.filter { store.filter.matches($0) },
                now: now,
                limit: store.revealLimit
            )

            ScrollView {
                VStack(spacing: 0) {
                    MessagesDigestHero(messages: overview.messages) {
                        store.send(.markAllReadTapped)
                    }
                    .scaleIn(delay: 0.08, duration: 0.62)
                    .padding(EdgeInsets(top: 8, leading: 16, bottom: 22, trailing: 16))

                    MessageFilterChips(active: store.filter, messages: overview.messages) {
                        store.send(.filterSelected($0))
                    }
                    .fadeUp(delay: 0.16)
                    .padding(.bottom, 18)

                    if inbox.groups.isEmpty {
                        Text("Nenhuma mensagem neste filtro.")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .padding(EdgeInsets(top: 70, leading: 40, bottom: 70, trailing: 40))
                    }

                    ForEach(Array(inbox.groups.enumerated()), id: \.element.bucket) { index, group in
                        section(group, now: now)
                            .fadeUp(delay: 0.22 + Double(index) * 0.06)
                            .padding(EdgeInsets(top: 0, leading: 16, bottom: 22, trailing: 16))
                    }

                    if let syncedAt = overview.syncedAt, !inbox.groups.isEmpty {
                        Text(HomeFormat.updatedLabel(lastRefreshed: syncedAt, now: now))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                            .fadeUp(delay: 0.22 + Double(inbox.groups.count) * 0.06)
                            .padding(.bottom, 8)
                    }
                }
                .padding(.bottom, 12)
            }
            .refreshable {
                await store.send(.refreshPulled).finish()
            }
            // The rows are eager (plain VStack), so an onAppear sentinel
            // would fire at build time; proximity has to come from the
            // scroll geometry instead. ~900pt ≈ one screen of lookahead.
            .onScrollGeometryChange(for: Bool.self) { geometry in
                geometry.contentOffset.y + geometry.containerSize.height > geometry.contentSize.height - 900
            } action: { _, isNearEnd in
                if isNearEnd, inbox.hiddenCount > 0 {
                    store.send(.endApproached)
                }
            }
        }
    }

    private func section(_ group: MessageBucketGroup, now: Date) -> some View {
        VStack(spacing: 0) {
            HStack(alignment: .lastTextBaseline, spacing: 8) {
                Text(group.bucket.label)
                    .font(.system(size: 20, weight: .bold))
                    .tracking(-0.6)
                    .foregroundStyle(UNESColor.ink)
                Text("\(group.totalCount)")
                    .font(.system(size: 14, weight: .medium))
                    .monospacedDigit()
                    .foregroundStyle(UNESColor.ink4)
                Spacer()
            }
            .padding(EdgeInsets(top: 0, leading: 4, bottom: 10, trailing: 4))

            VStack(spacing: 0) {
                ForEach(group.messages) { message in
                    MessageRow(
                        message: message,
                        relativeTime: MessagesFormat.relativeTime(for: message.receivedAt, now: now),
                        isLast: message.id == group.messages.last?.id
                    ) {
                        store.send(.messageTapped(message))
                    }
                }
            }
            .background(UNESColor.card)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .strokeBorder(UNESColor.cardLine)
            }
            .shadow(color: Color(hex: 0x141020, opacity: 0.05), radius: 9, y: 6)
        }
    }

    private struct MessageBucketGroup {
        var bucket: MessageBucket
        /// The rows actually rendered — the newest slice of the bucket once
        /// the reveal limit is spread across sections.
        var messages: [MessageItem]
        /// Everything the bucket holds under the active filter; the header
        /// count, so revealing more never changes the numbers.
        var totalCount: Int
    }

    private struct BucketedInbox {
        var groups: [MessageBucketGroup]
        var hiddenCount: Int
    }

    /// Groups the newest `limit` messages into date sections; the rest
    /// reveal as the scroll nears the end, so a backfilled mirror
    /// (~300 messages) doesn't build hundreds of rows on tab switch.
    private func bucketed(_ messages: [MessageItem], now: Date, limit: Int) -> BucketedInbox {
        let grouped = Dictionary(grouping: messages) {
            MessagesFormat.bucket(for: $0.receivedAt, now: now)
        }
        var groups: [MessageBucketGroup] = []
        var remaining = limit
        var hiddenCount = 0
        for bucket in MessageBucket.allCases {
            guard let all = grouped[bucket] else { continue }
            let visible = Array(all.prefix(remaining))
            remaining -= visible.count
            hiddenCount += all.count - visible.count
            if !visible.isEmpty {
                groups.append(MessageBucketGroup(bucket: bucket, messages: visible, totalCount: all.count))
            }
        }
        return BucketedInbox(groups: groups, hiddenCount: hiddenCount)
    }

    // MARK: States

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("Sem mensagens por aqui")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text("Os recados dos professores e da universidade aparecem aqui assim que chegarem.")
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    private func errorState(_ message: String) -> some View {
        VStack(spacing: 8) {
            Text("Não deu para carregar suas mensagens")
                .font(.system(size: 17, weight: .semibold))
                .tracking(-0.34)
                .foregroundStyle(UNESColor.ink)
            Text(message)
                .font(.system(size: 13))
                .foregroundStyle(UNESColor.ink3)
                .multilineTextAlignment(.center)
            Button("Tentar novamente") {
                store.send(.refreshPulled)
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(UNESColor.accent)
            .padding(.top, 8)
        }
        .padding(.horizontal, 32)
        .frame(maxHeight: .infinity)
    }

    /// Faint warm mesh washing down from behind the large title.
    private var ambientWash: some View {
        MeshView(variant: .warm, intensity: 0.5)
            .frame(height: 300)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.92),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.26)
            .offset(y: -80)
            .ignoresSafeArea()
    }
}

#Preview {
    MessagesView(
        store: Store(initialState: MessagesFeature.State()) {
            MessagesFeature()
        }
    )
}

#Preview("Vazio") {
    MessagesView(
        store: Store(initialState: MessagesFeature.State()) {
            MessagesFeature()
        } withDependencies: {
            $0.messagesRepository.observe = {
                AsyncStream { continuation in
                    continuation.yield(.empty)
                }
            }
        }
    )
}

#Preview("Caixa cheia") {
    MessagesView(
        store: Store(initialState: MessagesFeature.State()) {
            MessagesFeature()
        } withDependencies: {
            $0.messagesRepository.observe = {
                AsyncStream { continuation in
                    let base = MessagesOverview.preview()
                    var messages: [MessageItem] = []
                    for round in 0..<12 {
                        for var message in base.messages {
                            message.id += "-\(round)"
                            message.receivedAt.addTimeInterval(-Double(round) * 5 * 86_400)
                            messages.append(message)
                        }
                    }
                    continuation.yield(MessagesOverview(messages: messages, syncedAt: base.syncedAt))
                }
            }
        }
    )
}
