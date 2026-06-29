import SwiftUI

/// Student identity displayed on the Me screen. Shaped after the `ME` record
/// in `screens-me.jsx` — everything rendered in the hero, the semester strip,
/// and the footer is sourced from a single value of this type.
struct ProfileIdentity {
    let name: String
    let firstName: String
    let course: String
    let campus: String
    let enrollment: String
    let username: String
    let avatarInitial: String
    let semester: String
    let semesterWeek: Int
    let semesterTotalWeeks: Int
    let progressPct: Int
    let cr: Double
    let crDelta: String
    let creditsDone: Int
    let creditsRequired: Int
    let semesterStart: String
    let semesterEnd: String
    let finalExam: String
}

/// Colored accent applied to a shortcut tile's icon badge. Mirrors the `TONES`
/// palette in `screens-me.jsx`.
enum ShortcutTone {
    case plum, magenta, teal, coral, amber

    var background: Color {
        switch self {
        case .plum:    return UNESColor.plum
        case .magenta: return UNESColor.magenta
        case .teal:    return MeColors.teal
        case .coral:   return UNESColor.coral
        case .amber:   return UNESColor.amber
        }
    }

    var foreground: Color {
        switch self {
        case .plum:    return UNESColor.peach
        case .amber:   return UNESColor.plum
        default:       return UNESColor.surfaceLight
        }
    }
}

/// One entry in the shortcut constellation. The same library is shared across
/// all shortcut sets — pinning swaps which entries appear, not their content.
struct Shortcut: Identifiable {
    enum Kind: String, CaseIterable {
        case account, zhonya, flowchart, bandejao, calendar,
             countdown, request, theme, reminders, adventure, enrollment
    }

    let id: Kind
    let label: String
    let hint: String
    let systemImage: String
    let tone: ShortcutTone
}

/// Named preset of pinned shortcut IDs. Swapped via the design's Tweak panel;
/// on iOS the selection will eventually move to Settings.
struct MeShortcutSet: Identifiable, Hashable {
    let id: String
    let label: String
    let pinned: [Shortcut.Kind]
}

/// Row in the quieter services/settings list below the shortcut grid.
struct MeSettingsRow: Identifiable {
    enum Kind: String { case settings, sync, about, feedback, licenses }

    let id: Kind
    let label: String
    let hint: String
    let systemImage: String
    var statusOK: Bool = false
}

/// Ad-hoc color palette for tones that aren't available as tokens on
/// `UNESColor`. Kept local to the Me feature so it doesn't leak into the
/// shared design system — teal and the "ok" green show up elsewhere (see
/// `OverviewFixtures`) but each feature owns its own literal.
enum MeColors {
    static let teal      = Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255)
    static let okGreen   = Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255)
    static let successFg = Color(red: 0x7F / 255, green: 0xD4 / 255, blue: 0xA2 / 255)
    static let signOut   = Color(red: 0xC9 / 255, green: 0x45 / 255, blue: 0x38 / 255)
    static let heroBg    = Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
}
