import ComposableArchitecture
import Foundation

/// Filter segments of the offers catalogue.
enum EnrollmentOfferFilter: String, Equatable, Sendable, CaseIterable, Identifiable {
    case all, mandatory, optional

    var id: String { rawValue }

    var label: String {
        switch self {
        case .all: "Todas"
        case .mandatory: "Obrigatórias"
        case .optional: "Optativas"
        }
    }
}

/// The offers browser: search + segment filter over the shared catalogue,
/// grouped by curriculum period.
@Reducer
struct EnrollmentOffersFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session
        var query = ""
        var filter: EnrollmentOfferFilter = .all

        init(session: EnrollmentSession? = nil) {
            if let session {
                $session.withLock { $0 = session }
            }
        }

        var groups: [EnrollmentPeriodGroup] {
            matchingDisciplines.groupedByPeriod
        }

        private var matchingDisciplines: [EnrollmentDiscipline] {
            session.disciplines.filter { discipline in
                switch filter {
                case .all: break
                case .mandatory: guard discipline.mandatory else { return false }
                case .optional: guard !discipline.mandatory else { return false }
                }
                let query = query.trimmingCharacters(in: .whitespaces)
                guard !query.isEmpty else { return true }
                return discipline.code.localizedStandardContains(query)
                    || discipline.name.localizedStandardContains(query)
            }
        }
    }

    enum Action: Equatable {
        case queryChanged(String)
        case filterChanged(EnrollmentOfferFilter)
        case disciplineTapped(Int64)
        case timetableTapped
        case reviewTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openDiscipline(Int64)
            case openTimetable
            case openReview
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case let .queryChanged(query):
                state.query = query
                return .none

            case let .filterChanged(filter):
                state.filter = filter
                return .none

            case let .disciplineTapped(id):
                return .send(.delegate(.openDiscipline(id)))

            case .timetableTapped:
                return .send(.delegate(.openTimetable))

            case .reviewTapped:
                return .send(.delegate(.openReview))

            case .delegate:
                return .none
            }
        }
    }
}
