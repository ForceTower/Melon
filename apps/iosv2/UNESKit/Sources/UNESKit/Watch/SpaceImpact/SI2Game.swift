#if os(watchOS)
import Foundation

// MARK: - Data model

/// A monochrome sprite. Rows use `#` for lit pixels so the generated data
/// file reads as pixel art.
struct SI2Sprite: Sendable {
    let width: Int
    let height: Int
    let bits: [Bool]

    init(_ rows: [String]) {
        width = rows.first?.count ?? 0
        height = rows.count
        bits = rows.flatMap { row in row.map { $0 == "#" } }
    }
}

/// An enemy definition (`data/enemies/*.dat`). `lives == 127` marks the
/// bonus-weapon carrier; crashing into it re-arms the player instead of hurting.
struct SI2EnemyType: Sendable {
    let model: Int
    let animCount: Int
    let lives: Int
    let floats: Bool
    let shotTime: Int
    let movesUp: Bool
    let movesDown: Bool
    let movesOffScreen: Bool
    let topBound: Int
    let bottomBound: Int
}

/// One spawn entry of a level (`data/levels/*.dat`).
struct SI2Spawn: Sendable {
    let x: Int
    let y: Int
    let enemy: Int
    let direction: Int
}

// MARK: - Game

/// Faithful Swift port of the original Space Impact II game loop (main.c,
/// enemies.c, shotlist.c, scenery.c), running the real levels on the real
/// 84×48 LCD, with the watch control scheme from the v2-alt-mix design:
/// the digital crown flies the ship, the standard cannon auto-fires, and a
/// tap spends the bonus weapon (the original's Ctrl: missile / beam / wall).
struct SI2Game {
    static let screenWidth = 84
    static let screenHeight = 48
    static let ticksPerSecond: Double = 18 // the original runs at ~18fps

    enum Phase: Equatable {
        case intro
        case playing
        case gameOver
    }

    enum Event: Equatable {
        case started
        case bonusFired
        case lifeLost
        case gameEnded(newRecord: Bool)
    }

    struct Player {
        var x = 3
        var y = 20
        var lives = 3
        var score = 0
        var bonus = 3
        /// Bonus weapon kind: 1 missile, 2 beam, 3 wall (0 is the standard shot).
        var weapon = 1
        /// Frames of spawn protection left; blinks and blocks damage.
        var protection = 50
    }

    struct Shot {
        var x: Int
        var y: Int
        let velocity: Int
        let fromPlayer: Bool
        let kind: Int
        var damage: Int
        let width: Int
        let height: Int
        var dead = false
        var hitPlayer = false
    }

    struct ActiveEnemy {
        let id: Int
        var x: Int
        var y: Int
        let type: SI2EnemyType
        let width: Int
        let height: Int
        var animState = 0
        var lives: Int
        let maxLives: Int
        var moveDir: Int
        var cooldown: Int
        /// Copies of the type's flags, zeroed on death so explosions don't drift.
        var movesUp: Bool
        var movesDown: Bool
    }

    struct SceneryItem {
        var x: Int
        let object: Int
    }

    private struct ScenerySet {
        let firstObject: Int
        let count: Int
        let upper: Bool
    }

    /// Per-level scenery bands (scenery.c ScData); level 0 flies in open space.
    private static let scenery: [ScenerySet?] = [
        nil,
        ScenerySet(firstObject: 0, count: 2, upper: false),
        ScenerySet(firstObject: 2, count: 6, upper: false),
        ScenerySet(firstObject: 8, count: 6, upper: false),
        ScenerySet(firstObject: 14, count: 4, upper: true),
        ScenerySet(firstObject: 14, count: 4, upper: true),
    ]

    /// Damage and size per weapon kind: standard, missile, beam, wall.
    private static let shotDamage = [1, 3, 10, 25]
    private static let shotSize = [(3, 1), (5, 3), (84, 3), (1, 43)]

