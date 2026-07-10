import ComposableArchitecture
import Foundation

/// The grade-calculator sandbox: rows seed from a real discipline's
/// evaluations, then the student edits freely — nothing writes back.
@Reducer
struct FinalCountdownFeature {
    @ObservableState
    struct State: Equatable {
        var discipline: FCDiscipline?
        var rows: IdentifiedArrayOf<FCRow> = []
        var weighted = false
        /// Current-semester disciplines feeding the "Trocar" picker.
        var choices: [DisciplineSummary] = []
        var currentSemesterCode: String?
        var isPickerPresented = false

        var verdict: FCVerdict {
            FinalCountdownMath.verdict(for: Array(rows), weighted: weighted)
        }

        /// Me-tab entry — modo livre by default; the overview only feeds the
        /// "Trocar" picker, where a discipline is one tap away.
        init() {
            rows = Self.fallbackRows
        }

        /// Discipline-detail entry, honoring the detail's group filter.
        init(detail: DisciplineDetail, selectedGroup: String?, semesterCode: String?) {
            discipline = FCDiscipline(
                id: detail.id,
                name: detail.name,
                teacherName: detail.teacherName ?? detail.groups.first?.teacherName,
                colorIndex: detail.colorIndex,
                semesterCode: semesterCode
            )
            rows = Self.seededRows(from: detail.grades(forGroup: selectedGroup))
        }

        static func seededRows(from grades: [DisciplineDetailGrade]) -> IdentifiedArrayOf<FCRow> {
            guard !grades.isEmpty else { return fallbackRows }
            return IdentifiedArray(uniqueElements: grades.map { grade in
                FCRow(
                    id: grade.id,
                    label: String(grade.label.prefix(6)),
                    scoreText: FCRow.text(for: grade.value),
                    weight: clampedWeight(grade.weight)
                )
            })
        }

        static func seededRows(from grades: [DisciplineGrade]) -> IdentifiedArrayOf<FCRow> {
            guard !grades.isEmpty else { return fallbackRows }
            return IdentifiedArray(uniqueElements: grades.map { grade in
                FCRow(
                    id: grade.id,
                    label: String(grade.label.prefix(6)),
                    scoreText: FCRow.text(for: grade.value)
                )
            })
        }

        /// The blank trio shown when there's no discipline to seed from.
        static let fallbackRows: IdentifiedArrayOf<FCRow> = [
            FCRow(id: "fallback-1", label: "AV1"),
            FCRow(id: "fallback-2", label: "AV2"),
            FCRow(id: "fallback-3", label: "Trab"),
        ]

        private static func clampedWeight(_ weight: Double?) -> Int {
            guard let weight else { return 1 }
            return min(max(Int(weight.rounded()), 1), 9)
        }
    }

    enum Action: Equatable {
        case task
        case overviewLoaded(DisciplinesOverview?)
        case scoreEdited(id: FCRow.ID, text: String)
        case labelEdited(id: FCRow.ID, text: String)
        case weightStepped(id: FCRow.ID, delta: Int)
        case rowRemoved(id: FCRow.ID)
        case addRowTapped
        case clearTapped
        case weightedToggled(Bool)
        case changeTapped
        case pickerDismissed
        /// Nil is "modo livre" — hypotheticals with no discipline attached.
        case disciplinePicked(String?)
    }

    @Dependency(\.disciplinesRepository) var disciplinesRepository
    @Dependency(\.date.now) var now
    @Dependency(\.uuid) var uuid

    private let log = Log.scoped("FinalCountdownFeature")

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return .run { [log] send in
                    var overview: DisciplinesOverview?
                    do {
                        overview = try await disciplinesRepository.cached(now: now)
                    } catch {
                        log.warn("final countdown overview load failed", error: error)
                    }
                    await send(.overviewLoaded(overview))
                }

            case let .overviewLoaded(overview):
                state.choices = overview?.current?.disciplines ?? []
                state.currentSemesterCode = overview?.current?.code
                return .none

            case let .scoreEdited(id, text):
                state.rows[id: id]?.scoreText = FCRow.sanitizeScoreText(text)
                return .none

            case let .labelEdited(id, text):
                state.rows[id: id]?.label = String(text.prefix(6))
                return .none

            case let .weightStepped(id, delta):
                guard let row = state.rows[id: id] else { return .none }
                state.rows[id: id]?.weight = min(max(row.weight + delta, 1), 9)
                return .none

            case let .rowRemoved(id):
                guard state.rows.count > 1 else { return .none }
                state.rows.remove(id: id)
                return .none

            case .addRowTapped:
                state.rows.append(FCRow(
                    id: uuid().uuidString,
                    label: "AV\(state.rows.count + 1)"
                ))
                return .none

            case .clearTapped:
                for id in state.rows.ids {
                    state.rows[id: id]?.scoreText = ""
                }
                return .none

            case let .weightedToggled(isOn):
                state.weighted = isOn
                return .none

            case .changeTapped:
                state.isPickerPresented = true
                return .none

            case .pickerDismissed:
                state.isPickerPresented = false
                return .none

            case let .disciplinePicked(id):
                state.isPickerPresented = false
                guard let id else {
                    state.discipline = nil
                    state.rows = State.fallbackRows
                    return .none
                }
                guard let summary = state.choices.first(where: { $0.id == id }) else { return .none }
                state.seed(from: summary)
                return .none
            }
        }
    }
}

extension FinalCountdownFeature.State {
    fileprivate mutating func seed(from summary: DisciplineSummary) {
        discipline = FCDiscipline(
            id: summary.id,
            name: summary.name,
            teacherName: summary.teacherName,
            colorIndex: summary.colorIndex,
            semesterCode: currentSemesterCode
        )
        rows = Self.seededRows(from: summary.grades)
    }
}
