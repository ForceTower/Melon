import Foundation
import Observation
import OSLog
import SwiftUI
@preconcurrency import Umbrella

// Each UI section is driven by its own KMP Flow. `observe()` fans out with
// `async let` so SwiftUI's `.task` cancellation tears down every subscription
// together. The 30s `clock` tick is what refreshes clock-derived labels
// without round-tripping through Kotlin.
@MainActor
@Observable
final class OverviewViewModel {
    private(set) var userName: String?
    private(set) var now: OverviewNowClass?
    private(set) var today: [OverviewTodayItem] = []
    private(set) var disciplines: [OverviewDiscipline] = []
    private(set) var semesterLabel: String = ""
    private(set) var messagesTile: OverviewMessagesTileData?
    private(set) var nextTestTile: OverviewNextTestTileData?
    private(set) var attendanceTile: OverviewAttendanceTileData?
    // CR tile pending client-side calculation; keep nil so the tile renders
    // its empty state without extra flags.
    private(set) var gradeTile: OverviewGradeTileData?
    private(set) var lastSyncIso: String?
    // Ticked every 30s so greeting / date / relative labels refresh without
    // forcing KMP flows to re-emit.
    private(set) var clock: Date = Date()

    private let useCases: OverviewUseCases
    private var didStart = false

    private static let logger = Logger(subsystem: "dev.forcetower.melon", category: "overview")

    init(useCases: OverviewUseCases) {
        self.useCases = useCases
    }

    // SwiftUI's `.task { await viewModel.observe() }` cancels this when the
    // view disappears; each `async let` branch inherits that cancellation, so
    // every Flow subscription tears down together.
    func observe() async {
        guard !didStart else { return }
        didStart = true

        async let h: Void = observeHeader()
        async let n: Void = observeNowClass()
        async let t: Void = observeToday()
        async let d: Void = observeDisciplines()
        async let m: Void = observeMessagesTile()
        async let x: Void = observeNextTestTile()
        async let a: Void = observeAttendanceTile()
        async let l: Void = observeLastSync()
        async let c: Void = runClockTicker()

        _ = await (h, n, t, d, m, x, a, l, c)
    }

    private func observeHeader() async {
        for await value in useCases.header.invoke() {
            apply(header: value)
        }
    }

    private func observeNowClass() async {
        for await value in useCases.nowClass.invoke() {
            apply(nowClass: value)
        }
    }

    private func observeToday() async {
        for await value in useCases.today.invoke() {
            apply(today: value)
        }
    }

    private func observeDisciplines() async {
        for await value in useCases.disciplines.invoke() {
            apply(disciplines: value)
        }
    }

    private func observeMessagesTile() async {
        for await value in useCases.messagesTile.invoke() {
            apply(messagesTile: value)
        }
    }

    private func observeNextTestTile() async {
        for await value in useCases.nextTestTile.invoke() {
            apply(nextTestTile: value)
        }
    }

    private func observeAttendanceTile() async {
        for await value in useCases.attendanceTile.invoke() {
            apply(attendanceTile: value)
        }
    }

    private func observeLastSync() async {
        for await value in useCases.lastSync.invoke() {
            apply(lastSync: value)
        }
    }

    // Re-evaluates derived clock labels every 30s. Exits when the parent task
    // is cancelled.
    private func runClockTicker() async {
        while !Task.isCancelled {
            clock = Date()
            try? await Task.sleep(nanoseconds: 30 * 1_000_000_000)
        }
    }

    // MARK: - Slice appliers (MainActor)

    private func apply(header: OverviewOverviewHeader?) {
        userName = header?.userName
    }

    private func apply(nowClass: OverviewOverviewNowClass?) {
        guard let raw = nowClass else {
            self.now = nil
            return
        }
        self.now = OverviewNowClass(
            code: raw.code,
            title: raw.title,
            prof: raw.teacherName ?? "",
            room: raw.roomLocation ?? "",
            startsIn: Int(raw.startsInMinutes),
            time: Self.formatTimeRange(startTime: raw.startTime, endTime: raw.endTime),
            topic: raw.topic,
            color: ColorFor.discipline(code: raw.code),
            meshVariant: ColorFor.meshVariant(code: raw.code)
        )
    }

