import SwiftUI

/// Hand-rolled SVG-equivalent glyph used in the eyebrow row of every calendar
/// surface. Mirrors `CalCatGlyph` in `screens-calendar.jsx`.
///
/// Three shapes:
///   - **holiday** — sun (circle + 8 rays)
///   - **exam**    — folded paper with rule-lines
///   - **deadline** — bowtie / hourglass crossings
struct CalCategoryGlyph: View {
    let category: CalendarCategory
    var color: Color
    var size: CGFloat = 14

    var body: some View {
        switch category {
        case .holiday:  HolidayGlyph(color: color, size: size)
        case .exam:     ExamGlyph(color: color, size: size)
        case .deadline: DeadlineGlyph(color: color, size: size)
        }
    }
}

private struct HolidayGlyph: View {
    let color: Color
    let size: CGFloat

    var body: some View {
        let stroke = StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round)
        Canvas { ctx, _ in
            let s = size / 14
            // Sun centre at (7,7), r=4. Rays at cardinals + diagonals.
            let center = CGPoint(x: 7 * s, y: 7 * s)
            let circle = Path(ellipseIn: CGRect(x: 3 * s, y: 3 * s, width: 8 * s, height: 8 * s))
            ctx.stroke(circle, with: .color(color), style: stroke)

            // Cardinals: top, bottom, left, right (length 1.5 inset by 0.5)
            let pairs: [(CGPoint, CGPoint)] = [
                (CGPoint(x: 7,   y: 1.5), CGPoint(x: 7,   y: 3)),
                (CGPoint(x: 7,   y: 11),  CGPoint(x: 7,   y: 12.5)),
                (CGPoint(x: 1.5, y: 7),   CGPoint(x: 3,   y: 7)),
                (CGPoint(x: 11,  y: 7),   CGPoint(x: 12.5, y: 7)),
                // Diagonals: 4.2 4.2 → 3.1 3.1 etc.
                (CGPoint(x: 4.2, y: 4.2), CGPoint(x: 3.1, y: 3.1)),
                (CGPoint(x: 10.9, y: 10.9), CGPoint(x: 9.8, y: 9.8)),
                (CGPoint(x: 4.2, y: 9.8), CGPoint(x: 3.1, y: 10.9)),
                (CGPoint(x: 10.9, y: 3.1), CGPoint(x: 9.8, y: 4.2)),
            ]
            var rays = Path()
            for (a, b) in pairs {
                rays.move(to: CGPoint(x: a.x * s, y: a.y * s))
                rays.addLine(to: CGPoint(x: b.x * s, y: b.y * s))
            }
            ctx.stroke(rays, with: .color(color), style: stroke)
            _ = center
        }
        .frame(width: size, height: size)
    }
}

private struct ExamGlyph: View {
    let color: Color
    let size: CGFloat

    var body: some View {
        let stroke = StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round)
        Canvas { ctx, _ in
            let s = size / 14
            // Folded paper outline: 3,2 → 9,2 → 11,4 → 11,12.5 → 2,12.5 → 2,2 → close
            var outline = Path()
            outline.move(to: CGPoint(x: 3 * s, y: 2 * s))
            outline.addLine(to: CGPoint(x: 9 * s, y: 2 * s))
            outline.addLine(to: CGPoint(x: 11 * s, y: 4 * s))
            outline.addLine(to: CGPoint(x: 11 * s, y: 12.5 * s))
            outline.addLine(to: CGPoint(x: 2 * s, y: 12.5 * s))
            outline.addLine(to: CGPoint(x: 2 * s, y: 2 * s))
            outline.closeSubpath()
            ctx.stroke(outline, with: .color(color), style: stroke)

            // Fold + rule lines.
            var details = Path()
            // Fold: 9,2 → 9,4.5 → 11,4.5
            details.move(to: CGPoint(x: 9 * s, y: 2 * s))
            details.addLine(to: CGPoint(x: 9 * s, y: 4.5 * s))
            details.addLine(to: CGPoint(x: 11 * s, y: 4.5 * s))
            // Three rule lines.
            for (y, len): (CGFloat, CGFloat) in [(7, 5), (9.5, 5), (12, 3)] {
                details.move(to: CGPoint(x: 4.5 * s, y: y * s))
                details.addLine(to: CGPoint(x: (4.5 + len) * s, y: y * s))
            }
            ctx.stroke(details, with: .color(color), style: stroke)
        }
        .frame(width: size, height: size)
    }
}

private struct DeadlineGlyph: View {
    let color: Color
    let size: CGFloat

    var body: some View {
        let stroke = StrokeStyle(lineWidth: 1.5, lineCap: .round, lineJoin: .round)
        Canvas { ctx, _ in
            let s = size / 14
            // Top + bottom bars
            var bars = Path()
            bars.move(to: CGPoint(x: 3.5 * s, y: 2 * s))
            bars.addLine(to: CGPoint(x: 10.5 * s, y: 2 * s))
            bars.move(to: CGPoint(x: 3.5 * s, y: 12 * s))
            bars.addLine(to: CGPoint(x: 10.5 * s, y: 12 * s))
            ctx.stroke(bars, with: .color(color), style: stroke)

            // Crossing curves: simulate the hourglass-ish quadratic curves.
            var curves = Path()
            curves.move(to: CGPoint(x: 4.5 * s, y: 2 * s))
            curves.addQuadCurve(to: CGPoint(x: 9.5 * s, y: 12 * s),
                                control: CGPoint(x: 4.5 * s, y: 7 * s))
            curves.move(to: CGPoint(x: 9.5 * s, y: 2 * s))
            curves.addQuadCurve(to: CGPoint(x: 4.5 * s, y: 12 * s),
                                control: CGPoint(x: 9.5 * s, y: 7 * s))
            ctx.stroke(curves, with: .color(color), style: stroke)
        }
        .frame(width: size, height: size)
    }
}
