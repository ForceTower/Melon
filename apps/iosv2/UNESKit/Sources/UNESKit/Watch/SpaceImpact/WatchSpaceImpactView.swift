#if os(watchOS)
import ComposableArchitecture
import SwiftUI
import WatchKit

/// The Space Impact easter egg ("UNES Destroyer"): the original game on its
/// 84×48 LCD, framed by the watch HUD from the v2-alt-mix design. Crown flies
/// the ship, the cannon auto-fires, a tap spends the bonus weapon.
struct WatchSpaceImpactView: View {
    @State private var model = SI2GameModel()
    @State private var crownY = 20.0
    @Dependency(\.watchRepository) private var watchRepository

    /// Watch HUD palette from the design (ink-on-dark, independent of theme).
    private static let background = Color(hex: 0x0A0710)
    private static let ink = Color(hex: 0xF5EFE6)
    private static let dim = Color(hex: 0x9F9386)

    var body: some View {
        ZStack {
            Self.background.ignoresSafeArea()
            if model.game.phase == .gameOver {
                gameOver
            } else {
                hud
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { model.tap() }
        .focusable()
        .digitalCrownRotation(
            $crownY,
            from: 0,
            through: Double(SI2Game.screenHeight - 1),
            by: 0.5,
            sensitivity: .medium,
            isContinuous: false,
            isHapticFeedbackEnabled: false
        )
        .onChange(of: crownY) { _, y in
            model.setShipY(Int(y.rounded()))
        }
        .onChange(of: model.game.shipResets) {
            crownY = Double(model.game.player.y)
        }
        .task {
            // Finding the game unlocks the NowShip icon on the phone.
            watchRepository.reportSpaceImpactOpened()
            await model.run()
        }
    }

    private var hud: some View {
        // The LCD fills the watch's width, like the design's ×5-scaled band.
        GeometryReader { proxy in
            VStack(spacing: 0) {
                if model.game.phase == .playing {
                    topBar
                        .padding(.horizontal, 6)
                }
                Spacer(minLength: 0)
                lcd
                    .frame(
                        width: proxy.size.width,
                        height: proxy.size.width
                            * CGFloat(SI2Game.screenHeight) / CGFloat(SI2Game.screenWidth)
                    )
                Spacer(minLength: 0)
                if model.game.phase == .playing {
                    weaponBar
                        .padding(.horizontal, 6)
                    Text(.watchSi2BonusHint)
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(Self.dim.opacity(0.75))
                        .padding(.top, 3)
                }
            }
        }
    }

    private var topBar: some View {
        HStack {
            HStack(spacing: 3) {
                ForEach(0..<max(model.game.player.lives, 0), id: \.self) { _ in
                    SI2SpriteView(sprite: .heart, scale: 1.5, color: UNESColor.coral)
                }
            }
            .frame(minWidth: 34, alignment: .leading)
            Spacer(minLength: 0)
            // The center slot: the stage number, or the boss bar during the
            // boss fight (the watch lacks the design's spare row for it).
            if model.game.boss != nil {
                bossBar
            } else {
                Text(.watchSi2Phase(model.game.level + 1))
                    .font(.system(size: 11, weight: .semibold, design: .monospaced))
                    .foregroundStyle(Self.dim)
            }
            Spacer(minLength: 0)
            Text(verbatim: Self.padded(model.game.player.score))
                .font(.system(size: 14, weight: .bold, design: .monospaced))
                .foregroundStyle(Self.ink)
        }
    }

    private var bossBar: some View {
        VStack(spacing: 2) {
            Text(.watchSi2Boss)
                .font(.system(size: 9, weight: .bold, design: .monospaced))
                .foregroundStyle(UNESColor.coral)
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(.white.opacity(0.12))
                    Capsule()
                        .fill(UNESColor.coral)
                        .frame(width: proxy.size.width * bossHealth)
                }
            }
            .frame(width: 64, height: 4)
        }
    }

    private var bossHealth: Double {
        guard let boss = model.game.boss else { return 0 }
        return max(0, Double(boss.lives) / Double(boss.maxLives))
    }

    private var lcd: some View {
        Group {
            if let frame = model.frame {
                Image(decorative: frame, scale: 1)
                    .interpolation(.none)
                    .resizable()
            } else {
                Color.clear
            }
        }
        .overlay {
            // Faint LCD scanlines, like the design's overlay band.
            Canvas { context, size in
                var y: CGFloat = 0
                while y < size.height {
                    context.fill(
                        Path(CGRect(x: 0, y: y, width: size.width, height: 0.5)),
                        with: .color(.black.opacity(0.06))
                    )
                    y += 3
                }
            }
            .allowsHitTesting(false)
        }
        .border(.white.opacity(0.1), width: 0.5)
    }

    private var weaponBar: some View {
        HStack(spacing: 5) {
            Text(.watchSi2Weapon)
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .foregroundStyle(Self.dim)
            SI2SpriteView(sprite: weaponIcon, scale: 2, color: UNESColor.coral)
            Text(verbatim: "×\(model.game.player.bonus)")
                .font(.system(size: 13, weight: .bold, design: .monospaced))
                .foregroundStyle(Self.ink)
            Spacer(minLength: 0)
        }
    }

    private var weaponIcon: SI2Sprite {
        switch model.game.player.weapon {
        case 2: SI2Data.beamIcon
        case 3: SI2Data.wallIcon
        default: SI2Data.missileIcon
        }
    }

