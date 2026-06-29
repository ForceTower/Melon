import SwiftUI
import UIKit

// UNES — Enrollment (course registration) data model + scheduling helpers.
// Ported from `screens-matricula-data.jsx`. Pure value types + stateless
// helpers; the mutable proposal lives in `EnrollmentState`.

// MARK: - Enrollment window

/// The institution's selection window. `state` drives which entry hero the
/// flow opens with; the hour bounds gate the proposal's workload meter.
struct EnrollmentWindow {
    let semester: String
    let startLabel: String
    let endLabel: String
    let minHours: Int
    let maxHours: Int
    /// When true, full sections offer a waitlist instead of blocking selection.
    let useQueue: Bool
}

enum EnrollmentWindowState {
    case open       // janela aberta — selection enabled
    case upcoming   // abre em breve — read-only preview
    case closed     // proposta enviada — comprovante / read-only
}

/// The student the enrollment proposal belongs to. Drives the window-status
/// identity strip; sourced from the signed-in profile, not the enrollment feed.
struct EnrollmentStudent {
    let name: String
    let course: String
    let period: String
    let avatarInitial: String
}

// MARK: - Discipline / section / meeting

/// A single weekly time block of a meeting (1-based weekday, "HH:mm" bounds).
struct MeetingSlot: Hashable {
    let day: Int        // 1 = Seg … 6 = Sáb
    let start: String
    let end: String
}

/// One teaching meeting of a section — a theory or practice block with its own
/// shift, room, professors and weekly slots.
struct SectionMeeting: Hashable {
    let kind: String            // "Teórica" / "Prática"
    let shift: ClassShift
    let professors: [String]
    let room: String?
    let slots: [MeetingSlot]
}

enum ClassShift: String, Hashable {
    case morning = "Matutino"
    case afternoon = "Vespertino"
    case evening = "Noturno"
    case undefined = "A definir"

    var label: String { rawValue }
}

/// A turma a student can pick. `proposalsCount` vs `vacancies` drives the seat
/// meter; `allowsOtherDefault` seeds the "aceitar outra turma" toggle when the
/// section is selected.
struct ClassSection: Identifiable, Hashable {
    let id: Int64
    let label: String                   // "T01", "T01P01", …
    let tone: EnrollmentTone
    let coursePreferential: Bool
    let suggestion: Bool
    let vacancies: Int
    let proposalsCount: Int
    let allowsOtherDefault: Bool
    let waitlistCount: Int              // students already queued; 0 when none
    let meetings: [SectionMeeting]

    init(
        id: Int64,
        label: String,
        tone: EnrollmentTone,
        coursePreferential: Bool,
        suggestion: Bool,
        vacancies: Int,
        proposalsCount: Int,
        allowsOtherDefault: Bool,
        waitlistCount: Int = 0,
        meetings: [SectionMeeting]
    ) {
        self.id = id
        self.label = label
        self.tone = tone
        self.coursePreferential = coursePreferential
        self.suggestion = suggestion
        self.vacancies = vacancies
        self.proposalsCount = proposalsCount
        self.allowsOtherDefault = allowsOtherDefault
        self.waitlistCount = waitlistCount
        self.meetings = meetings
    }
}

/// A prerequisite line. `met == false` surfaces the "pré-requisito pendente"
/// warning but never hard-blocks selection (the colegiado decides).
struct Prerequisite: Hashable {
    let code: String
    let name: String
    let met: Bool
}

struct OfferedDiscipline: Identifiable, Hashable {
    let id: Int64
    let code: String
    let name: String
    let workload: Int
    let mandatory: Bool
    /// Curriculum period the discipline belongs to; 0 = optativa / eletiva.
    let gradePeriod: Int
    let tone: EnrollmentTone
    let suggestion: Bool
    let prereqs: [Prerequisite]
    let sections: [ClassSection]

    var hasUnmetPrereq: Bool { prereqs.contains { !$0.met } }
}

// MARK: - Scheduling helpers

