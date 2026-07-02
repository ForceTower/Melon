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

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .sectionTapped(sectionId):
                guard let discipline = state.discipline,
                      let section = discipline.section(sectionId)
                else { return .none }

                if state.pick?.sectionId == sectionId {
                    state.$session.withLock { $0.remove(disciplineId: discipline.id) }
                    return .none
                }
                // The card disables these paths; the guards keep the rules
                // honest regardless of what the UI lets through.
                guard state.session.clash(with: section, excluding: discipline.id) == nil else { return .none }
                let useQueue = state.session.window?.useQueue ?? false
                guard !section.seats.isFull || useQueue else { return .none }

                state.$session.withLock { $0.select(discipline, section: section) }
                return .none

            case let .allowsOtherChanged(value):
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
