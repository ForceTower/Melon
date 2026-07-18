import ComposableArchitecture
import Foundation

/// One discipline's section picker: choose a turma, queue when it's full,
/// and tune the "aceitar outra turma" preference.
@Reducer
struct EnrollmentDisciplineFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session
        let disciplineId: Int64

        init(disciplineId: Int64, session: EnrollmentSession? = nil) {
            self.disciplineId = disciplineId
            if let session {
                $session.withLock { $0 = session }
            }
        }

        var discipline: EnrollmentDiscipline? {
            session.discipline(disciplineId)
        }

        var pick: EnrollmentPick? {
            session.pick(for: disciplineId)
        }
    }

    enum Action: Equatable {
        case task
        /// Selects the section — or removes it when it's the current pick.
        case sectionTapped(Int64)
        case allowsOtherChanged(Bool)
        case timetableTapped
        case doneTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openTimetable
        }
    }

    @Dependency(\.dismiss) var dismiss
    @Dependency(\.analytics) var analytics

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                analytics.screen(name: Screens.enrollmentDiscipline, properties: ["offer_id": state.disciplineId])
                return .none

            case let .sectionTapped(sectionId):
                // Comprovante mode — the cards are informational only until
                // the student explicitly reopens the enrollment.
                guard state.session.canEdit else { return .none }
                guard let discipline = state.discipline,
                      let section = discipline.section(sectionId)
                else { return .none }

                if state.pick?.sectionId == sectionId {
                    analytics.selectContent(
                        contentType: ContentTypes.offer,
                        itemId: sectionId.description,
                        properties: ["action": "remove"]
                    )
                    state.$session.withLock { $0.remove(disciplineId: discipline.id) }
                    return .none
                }
                // The card disables these paths; the guards keep the rules
                // honest regardless of what the UI lets through.
                guard state.session.clash(with: section, excluding: discipline.id) == nil else { return .none }
                let useQueue = state.session.window?.useQueue ?? false
                guard !section.seats.isFull || useQueue else { return .none }

                analytics.selectContent(
                    contentType: ContentTypes.offer,
                    itemId: sectionId.description,
                    properties: ["action": "select"]
                )
                state.$session.withLock { $0.select(discipline, section: section) }
                return .none

            case let .allowsOtherChanged(value):
                guard state.session.canEdit else { return .none }
                state.$session.withLock { $0.setAllowsOther(value, disciplineId: state.disciplineId) }
                return .none

            case .timetableTapped:
                return .send(.delegate(.openTimetable))

            case .doneTapped:
                return .run { _ in await dismiss() }

            case .delegate:
                return .none
            }
        }
    }
}
