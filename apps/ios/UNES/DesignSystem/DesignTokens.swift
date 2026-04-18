import SwiftUI
import UIKit

enum UNESColor {
    static let ink       = Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255)
    static let ink2      = Color(red: 0x3A / 255, green: 0x2F / 255, blue: 0x42 / 255)
    static let ink3      = Color(red: 0x6B / 255, green: 0x5E / 255, blue: 0x70 / 255)
    static let ink4      = Color(red: 0x9C / 255, green: 0x8F / 255, blue: 0xA0 / 255)

    static let surface   = Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
    static let surface2  = Color(red: 0xF3 / 255, green: 0xED / 255, blue: 0xE4 / 255)
    static let surface3  = Color(red: 0xE9 / 255, green: 0xE0 / 255, blue: 0xD2 / 255)
    static let line      = Color.black.opacity(0.08)

    static let darkBg    = Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)

    static let plum      = Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
    static let magenta   = Color(red: 0xB2 / 255, green: 0x3A / 255, blue: 0x7A / 255)
    static let coral     = Color(red: 0xE8 / 255, green: 0x5D / 255, blue: 0x4E / 255)
    static let amber     = Color(red: 0xF4 / 255, green: 0xA2 / 255, blue: 0x3C / 255)
    static let peach     = Color(red: 0xFB / 255, green: 0xD9 / 255, blue: 0xA8 / 255)

    static let accent       = coral
    static let accentPress  = Color(red: 0xC9 / 255, green: 0x45 / 255, blue: 0x38 / 255)
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