enum EnrollmentScheduling {
    /// 1-based weekday labels (index 0 unused, mirroring the prototype).
    static let daysShort: [String] = ["", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"]
    static let daysFull: [String] = ["", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"]

    static func toMinutes(_ t: String) -> Int {
        let parts = t.split(separator: ":").compactMap { Int($0) }
        guard parts.count == 2 else { return 0 }
        return parts[0] * 60 + parts[1]
    }

    /// All slots across every meeting of a section.
    static func slots(_ section: ClassSection) -> [MeetingSlot] {
        section.meetings.flatMap(\.slots)
    }

    static func hasSchedule(_ section: ClassSection) -> Bool {
        !slots(section).isEmpty
    }

    /// Do two slots share a day and overlap in time?
    static func overlap(_ a: MeetingSlot, _ b: MeetingSlot) -> Bool {
        guard a.day == b.day else { return false }
        return toMinutes(a.start) < toMinutes(b.end) && toMinutes(b.start) < toMinutes(a.end)
    }

    /// First clashing weekday between two sections, or nil when they fit.
    static func conflictDay(_ lhs: ClassSection, _ rhs: ClassSection) -> Int? {
        for a in slots(lhs) {
            for b in slots(rhs) where overlap(a, b) {
                return a.day
            }
        }
        return nil
    }

    /// Disciplines grouped by curriculum period, mandatory periods ascending
    /// with optativas (period 0) sorted last.
    static func byPeriod(_ list: [OfferedDiscipline]) -> [(period: Int, items: [OfferedDiscipline])] {
        Dictionary(grouping: list, by: \.gradePeriod)
            .sorted { lhs, rhs in
                if lhs.key == 0 { return false }
                if rhs.key == 0 { return true }
                return lhs.key < rhs.key
            }
            .map { (period: $0.key, items: $0.value) }
    }
}

/// Filled / total seats and the derived "vagas / quase cheia / lotada" state.
struct SeatState {
    let filled: Int
    let total: Int
    var pct: Double { total > 0 ? Double(filled) / Double(total) : 0 }
    var isFull: Bool { filled >= total }
    var isTight: Bool { pct >= 0.85 && filled < total }

    init(_ section: ClassSection) {
        filled = section.proposalsCount
        total = section.vacancies
    }
}

// MARK: - Palette

/// Enrollment-local color palette. Tones mirror the prototype's `MAT_TONES`;
/// status colors mirror its danger / warn / ok roles. Each is adaptive — the
/// light value matches the design, the dark value is lifted for legibility on
/// `UNESColor.surface`. Kept local to the feature so it doesn't leak into the
/// shared design system (precedent: `MeColors`, `DisciplineScoreColor`).
enum EnrollmentTone: String, Hashable {
    case coral, teal, magenta, plum, amber, green

    /// Stable per-discipline tone keyed off the code — there is no upstream
    /// tone, so the color is derived on-device and stays consistent across
    /// launches (a discipline keeps its color). Mirrors `ColorFor.discipline`.
    private static let cycle: [EnrollmentTone] = [.teal, .coral, .magenta, .plum, .amber, .green]

    static func forCode(_ code: String) -> EnrollmentTone {
        let sum = code.unicodeScalars.reduce(0) { $0 + Int($1.value) }
        return cycle[sum % cycle.count]
    }

    var color: Color {
        switch self {
        case .coral:   return EnrollmentPalette.dynamic(light: 0xE8_5D_4E, dark: 0xF2_7E_6E)
        case .teal:    return EnrollmentPalette.dynamic(light: 0x3B_9E_AE, dark: 0x5B_B8_C6)
        case .magenta: return EnrollmentPalette.dynamic(light: 0xB2_3A_7A, dark: 0xD4_62_99)
        case .plum:    return EnrollmentPalette.dynamic(light: 0x5B_3B_8C, dark: 0xB3_9D_DB)
        case .amber:   return EnrollmentPalette.dynamic(light: 0xD9_85_2E, dark: 0xE8_A2_4C)
        case .green:   return EnrollmentPalette.dynamic(light: 0x4A_A6_79, dark: 0x6F_C4_8E)
        }
    }
}

enum EnrollmentPalette {
    /// Conflict / over-limit red.
    static let danger = dynamic(light: 0xD8_45_3B, dark: 0xF0_83_79)
    /// Warning amber — banner accent + caption text.
    static let warn = dynamic(light: 0xB6_6A_1C, dark: 0xE0_A3_5C)
    static let warnSolid = dynamic(light: 0xD9_85_2E, dark: 0xD9_85_2E)
    /// "Tudo certo" green — banner accent + caption text.
    static let ok = dynamic(light: 0x3C_7E_5C, dark: 0x7F_D3_A6)
    static let okSolid = dynamic(light: 0x4A_A6_79, dark: 0x4A_A6_79)

    static func dynamic(light: Int, dark: Int) -> Color {
        Color(uiColor: UIColor { @Sendable trait in
            trait.userInterfaceStyle == .dark ? uiColor(dark) : uiColor(light)
        })
    }

    private static func uiColor(_ rgb: Int) -> UIColor {
        UIColor(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}
