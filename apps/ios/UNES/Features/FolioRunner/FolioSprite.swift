import SwiftUI

/// Folio — the paper-corner mascot from `mascot-concepts.jsx` (Concept 02),
/// translated from SVG into a Canvas-friendly drawer. Five poses cover a
/// Chrome-dino style runner: idle, runA/runB, jump, duck.
enum FolioPose {
    case idle, runA, runB, jump, duck
}

/// Static sprite drawer. Authored on a 200×200 viewBox; the caller passes
/// a destination `frame` and the drawer scales into it. Used both by the
/// game's main Canvas (which packs the whole scene into one renderer) and
/// by `FolioSpriteView` for static placements.
enum FolioSprite {
    static func draw(
        in ctx: GraphicsContext,
        pose: FolioPose,
        frame: CGRect,
        blink: Bool = false
    ) {
        let cfg = config(for: pose)
        let isJump = pose == .jump
        let isDuck = pose == .duck
        // Eyes are drawn closed in duck (matches the squash) or whenever
        // the caller asks for a blink — the game uses duck only, the
        // empty-state mascot uses blink.
        let eyesClosed = isDuck || blink

        var sprite = ctx
        sprite.translateBy(x: frame.minX, y: frame.minY)
        sprite.scaleBy(x: frame.width / 200, y: frame.height / 200)

        // Shadow on the ground line — shrinks while airborne.
        let shadowCY: CGFloat = isJump ? 178 : 174
        let shadowRX: CGFloat = isJump ? 22 : 38
        sprite.fill(
            Path(ellipseIn: CGRect(
                x: 100 - shadowRX, y: shadowCY - 4,
                width: shadowRX * 2, height: 8
            )),
            with: .color(.black.opacity(0.14))
        )

        // Body transform: translate, rotate, squash. Mirrors the SVG's
        // nested <g transform> so the dog-ear and rule lines move with the
        // page.
        var body = sprite
        body.translateBy(x: 100, y: 100 + cfg.bodyY)
        body.rotate(by: .degrees(cfg.bodyR))
        body.scaleBy(x: 1, y: cfg.squash)
        body.translateBy(x: -100, y: -100)

        let bottomBase: CGFloat = isDuck ? 130 : 145
        let bottomMid: CGFloat = isDuck ? 134 : 149
        let bottomCurve: CGFloat = isDuck ? 138 : 153
        let paper = Path { p in
            p.move(to: CGPoint(x: 50, y: 50))
            p.addLine(to: CGPoint(x: 138, y: 50))
            p.addLine(to: CGPoint(x: 152, y: 64))
            p.addLine(to: CGPoint(x: 152, y: bottomBase))
            p.addQuadCurve(
                to: CGPoint(x: 100, y: bottomMid),
                control: CGPoint(x: 130, y: bottomCurve)
            )
            p.addQuadCurve(
                to: CGPoint(x: 50, y: bottomMid),
                control: CGPoint(x: 70, y: bottomCurve)
            )
            p.closeSubpath()
        }
        body.fill(paper, with: .color(cream))
        body.stroke(
            paper, with: .color(ink),
            style: StrokeStyle(lineWidth: 2.5, lineJoin: .round)
        )

        // Folded corner (dog ear) — pivots independently around its own
        // hinge so the run cycle wags it.
        var ear = body
        ear.translateBy(x: 145, y: 57)
        ear.rotate(by: .degrees(cfg.earR))
        ear.translateBy(x: -145, y: -57)
        let earPath = Path { p in
            p.move(to: CGPoint(x: 138, y: 50))
            p.addLine(to: CGPoint(x: 152, y: 64))
            p.addLine(to: CGPoint(x: 138, y: 64))
            p.closeSubpath()
        }
        ear.fill(earPath, with: .color(creamDark))
        ear.stroke(earPath, with: .color(ink), style: StrokeStyle(lineWidth: 2))

        // Coral margin rule.
        let marginEnd: CGFloat = isDuck ? 132 : 144
        var margin = Path()
        margin.move(to: CGPoint(x: 68, y: 58))
        margin.addLine(to: CGPoint(x: 68, y: marginEnd))
        body.stroke(
            margin, with: .color(coral),
            style: StrokeStyle(lineWidth: 2, lineCap: .round)
        )

        // Faint horizontal rule lines.
        for y in [78.0, 92.0, 106.0] as [CGFloat] {
            var line = Path()
            line.move(to: CGPoint(x: 78, y: y))
            line.addLine(to: CGPoint(x: 148, y: y))
            body.stroke(
                line, with: .color(ink.opacity(0.10)),
                style: StrokeStyle(lineWidth: 1)
            )
        }

        if !eyesClosed {
            body.fill(
                Path(ellipseIn: CGRect(x: 98 - 3.5, y: 86 - 3.5, width: 7, height: 7)),
                with: .color(ink)
            )
            body.fill(
                Path(ellipseIn: CGRect(x: 120 - 3.5, y: 86 - 3.5, width: 7, height: 7)),
                with: .color(ink)
            )
            body.fill(
                Path(ellipseIn: CGRect(x: 99 - 1, y: 85 - 1, width: 2, height: 2)),
                with: .color(cream)
            )
            body.fill(
                Path(ellipseIn: CGRect(x: 121 - 1, y: 85 - 1, width: 2, height: 2)),
                with: .color(cream)
            )
        } else {
            // Sit the lids at the open-eye Y in non-duck poses so a blink
            // looks like eyes shutting in place; duck keeps its lower
            // squashed-head position.
            let lidY: CGFloat = isDuck ? 92 : 86
            var leftLid = Path()
            leftLid.move(to: CGPoint(x: 94, y: lidY))
            leftLid.addLine(to: CGPoint(x: 102, y: lidY))
            body.stroke(
                leftLid, with: .color(ink),
                style: StrokeStyle(lineWidth: 2.5, lineCap: .round)
            )
            var rightLid = Path()
            rightLid.move(to: CGPoint(x: 116, y: lidY))
            rightLid.addLine(to: CGPoint(x: 124, y: lidY))
            body.stroke(
                rightLid, with: .color(ink),
                style: StrokeStyle(lineWidth: 2.5, lineCap: .round)
            )
        }

        var smile = Path()
        smile.move(to: CGPoint(x: 102, y: 100))
        smile.addQuadCurve(to: CGPoint(x: 116, y: 100), control: CGPoint(x: 109, y: 104))
        body.stroke(
            smile, with: .color(ink),
            style: StrokeStyle(lineWidth: 2, lineCap: .round)
        )

        // Rippled bottom edge as legs — drawn on the parent transform (not
        // the body's) so the squash doesn't deform the gait.
        if !isJump && !isDuck {
            var legs = sprite
            legs.translateBy(x: 0, y: cfg.bodyY * 0.6)
            var legPath = Path()
            legPath.move(to: CGPoint(x: 70, y: 152))
            legPath.addQuadCurve(
                to: CGPoint(x: 86, y: 152),
                control: CGPoint(x: 78, y: 162 + cfg.rippleA)
            )
            legPath.addQuadCurve(
                to: CGPoint(x: 102, y: 152),
                control: CGPoint(x: 94, y: 162 - cfg.rippleA)
            )
            legPath.addQuadCurve(
                to: CGPoint(x: 118, y: 152),
                control: CGPoint(x: 110, y: 162 + cfg.rippleA)
            )
            legPath.addQuadCurve(
                to: CGPoint(x: 134, y: 152),
                control: CGPoint(x: 126, y: 162 - cfg.rippleA)
            )
            legs.stroke(
                legPath, with: .color(ink),
                style: StrokeStyle(lineWidth: 2.5, lineCap: .round)
            )
        }
    }

