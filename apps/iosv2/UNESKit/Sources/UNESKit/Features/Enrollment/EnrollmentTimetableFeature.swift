import ComposableArchitecture
import Foundation

/// The weekly grid preview of the current picks: conflict summary on top,
/// Monday–Saturday columns, and a legend of the picked sections.
@Reducer
struct EnrollmentTimetableFeature {
    @ObservableState
    struct State: Equatable {
        @Shared(.enrollmentSession) var session

        init(session: EnrollmentSession? = nil) {
            if let session {
                $session.withLock { $0 = session }
            }
        }

        var scheduledPicks: [EnrollmentResolvedPick] {
            session.resolvedPicks.filter(\.section.hasSchedule)
        }

        var pendingScheduleCount: Int {
            session.resolvedPicks.count { !$0.section.hasSchedule }
        }
    }

    enum Action: Equatable {
        case reviewTapped
        case delegate(Delegate)

        enum Delegate: Equatable {
            case openReview
        }
    }

    var body: some ReducerOf<Self> {
        Reduce { _, action in
            switch action {
            case .reviewTapped:
                return .send(.delegate(.openReview))
            case .delegate:
                return .none
            }
        }
    }
}

// MARK: - Grid layout

/// One rendered block of the weekly grid, already assigned to a lane so
/// same-day overlaps sit side by side.
struct EnrollmentTimetableBlock: Equatable, Identifiable {
    let id: String
    var day: Int
    var startMinute: Int
    var endMinute: Int
    var code: String
    var colorIndex: Int
    var conflicting = false
    var lane = 0
}

/// A day column's blocks plus how many lanes they need.
struct EnrollmentTimetableColumn: Equatable {
    var lanes = 1
    var blocks: [EnrollmentTimetableBlock] = []
}

enum EnrollmentTimetableLayout {
    /// The visible axis: 07:00–23:00, Monday through Saturday.
    static let startMinute = 7 * 60
    static let endMinute = 23 * 60
    static let days = Array(1...6)

    static func columns(for picks: [EnrollmentResolvedPick]) -> [Int: EnrollmentTimetableColumn] {
        var blocksByDay: [Int: [EnrollmentTimetableBlock]] = [:]
        for pick in picks {
            for slot in pick.section.slots where days.contains(slot.day) {
                let block = EnrollmentTimetableBlock(
                    // End minute included: a section's theory and practice can
                    // share a start slot and must stay distinct rows.
                    id: "\(pick.discipline.id)-\(pick.section.id)-\(slot.day)-\(slot.startMinute)-\(slot.endMinute)",
                    day: slot.day,
                    startMinute: slot.startMinute,
                    endMinute: slot.endMinute,
                    code: pick.discipline.code,
                    colorIndex: pick.discipline.colorIndex
                )
                blocksByDay[slot.day, default: []].append(block)
            }
        }
        return blocksByDay.mapValues(layoutDay)
    }

    /// Greedy first-free-lane packing, plus conflict marks between blocks of
    /// different disciplines.
    private static func layoutDay(_ blocks: [EnrollmentTimetableBlock]) -> EnrollmentTimetableColumn {
        var sorted = blocks.sorted { $0.startMinute < $1.startMinute }
        var laneEnds: [Int] = []
        for index in sorted.indices {
            let block = sorted[index]
            if let lane = laneEnds.firstIndex(where: { $0 <= block.startMinute }) {
                sorted[index].lane = lane
                laneEnds[lane] = block.endMinute
            } else {
                sorted[index].lane = laneEnds.count
                laneEnds.append(block.endMinute)
            }
        }
        for i in sorted.indices {
            for j in sorted.indices.dropFirst(i + 1) {
                guard sorted[i].code != sorted[j].code,
                      sorted[i].startMinute < sorted[j].endMinute,
                      sorted[j].startMinute < sorted[i].endMinute
                else { continue }
                sorted[i].conflicting = true
                sorted[j].conflicting = true
            }
        }
        return EnrollmentTimetableColumn(lanes: max(1, laneEnds.count), blocks: sorted)
    }
}