    private(set) var phase = Phase.intro
    private(set) var player = Player()
    private(set) var level = 0
    private(set) var best: Int
    private(set) var newRecord = false
    /// The current frame, row-major; `inverted` levels light the background.
    private(set) var pixels = [Bool](repeating: false, count: screenWidth * screenHeight)
    private(set) var inverted = false
    /// Bumped whenever the game repositions the ship, so the crown can re-sync.
    private(set) var shipResets = 0
    /// Haptic-worthy moments since the last drain.
    private(set) var events: [Event] = []

    private var shots: [Shot] = []
    private var enemies: [ActiveEnemy] = []
    private var sceneryItems: [SceneryItem] = []
    private var moveScene = true
    private var animPulse = false
    private var shootTimer = 0
    private var bossId: Int?
    private var nextEnemyId = 0
    private var introPhase = 12
    private var introHold = 3

    init(best: Int) {
        self.best = best
    }

    /// The level boss while it's alive and on screen — drives the HUD bar.
    var boss: ActiveEnemy? {
        guard let bossId else { return nil }
        guard let boss = enemies.first(where: { $0.id == bossId }),
              boss.lives > 0, boss.x < Self.screenWidth else { return nil }
        return boss
    }

    /// Whether crown input should steer the ship (not during flyouts or menus).
    var acceptsShipInput: Bool {
        phase == .playing && !enemies.isEmpty
    }

    mutating func drainEvents() -> [Event] {
        defer { events.removeAll() }
        return events
    }

    // MARK: Input

    /// Tap: skip the intro, restart after game over, or fire the bonus weapon.
    mutating func tap() {
        switch phase {
        case .intro:
            startPlay()
        case .gameOver:
            startPlay()
        case .playing:
            guard player.bonus > 0 else { return }
            addShot(
                x: player.x + 9,
                y: player.weapon == 3 ? 5 : player.y + 2,
                velocity: player.weapon == 2 ? 0 : 2,
                fromPlayer: true,
                kind: player.weapon
            )
            player.bonus -= 1
            events.append(.bonusFired)
        }
    }

    /// Crown: place the ship at an absolute height (clamped like the design's
    /// `moveShip`; the tick re-clamps into the level's playable band).
    mutating func setShipY(_ y: Int) {
        guard acceptsShipInput else { return }
        player.y = min(max(y, 0), Self.screenHeight - 1)
    }

    // MARK: Lifecycle

    private mutating func startPlay() {
        phase = .playing
        level = 0
        player = Player()
        newRecord = false
        shots = []
        sceneryItems = []
        moveScene = true
        shootTimer = 0
        spawnLevel(0)
        shipResets += 1
        events.append(.started)
    }

    private mutating func spawnLevel(_ level: Int) {
        enemies = []
        for spawn in SI2Data.levels[level] {
            addEnemy(spawn)
        }
        // The level's boss is its last (rightmost) spawn — far more lives
        // than any grunt.
        bossId = enemies.last.flatMap { $0.maxLives >= 20 ? $0.id : nil }
    }

    private mutating func addEnemy(_ spawn: SI2Spawn) {
        guard let type = SI2Data.enemies[spawn.enemy],
              let model = SI2Data.objects[type.model] else { return }
        enemies.append(ActiveEnemy(
            id: nextEnemyId,
            x: spawn.x,
            y: spawn.y,
            type: type,
            width: model.width,
            height: model.height,
            lives: type.lives,
            maxLives: type.lives,
            moveDir: spawn.direction,
            cooldown: type.shotTime,
            movesUp: type.movesUp,
            movesDown: type.movesDown
        ))
        nextEnemyId += 1
    }

    private mutating func endGame() {
        newRecord = player.score > 0 && player.score > best
        best = max(best, player.score)
        phase = .gameOver
        events.append(.gameEnded(newRecord: newRecord))
    }

    // MARK: Tick

    mutating func tick() {
        pixels = [Bool](repeating: false, count: Self.screenWidth * Self.screenHeight)
        inverted = false
        switch phase {
        case .intro:
            tickIntro()
        case .gameOver:
            break // the game-over screen is a watch overlay, not LCD pixels
        case .playing:
            tickPlay()
        }
    }

