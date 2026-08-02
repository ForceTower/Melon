import ComposableArchitecture
import Foundation

/// One catalogue work. Answers "can I get it now, and where is it on the
/// shelf" — borrowing itself still happens at the library counter, so the
/// screen is a briefing, not a checkout.
@Reducer
struct LibraryWorkDetailFeature {
    @ObservableState
    struct State: Equatable {
        var work: LibraryWork
        var reading: LibraryReading?
        var isRecordShown = true
        var toast: Toast?
        /// Anchor for "due in N days" math, refreshed with the reading.
        var now = Date.distantPast

        init(work: LibraryWork) {
            self.work = work
        }

        enum Toast: Equatable {
            case callNumberCopied
            case isbnCopied
            case referenceCopied
            case idCopied
        }
    }

    enum Action: Equatable {
        case task
        case readingLoaded(LibraryAvailabilitySnapshot)
        case refreshTapped
        case recordToggled
        case copyCallNumberTapped
        case copyISBNTapped
        case copyReferenceTapped
        case copyIdTapped
        case subjectTapped(String)
        case authorTapped(String)
        case toastExpired
        case delegate(Delegate)

        enum Delegate: Equatable {
            case search(query: String, scope: LibrarySearchScope)
        }
    }

    @Dependency(\.libraryRepository) var libraryRepository
    @Dependency(\.pasteboard) var pasteboard
    @Dependency(\.continuousClock) var clock
    @Dependency(\.analytics) var analytics
    @Dependency(\.date) var date

    private let log = Log.scoped("LibraryWorkDetailFeature")

    private enum CancelID { case reading, toast }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.libraryWork, properties: ["work_id": state.work.id])
                state.now = date.now
                guard state.reading == nil else { return .none }
                return checkReading(state)

            case let .readingLoaded(snapshot):
                state.reading = snapshot.reading
                // A live reading carries the copies — replace the last-known
                // set so every count on screen reflects what was just read.
                if snapshot.reading != .unavailable, !snapshot.copies.isEmpty {
                    state.work.copies = snapshot.copies
                }
                return .none

            case .refreshTapped:
                log.info("refresh availability id=\(state.work.id)")
                state.now = date.now
                state.reading = nil
                return checkReading(state)

            case .recordToggled:
                state.isRecordShown.toggle()
                return .none

            case .copyCallNumberTapped:
                return copy(state.work.callNumber, toast: .callNumberCopied, state: &state)

            case .copyISBNTapped:
                guard let isbn = state.work.isbn, let value = isbn.pretty ?? isbn.value else { return .none }
                return copy(value, toast: .isbnCopied, state: &state)

            case .copyReferenceTapped:
                guard let reference = state.work.reference else { return .none }
                // Strip the markdown emphasis — the pasteboard gets prose.
                return copy(
                    reference.replacingOccurrences(of: "**", with: ""),
                    toast: .referenceCopied,
                    state: &state
                )

            case .copyIdTapped:
                return copy(state.work.id, toast: .idCopied, state: &state)

            case let .subjectTapped(subject):
                log.info("subject search from id=\(state.work.id)")
                return .send(.delegate(.search(query: subject, scope: .subject)))

            case let .authorTapped(author):
                log.info("author search from id=\(state.work.id)")
                return .send(.delegate(.search(query: author, scope: .author)))

            case .toastExpired:
                state.toast = nil
                return .none

            case .delegate:
                return .none
            }
        }
    }

    private func checkReading(_ state: State) -> Effect<Action> {
        .run { [id = state.work.id] send in
            await send(.readingLoaded(await libraryRepository.checkAvailability(id)))
        }
        .cancellable(id: CancelID.reading, cancelInFlight: true)
    }

    private func copy(_ text: String, toast: State.Toast, state: inout State) -> Effect<Action> {
        state.toast = toast
        return .run { send in
            await pasteboard.copy(text)
            try await clock.sleep(for: .milliseconds(2200))
            await send(.toastExpired)
        }
        .cancellable(id: CancelID.toast, cancelInFlight: true)
    }
}
