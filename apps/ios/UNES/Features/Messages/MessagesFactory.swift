@preconcurrency import Umbrella

// Bundle of KMP use cases the Mensagens feature leans on. `observeInbox`
// feeds the list, `observeDetail` hydrates the reader, `markRead` persists a
// local readAt on first view, and `markAllRead` flips every unread row in a
// single transaction so the list flow re-emits once. Mirrors DisciplinesUseCases /
// OverviewUseCases so feature code never sees `UmbrellaGraph` directly.
struct MessagesUseCases {
    let observeInbox: MessagesObserveMessagesInboxUseCase
    let observeDetail: MessagesObserveMessageDetailUseCase
    let markRead: MessagesMarkMessageAsReadUseCase
    let markAllRead: MessagesMarkAllMessagesAsReadUseCase
}

@MainActor
struct MessagesFactory {
    let useCases: MessagesUseCases

    func makeListViewModel() -> MessagesListViewModel {
        MessagesListViewModel(useCases: useCases)
    }

    // Seeded with the tapped row so the detail screen renders instantly
    // against the list payload before the per-message flow hydrates.
    func makeDetailViewModel(seed: Message) -> MessageDetailViewModel {
        MessageDetailViewModel(seed: seed, useCases: useCases)
    }
}
