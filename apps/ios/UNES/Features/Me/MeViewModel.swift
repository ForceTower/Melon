import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `MeView`. A single KMP Flow carries the full hero payload; everything
// visible on the screen that isn't fixture data (shortcut grid / settings
// rows) is projected out of `ProfileIdentity` here. Fixture mode (useCases ==
// nil) keeps `#Preview` rendering without a live graph.
@MainActor
@Observable
final class MeViewModel {
    private(set) var identity: ProfileIdentity?

    @ObservationIgnored private let useCases: MeUseCases?

    init(useCases: MeUseCases?) {
        self.useCases = useCases
    }

    // Factory-less init — retained so `MeView()` and its `#Preview` keep
    // working against `MeFixtures` until a real graph is wired.
    convenience init() {
        self.init(useCases: nil)
    }

    func observe() async {
        guard let useCases else { return }
        for await snapshot in useCases.observeProfile.invoke() {
            identity = Self.map(profile: snapshot)
        }
    }

    // MARK: - Mapping

    private static func map(profile raw: MeMeProfile) -> ProfileIdentity {
        let name = raw.identity.userName
        let firstName = raw.identity.firstName.isEmpty ? name : raw.identity.firstName
        let avatarInitial = firstName.first.map { String($0).uppercased() } ?? "?"

        let semester = raw.semester
        let crValue = raw.enrollment.cr.map { Double(truncating: $0) }
        let crDeltaValue = raw.enrollment.crDelta.map { Double(truncating: $0) }

        return ProfileIdentity(
            name: name,
            firstName: firstName,
            course: raw.identity.courseName ?? "",
            campus: "Universidade Estadual de Feira de Santana",
            enrollment: raw.identity.enrollmentNumber,
            avatarInitial: avatarInitial,
            semester: semester?.code ?? "",
            semesterWeek: Int(semester?.currentWeek ?? 0),
            semesterTotalWeeks: Int(semester?.totalWeeks ?? 0),
            progressPct: Int(semester?.progressPercent ?? 0),
            cr: crValue ?? 0,
            crDelta: formatCrDelta(crDeltaValue),
            creditsDone: Int(raw.enrollment.completedHours),
            creditsRequired: Int(raw.enrollment.totalHours),
            semesterStart: formatSemesterStart(semester?.startDate),
            finalExam: formatFinalExam(raw.nextExam)
        )
    }

    private static func formatCrDelta(_ value: Double?) -> String {
        guard let value else { return "" }
        // One decimal place, comma separator, explicit sign to match the
        // design's "+0,3" / "-0,2" chip. Zero reads as "±0,0" to keep the
        // presence of the field obvious.
        let scaled = (value * 10).rounded()
        let absWhole = Int(abs(scaled)) / 10
        let absTenth = Int(abs(scaled)) % 10
        let sign: String
        if scaled > 0 {
            sign = "+"
        } else if scaled < 0 {
            sign = "-"
        } else {
            sign = "±"
        }
        return "\(sign)\(absWhole),\(absTenth)"
    }

    private static func formatSemesterStart(_ iso: String?) -> String {
        guard let iso, let date = isoDayFormatter.date(from: iso) else { return "" }
        return "início · \(shortDateFormatter.string(from: date))"
    }

    private static func formatFinalExam(_ exam: MeMeNextExam?) -> String {
        guard let exam, let date = isoDayFormatter.date(from: exam.date) else { return "" }
        return "prova final · \(shortDateFormatter.string(from: date))"
    }

    private static let isoDayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyy-MM-dd"
        f.timeZone = TimeZone.current
        return f
    }()

    private static let shortDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "pt_BR")
        f.dateFormat = "d MMM"
        return f
    }()
}
