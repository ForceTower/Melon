import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `MessagesListView`. Subscribes to `ObserveMessagesInboxUseCase` and
// re-projects each emission through `MessageMapping` so the existing list
// UI keeps working against the presentation `Message` struct. Fixture mode
// (useCases == nil) is retained for `#Preview` and feeds MessageFixtures so
// the canvas keeps rendering offline.
@MainActor
@Observable
final class MessagesListViewModel {
    var filter: MessageFilter = .all
    private(set) var messages: [Message]

    @ObservationIgnored private let useCases: MessagesUseCases?
    @ObservationIgnored private var didStart = false

    init(useCases: MessagesUseCases?) {
        self.useCases = useCases
        // Seed previews/fixture mode with local data. With a real factory we
        // start empty and let the DB flow emit the first snapshot.
        self.messages = useCases == nil ? MessageFixtures.messages : []
    }

    // Factory-less init for `#Preview`.
    convenience init() {
        self.init(useCases: nil)
    }

    func observe() async {
        guard !didStart else { return }
        didStart = true
        guard let useCases else { return }
        for await items in useCases.observeInbox.invoke() {
            messages = items.map(MessageMapping.map)
        }
    }

    // Optimistic local flip so the list row stops looking unread immediately;
    // the DB flow will emit the real state a moment later. Without a factory
    // (preview mode) we just keep the local flip — no side effects.
    func markRead(_ message: Message) {
        guard message.unread, let idx = messages.firstIndex(where: { $0.id == message.id }) else {
            return
        }
        messages[idx].unread = false
        guard let useCases else { return }
        Task { try? await useCases.markRead.invoke(messageId: message.id) }
    }
}
