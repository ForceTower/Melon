import SwiftUI
import WidgetKit

/// What state the widget should render. Drives copy, the live dot, and (for
/// `inClass`) the progress bar.
enum NextClassState: String, Codable, Hashable {
    /// There is a future class today. `startsIn` counts down to it.
    case upcoming
    /// A class is happening right now. `endsIn` counts down to its end.
    case inClass
    /// All of today's classes are done. `nextDayLine` describes tomorrow's
    /// first class.
    case dayDone
}

struct NextClassEntry: TimelineEntry, Hashable {
    let date: Date
    let state: NextClassState

    let code: String          // "CALC II"
    let shortCode: String     // "CALC"
    let title: String         // "Cálculo Diferencial II"
    let shortTitle: String    // "Cálculo II"
    let prof: String          // "Adriana Matos"
    let room: String          // "MT-14"
    let building: String      // "Matemática"

    let startsIn: Int         // minutes until start (upcoming) — fallback for previews / nil refs
    let endsIn: Int           // minutes until end (inClass)
    let totalDurationMin: Int // total class duration (inClass progress bar)

    let startTime: String     // "10:20"
    let endTime: String       // "12:00"
    let topic: String?        // "Integrais por partes"

    // Absolute datetimes of the focused class — drive live countdowns
    // through `TimelineView(.everyMinute)` inside the widget views. Both
    // nil for the static placeholder fixture; views fall back to the
    // integer fields above when nil.
    let referenceStart: Date?
    let referenceEnd: Date?

    let todayBars: [TodayBar]
    let dayDoneLine: String?  // e.g. "amanhã, 08:00 · Algoritmos I — Lab LC-03"
    let completedTodayCount: Int

    struct TodayBar: Hashable, Codable {
        enum State: String, Codable { case done, next, later }
        let code: String
        let time: String
        let state: State
        let colorHex: UInt32
    }
}

extension NextClassEntry {
    /// Placeholder shown in the widget gallery and during initial load.
    static let placeholder = NextClassEntry(
        date: Date(),
        state: .upcoming,
        code: "CALC II",
        shortCode: "CALC",
        title: "Cálculo Diferencial II",
        shortTitle: "Cálculo II",
        prof: "Adriana Matos",
        room: "MT-14",
        building: "Matemática",
        startsIn: 72,
        endsIn: 0,
        totalDurationMin: 100,
        startTime: "10:20",
        endTime: "12:00",
        topic: "Integrais por partes",
        referenceStart: nil,
        referenceEnd: nil,
        todayBars: [
            .init(code: "ALGI", time: "08:00", state: .done,  colorHex: 0xE85D4E),
            .init(code: "CALC", time: "10:20", state: .next,  colorHex: 0x3B9EAE),
            .init(code: "LPOO", time: "14:00", state: .later, colorHex: 0xB23A7A),
            .init(code: "FIS2", time: "16:20", state: .later, colorHex: 0x2D1B4E),
        ],
        dayDoneLine: nil,
        completedTodayCount: 1
    )

    static let inClassPlaceholder: NextClassEntry = {
        var e = placeholder
        return NextClassEntry(
            date: e.date,
            state: .inClass,
            code: e.code, shortCode: e.shortCode,
            title: e.title, shortTitle: e.shortTitle,
            prof: e.prof, room: e.room, building: e.building,
            startsIn: 0, endsIn: 38, totalDurationMin: 100,
            startTime: e.startTime, endTime: e.endTime,
            topic: e.topic,
            referenceStart: nil, referenceEnd: nil,
            todayBars: e.todayBars,
            dayDoneLine: nil, completedTodayCount: 1
        )
    }()

    static let dayDonePlaceholder = NextClassEntry(
        date: Date(),
        state: .dayDone,
        code: "", shortCode: "",
        title: "", shortTitle: "",
        prof: "", room: "", building: "",
        startsIn: 0, endsIn: 0, totalDurationMin: 0,
        startTime: "", endTime: "",
        topic: nil,
        referenceStart: nil, referenceEnd: nil,
        todayBars: [],
        dayDoneLine: "amanhã, 08:00 · Algoritmos I — Lab LC-03",
        completedTodayCount: 4
    )
}

extension NextClassEntry {
    /// Minutes from `now` until the focused class starts. Falls back to the
    /// integer field when no absolute reference is available (placeholder).
    func liveStartsIn(at now: Date) -> Int {
        guard let referenceStart else { return startsIn }
        return max(0, Int((referenceStart.timeIntervalSince(now) / 60).rounded()))
    }

    /// Minutes from `now` until the focused class ends.
    func liveEndsIn(at now: Date) -> Int {
        guard let referenceEnd else { return endsIn }
        return max(0, Int((referenceEnd.timeIntervalSince(now) / 60).rounded()))
    }

    /// Fraction [0, 1] of the focused class elapsed.
    func liveProgress(at now: Date) -> Double {
        guard let referenceStart, let referenceEnd else {
            guard totalDurationMin > 0 else { return 0 }
            let elapsed = max(0, totalDurationMin - endsIn)
            return min(1, Double(elapsed) / Double(totalDurationMin))
        }
        let total = referenceEnd.timeIntervalSince(referenceStart)
        guard total > 0 else { return 0 }
        let elapsed = now.timeIntervalSince(referenceStart)
        return min(1, max(0, elapsed / total))
    }
}

/// Bridge from the entry's class color (stored as hex) back to a SwiftUI
/// `Color`. We carry hex through the timeline so entries stay `Codable`.
extension NextClassEntry.TodayBar {
    var color: Color {
        Color(
            red: Double((colorHex >> 16) & 0xFF) / 255,
            green: Double((colorHex >> 8) & 0xFF) / 255,
            blue: Double(colorHex & 0xFF) / 255
        )
    }
}

func formatCountdown(_ mins: Int) -> String {
    let h = mins / 60
    let m = mins % 60
    if h == 0 { return "\(m) min" }
    if m == 0 { return "\(h)h" }
    return "\(h)h \(m)min"
}

/// Top-eyebrow label for the upcoming-style layouts (Small, Medium upcoming,
/// Large). Switches copy when a class is in session so the eyebrow doesn't
/// read "em 0 min" — matches the Medium in-class header pattern.
func countdownEyebrow(state: NextClassState, startsIn: Int, endsIn: Int) -> String {
    switch state {
    case .inClass:
        return "agora · termina em \(formatCountdown(endsIn))"
    case .upcoming, .dayDone:
        return "em \(formatCountdown(startsIn))"
    }
}
