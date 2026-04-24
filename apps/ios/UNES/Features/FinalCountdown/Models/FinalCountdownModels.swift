import SwiftUI

/// One evaluation row in the calculator. `score` is nil when the grade hasn't
/// happened yet; `wildcard` marks the row the student can still influence
/// (the one whose required mark the UI solves for).
struct FCRow: Identifiable, Hashable {
    let id: UUID
    var label: String
    var score: Double?
    var weight: Int
    var wildcard: Bool

    init(id: UUID = UUID(), label: String, score: Double? = nil, weight: Int = 1, wildcard: Bool = false) {
        self.id = id
        self.label = label
        self.score = score
        self.weight = weight
        self.wildcard = wildcard
    }
}

/// The seven outcomes the calculator surfaces, plus `empty` when there's not
/// enough data to compute anything yet. Mirrors `computeVerdict` in
/// `screens-final-countdown.jsx`.
///
/// - `passed`: all rows filled, avg ≥ 7 (or best-case ≥ 7 with one missing).
/// - `ontrack`: one row missing, but the avg is comfortably above 7.
/// - `borderline`: one row missing; needs ≥ X on that row to skip VF.
/// - `borderlineFinal`: even a 10 on the missing row won't reach 7 — VF
///   inevitable but still winnable.
/// - `final`: all filled, 3 ≤ avg < 7, needs `need` on VF to close at 5.
/// - `impossible`: all filled, VF math needs > 10 — reprovação mesmo.
/// - `failed`: all filled, avg < 3, no right to VF.
/// - `failingTrack`: one missing, even best case stays below 3.
/// - `empty`: nothing to compute.
enum FCVerdictKind: String, Hashable {
    case passed, ontrack, borderline, borderlineFinal, final, impossible, failed, failingTrack, empty
}

struct FCVerdict: Hashable {
    let kind: FCVerdictKind
    let avg: Double?
    let best: Double?
    let worst: Double?
    /// Grade required on the wildcard row to close at 7 (partial-fill states).
    let wildcardNeeded: Double?
    /// Grade required on the VF to close at 5 (`final` state).
    let need: Double?
}

enum FCTone: String, Hashable {
    case plum, magenta, teal, coral, amber, green

    var bg: Color {
        switch self {
        case .plum:    return Color(red: 0x2D/255, green: 0x1B/255, blue: 0x4E/255)
        case .magenta: return Color(red: 0xB2/255, green: 0x3A/255, blue: 0x7A/255)
        case .teal:    return Color(red: 0x3B/255, green: 0x9E/255, blue: 0xAE/255)
        case .coral:   return Color(red: 0xE8/255, green: 0x5D/255, blue: 0x4E/255)
        case .amber:   return Color(red: 0xF4/255, green: 0xA2/255, blue: 0x3C/255)
        case .green:   return Color(red: 0x4A/255, green: 0xA6/255, blue: 0x79/255)
        }
    }

    /// Foreground used on the tone-colored chip square.
    var fg: Color {
        switch self {
        case .plum:  return Color(red: 0xFB/255, green: 0xD9/255, blue: 0xA8/255)
        case .amber: return Color(red: 0x2D/255, green: 0x1B/255, blue: 0x4E/255)
        default:     return Color(red: 0xFB/255, green: 0xF7/255, blue: 0xF2/255)
        }
    }

    /// Softened tint used as a rule pill background. The opacity matches the
    /// `soft` alpha from `FC_TONES` in the prototype (0.10–0.14).
    func soft(_ scheme: ColorScheme) -> Color {
        let alpha: Double
        switch self {
        case .amber:   alpha = 0.14
        case .green:   alpha = 0.12
        default:       alpha = 0.10
        }
        return bg.opacity(alpha)
    }
}

struct FCVerdictCopy {
    let eyebrow: String
    let titleLines: [String]
    let headline: String
    let sub: String
    let message: String
    let tone: FCTone
    let icon: String
}
