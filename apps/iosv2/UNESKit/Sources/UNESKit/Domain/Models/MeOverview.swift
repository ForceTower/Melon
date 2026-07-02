import Foundation

/// Everything the Eu screen derives from the local mirror: the identity
/// garnishes, the hero stat row, the semester progress widget, and the
/// Final Countdown teaser.
struct MeOverview: Equatable, Sendable {
    /// Raw upstream code, e.g. "20261" — display through
    /// `DisciplinesFormat.semesterLabel`.
    var semesterCode: String?
    /// "Campus · Módulo" where the student's classes most often meet.
    var campus: String?
    var coefficient: CoefficientSummary?
    var attendancePercent: Int?
    var progress: SemesterProgress?
    var countdown: SemesterCountdown?

    static let empty = MeOverview()
}

/// A Me snapshot served from the local mirror, stamped with the moment of
/// the refresh that produced it.
struct CachedMeOverview: Equatable, Sendable {
    var overview: MeOverview
    var syncedAt: Date
}

/// Where the running semester sits between its start and end dates.
struct SemesterProgress: Equatable, Sendable {
    /// 1-based, clamped to `1...totalWeeks`.
    var week: Int
    var totalWeeks: Int
    /// Elapsed share of the semester, 0–100.
    var percent: Int
    /// yyyy-MM-dd.
    var startStamp: String
    var endStamp: String
}

/// The Final Countdown teaser: how much semester is left, counted a few
/// different ways.
struct SemesterCountdown: Equatable, Sendable {
    /// Whole calendar days from today to the semester's end date.
    var daysLeft: Int
    var weeksLeft: Int
    var hoursLeft: Int
    /// Scheduled class sessions still ahead (today's remaining ones included).
    var classesLeft: Int
    /// Saturdays between today (exclusive) and the end date.
    var weekendsLeft: Int
    /// Unreleased evaluations with a scheduled date still ahead.
    var scheduledExams: Int
    var disciplineCount: Int
}

/// What stays on the device after a logout that keeps local data.
struct LocalDataSummary: Equatable, Sendable {
    /// Semesters whose payload is mirrored, not just listed.
    var semesters: Int
    var messages: Int
}

extension MeOverview {
    static let preview = MeOverview(
        semesterCode: "20261",
        campus: "UEFS · Módulo 5",
        coefficient: CoefficientSummary(value: 8.5, spark: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5], delta: 0.3),
        attendancePercent: 96,
        progress: SemesterProgress(
            week: 7,
            totalWeeks: 18,
            percent: 38,
            startStamp: "2026-02-24",
            endStamp: "2026-07-07"
        ),
        countdown: SemesterCountdown(
            daysLeft: 77,
            weeksLeft: 11,
            hoursLeft: 1848,
            classesLeft: 33,
            weekendsLeft: 11,
            scheduledExams: 4,
            disciplineCount: 6
        )
    )
}
