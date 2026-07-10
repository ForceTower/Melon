import ComposableArchitecture
import Foundation

@Reducer
struct MessageDetailFeature {
    @ObservableState
    struct State: Equatable {
        var message: MessageItem
    }

    enum Action: Equatable {
        case starTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case starredChanged(id: String, starred: Bool)
        }
    }

    @Dependency(\.messagesRepository) var messagesRepository

    private let log = Log.scoped("MessageDetailFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .starTapped:
                state.message.starred.toggle()
                let id = state.message.id
                let starred = state.message.starred
                log.info("toggle message star id=\(id) starred=\(starred)")
                return .merge(
                    .send(.delegate(.starredChanged(id: id, starred: starred))),
                    .run { [log] _ in
                        do {
                            try await messagesRepository.setStarred(id: id, starred: starred)
                        } catch {
                            log.warn("set message starred failed id=\(id)", error: error)
                        }
                    }
                )

            case .delegate:
                return .none
            }
        }
    }
}
