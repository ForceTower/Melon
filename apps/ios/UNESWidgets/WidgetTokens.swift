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

/// Theme palette for the next-class widget. Mirrors the `LIGHT`/`DARK`
/// constants in `screens-widgets.jsx`. The widget follows the **system**
/// appearance (not the app's theme override) per the handoff.
struct WidgetTheme {
    let surface: Color
    let ink: Color
    let ink2: Color
    let ink3: Color
    let ink4: Color
    /// Hairline separator between sections inside the card.
    let line: Color
    /// Vertical separator between footer chips.
    let divider: Color
    /// Card edge border.
    let cardLine: Color
    /// Linear gradient veil painted on top of the mesh, top → bottom.
    let veilTop: Color
    let veilBottom: Color
    /// Subtle background for the non-`next` cells in the "seu dia" strip on
    /// the large widget.
    let todayCellBackground: Color
    /// Progress-bar track on the in-class state.
    let progressTrack: Color
    let meshVariant: WidgetMeshVariant
    let meshIntensity: Double

    static let light = WidgetTheme(
        surface: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255),
        ink: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255),
        ink2: Color(red: 0x3A / 255, green: 0x2F / 255, blue: 0x42 / 255),
        ink3: Color(red: 0x6B / 255, green: 0x5E / 255, blue: 0x70 / 255),
        ink4: Color(red: 0x9C / 255, green: 0x8F / 255, blue: 0xA0 / 255),
        line: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.08),
        divider: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.15),
        cardLine: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.06),
        veilTop: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.55),
        veilBottom: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.78),
        todayCellBackground: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.04),
        progressTrack: Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255).opacity(0.1),
        meshVariant: .sun,
        meshIntensity: 0.35
    )

    static let dark = WidgetTheme(
        surface: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255),
        ink: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255),
        ink2: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.92),
        ink3: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.78),
        ink4: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.55),
        line: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.15),
        divider: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.2),
        cardLine: Color.white.opacity(0.06),
        veilTop: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.08),
        veilBottom: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.55),
        todayCellBackground: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.06),
        progressTrack: Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255).opacity(0.12),
        meshVariant: .cool,
        meshIntensity: 1.0
    )

    static func resolve(_ scheme: ColorScheme) -> WidgetTheme {
        scheme == .light ? .light : .dark
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
