import ComposableArchitecture
import Foundation

private let log = Log.scoped("RetrospectiveFeature")

/// The story beats, in the design's arc plus the turma percentile beat. Data-dependent cards only exist when their numbers do.
enum RetroCard: Equatable, Sendable {
    case abertura
    case notas
    case frequencia
    case conquista
    case score
    case turma
    case encerramento

    static func cards(for deck: RetrospectiveDeck?) -> [RetroCard] {
        guard let deck else { return [.abertura] }
        var cards: [RetroCard] = [.abertura]
        if deck.grades != nil { cards.append(.notas) }
        if deck.attendance != nil { cards.append(.frequencia) }
        if deck.victory != nil { cards.append(.conquista) }
        if deck.score != nil { cards.append(.score) }
        if deck.turma != nil { cards.append(.turma) }
        cards.append(.encerramento)
        return cards
    }
}

@Reducer
struct RetrospectiveFeature {
    /// Auto-advance dwell per card.
    static let cardDuration: TimeInterval = 7

    /// The once-per-semester announce reveal's seen marker.
    static let seenSemesterKey = "retrospective_seen_semester"

    @ObservableState
    struct State: Equatable {
        var semesterCode: String
        var deck: RetrospectiveDeck?
        /// Signs the share render's footer.
        var firstName = ""
        var cards: [RetroCard] = [.abertura]
        var index = 0
        var isShowingAnnounce: Bool
        var isPaused = false
        var isSharePresented = false
        /// Progress bookkeeping for the segmented bars: when the current
        /// card's dwell started, and how much had elapsed before a pause.
        var cardStartedAt: Date?
        var elapsedBeforePause: TimeInterval = 0
        /// Press latch for the tap/hold gesture.
        var pressBeganAt: Date?
        /// No auto-advance under Reduce Motion, like the design.
        var reduceMotion = false
        @Shared(.appStorage(RetrospectiveFeature.seenSemesterKey)) var seenSemester = ""

        var currentCard: RetroCard { cards[min(index, cards.count - 1)] }

        init(semesterCode: String) {
            self.semesterCode = semesterCode
            @Shared(.appStorage(RetrospectiveFeature.seenSemesterKey)) var seen = ""
            isShowingAnnounce = seen != semesterCode
        }
    }

    enum Action: Equatable {
        case task
        case reduceMotionChanged(Bool)
        case deckLoaded(RetrospectiveDeck?)
        case percentilesLoaded([RetrospectivePercentile])
        case announceEnterTapped
        case pressBegan
        case pressEnded(fraction: Double, moved: Bool)
        case autoAdvanced
        case shareTapped
        case shareDismissed
        case closeTapped
    }

    @Dependency(\.database) var database
    @Dependency(\.sessionStore) var sessionStore
    @Dependency(\.retrospectiveRepository) var retrospectiveRepository
    @Dependency(\.date) var date
    @Dependency(\.continuousClock) var clock
    @Dependency(\.dismiss) var dismiss
    @Dependency(\.analytics) var analytics

    private enum CancelID { case autoAdvance }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(Screens.retrospective)
                state.firstName = sessionStore.current()?.user.name
                    .split(separator: " ").first.map(String.init) ?? ""
                let code = state.semesterCode
                return .run { send in
                    let mirror = MirrorStore(writer: database)
                    let deck = try? await mirror.retrospective(semesterCode: code, now: date.now)
                    await send(.deckLoaded(deck))
                    guard deck != nil else { return }
                    // Best-effort: offline just means no turma beat.
                    guard let percentiles = try? await retrospectiveRepository.percentiles(code),
                          !percentiles.isEmpty
                    else { return }
                    await send(.percentilesLoaded(percentiles))
                }

            case let .reduceMotionChanged(reduceMotion):
                state.reduceMotion = reduceMotion
                return .none

            case let .deckLoaded(deck):
                log.info("retrospective loaded semester=\(state.semesterCode) hasDeck=\(deck != nil)")
                state.deck = deck
                state.cards = RetroCard.cards(for: deck)
                guard !state.isShowingAnnounce else { return .none }
                return restartDwell(&state)

            case let .percentilesLoaded(percentiles):
                guard let deck = state.deck else { return .none }
                // Re-anchoring by card identity keeps the reader on the same
                // beat when the turma card slots in mid-story.
                let current = state.currentCard
                state.deck = deck.merging(percentiles)
                state.cards = RetroCard.cards(for: state.deck)
                state.index = state.cards.firstIndex(of: current) ?? min(state.index, state.cards.count - 1)
                return .none

            case .announceEnterTapped:
                state.$seenSemester.withLock { [code = state.semesterCode] in $0 = code }
                state.isShowingAnnounce = false
                return restartDwell(&state)

            case .pressBegan:
                guard !state.isSharePresented else { return .none }
                state.pressBeganAt = date.now
                pause(&state)
                return .cancel(id: CancelID.autoAdvance)

            case let .pressEnded(fraction, moved):
                let held = state.pressBeganAt.map { date.now.timeIntervalSince($0) } ?? 0
                state.pressBeganAt = nil
                // Short still press navigates (left third rewinds); anything
                // longer was a hold — just resume.
                if held < 0.26, !moved {
                    if fraction < 0.32 {
                        state.index = max(0, state.index - 1)
                    } else if state.index < state.cards.count - 1 {
                        state.index += 1
                    }
                    return restartDwell(&state)
                }
                return resumeDwell(&state)

            case .autoAdvanced:
                guard state.index < state.cards.count - 1 else {
                    // The story parks on the closing card, like the design.
                    pause(&state)
                    return .none
                }
                state.index += 1
                return restartDwell(&state)

            case .shareTapped:
                analytics.selectContent(
                    contentType: ContentTypes.cta,
                    itemId: "retrospective_share",
                    properties: ["semester": state.semesterCode, "card": "\(state.currentCard)"]
                )
                state.isSharePresented = true
                pause(&state)
                return .cancel(id: CancelID.autoAdvance)

            case .shareDismissed:
                state.isSharePresented = false
                return resumeDwell(&state)

            case .closeTapped:
                log.info("retrospective closed card=\(state.index)/\(state.cards.count)")
                return .run { _ in await dismiss() }
            }
        }
    }

    /// Fresh dwell for the current card: progress restarts and the
    /// auto-advance rearms (unless Reduce Motion turned it off).
    private func restartDwell(_ state: inout State) -> Effect<Action> {
        state.elapsedBeforePause = 0
        state.cardStartedAt = date.now
        state.isPaused = false
        return scheduleAdvance(after: Self.cardDuration, reduceMotion: state.reduceMotion)
    }

    private func pause(_ state: inout State) {
        guard !state.isPaused else { return }
        if let startedAt = state.cardStartedAt {
            state.elapsedBeforePause += date.now.timeIntervalSince(startedAt)
        }
        state.cardStartedAt = nil
        state.isPaused = true
    }

    private func resumeDwell(_ state: inout State) -> Effect<Action> {
        state.cardStartedAt = date.now
        state.isPaused = false
        let remaining = max(0.05, Self.cardDuration - state.elapsedBeforePause)
        return scheduleAdvance(after: remaining, reduceMotion: state.reduceMotion)
    }

    private func scheduleAdvance(after seconds: TimeInterval, reduceMotion: Bool) -> Effect<Action> {
        guard !reduceMotion else { return .cancel(id: CancelID.autoAdvance) }
        return .run { send in
            try await clock.sleep(for: .seconds(seconds))
            await send(.autoAdvanced)
        }
        .cancellable(id: CancelID.autoAdvance, cancelInFlight: true)
    }
}