    private func apply(today: [OverviewOverviewTodayItem]) {
        self.today = today.map { raw in
            OverviewTodayItem(
                time: Self.trimTime(raw.startTime),
                code: raw.code,
                title: raw.title,
                room: raw.roomLocation ?? "",
                color: ColorFor.discipline(code: raw.code),
                state: Self.map(state: raw.state),
                topic: raw.topic
            )
        }
    }

    private func apply(disciplines: [OverviewOverviewDiscipline]) {
        self.disciplines = disciplines.map { raw in
            OverviewDiscipline(
                code: raw.code,
                title: raw.title,
                grade: raw.gradeLabel,
                color: ColorFor.discipline(code: raw.code),
                statusLabel: Self.statusLabel(for: raw.status)
            )
        }
        semesterLabel = disciplines.first?.semesterCode ?? semesterLabel
    }

    private func apply(messagesTile raw: OverviewOverviewMessagesTile) {
        messagesTile = OverviewMessagesTileData(
            unreadCount: Int(raw.unreadCount),
            lastSender: raw.lastSender,
            lastPreview: raw.lastPreview
        )
    }

    private func apply(nextTestTile raw: OverviewOverviewNextTestTile?) {
        guard let value = raw else {
            nextTestTile = nil
            return
        }
        nextTestTile = OverviewNextTestTileData(
            label: value.label,
            disciplineName: value.disciplineName,
            daysUntil: Int(value.daysUntil),
            dateLabel: Self.formatShortDate(iso: value.date)
        )
    }

    private func apply(attendanceTile raw: OverviewOverviewAttendanceTile) {
        attendanceTile = OverviewAttendanceTileData(
            percentage: raw.percentage.map { Int(truncating: $0) },
            days: raw.lastDays.map { $0.boolValue },
            allowedAbsences: Int(raw.allowedAbsences),
            periodDays: Int(raw.periodDays)
        )
    }

    private func apply(lastSync: String?) {
        lastSyncIso = lastSync
    }

    // MARK: - Derived state (read by the view)

    var avatarInitial: String {
        userName?.first.map { String($0).uppercased() } ?? "?"
    }

    var greeting: String {
        switch Calendar.current.component(.hour, from: clock) {
        case ..<12: return "Bom dia"
        case ..<18: return "Boa tarde"
        default: return "Boa noite"
        }
    }

    var dateEyebrow: String {
        Self.eyebrowFormatter.string(from: clock)
            .replacingOccurrences(of: "-feira", with: "")
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    // "◦ ATUALIZADO HÁ 2 MIN ◦" or fallback while no sync has landed.
    var lastUpdatedLabel: String {
        guard let iso = lastSyncIso,
              let relative = Self.formatRelative(iso: iso, against: clock)
        else { return "◦ SINCRONIZANDO ◦" }
        return "◦ ATUALIZADO \(relative.uppercased()) ◦"
    }

    // MARK: - Formatting helpers

    private static func formatTimeRange(startTime: String, endTime: String?) -> String {
        let start = trimTime(startTime)
        guard let end = endTime.map(trimTime), !end.isEmpty else { return start }
        return "\(start) – \(end)"
    }

    private static func trimTime(_ value: String) -> String {
        // Upstream shape is "HH:mm" or "HH:mm:ss" — trim to the first five.
        String(value.prefix(5))
    }

    private static func map(state: OverviewOverviewClassState) -> OverviewClassState {
        switch state {
        case .done: return .done
        case .now: return .now
        case .next: return .next
        case .later: return .later
        }
    }

    private static func statusLabel(for status: OverviewOverviewDisciplineStatus) -> String {
        switch status {
        case .parcial: return "PARCIAL"
        case .final: return "FINAL"
        case .aprovado: return "APROVADO"
        case .reprovado: return "REPROVADO"
        }
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone.current
        return f
    }()

    private static let eyebrowFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "EEEE · d MMM"
        return f
    }()

