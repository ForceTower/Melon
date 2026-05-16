import SwiftUI
import UIKit

extension Color {
    /// Linear sRGB blend with another color. `fraction` weights `self`; the
    /// remainder goes to `other`. Dynamic colors are resolved against the
    /// current scheme so the blend picks the right base (the CSS design uses
    /// `color-mix(in oklab, ...)` against live theme tokens).
    func mix(with other: Color, by fraction: Double, in scheme: ColorScheme) -> Color {
        let trait = UITraitCollection(userInterfaceStyle: scheme == .dark ? .dark : .light)
        let a = UIColor(self).resolvedColor(with: trait)
        let b = UIColor(other).resolvedColor(with: trait)
        var ar: CGFloat = 0, ag: CGFloat = 0, ab: CGFloat = 0, aa: CGFloat = 0
        var br: CGFloat = 0, bg: CGFloat = 0, bb: CGFloat = 0, ba: CGFloat = 0
        a.getRed(&ar, green: &ag, blue: &ab, alpha: &aa)
        b.getRed(&br, green: &bg, blue: &bb, alpha: &ba)
        let f = fraction
        return Color(
            red:     Double(ar) * f + Double(br) * (1 - f),
            green:   Double(ag) * f + Double(bg) * (1 - f),
            blue:    Double(ab) * f + Double(bb) * (1 - f),
            opacity: Double(aa) * f + Double(ba) * (1 - f)
        )
    }
}
