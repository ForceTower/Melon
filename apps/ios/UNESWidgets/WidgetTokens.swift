import SwiftUI
import UIKit

/// Subset of the app's design tokens that the widget extension needs. Kept
/// in-target because WidgetKit extensions can't link the main app's Swift
/// modules. Mirrors `UNES/DesignSystem/DesignTokens.swift`.
enum WidgetColor {
    static let ink       = dynamic(light: hex(0x1A, 0x14, 0x20),
                                   dark:  hex(0xF5, 0xEF, 0xE6))
    static let ink3      = dynamic(light: hex(0x6B, 0x5E, 0x70),
                                   dark:  hex(0x9F, 0x93, 0x86))

    static let surface   = dynamic(light: hex(0xFB, 0xF7, 0xF2),
                                   dark:  hex(0x15, 0x10, 0x1A))

    static let darkBg    = Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
    static let surfaceLight = Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
    static let inkFixed     = Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255)

    static let plum      = Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
    static let magenta   = Color(red: 0xB2 / 255, green: 0x3A / 255, blue: 0x7A / 255)
    static let coral     = Color(red: 0xE8 / 255, green: 0x5D / 255, blue: 0x4E / 255)
    static let amber     = Color(red: 0xF4 / 255, green: 0xA2 / 255, blue: 0x3C / 255)
    static let peach     = Color(red: 0xFB / 255, green: 0xD9 / 255, blue: 0xA8 / 255)

    /// Class subject teal — matches `NEXT_CLASS.color` in the design.
    static let subjectTeal = Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255)

    static let accent       = dynamic(light: hex(0xE8, 0x5D, 0x4E),
                                      dark:  hex(0xF4, 0xA2, 0x3C))

    private static func hex(_ r: Int, _ g: Int, _ b: Int) -> UIColor {
        UIColor(red: CGFloat(r) / 255, green: CGFloat(g) / 255, blue: CGFloat(b) / 255, alpha: 1)
    }

    private static func dynamic(light: UIColor, dark: UIColor) -> Color {
        Color(uiColor: UIColor { @Sendable trait in
            trait.userInterfaceStyle == .dark ? dark : light
        })
    }
}

enum WidgetFont {
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
