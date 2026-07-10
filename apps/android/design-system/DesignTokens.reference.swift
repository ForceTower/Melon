// Reference only — not compiled. Design tokens from the original iOS app
// (formerly apps/ios, deleted); this Android design system mirrors them.

import SwiftUI
import UIKit

enum UNESColor {
    // MARK: Neutrals (adaptive)
    // Light values match the `:root` CSS tokens; dark values match
    // `:root[data-theme="dark"]` from `styles.css`.
    static let ink       = dynamic(light: hex(0x1A, 0x14, 0x20),
                                   dark:  hex(0xF5, 0xEF, 0xE6))
    static let ink2      = dynamic(light: hex(0x3A, 0x2F, 0x42),
                                   dark:  hex(0xD6, 0xCE, 0xC2))
    static let ink3      = dynamic(light: hex(0x6B, 0x5E, 0x70),
                                   dark:  hex(0x9F, 0x93, 0x86))
    static let ink4      = dynamic(light: hex(0x9C, 0x8F, 0xA0),
                                   dark:  hex(0x6B, 0x61, 0x56))

    static let surface   = dynamic(light: hex(0xFB, 0xF7, 0xF2),
                                   dark:  hex(0x15, 0x10, 0x1A))
    static let surface2  = dynamic(light: hex(0xF3, 0xED, 0xE4),
                                   dark:  hex(0x1E, 0x18, 0x24))
    static let surface3  = dynamic(light: hex(0xE9, 0xE0, 0xD2),
                                   dark:  hex(0x2A, 0x22, 0x32))

    static let line      = dynamic(light: UIColor.black.withAlphaComponent(0.08),
                                   dark:  hex(0xF5, 0xEF, 0xE6).withAlphaComponent(0.09))

    static let card      = dynamic(light: UIColor.white,
                                   dark:  hex(0x1C, 0x16, 0x24))
    static let cardLine  = dynamic(light: UIColor.black.withAlphaComponent(0.05),
                                   dark:  hex(0xF5, 0xEF, 0xE6).withAlphaComponent(0.06))

    static let pageBg    = dynamic(light: hex(0xED, 0xE7, 0xDD),
                                   dark:  hex(0x0C, 0x08, 0x10))

    /// Flat dark background used by screens that are intentionally dark in
    /// both modes (splash, welcome, sync). Not adaptive.
    static let darkBg    = Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)

    /// Fixed-value neutrals for use on the always-dark screens, so they don't
    /// flip with system appearance (splash/welcome/sync always render light
    /// type on a dark mesh).
    static let surfaceLight = Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
    static let inkFixed     = Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255)

    // MARK: Brand (fixed, identity-carrying)
    static let plum      = Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
    static let magenta   = Color(red: 0xB2 / 255, green: 0x3A / 255, blue: 0x7A / 255)
    static let coral     = Color(red: 0xE8 / 255, green: 0x5D / 255, blue: 0x4E / 255)
    static let amber     = Color(red: 0xF4 / 255, green: 0xA2 / 255, blue: 0x3C / 255)
    static let peach     = Color(red: 0xFB / 255, green: 0xD9 / 255, blue: 0xA8 / 255)

    // MARK: Accent (adaptive: coral in light, amber in dark)
    static let accent       = dynamic(light: hex(0xE8, 0x5D, 0x4E),
                                      dark:  hex(0xF4, 0xA2, 0x3C))
    static let accentPress  = dynamic(light: hex(0xC9, 0x45, 0x38),
                                      dark:  hex(0xE8, 0x8A, 0x1D))

    // MARK: - Helpers
    private static func hex(_ r: Int, _ g: Int, _ b: Int) -> UIColor {
        UIColor(red: CGFloat(r) / 255, green: CGFloat(g) / 255, blue: CGFloat(b) / 255, alpha: 1)
    }

    /// `@Sendable` is load-bearing: SwiftUI's async render display link
    /// resolves dynamic colors off the main thread, and an inferred
    /// `@MainActor` closure here trips Swift 6's isolation check (SIGTRAP
    /// inside `_swift_task_checkIsolatedSwift`).
    private static func dynamic(light: UIColor, dark: UIColor) -> Color {
        Color(uiColor: UIColor { @Sendable trait in
            trait.userInterfaceStyle == .dark ? dark : light
        })
    }
}

enum UNESFont {
    /// Design selection: "Fraunces" for serif moments (from Tweaks panel).
    /// Falls back to system `.serif` design (New York on Apple platforms) if
    /// the font file isn't bundled.
    private static let serifFamily = "Fraunces"
    private static let sansFamily  = "Inter"

    static func serif(_ size: CGFloat, italic: Bool = false) -> Font {
        if UIFont(name: serifFamily, size: size) != nil {
            let font = Font.custom(serifFamily, size: size)
            return italic ? font.italic() : font
        }
        let base = Font.system(size: size, weight: .regular, design: .serif)
        return italic ? base.italic() : base
    }

    static func sans(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        if UIFont(name: sansFamily, size: size) != nil {
            return Font.custom(sansFamily, size: size).weight(weight)
        }
        return Font.system(size: size, weight: weight, design: .default)
    }

    static func mono(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        Font.system(size: size, weight: weight, design: .monospaced)
    }
}

enum UNESMotion {
    static let spring: Animation  = .spring(response: 0.45, dampingFraction: 0.78)
    static let pop: Animation     = .spring(response: 0.5, dampingFraction: 0.6)
    static let ease: Animation    = .easeOut(duration: 0.35)
    static let easeSlow: Animation = .easeOut(duration: 0.7)
    static let easeEmphasized: Animation = .timingCurve(0.2, 0.8, 0.2, 1, duration: 0.6)
}
