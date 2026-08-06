import Foundation

/// Which Horário the schedule tab opens: the week grid (`ScheduleGridView`)
/// or the single-day list (`ScheduleView`). Device-local, like the theme;
/// the grid is the default for everyone.
enum ScheduleViewMode: String, CaseIterable, Equatable, Sendable {
    case grid, list
}