    private struct Config {
        var bodyY: CGFloat
        var bodyR: CGFloat
        var rippleA: CGFloat
        var squash: CGFloat
        var earR: CGFloat
    }

    private static func config(for pose: FolioPose) -> Config {
        switch pose {
        case .idle: return Config(bodyY: 0,   bodyR: 0,  rippleA: 0,  squash: 1.0,  earR: 0)
        case .runA: return Config(bodyY: -2,  bodyR: -2, rippleA: 6,  squash: 1.02, earR: -3)
        case .runB: return Config(bodyY: -2,  bodyR: 2,  rippleA: -6, squash: 1.02, earR: 3)
        case .jump: return Config(bodyY: -22, bodyR: -6, rippleA: 0,  squash: 1.06, earR: -8)
        case .duck: return Config(bodyY: 12,  bodyR: 0,  rippleA: 0,  squash: 0.6,  earR: 0)
        }
    }

    // Fixed palette from `mascot-concepts.jsx`. Not adaptive — the runner
    // is a paper-themed scene and reads the same in light/dark.
    static let cream     = Color(red: 0xFB / 255, green: 0xF7 / 255, blue: 0xF2 / 255)
    static let creamDark = Color(red: 0xE9 / 255, green: 0xE0 / 255, blue: 0xD2 / 255)
    static let ink       = Color(red: 0x1A / 255, green: 0x14 / 255, blue: 0x20 / 255)
    static let coral     = Color(red: 0xE8 / 255, green: 0x5D / 255, blue: 0x4E / 255)
    static let plum      = Color(red: 0x2D / 255, green: 0x1B / 255, blue: 0x4E / 255)
    static let amber     = Color(red: 0xF4 / 255, green: 0xA2 / 255, blue: 0x3C / 255)
    static let peach     = Color(red: 0xFB / 255, green: 0xD9 / 255, blue: 0xA8 / 255)
}

/// Static SwiftUI wrapper — used outside the running game's Canvas (e.g.
/// the start overlay).
struct FolioSpriteView: View {
    var pose: FolioPose
    var size: CGFloat = 90
    var blink: Bool = false

    var body: some View {
        Canvas { ctx, _ in
            FolioSprite.draw(
                in: ctx,
                pose: pose,
                frame: CGRect(x: 0, y: 0, width: size, height: size),
                blink: blink
            )
        }
        .frame(width: size, height: size)
    }
}

/// Idle Folio that closes its eyes once or twice every ~25 seconds.
/// Used as the empty-state mascot in `DayColumn` — also acts as the
/// long-press target for the runner easter egg.
struct BlinkingFolio: View {
    var size: CGFloat = 96

    @State private var blink = false

    var body: some View {
        FolioSpriteView(pose: .idle, size: size, blink: blink)
            .task { await runBlinkLoop() }
    }

    private func runBlinkLoop() async {
        while !Task.isCancelled {
            // Random gap (avg ~25s) between blink events. Re-rolled each
            // loop so the rhythm doesn't feel mechanical.
            try? await Task.sleep(for: .seconds(.random(in: 18 ... 32)))
            await singleBlink()
            // Roughly a third of the time, follow up with a quick double
            // blink — feels natural without becoming twitchy.
            if Int.random(in: 0 ..< 3) == 0 {
                try? await Task.sleep(for: .milliseconds(110))
                await singleBlink()
            }
        }
    }

    private func singleBlink() async {
        blink = true
        try? await Task.sleep(for: .milliseconds(140))
        blink = false
    }
}
