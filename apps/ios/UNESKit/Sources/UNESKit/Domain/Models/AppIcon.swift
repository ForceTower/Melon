import ComposableArchitecture
import Foundation

/// The Home Screen icons the app ships. `aurora` is the primary icon; the
/// others are alternates registered with the asset catalog compiler. The
/// secret ones each have their own discovery path.
enum AppIcon: String, Equatable, Sendable, CaseIterable, Identifiable {
    case aurora = "Aurora"
    case ocean = "Ocean"
    case stripped = "Stripped"
    /// Discovered by tapping the version footer in Configurações seven times.
    case baseSans = "BaseSans"
    /// Discovered by entering the Folio Runner for the first time.
    case paper = "Paper"
    /// Discovered by entering Space Impact on the watch for the first time.
    case nowShip = "NowShip"

    var id: String { rawValue }

    /// The name `setAlternateIconName` expects — `nil` means the primary icon.
    var alternateIconName: String? { self == .aurora ? nil : rawValue }

    init(alternateIconName: String?) {
        self = alternateIconName.flatMap(AppIcon.init(rawValue:)) ?? .aurora
    }

    var isSecret: Bool {
        switch self {
        case .aurora, .ocean, .stripped: false
        case .baseSans, .paper, .nowShip: true
        }
    }

    static let secrets = allCases.filter(\.isSecret)

    /// The preview imageset in the app's asset catalog (e.g. `icon.aurora`).
    var assetName: String { "icon.\(rawValue.lowercased())" }
}

/// A set of icons stored as one comma-joined `appStorage` slot, so every
/// discovery path writes the same shared value.
struct AppIconSet: RawRepresentable, Equatable, Sendable {
    var icons: Set<AppIcon>

    init(_ icons: Set<AppIcon> = []) {
        self.icons = icons
    }

    init?(rawValue: String) {
        self.init(Set(rawValue.split(separator: ",").compactMap { AppIcon(rawValue: String($0)) }))
    }

    var rawValue: String {
        icons.map(\.rawValue).sorted().joined(separator: ",")
    }

    func contains(_ icon: AppIcon) -> Bool { icons.contains(icon) }

    mutating func insert(_ icon: AppIcon) { icons.insert(icon) }

    mutating func formUnion(_ other: some Sequence<AppIcon>) { icons.formUnion(other) }
}

extension SharedKey where Self == AppStorageKey<AppIconSet>.Default {
    /// Secret icons the user has discovered, across all discovery paths.
    static var unlockedSecretIcons: Self {
        Self[.appStorage("unlockedSecretIcons"), default: AppIconSet()]
    }

    /// Discoveries already celebrated with the unlock sheet — marked when the
    /// sheet is dismissed, so an interrupted celebration fires again.
    static var announcedSecretIcons: Self {
        Self[.appStorage("announcedSecretIcons"), default: AppIconSet()]
    }
}