    private static let shortDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "d MMM"
        return f
    }()

    private static let isoInstantFormatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    private static func formatShortDate(iso: String) -> String {
        guard let date = isoDayFormatter.date(from: iso) else { return iso }
        return shortDateFormatter.string(from: date)
            .replacingOccurrences(of: ".", with: "")
            .lowercased()
    }

    // "há 2 min" / "há 1 h" — the footer's relative timestamp.
    private static func formatRelative(iso: String, against now: Date) -> String? {
        let parsed = isoInstantFormatter.date(from: iso)
            ?? ISO8601DateFormatter().date(from: iso)
        guard let date = parsed else { return nil }
        let seconds = max(0, Int(now.timeIntervalSince(date)))
        let minutes = seconds / 60
        if minutes < 1 { return "agora mesmo" }
        if minutes < 60 { return "há \(minutes) min" }
        let hours = minutes / 60
        if hours < 24 { return "há \(hours) h" }
        let days = hours / 24
        return "há \(days) d"
    }
}

// MARK: - iOS-local presentation types for tile slots

struct OverviewMessagesTileData {
    let unreadCount: Int
    let lastSender: String?
    let lastPreview: String?
}

struct OverviewNextTestTileData {
    let label: String
    let disciplineName: String
    let daysUntil: Int
    let dateLabel: String
}

struct OverviewAttendanceTileData {
    let percentage: Int?
    let days: [Bool]
    let allowedAbsences: Int
    let periodDays: Int
}

// Placeholder for the deferred coeficiente tile. Shape matches the fixture's
// fields so when CR is wired up later we only need to plug a source.
struct OverviewGradeTileData {
    let value: Double
    let delta: Double
    let comparisonSemester: String
}

// Maps a discipline code to a stable visual color + mesh variant. Keeps all
// palette decisions in one place; ViewModel hands the result to components.
//
// Colors are adaptive: the deep brand values (especially `plum`) disappear
// against the dark surface, so each slot ships a lifted dark-mode counterpart
// tuned for text + chip contrast on `UNESColor.surface`.
enum ColorFor {
    static let coral   = dynamic(light: hex(0xE8, 0x5D, 0x4E), dark: hex(0xF2, 0x7E, 0x6E))
    static let amber   = dynamic(light: hex(0xF4, 0xA2, 0x3C), dark: hex(0xF4, 0xA2, 0x3C))
    static let magenta = dynamic(light: hex(0xB2, 0x3A, 0x7A), dark: hex(0xD4, 0x62, 0x99))
    static let teal    = dynamic(light: hex(0x3B, 0x9E, 0xAE), dark: hex(0x5B, 0xB8, 0xC6))
    static let plum    = dynamic(light: hex(0x2D, 0x1B, 0x4E), dark: hex(0xB3, 0x9D, 0xDB))
    static let rose    = dynamic(light: hex(0xC6, 0x4A, 0x6D), dark: hex(0xE8, 0x8A, 0xA5))
    static let sky     = dynamic(light: hex(0x3C, 0x7D, 0xC9), dark: hex(0x79, 0xAE, 0xE8))
    static let emerald = dynamic(light: hex(0x2E, 0x8B, 0x5C), dark: hex(0x5F, 0xC4, 0x8E))
    static let indigo  = dynamic(light: hex(0x4A, 0x5F, 0xB8), dark: hex(0x8A, 0x9E, 0xE8))
    static let mustard = dynamic(light: hex(0xA0, 0x74, 0x1F), dark: hex(0xD4, 0xA8, 0x4C))

    private static let palette: [Color] = [
        coral, amber, magenta, teal, plum,
        rose, sky, emerald, indigo, mustard,
    ]

    static func discipline(code: String) -> Color {
        let bucket = abs(stableHash(code)) % palette.count
        return palette[bucket]
    }

    static func meshVariant(code: String) -> MeshVariant {
        let variants: [MeshVariant] = [.cool, .warm, .rose]
        let bucket = abs(stableHash(code)) % variants.count
        return variants[bucket]
    }

    // Stable across processes — Swift's `hashValue` is randomized per process.
    private static func stableHash(_ s: String) -> Int {
        var hash = 5381
        for scalar in s.unicodeScalars {
            hash = ((hash << 5) &+ hash) &+ Int(scalar.value)
        }
        return hash
    }

    private static func hex(_ r: Int, _ g: Int, _ b: Int) -> UIColor {
        UIColor(red: CGFloat(r) / 255, green: CGFloat(g) / 255, blue: CGFloat(b) / 255, alpha: 1)
    }

    private static func dynamic(light: UIColor, dark: UIColor) -> Color {
        Color(uiColor: UIColor { trait in
            trait.userInterfaceStyle == .dark ? dark : light
        })
    }
}
