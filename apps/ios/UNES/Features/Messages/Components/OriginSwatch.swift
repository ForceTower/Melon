import SwiftUI

/// Origin swatch shown at the left of every message row and on the detail
/// sender card. Shape and glyph depend on `message.origin`:
///
/// * `.discipline` — rounded rect in the discipline's accent color with the
///   short code on top.
/// * `.direct`     — circle with a subtle tinted ring + person glyph.
/// * `.secretariat`/`.campus`/`.app`/`.module` — rounded tile with tinted
///   background and a kind-specific icon.
struct OriginSwatch: View {
    let message: Message
    var size: CGFloat = 40

    var body: some View {
        let meta = message.meta
        switch message.origin {
        case .direct:
            directSwatch(meta: meta)
        case .discipline:
            disciplineSwatch(meta: meta)
        default:
            iconSwatch(meta: meta)
        }
    }

    private func directSwatch(meta: MessageOriginMeta) -> some View {
        ZStack {
            Circle()
                .fill(meta.color.opacity(0.09))
            Circle()
                .stroke(meta.color.opacity(0.4), lineWidth: 1.5)
            PersonGlyph()
                .stroke(meta.color, style: StrokeStyle(lineWidth: 1.3, lineCap: .round, lineJoin: .round))
                .frame(width: size * 0.38, height: size * 0.38)
        }
        .frame(width: size, height: size)
    }

    private func disciplineSwatch(meta: MessageOriginMeta) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .fill(meta.color)
            Text(meta.label)
                .font(UNESFont.mono(10, weight: .bold))
                .tracking(0.4)
                .foregroundStyle(Color.white)
        }
        .frame(width: size, height: size)
    }

    private func iconSwatch(meta: MessageOriginMeta) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .fill(meta.color.opacity(0.12))
            iconContent(for: message.origin)
                .foregroundStyle(meta.color)
        }
        .frame(width: size, height: size)
    }

    @ViewBuilder
    private func iconContent(for origin: MessageOrigin) -> some View {
        switch origin {
        case .app:
            // UNES speech card
            AppGlyph()
                .stroke(style: StrokeStyle(lineWidth: 1.4, lineJoin: .round))
                .frame(width: 18, height: 18)
        case .module:
            // 2×2 grid of tiles
            ModuleGlyph()
                .stroke(style: StrokeStyle(lineWidth: 1.4))
                .frame(width: 18, height: 18)
        case .secretariat:
            // envelope
            EnvelopeGlyph()
                .stroke(style: StrokeStyle(lineWidth: 1.3, lineCap: .round))
                .frame(width: 18, height: 18)
        case .campus:
            // graduation cap
            CampusGlyph()
                .stroke(style: StrokeStyle(lineWidth: 1.3, lineCap: .round, lineJoin: .round))
                .frame(width: 18, height: 18)
        default:
            EmptyView()
        }
    }
}

// MARK: - Glyphs (shape primitives scaled to the view's frame)

private struct PersonGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let sx = rect.width / 15.0
        let sy = rect.height / 15.0
        // head
        p.addEllipse(in: CGRect(x: (7.5 - 2.3) * sx, y: (5 - 2.3) * sy,
                                width: 4.6 * sx, height: 4.6 * sy))
        // shoulders
        p.move(to: CGPoint(x: 2.5 * sx, y: 13 * sy))
        p.addQuadCurve(to: CGPoint(x: 12.5 * sx, y: 13 * sy),
                       control: CGPoint(x: 7.5 * sx, y: 7.3 * sy))
        return p
    }
}

private struct AppGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        // chat card: M4 4h10v7l-4-2-4 3V4z
        var p = Path()
        let sx = rect.width / 18.0
        let sy = rect.height / 18.0
        p.move(to: CGPoint(x: 4 * sx, y: 4 * sy))
        p.addLine(to: CGPoint(x: 14 * sx, y: 4 * sy))
        p.addLine(to: CGPoint(x: 14 * sx, y: 11 * sy))
        p.addLine(to: CGPoint(x: 10 * sx, y: 9 * sy))
        p.addLine(to: CGPoint(x: 6 * sx, y: 12 * sy))
        p.closeSubpath()
        return p
    }
}

private struct ModuleGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let sx = rect.width / 18.0
        let sy = rect.height / 18.0
        let r: CGFloat = 1.2
        let side: CGFloat = 5.5
        for (x, y) in [(3, 3), (9.5, 3), (3, 9.5), (9.5, 9.5)] {
            p.addRoundedRect(
                in: CGRect(x: CGFloat(x) * sx, y: CGFloat(y) * sy,
                           width: side * sx, height: side * sy),
                cornerSize: CGSize(width: r * sx, height: r * sy)
            )
        }
        return p
    }
}

private struct EnvelopeGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let sx = rect.width / 18.0
        let sy = rect.height / 18.0
        // outer rect
        p.addRoundedRect(
            in: CGRect(x: 3 * sx, y: 4 * sy, width: 12 * sx, height: 10 * sy),
            cornerSize: CGSize(width: 1.4 * sx, height: 1.4 * sy)
        )
        // flap: M3 4l6 4.5L15 4
        p.move(to: CGPoint(x: 3 * sx, y: 4 * sy))
        p.addLine(to: CGPoint(x: 9 * sx, y: 8.5 * sy))
        p.addLine(to: CGPoint(x: 15 * sx, y: 4 * sy))
        // small line at bottom
        p.move(to: CGPoint(x: 6 * sx, y: 13 * sy))
        p.addLine(to: CGPoint(x: 10 * sx, y: 13 * sy))
        return p
    }
}

private struct CampusGlyph: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let sx = rect.width / 18.0
        let sy = rect.height / 18.0
        // roof diamond: M9 2L2 6l7 3 7-3-7-4z
        p.move(to: CGPoint(x: 9 * sx, y: 2 * sy))
        p.addLine(to: CGPoint(x: 2 * sx, y: 6 * sy))
        p.addLine(to: CGPoint(x: 9 * sx, y: 9 * sy))
        p.addLine(to: CGPoint(x: 16 * sx, y: 6 * sy))
        p.closeSubpath()
        // curve under
        p.move(to: CGPoint(x: 4 * sx, y: 8 * sy))
        p.addLine(to: CGPoint(x: 4 * sx, y: 11.5 * sy))
        p.addQuadCurve(to: CGPoint(x: 14 * sx, y: 11.5 * sy),
                       control: CGPoint(x: 9 * sx, y: 15.5 * sy))
        p.addLine(to: CGPoint(x: 14 * sx, y: 8 * sy))
        return p
    }
}
