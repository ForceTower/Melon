import SwiftUI

/// What the grade notification preview actually reveals on the lock screen.
/// Mirrors `SPOILER` in `screens-settings.jsx`.
enum SpoilerMode: String, CaseIterable, Hashable, Identifiable {
    case value, comment, posted

    var id: String { rawValue }

    var label: String {
        switch self {
        case .value:   return "Valor"
        case .comment: return "Comentário"
        case .posted:  return "Apenas aviso"
        }
    }

    var hint: String {
        switch self {
        case .value:   return "Cálc. II · 8,5"
        case .comment: return "\"Uma boa nota em Calc. II\""
        case .posted:  return "\"Nova nota em Cálc. II\""
        }
    }

    /// The full body text the lock-screen preview renders.
    var previewText: String {
        switch self {
        case .value:   return "Você tirou 8,5 em Cálc. II"
        case .comment: return "Nova nota em Cálc. II. Muito bom!"
        case .posted:  return "Nova nota em Cálc. II"
        }
    }
}

/// Accent palette used by Settings icon tiles. Matches `CFG_TONES` in
/// `screens-settings.jsx`; duplicated here rather than imported from the Me
/// feature so Settings stays self-contained.
enum SettingsTone {
    case plum, magenta, teal, coral, amber

    var background: Color {
        switch self {
        case .plum:    return UNESColor.plum
        case .magenta: return UNESColor.magenta
        case .teal:    return SettingsColors.teal
        case .coral:   return UNESColor.coral
        case .amber:   return UNESColor.amber
        }
    }

    var foreground: Color {
        switch self {
        case .plum:  return UNESColor.peach
        case .amber: return UNESColor.plum
        default:     return UNESColor.surfaceLight
        }
    }
}

enum SettingsColors {
    static let teal = Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255)
}

/// Flat state bag for the Settings screen. One-to-one with `SETTINGS_DEFAULTS`
/// in `screens-settings.jsx`. Held as `@State` by `SettingsView` — persistence
/// will land once the keys move to the shared prefs store.
struct SettingsState {
    var spoiler: SpoilerMode = .comment
    var credentialsRevealed: Bool = false

    // messages
    var notifMsgBroadcast: Bool = true
    var notifMsgClass: Bool = true
    var notifMsgDirect: Bool = true

    // grades
    var notifGradePosted: Bool = true
    var notifGradeChanged: Bool = true
    var notifGradeDateChanged: Bool = false

    // classes
    var notifClassLocation: Bool = true
    var notifClassMaterial: Bool = true
    var notifClassSubject: Bool = false

    var messageKeyPaths: [WritableKeyPath<SettingsState, Bool>] {
        [\.notifMsgBroadcast, \.notifMsgClass, \.notifMsgDirect]
    }

    var gradeKeyPaths: [WritableKeyPath<SettingsState, Bool>] {
        [\.notifGradePosted, \.notifGradeChanged, \.notifGradeDateChanged]
    }

    var classKeyPaths: [WritableKeyPath<SettingsState, Bool>] {
        [\.notifClassLocation, \.notifClassMaterial, \.notifClassSubject]
    }

    var allNotificationKeyPaths: [WritableKeyPath<SettingsState, Bool>] {
        messageKeyPaths + gradeKeyPaths + classKeyPaths
    }
}

/// Credentials rendered by the vault card. Real values come from the secure
/// store once that's wired; today the fixture keeps the screen populated
/// during the UI build-out.
struct SettingsCredentials {
    let username: String
    let password: String
}

enum SettingsFixtures {
    #if DEBUG
        static let credentials = SettingsCredentials(
            username: "118.104.072",
            password: "ma·nogue!ra·2024"
        )
    #endif

    static let lastSyncLabel = "há 2 min"
}
