import Foundation

/// What a grade push reveals on the lock screen. The wire carries the raw
/// value (`gradeSpoiler`); higher values reveal less.
enum GradeSpoiler: Int, Equatable, Sendable, CaseIterable {
    /// "Cálculo Diferencial B2 · 8,5" — the grade itself.
    case value = 0
    /// "nova nota em Cálc. Diferencial B2" — what happened, not the number.
    case summary = 1
    /// "uma nota foi publicada" — not even the discipline.
    case discreet = 2
}

/// One push-notification switch on the Settings screen.
enum NotificationToggle: Equatable, Hashable, Sendable, CaseIterable {
    case messageBroadcast, messageClass, messageDirect
    case gradePosted, gradeChanged, gradeDateChanged
    case classLocation, classMaterial, classSubject

    var keyPath: WritableKeyPath<UserSettings, Bool> {
        switch self {
        case .messageBroadcast: \.messageBroadcast
        case .messageClass: \.messageClass
        case .messageDirect: \.messageDirect
        case .gradePosted: \.gradePosted
        case .gradeChanged: \.gradeChanged
        case .gradeDateChanged: \.gradeDateChanged
        case .classLocation: \.classLocation
        case .classMaterial: \.classMaterial
        case .classSubject: \.classSubject
        }
    }
}

/// Per-user notification + spoiler preferences, owned by the server. The
/// defaults mirror the server's row defaults so the screen renders sensibly
/// until the fetch lands.
struct UserSettings: Equatable, Sendable {
    var gradeSpoiler: GradeSpoiler = .summary
    var messageBroadcast = true
    var messageClass = true
    var messageDirect = true
    var gradePosted = true
    var gradeChanged = true
    var gradeDateChanged = false
    var classLocation = true
    var classMaterial = true
    var classSubject = false
    /// Secret icon discoveries, account-wide — the server merges, never removes.
    var unlockedIcons: Set<AppIcon> = []

    var activeNotificationCount: Int {
        NotificationToggle.allCases.count { self[keyPath: $0.keyPath] }
    }

    mutating func apply(_ change: SettingsChange) {
        switch change {
        case let .gradeSpoiler(spoiler):
            gradeSpoiler = spoiler
        case let .notification(toggle, isOn):
            self[keyPath: toggle.keyPath] = isOn
        case let .unlockedIcons(icons):
            unlockedIcons.formUnion(icons)
        }
    }
}

/// A single-field settings mutation — `PATCH api/me/settings` ships exactly
/// the field that changed.
enum SettingsChange: Equatable, Sendable {
    case gradeSpoiler(GradeSpoiler)
    case notification(NotificationToggle, isOn: Bool)
    case unlockedIcons(Set<AppIcon>)

    /// The wire field a change touches — one in-flight PATCH per field.
    enum Field: Hashable, Sendable {
        case gradeSpoiler
        case notification(NotificationToggle)
        case unlockedIcons
    }

    var field: Field {
        switch self {
        case .gradeSpoiler: .gradeSpoiler
        case let .notification(toggle, _): .notification(toggle)
        case .unlockedIcons: .unlockedIcons
        }
    }
}

/// The stored SAGRES login pair, mirrored from the server for the
/// credentials vault in Settings.
struct AccountCredentials: Equatable, Sendable {
    var username: String
    var password: String
}

/// Identity + preferences from `GET api/sync/profile` in one round-trip.
struct SettingsAccount: Equatable, Sendable {
    var profile: Profile
    var settings: UserSettings
}

extension AccountCredentials {
    static let preview = AccountCredentials(
        username: "mariana.nogueira",
        password: "ma·nogue!ra·2024"
    )
}

extension SettingsAccount {
    static let preview = SettingsAccount(profile: .preview, settings: UserSettings())
}