    private var gameOver: some View {
        VStack(spacing: 3) {
            Text(.watchSi2GameOver)
                .font(.system(size: 16, weight: .bold, design: .monospaced))
                .foregroundStyle(UNESColor.coral)
            Text(verbatim: Self.padded(model.game.player.score))
                .font(.system(size: 30, weight: .bold, design: .monospaced))
                .foregroundStyle(Self.ink)
                .padding(.top, 6)
            Text(.watchSi2Points)
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .foregroundStyle(Self.dim)
            Group {
                if model.game.newRecord {
                    Text(.watchSi2NewRecord(Self.padded(model.game.best)))
                        .foregroundStyle(UNESColor.liveGreen)
                } else {
                    Text(.watchSi2Record(Self.padded(model.game.best)))
                        .foregroundStyle(Self.dim)
                }
            }
            .font(.system(size: 11, weight: .semibold, design: .monospaced))
            .padding(.top, 8)
            Text(.watchSi2TapToPlay)
                .font(.system(size: 13, weight: .bold, design: .monospaced))
                .foregroundStyle(UNESColor.coral)
                .opacity((model.tickCount / 5) % 2 == 0 ? 1 : 0)
                .padding(.top, 10)
        }
    }

    private static func padded(_ score: Int) -> String {
        String(format: "%05d", score)
    }
}

// MARK: - Model

/// Runs the game at its original 18fps and turns the pixel buffer into the
/// LCD frame; the view stays a pure function of this model.
@MainActor
@Observable
final class SI2GameModel {
    private static let bestScoreKey = "spaceImpact.bestScore"

    /// White ("branco") LCD from the design tweaks: warm-white backlight,
    /// near-black ink; inverted levels light their pixels paper-white.
    private static let backlight: (UInt8, UInt8, UInt8) = (232, 233, 225)
    private static let ink: (UInt8, UInt8, UInt8) = (20, 21, 14)
    private static let litInverted: (UInt8, UInt8, UInt8) = (236, 237, 229)

    private(set) var game = SI2Game(
        best: UserDefaults.standard.integer(forKey: bestScoreKey)
    )
    private(set) var frame: CGImage?
    private(set) var tickCount = 0

    func run() async {
        let clock = ContinuousClock()
        var next = clock.now
        while !Task.isCancelled {
            game.tick()
            handleEvents()
            frame = Self.render(pixels: game.pixels, inverted: game.inverted)
            tickCount += 1
            next = max(next.advanced(by: .seconds(1 / SI2Game.ticksPerSecond)), clock.now)
            try? await clock.sleep(until: next)
        }
    }

    func tap() {
        game.tap()
        handleEvents()
    }

    func setShipY(_ y: Int) {
        game.setShipY(y)
    }

    private func handleEvents() {
        for event in game.drainEvents() {
            switch event {
            case .started:
                WKInterfaceDevice.current().play(.start)
            case .bonusFired:
                WKInterfaceDevice.current().play(.click)
            case .lifeLost:
                WKInterfaceDevice.current().play(.directionDown)
            case let .gameEnded(newRecord):
                WKInterfaceDevice.current().play(newRecord ? .success : .failure)
                UserDefaults.standard.set(game.best, forKey: Self.bestScoreKey)
            }
        }
    }

    private static func render(pixels: [Bool], inverted: Bool) -> CGImage? {
        let off = inverted ? litInverted : backlight
        var data = [UInt8]()
        data.reserveCapacity(pixels.count * 4)
        for lit in pixels {
            let color = lit ? ink : off
            data.append(contentsOf: [color.0, color.1, color.2, 255])
        }
        guard let provider = CGDataProvider(data: Data(data) as CFData) else { return nil }
        return CGImage(
            width: SI2Game.screenWidth,
            height: SI2Game.screenHeight,
            bitsPerComponent: 8,
            bitsPerPixel: 32,
            bytesPerRow: SI2Game.screenWidth * 4,
            space: CGColorSpace(name: CGColorSpace.sRGB)!,
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.noneSkipLast.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: false,
            intent: .defaultIntent
        )
    }
}

// MARK: - HUD sprites

/// Renders one of the game's monochrome sprites at an integer-ish pixel scale,
/// for HUD chrome (hearts, weapon icons) outside the LCD.
struct SI2SpriteView: View {
    let sprite: SI2Sprite
    let scale: CGFloat
    let color: Color

    var body: some View {
        Canvas { context, _ in
            for row in 0..<sprite.height {
                for column in 0..<sprite.width where sprite.bits[row * sprite.width + column] {
                    context.fill(
                        Path(CGRect(
                            x: CGFloat(column) * scale,
                            y: CGFloat(row) * scale,
                            width: scale,
                            height: scale
                        )),
                        with: .color(color)
                    )
                }
            }
        }
        .frame(
            width: CGFloat(sprite.width) * scale,
            height: CGFloat(sprite.height) * scale
        )
    }
}

extension SI2Sprite {
    /// The HUD heart from the design (not part of the original game data).
    static let heart = SI2Sprite([
        ".##.##.",
        "#######",
        "#######",
        ".#####.",
        "..###..",
        "...#...",
    ])
}

extension SI2Data {
    /// The player ship, which doubles as the easter egg's clue outside the game.
    static let playerShip = objects[255] ?? SI2Sprite([])
}

/// The easter egg's hidden trigger: a tiny player ship that launches the
/// game on a long-press.
struct SI2ShipTrigger: View {
    let color: Color
    let action: () -> Void

    var body: some View {
        SI2SpriteView(sprite: SI2Data.playerShip, scale: 1.2, color: color)
            .padding(4)
            .contentShape(Rectangle())
            .onLongPressGesture(perform: action)
            .accessibilityLabel(Text(.watchSi2Title))
            .accessibilityAddTraits(.isButton)
    }
}

#Preview {
    NavigationStack {
        WatchSpaceImpactView()
    }
}
#endif
