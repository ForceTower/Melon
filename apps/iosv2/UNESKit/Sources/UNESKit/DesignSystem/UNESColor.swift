import SwiftUI

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }

    init(light: Color, dark: Color) {
        #if canImport(UIKit)
        self.init(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(dark) : UIColor(light)
        })
        #else
        self.init(nsColor: NSColor(name: nil) { appearance in
            appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua ? NSColor(dark) : NSColor(light)
        })
        #endif
    }
}

/// Design tokens for the v2 redesign. Neutrals adapt to light/dark; brand colors are fixed.
enum UNESColor {
    // MARK: Adaptive neutrals

    static let ink = Color(light: Color(hex: 0x1A1420), dark: Color(hex: 0xF5EFE6))
    static let ink2 = Color(light: Color(hex: 0x3A2F42), dark: Color(hex: 0xD6CEC2))
    static let ink3 = Color(light: Color(hex: 0x6B5E70), dark: Color(hex: 0x9F9386))
    static let ink4 = Color(light: Color(hex: 0x9C8FA0), dark: Color(hex: 0x6B6156))

    static let surface = Color(light: Color(hex: 0xFBF7F2), dark: Color(hex: 0x15101A))
    static let surface2 = Color(light: Color(hex: 0xF3EDE4), dark: Color(hex: 0x1E1824))
    static let surface3 = Color(light: Color(hex: 0xE9E0D2), dark: Color(hex: 0x2A2232))

    static let line = Color(light: Color(hex: 0x1A1420, opacity: 0.08), dark: Color(hex: 0xF5EFE6, opacity: 0.09))
    static let card = Color(light: .white, dark: Color(hex: 0x1C1624))
    static let cardLine = Color(light: Color(hex: 0x1A1420, opacity: 0.05), dark: Color(hex: 0xF5EFE6, opacity: 0.06))
    static let pageBg = Color(light: Color(hex: 0xEDE7DD), dark: Color(hex: 0x0C0810))

    /// Coral in light mode, amber in dark mode.
    static let accent = Color(light: Color(hex: 0xE85D4E), dark: Color(hex: 0xF4A23C))
    static let accentPress = Color(light: Color(hex: 0xC94538), dark: Color(hex: 0xE88A1D))

    // MARK: Fixed brand palette

    static let plum = Color(hex: 0x2D1B4E)
    static let magenta = Color(hex: 0xB23A7A)
    static let coral = Color(hex: 0xE85D4E)
    static let amber = Color(hex: 0xF4A23C)
    static let peach = Color(hex: 0xFBD9A8)

    // MARK: Fixed accents

    static let teal = Color(hex: 0x3B9EAE)
    static let tangerine = Color(hex: 0xE8894E)
    static let violet = Color(hex: 0x7A6CE0)
    static let liveGreen = Color(hex: 0x34C759)
    static let successGreen = Color(hex: 0x2F9E5E)
    static let alertRed = Color(hex: 0xFF3B30)
    /// Warning orange — shaky grades and absence bars running hot.
    static let caution = Color(hex: 0xD9852E)

    /// The grade ramp: teal for excellent, ink for solid, caution for
    /// passing-but-shaky, coral below the cutoff, muted while unreleased.
    static func score(_ value: Double?) -> Color {
        guard let value else { return ink4 }
        if value >= 8.5 { return teal }
        if value >= 7 { return ink }
        if value >= 5 { return caution }
        return coral
    }

    /// Stable per-discipline tints, assigned by the discipline's color index.
    static let disciplinePalette: [Color] = [coral, teal, magenta, violet, amber]

    static func disciplineColor(_ index: Int) -> Color {
        let count = disciplinePalette.count
        return disciplinePalette[((index % count) + count) % count]
    }

    // MARK: Mensagens widget (always dark, like the hero)

    /// Backdrop behind the rose mesh.
    static let roseBg = Color(hex: 0x1A0F28)
    /// Base of the scrim layered over the rose mesh.
    static let roseScrim = Color(hex: 0x100A1A)

    // MARK: Always-dark screens (splash / welcome / sync)

    /// Backdrop behind the mesh on always-dark screens, in both themes.
    static let darkBg = Color(hex: 0x12100E)
    /// Foreground on always-dark screens — the light-theme surface, fixed.
    static let paper = Color(hex: 0xFBF7F2)
    /// Base of the gradient scrims layered over the mesh.
    static let scrim = Color(hex: 0x0A0810)
}

extension LinearGradient {
    /// CSS-style linear gradient: `angle` in degrees, clockwise from "to top".
    static func css(stops: [Gradient.Stop], angle: Double) -> LinearGradient {
        let radians = angle * .pi / 180
        let dx = sin(radians) / 2
        let dy = -cos(radians) / 2
        return LinearGradient(
            stops: stops,
            startPoint: UnitPoint(x: 0.5 - dx, y: 0.5 - dy),
            endPoint: UnitPoint(x: 0.5 + dx, y: 0.5 + dy)
        )
    }
}