    private mutating func tickIntro() {
        draw(SI2Data.introSpace, x: 8, y: 12 - introPhase)
        draw(SI2Data.introImpact, x: 4, y: 24 + introPhase)
        drawOutlined(SI2Data.introShips, x: 56 - introPhase * 4, y: 20)
        guard introHold > 0 else { return }
        introHold -= 1
        if introHold == 0 {
            introPhase -= 1
            if introPhase <= 0 {
                startPlay()
            } else {
                introHold = introPhase == 1 ? Int(Self.ticksPerSecond) : 2
            }
        }
    }

    private mutating func tickPlay() {
        // On levels 4 and 5 the scenery hangs from the top and the playable
        // band shifts down; those levels (and level 0) render inverted.
        let openBand = level < 4 || level > 5
        let startLives = player.lives

        tickShots()
        if !enemies.isEmpty {
            let margin = player.protection > 0 ? 2 : 0
            let top = (openBand ? 5 : 0) + margin
            let bottom = 36 + (openBand ? 5 : 0) - margin
            player.y = min(max(player.y, top), bottom)
        } else {
            // Level complete: fly out right, then load the next level.
            shots = []
            if player.x > Self.screenWidth {
                player.x = 3
                player.y = 20
                shipResets += 1
                shootTimer = 0
                level += 1
                if level >= SI2Data.levels.count {
                    endGame()
                    return
                }
                spawnLevel(level)
                sceneryItems = []
                moveScene = true
            } else {
                let outY = openBand ? 10 : 31
                if player.y < outY {
                    player.y += 1
                } else if player.y > outY {
                    player.y -= 1
                } else {
                    player.x += 3
                }
            }
        }

        if player.protection > 0 {
            let blink = (player.protection / 2) % 2
            if let sprite = SI2Data.objects[250 + blink] {
                draw(sprite, x: player.x - 2, y: player.y - 2)
            }
        } else if let ship = SI2Data.objects[255] {
            draw(ship, x: player.x, y: player.y)
        }

        // The standard cannon auto-fires (watch adaptation).
        if shootTimer > 0 { shootTimer -= 1 }
        if !enemies.isEmpty, shootTimer == 0 {
            addShot(x: player.x + 9, y: player.y + 3, velocity: 2, fromPlayer: true, kind: 0)
            shootTimer = 4
        }

        animPulse.toggle()
        tickEnemies()
        tickScenery()

        inverted = level == 0 || !openBand
        if inverted {
            for index in pixels.indices {
                pixels[index].toggle()
            }
        }

        if player.protection > 0 { player.protection -= 1 }
        if player.lives <= 0 {
            endGame()
        } else if player.lives != startLives {
            player.x = 3
            player.y = 20
            player.protection = 50
            shipResets += 1
            events.append(.lifeLost)
        }
    }

    // MARK: Shots (shotlist.c)

    private mutating func addShot(x: Int, y: Int, velocity: Int, fromPlayer: Bool, kind: Int) {
        let (width, height) = Self.shotSize[kind]
        shots.append(Shot(
            x: x,
            y: y,
            velocity: velocity,
            fromPlayer: fromPlayer,
            kind: kind,
            damage: Self.shotDamage[kind],
            width: width,
            height: height
        ))
    }

