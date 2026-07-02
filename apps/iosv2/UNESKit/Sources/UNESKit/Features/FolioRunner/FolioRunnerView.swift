import SwiftUI

/// Easter-egg Chrome-dino style runner starring Folio. Reachable from the
/// free-day empty state in `ScheduleDayTimeline` — long-press the "Dia livre"
/// mascot to open it as a full-screen cover.
struct FolioRunnerView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var engine = FolioRunnerEngine()

    var body: some View {
        ZStack(alignment: .top) {
            FolioSprite.cream.ignoresSafeArea()

            // Everything that depends on engine state lives *inside* the
            // TimelineView so each tick re-evaluates with fresh values.
            // Keeping the HUD/overlays here (instead of as siblings of the
            // TimelineView) avoids relying on SwiftUI observation across
            // the render boundary — the engine is a plain class.
            TimelineView(.animation(minimumInterval: 1.0 / 60.0)) { tl in
                ZStack(alignment: .top) {
                    Canvas { ctx, size in
                        engine.advance(to: tl.date, size: size)
                        engine.render(into: ctx, size: size)
                    }
                    .ignoresSafeArea()

                    hud

                    switch engine.phase {
                    case .ready:    readyOverlay
                    case .playing:  EmptyView()
                    case .gameOver: gameOverOverlay
                    }
                }
            }

            closeButton
        }
        .contentShape(Rectangle())
        // Single drag gesture covers both inputs: a stationary press +
        // release is the jump tap; pulling down past the threshold puts
        // Folio in a duck for the duration of the touch.
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { v in
                    if v.translation.height > 28 {
                        engine.beginDuck()
                    }
                }
                .onEnded { v in
                    engine.endDuck()
                    if hypot(v.translation.width, v.translation.height) < 10 {
                        engine.tap()
                    }
                }
        )
        .preferredColorScheme(.light)
        .immersiveChromeHidden()
    }

    // MARK: - Overlays

    private var hud: some View {
        VStack {
            HStack(alignment: .top) {
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    if engine.bestScore > 0 {
                        Text("HI \(scoreString(engine.bestScore))")
                            .font(.system(size: 11, weight: .medium, design: .monospaced))
                            .foregroundStyle(FolioSprite.ink.opacity(0.4))
                    }
                    Text(scoreString(engine.score))
                        .font(.system(size: 13, weight: .medium, design: .monospaced))
                        .foregroundStyle(FolioSprite.ink.opacity(0.7))
                }
            }
            Spacer()
        }
        .padding(.horizontal, 22)
        .padding(.top, 12)
    }

    private var readyOverlay: some View {
        VStack(spacing: 10) {
            Text("Folio")
                .font(.system(size: 38, weight: .bold))
                .tracking(-0.5)
                .foregroundStyle(FolioSprite.ink)
            Text("toque para pular · arraste pra baixo para abaixar")
                .font(.system(size: 12))
                .tracking(0.2)
                .foregroundStyle(FolioSprite.ink.opacity(0.55))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .padding(.bottom, 220)
        .allowsHitTesting(false)
    }

    private var gameOverOverlay: some View {
        VStack(spacing: 6) {
            Text("acabou")
                .font(.system(size: 34, weight: .bold))
                .tracking(-0.4)
                .foregroundStyle(FolioSprite.ink)
            Text(scoreString(engine.score))
                .font(.system(size: 22, weight: .medium, design: .monospaced))
                .foregroundStyle(FolioSprite.ink)
            Text("toque para tentar de novo")
                .font(.system(size: 12))
                .tracking(0.2)
                .foregroundStyle(FolioSprite.ink.opacity(0.55))
                .padding(.top, 4)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        .padding(.bottom, 220)
        .allowsHitTesting(false)
    }

    private var closeButton: some View {
        VStack {
            HStack {
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(FolioSprite.ink.opacity(0.6))
                        .frame(width: 32, height: 32)
                        .background(
                            Circle().fill(FolioSprite.ink.opacity(0.06))
                        )
                }
                Spacer()
            }
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
    }

    private func scoreString(_ s: Int) -> String { String(format: "%05d", s) }
}

// MARK: - Engine

@MainActor
final class FolioRunnerEngine {
    // Plain class (not @Observable). The TimelineView ticks the view
    // body 60×/sec, so reads of `phase`, `score`, etc. always see fresh
    // values without relying on SwiftUI's observation graph — and Canvas
    // mutations during render don't ripple into spurious view invalidations.
    enum Phase { case ready, playing, gameOver }

    private(set) var phase: Phase = .ready
    private(set) var score: Int = 0
    private(set) var bestScore: Int

    // Folio kinematics — y is vertical offset above the ground in points,
    // velocity is in points/second.
    private var folioY: CGFloat = 0
    private var folioVelocity: CGFloat = 0
    private var isDucking = false

    private var obstacles: [Obstacle] = []
    private var nextSpawnIn: TimeInterval = 1.2

    private var elapsed: TimeInterval = 0
    private var lastTickAt: Date?
    private var groundOffset: CGFloat = 0
    private var cloudOffset: CGFloat = 0
    private var runFrame: Bool = false
    private var runFlipIn: TimeInterval = 0.13
    private var restartLockUntil: Date?

    private static let bestScoreKey = "FolioRunner.bestScore"
    private static let folioSize: CGFloat = 92
    private static let gravity: CGFloat = 1900
    private static let jumpVelocity: CGFloat = 760

    init() {
        bestScore = UserDefaults.standard.integer(forKey: Self.bestScoreKey)
    }

    private struct Obstacle: Identifiable {
        enum Kind { case books, plane }
        let id = UUID()
        var x: CGFloat
        let kind: Kind
    }

    // MARK: Input

    func tap() {
        switch phase {
        case .ready:
            phase = .playing
            jump()
        case .playing:
            jump()
        case .gameOver:
            // Brief grace period after death so the deciding tap doesn't
            // immediately restart and drop you onto the same obstacle.
            if let until = restartLockUntil, Date() < until { return }
            reset()
            phase = .playing
        }
    }

    func beginDuck() {
        guard phase == .playing else { return }
        isDucking = true
    }

    func endDuck() { isDucking = false }

    private func jump() {
        guard folioY <= 0.001 else { return }
        folioVelocity = Self.jumpVelocity
    }

    private func reset() {
        score = 0
        elapsed = 0
        folioY = 0
        folioVelocity = 0
        isDucking = false
        obstacles.removeAll()
        nextSpawnIn = 1.2
        groundOffset = 0
        cloudOffset = 0
        runFrame = false
        runFlipIn = 0.13
        lastTickAt = nil
    }

    // MARK: Tick

    func advance(to now: Date, size: CGSize) {
        let dt: TimeInterval
        if let last = lastTickAt {
            // Cap dt so a stutter doesn't teleport obstacles through Folio.
            dt = min(now.timeIntervalSince(last), 1.0 / 30.0)
        } else {
            dt = 0
        }
        lastTickAt = now

        guard phase == .playing else { return }
        elapsed += dt
        let speed = currentSpeed()

        folioVelocity -= Self.gravity * CGFloat(dt)
        folioY = max(0, folioY + folioVelocity * CGFloat(dt))
        if folioY == 0 { folioVelocity = 0 }

        groundOffset = (groundOffset + speed * CGFloat(dt))
            .truncatingRemainder(dividingBy: 24)
        cloudOffset += speed * 0.18 * CGFloat(dt)

        runFlipIn -= dt
        if runFlipIn <= 0 {
            runFrame.toggle()
            runFlipIn = 0.12
        }

        for i in obstacles.indices {
            obstacles[i].x -= speed * CGFloat(dt)
        }
        obstacles.removeAll { $0.x < -60 }

        nextSpawnIn -= dt
        if nextSpawnIn <= 0 {
            spawnObstacle(in: size)
            let s = Double(speed)
            let base = 320.0 / s
            nextSpawnIn = TimeInterval.random(in: base ... (base + 0.8))
        }

        let folioBox = folioCollisionRect(in: size)
        for obs in obstacles {
            if folioBox.intersects(obstacleCollisionRect(obs, in: size)) {
                gameOver()
                return
            }
        }

        score = Int(elapsed * 12)
    }

    private func currentSpeed() -> CGFloat {
        // Linear ramp from 230pt/s → ~545pt/s over 90s, then flat.
        230 + CGFloat(min(elapsed, 90)) * 3.5
    }

    private func spawnObstacle(in size: CGSize) {
        // Books (ground) only for the first stretch; planes (sky) mix in
        // once the speed has ramped enough that ducking matters.
        let kind: Obstacle.Kind
        if elapsed < 8 {
            kind = .books
        } else {
            kind = Int.random(in: 0 ..< 3) == 0 ? .plane : .books
        }
        obstacles.append(Obstacle(x: size.width + 40, kind: kind))
    }

    private func gameOver() {
        phase = .gameOver
        // Snap to the ground so a death mid-jump doesn't leave Folio
        // floating in a squashed pose.
        folioY = 0
        folioVelocity = 0
        isDucking = false
        if score > bestScore {
            bestScore = score
            UserDefaults.standard.set(score, forKey: Self.bestScoreKey)
        }
        restartLockUntil = Date().addingTimeInterval(0.6)
    }

    // MARK: Geometry

    private func groundLine(in size: CGSize) -> CGFloat { size.height * 0.78 }

    private func folioCollisionRect(in size: CGSize) -> CGRect {
        let footY = groundLine(in: size)
        let cx: CGFloat = 84
        if isDucking && phase == .playing {
            // Ducking shrinks the hitbox to roughly the lower half of the
            // sprite so the plane (sky obstacle) clears overhead.
            let w: CGFloat = 56
            let h: CGFloat = 38
            return CGRect(x: cx - w / 2, y: footY - h - 4, width: w, height: h)
        } else {
            let w: CGFloat = 50
            let h: CGFloat = 64
            let topY = footY - h - 6 - folioY
            return CGRect(x: cx - w / 2, y: topY, width: w, height: h)
        }
    }

    private func obstacleCollisionRect(_ obs: Obstacle, in size: CGSize) -> CGRect {
        let footY = groundLine(in: size)
        switch obs.kind {
        case .books:
            return CGRect(x: obs.x - 14, y: footY - 42, width: 28, height: 42)
        case .plane:
            return CGRect(x: obs.x - 16, y: footY - 84, width: 32, height: 18)
        }
    }

    // MARK: Render

    func render(into ctx: GraphicsContext, size: CGSize) {
        let footY = groundLine(in: size)

        drawClouds(into: ctx, size: size)

        // Ground line.
        var ground = Path()
        ground.move(to: CGPoint(x: 0, y: footY))
        ground.addLine(to: CGPoint(x: size.width, y: footY))
        ctx.stroke(
            ground, with: .color(FolioSprite.ink.opacity(0.35)),
            style: StrokeStyle(lineWidth: 1)
        )

        // Hash marks below the ground line (the dino-game scrolling
        // texture). Each tick is 8pt wide on a 24pt cycle.
        var marks = Path()
        var x = -groundOffset
        while x < size.width {
            marks.move(to: CGPoint(x: x, y: footY + 5))
            marks.addLine(to: CGPoint(x: x + 8, y: footY + 5))
            x += 24
        }
        ctx.stroke(
            marks, with: .color(FolioSprite.ink.opacity(0.25)),
            style: StrokeStyle(lineWidth: 1, lineCap: .round)
        )

        // Sparse pebbles, scrolled slightly faster than the hash marks for
        // a parallax cue.
        let pebbleSpan: CGFloat = size.width + 60
        for i in 0 ..< 4 {
            var px = (CGFloat(i) * 180 - groundOffset * 3.2)
                .truncatingRemainder(dividingBy: pebbleSpan)
            if px < 0 { px += pebbleSpan }
            ctx.fill(
                Path(ellipseIn: CGRect(x: px, y: footY + 9, width: 4, height: 2)),
                with: .color(FolioSprite.ink.opacity(0.4))
            )
        }

        for obs in obstacles {
            drawObstacle(obs, into: ctx, size: size)
        }

        let pose = currentPose()
        // Duck pose is drawn slightly larger so the squashed paper still
        // reads at the same visual weight as the upright run.
        let baseSize: CGFloat = (pose == .duck) ? Self.folioSize * 1.05 : Self.folioSize
        let cx: CGFloat = 84
        let extraDuckDrop: CGFloat = (pose == .duck) ? 6 : 0
        let yTop = footY - baseSize + 16 - folioY + extraDuckDrop
        FolioSprite.draw(
            in: ctx,
            pose: pose,
            frame: CGRect(x: cx - baseSize / 2, y: yTop, width: baseSize, height: baseSize)
        )
    }

    private func currentPose() -> FolioPose {
        switch phase {
        case .ready:
            return .idle
        case .gameOver:
            return .duck
        case .playing:
            if folioY > 0 { return .jump }
            if isDucking { return .duck }
            return runFrame ? .runA : .runB
        }
    }

    private func drawClouds(into ctx: GraphicsContext, size: CGSize) {
        // Two slow-moving paper-cutout clouds. The shape is a single
        // bumpy curve closed along the bottom — cheap and reads as a
        // friendly cloud at small sizes.
        func cloud(at origin: CGPoint, scale: CGFloat) -> Path {
            Path { p in
                let x = origin.x, y = origin.y
                p.move(to: CGPoint(x: x + 6 * scale, y: y + 14 * scale))
                p.addQuadCurve(
                    to: CGPoint(x: x + 14 * scale, y: y + 6 * scale),
                    control: CGPoint(x: x + 4 * scale, y: y + 4 * scale)
                )
                p.addQuadCurve(
                    to: CGPoint(x: x + 28 * scale, y: y + 4 * scale),
                    control: CGPoint(x: x + 20 * scale, y: y - 2 * scale)
                )
                p.addQuadCurve(
                    to: CGPoint(x: x + 44 * scale, y: y + 8 * scale),
                    control: CGPoint(x: x + 40 * scale, y: y - 1 * scale)
                )
                p.addQuadCurve(
                    to: CGPoint(x: x + 50 * scale, y: y + 14 * scale),
                    control: CGPoint(x: x + 52 * scale, y: y + 8 * scale)
                )
                p.closeSubpath()
            }
        }
        let span = size.width + 120
        var ax = (140 - cloudOffset).truncatingRemainder(dividingBy: span)
        if ax < -60 { ax += span }
        var bx = (-180 - cloudOffset * 1.5).truncatingRemainder(dividingBy: span)
        if bx < -60 { bx += span }

        ctx.fill(
            cloud(at: CGPoint(x: ax, y: 90), scale: 1.1),
            with: .color(FolioSprite.ink.opacity(0.16))
        )
        ctx.fill(
            cloud(at: CGPoint(x: bx, y: 140), scale: 0.9),
            with: .color(FolioSprite.ink.opacity(0.12))
        )
    }

    private func drawObstacle(_ obs: Obstacle, into ctx: GraphicsContext, size: CGSize) {
        let footY = groundLine(in: size)
        let inkStroke = StrokeStyle(lineWidth: 1.5, lineJoin: .round)

        switch obs.kind {
        case .books:
            // A small stack of textbooks in the brand palette — each
            // slightly narrower than the one below it.
            let books: [(rect: CGRect, color: Color)] = [
                (CGRect(x: obs.x - 16, y: footY - 14, width: 32, height: 14), FolioSprite.coral),
                (CGRect(x: obs.x - 14, y: footY - 28, width: 28, height: 14), FolioSprite.amber),
                (CGRect(x: obs.x - 12, y: footY - 42, width: 24, height: 14), FolioSprite.plum),
            ]
            for book in books {
                let path = Path(roundedRect: book.rect, cornerRadius: 2)
                ctx.fill(path, with: .color(book.color))
                ctx.stroke(path, with: .color(FolioSprite.ink), style: inkStroke)
                // A thin accent stripe near the spine for a book-cover cue.
                var stripe = Path()
                stripe.move(to: CGPoint(x: book.rect.minX + 4, y: book.rect.minY + 4))
                stripe.addLine(to: CGPoint(x: book.rect.minX + 4, y: book.rect.maxY - 4))
                ctx.stroke(
                    stripe, with: .color(FolioSprite.cream.opacity(0.7)),
                    style: StrokeStyle(lineWidth: 1.5, lineCap: .round)
                )
            }

        case .plane:
            // Paper airplane gliding nose-left.
            let baseY = footY - 78
            var body = Path()
            body.move(to: CGPoint(x: obs.x - 18, y: baseY + 9))
            body.addLine(to: CGPoint(x: obs.x + 16, y: baseY))
            body.addLine(to: CGPoint(x: obs.x + 4, y: baseY + 9))
            body.addLine(to: CGPoint(x: obs.x + 16, y: baseY + 18))
            body.closeSubpath()
            ctx.fill(body, with: .color(FolioSprite.cream))
            ctx.stroke(body, with: .color(FolioSprite.ink), style: inkStroke)

            var crease = Path()
            crease.move(to: CGPoint(x: obs.x + 16, y: baseY))
            crease.addLine(to: CGPoint(x: obs.x + 4, y: baseY + 9))
            ctx.stroke(
                crease, with: .color(FolioSprite.ink.opacity(0.4)),
                style: StrokeStyle(lineWidth: 1)
            )
        }
    }
}

#Preview {
    FolioRunnerView()
}
