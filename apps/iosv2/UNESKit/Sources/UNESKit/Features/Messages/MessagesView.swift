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
            let buckets = bucketed(overview.messages.filter { store.filter.matches($0) }, now: now)

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

                    if buckets.isEmpty {
                        Text("Nenhuma mensagem neste filtro.")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(UNESColor.ink3)
                            .padding(EdgeInsets(top: 70, leading: 40, bottom: 70, trailing: 40))
                    }

                    ForEach(Array(buckets.enumerated()), id: \.element.bucket) { index, group in
                        section(group, now: now)
                            .fadeUp(delay: 0.22 + Double(index) * 0.06)
                            .padding(EdgeInsets(top: 0, leading: 16, bottom: 22, trailing: 16))
                    }

                    if let syncedAt = overview.syncedAt, !buckets.isEmpty {
                        Text(HomeFormat.updatedLabel(lastRefreshed: syncedAt, now: now))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(UNESColor.ink4)
                            .fadeUp(delay: 0.22 + Double(buckets.count) * 0.06)
                            .padding(.bottom, 8)
                    }
                }
                .padding(.bottom, 12)
            }
            .refreshable {
                await store.send(.refreshPulled).finish()
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
                Text("\(group.messages.count)")
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
        var messages: [MessageItem]
    }

    private func bucketed(_ messages: [MessageItem], now: Date) -> [MessageBucketGroup] {
        let grouped = Dictionary(grouping: messages) {
            MessagesFormat.bucket(for: $0.receivedAt, now: now)
        }
        return MessageBucket.allCases.compactMap { bucket in
            grouped[bucket].map { MessageBucketGroup(bucket: bucket, messages: $0) }
        }
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
            $0.messagesRepository.cached = { _ in .empty }
            $0.messagesRepository.refresh = { _ in .empty }
        }
    )
}