    private mutating func tickShots() {
        for index in shots.indices {
            var shot = shots[index]
            shot.hitPlayer = !shot.fromPlayer && intersects(
                shot.x, shot.y, shot.width, shot.height,
                player.x, player.y, 10, 7
            )
            shot.x += shot.velocity
            if shot.hitPlayer, player.protection == 0 {
                player.lives -= 1
            }
            shots[index] = shot
        }
        // Opposing shots damage each other on contact.
        for index in shots.indices where !shots[index].dead {
            for otherIndex in shots.indices {
                guard otherIndex != index, !shots[otherIndex].dead else { continue }
                guard !(shots[index].fromPlayer && shots[otherIndex].fromPlayer) else { continue }
                if intersects(
                    shots[index].x, shots[index].y, shots[index].width, shots[index].height,
                    shots[otherIndex].x, shots[otherIndex].y, shots[otherIndex].width, shots[otherIndex].height
                ) {
                    shots[index].damage -= 1
                    shots[otherIndex].damage -= 1
                    if shots[otherIndex].damage <= 0 {
                        shots[otherIndex].dead = true
                    }
                    break
                }
            }
        }
        shots.removeAll { $0.dead || $0.x < -2 || $0.x > 83 || $0.hitPlayer || $0.damage <= 0 }
        for shot in shots {
            let sprite = shot.kind == 0 ? SI2Data.shot : SI2Data.objects[252 + shot.kind - 1]
            if let sprite {
                draw(sprite, x: shot.x, y: shot.y)
            }
        }
    }

    // MARK: Enemies (enemies.c)

    private mutating func tickEnemies() {
        var index = 0
        while index < enemies.count {
            var enemy = enemies[index]
            var alive = enemy.lives > 0
            let inScreen = enemy.x <= 60
            if index == enemies.count - 1, inScreen {
                moveScene = false // the boss arrived — the scenery halts
            }
            if enemy.movesUp || enemy.movesDown, inScreen || enemy.type.movesOffScreen {
                if enemy.moveDir == 1, enemy.y == enemy.type.bottomBound {
                    enemy.moveDir = enemy.movesUp ? -1 : 0
                } else if enemy.moveDir == -1, enemy.y == enemy.type.topBound {
                    enemy.moveDir = enemy.movesDown ? 1 : 0
                }
                enemy.y += enemy.moveDir
            }
            if !enemy.type.floats || !inScreen {
                enemy.x -= 1
            }
            if animPulse {
                enemy.animState = (enemy.animState + 1) % max(enemy.type.animCount, 1)
            }

            if alive {
                for shotIndex in shots.indices where shots[shotIndex].fromPlayer && !shots[shotIndex].dead {
                    let shot = shots[shotIndex]
                    guard intersects(
                        shot.x, shot.y, shot.width, shot.height,
                        enemy.x, enemy.y, enemy.width, enemy.height
                    ) else { continue }
                    if enemy.type.lives == 127 {
                        // Bonus carrier: shooting it just converts damage to points.
                        player.score += shot.damage * 5
                        shots[shotIndex].dead = true
                    } else {
                        enemy.lives -= shot.damage
                        player.score += 5
                        if enemy.lives < 0 {
                            shots[shotIndex].damage = -enemy.lives
                            enemy.lives = 0
                        } else {
                            shots[shotIndex].dead = true
                        }
                        if enemy.lives == 0 {
                            player.score += 5
                            alive = false
                            enemy.movesUp = false
                            enemy.movesDown = false
                        }
                    }
                    break
                }
            }
            if alive, intersects(player.x, player.y, 10, 7, enemy.x, enemy.y, enemy.width, enemy.height) {
                alive = false
                if enemy.type.lives == 127 {
                    let kind = Int.random(in: 1...3)
                    if kind != player.weapon {
                        player.bonus = 0
                        player.weapon = kind
                    }
                    player.bonus += 4 - kind // 3 missiles, 2 beams or 1 wall
                    enemy.lives = -2
                } else if player.lives > 0, player.protection == 0 {
                    player.lives -= 1
                }
            }

            if enemy.x < -enemy.width || !alive {
                enemy.lives -= 1
                if enemy.lives == -3 {
                    enemies.remove(at: index)
                    continue
                }
                // Explosion phases map to lives -1 and -2.
                let explosion = enemy.lives == -1 ? SI2Data.explosion1 : SI2Data.explosion2
                if enemy.lives == -1 || enemy.lives == -2 {
                    let centerX = enemy.x + enemy.width / 2
                    let centerY = enemy.y + enemy.height / 2
                    draw(explosion, x: centerX - 3 - enemy.lives, y: centerY - 2)
                }
            } else if enemy.x < Self.screenWidth {
                if let sprite = SI2Data.objects[enemy.type.model + enemy.animState] {
                    draw(sprite, x: enemy.x, y: enemy.y)
                }
                if enemy.type.shotTime > 0 {
                    enemy.cooldown -= 1
                    if enemy.cooldown == 0 {
                        addShot(
                            x: enemy.x - 1,
                            y: enemy.y + enemy.height / 2,
                            velocity: -2,
                            fromPlayer: false,
                            kind: 0
                        )
                        enemy.cooldown = enemy.type.shotTime
                    }
                }
            }
            enemies[index] = enemy
            index += 1
        }

        // Missiles home toward the nearest enemy ahead; beams live one frame.
        for index in shots.indices where shots[index].kind == 1 {
            var target = shots[index].y
            var bestX = Int.max
            for enemy in enemies where enemy.x > shots[index].x && enemy.x < bestX {
                bestX = enemy.x
                target = enemy.y
            }
            if shots[index].y < target {
                shots[index].y += 1
            } else if shots[index].y > target {
                shots[index].y -= 1
            }
        }
        shots.removeAll { $0.kind == 2 }
    }

