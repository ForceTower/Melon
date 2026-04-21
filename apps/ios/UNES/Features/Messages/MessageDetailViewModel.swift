import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `MessageDetailView`. Seeded with the `Message` the list passed so
// the screen renders instantly with whatever fields were on the row card,
// then subscribes to `ObserveMessageDetailUseCase` to fill in attachments /
// full scopes / authoritative read state. Marks the message read locally
// on the first appearance — the DB flow will echo that back into the list.
@MainActor
@Observable
final class MessageDetailViewModel {
    private(set) var message: Message

    @ObservationIgnored private let useCases: MessagesUseCases?
    @ObservationIgnored private var didStart = false
    @ObservationIgnored private var didMarkRead = false

    init(seed: Message, useCases: MessagesUseCases?) {
        self.message = seed
        self.useCases = useCases
    }

    // Factory-less init for `#Preview`.
    convenience init(seed: Message) {
        self.init(seed: seed, useCases: nil)
    }

    func observe() async {
        guard !didStart else { return }
        didStart = true
        guard let useCases else { return }
        for await detail in useCases.observeDetail.invoke(messageId: message.id) {
            guard let detail else { continue }
            message = MessageMapping.map(detail)
        }
    }

    func markReadOnAppear() {
        guard !didMarkRead else { return }
        didMarkRead = true
        if message.unread { message.unread = false }
        guard let useCases else { return }
        Task { try? await useCases.markRead.invoke(messageId: message.id) }
    }
}
