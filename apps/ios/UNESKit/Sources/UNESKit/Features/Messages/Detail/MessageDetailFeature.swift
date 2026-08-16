import ComposableArchitecture
import Foundation

@Reducer
struct MessageDetailFeature {
    @ObservableState
    struct State: Equatable {
        var message: MessageItem
        var isSharePresented = false
    }

    enum Action: Equatable {
        case task
        case starTapped
        case shareTapped
        case shareDismissed
        case delegate(Delegate)

        enum Delegate: Equatable {
            case starredChanged(id: String, starred: Bool)
        }
    }

    @Dependency(\.messagesRepository) var messagesRepository
    @Dependency(\.analytics) var analytics

    private let log = Log.scoped("MessageDetailFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.messageDetail, properties: ["message_id": state.message.id])
                return .none

            case .starTapped:
                state.message.starred.toggle()
                let id = state.message.id
                let starred = state.message.starred
                analytics.selectContent(
                    contentType: ContentTypes.message,
                    itemId: id,
                    properties: ["action": starred ? "star" : "unstar"]
                )
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

            case .shareTapped:
                analytics.selectContent(
                    contentType: ContentTypes.message,
                    itemId: state.message.id,
                    properties: ["action": "share"]
                )
                state.isSharePresented = true
                return .none

            case .shareDismissed:
                state.isSharePresented = false
                return .none

            case .delegate:
                return .none
            }
        }
    }
}
