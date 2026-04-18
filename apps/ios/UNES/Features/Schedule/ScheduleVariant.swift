import Foundation

/// User-selectable schedule layout. Persisted via `@AppStorage` under
/// `ScheduleVariant.storageKey`; a Settings screen will expose the picker
/// later.
enum ScheduleVariant: String, CaseIterable {
    case grid
    case focused

    static let storageKey = "schedule_variant"
    static let `default`: ScheduleVariant = .focused

    var label: String {
        switch self {
        case .grid:    return "Semana em grade"
        case .focused: return "Dia em foco"
        }
    }
}
