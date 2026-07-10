import Foundation

extension String {
    /// Resolves a generated `LocalizedStringResource` symbol (from
    /// `Resources/Localizable.xcstrings`) to a `String` in the current locale.
    ///
    /// Use in non-SwiftUI contexts — `FormatStyle` builders, models, anything that
    /// must hand back a plain `String`. In views prefer `Text(.symbol)` directly.
    /// This exists because `String(localized: .symbol)` resolves to the
    /// `String.LocalizationValue` overload, which the generated symbols don't extend.
    static func localized(_ resource: LocalizedStringResource) -> String {
        String(localized: resource)
    }
}
