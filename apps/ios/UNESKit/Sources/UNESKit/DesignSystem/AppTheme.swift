import SwiftUI

enum AppTheme: String, CaseIterable, Equatable, Sendable {
    case system, light, dark

    var label: String {
        switch self {
        case .system: String.localized(.themeSystem)
        case .light: String.localized(.themeLight)
        case .dark: String.localized(.themeDark)
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
