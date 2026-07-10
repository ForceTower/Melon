/// Snapshot displayed at the end of onboarding, derived from the freshly
/// synced active-semester payload.
struct ReadyOverview: Equatable, Sendable {
    var semesterCode: String?
    var classCount: Int
    var totalCredits: Int
    var nextClass: NextClassInfo?
    /// Plain mean of the grades posted so far; nil before any grade exists.
    /// Truncate (never round) to one decimal for display.
    var coefficient: Double?
    /// Posted grades in evaluation order, for the sparkline.
    var gradeSpark: [Double] = []
    /// Presence over lectures already held; nil before the first lecture.
    var attendancePercent: Int?

    static let empty = ReadyOverview(semesterCode: nil, classCount: 0, totalCredits: 0, nextClass: nil)
}

struct NextClassInfo: Equatable, Sendable {
    var disciplineName: String
    /// "HH:mm" strings as encoded upstream.
    var startTime: String
    var endTime: String?
    var location: String?
    var teacherName: String?
    /// Whole minutes from the snapshot instant to the class start. Always
    /// non-negative; rolls into next week when nothing is left this week.
    var startsInMinutes: Int
}

extension ReadyOverview {
    static let preview = ReadyOverview(
        semesterCode: "2026.1",
        classCount: 6,
        totalCredits: 24,
        nextClass: NextClassInfo(
            disciplineName: "Cálculo II",
            startTime: "10:20",
            endTime: "12:00",
            location: "MT-14",
            teacherName: "Adriana Matos",
            startsInMinutes: 39
        ),
        coefficient: 8.5,
        gradeSpark: [7.9, 8.0, 7.7, 8.2, 8.1, 8.5],
        attendancePercent: 96
    )
}