    // MARK: Scenery (scenery.c)

    private mutating func tickScenery() {
        var lastX = 0
        var kept: [SceneryItem] = []
        for var item in sceneryItems {
            if moveScene {
                item.x -= 1
            }
            guard let sprite = SI2Data.objects[item.object] else { continue }
            let y = (Self.scenery[level]?.upper ?? false) ? 0 : Self.screenHeight - sprite.height
            // Terrain kills through protection — but the cloud level is soft.
            if level != 1, intersects(item.x, y, sprite.width, sprite.height, player.x, player.y, 10, 7) {
                player.lives -= 1
            }
            if item.x >= -sprite.width {
                lastX = item.x + sprite.width
                draw(sprite, x: item.x, y: y)
                kept.append(item)
            }
        }
        sceneryItems = kept
        guard level != 0, let set = Self.scenery[level] else { return }
        while lastX < Self.screenWidth {
            let object = set.firstObject + Int.random(in: 0..<set.count)
            guard let sprite = SI2Data.objects[object] else { return }
            sceneryItems.append(SceneryItem(x: lastX, object: object))
            lastX += sprite.width
        }
    }

    // MARK: Drawing (graphics.c)

    private mutating func draw(_ sprite: SI2Sprite, x: Int, y: Int) {
        for row in 0..<sprite.height {
            let screenY = y + row
            guard screenY >= 0, screenY < Self.screenHeight else { continue }
            for column in 0..<sprite.width where sprite.bits[row * sprite.width + column] {
                let screenX = x + column
                if screenX >= 0, screenX < Self.screenWidth {
                    pixels[screenY * Self.screenWidth + screenX] = true
                }
            }
        }
    }

    /// Draws with a one-pixel cleared outline (the intro ships fly over text).
    private mutating func drawOutlined(_ sprite: SI2Sprite, x: Int, y: Int) {
        for row in 0..<sprite.height {
            for column in 0..<sprite.width where sprite.bits[row * sprite.width + column] {
                for dy in -1...1 {
                    for dx in -1...1 {
                        let screenX = x + column + dx
                        let screenY = y + row + dy
                        if screenX >= 0, screenX < Self.screenWidth, screenY >= 0, screenY < Self.screenHeight {
                            pixels[screenY * Self.screenWidth + screenX] = false
                        }
                    }
                }
            }
        }
        draw(sprite, x: x, y: y)
    }

    private func intersects(
        _ ax: Int, _ ay: Int, _ aw: Int, _ ah: Int,
        _ bx: Int, _ by: Int, _ bw: Int, _ bh: Int
    ) -> Bool {
        !(ax > bx + bw - 1 || ay > by + bh - 1 || ax + aw - 1 < bx || ay + ah - 1 < by)
    }
}
#endif
