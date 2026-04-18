import SwiftUI

/// UNES card-style surface — rounded fill with a hairline stroke.
///
/// - On iOS 26+: the fill is rendered as Liquid Glass tinted by `fill`, so
///   the card picks up environmental refraction and specular highlights
///   while still reading as the intended color.
/// - On older runtimes: flat `fill` + 1pt `strokeBorder` — the UNES card
///   pattern used across the app since before iOS 26.
///
/// The same shape value is reused for the fill, glass mask, and stroke so
/// the three stay pixel-aligned.
struct CardSurface<S: InsettableShape>: ViewModifier {
    let shape: S
    var fill: Color = UNESColor.card
    var stroke: Color = UNESColor.cardLine
    var strokeWidth: CGFloat = 1

    func body(content: Content) -> some View {
        content.background {
            if #available(iOS 26.0, *) {
                shape
                    .fill(Color.clear)
                    .glassEffect(.regular.tint(fill), in: shape)
                    .overlay(shape.strokeBorder(stroke, lineWidth: strokeWidth))
            } else {
                shape
                    .fill(fill)
                    .overlay(shape.strokeBorder(stroke, lineWidth: strokeWidth))
            }
        }
    }
}

extension View {
    /// Apply the UNES card surface. Defaults match the shared card tokens;
    /// pass the shape (typically `RoundedRectangle(cornerRadius:, style:)`)
    /// used by the caller.
    func cardSurface<S: InsettableShape>(
        _ shape: S,
        fill: Color = UNESColor.card,
        stroke: Color = UNESColor.cardLine,
        strokeWidth: CGFloat = 1
    ) -> some View {
        modifier(CardSurface(shape: shape, fill: fill, stroke: stroke, strokeWidth: strokeWidth))
    }
}
