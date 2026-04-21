import Foundation
import Observation
import OSLog
@preconcurrency import Umbrella

struct ReadyNextClass {
    let disciplineName: String
    let startTime: String
    let spaceLocation: String?
    let teacherName: String?
    let startsInLabel: String
}

@MainActor
@Observable
final class ReadyViewModel {
    private(set) var isLoading = true
    private(set) var semesterLine: String = ""
    private(set) var nextClass: ReadyNextClass?

    private let useCase: DashboardGetReadyOverviewUseCase?
    private var didLoad = false

    private static let logger = Logger(subsystem: "dev.forcetower.melon", category: "ready")

    init(useCase: DashboardGetReadyOverviewUseCase?) {
        self.useCase = useCase
    }

    func load() async {
        guard !didLoad else { return }
        didLoad = true

        guard let useCase else {
            isLoading = false
            return
        }

        do {
            let overview = try await useCase.invoke()
            apply(overview)
        } catch is CancellationError {
            return
        } catch {
            Self.logger.warning("ready overview load failed: \(String(describing: error))")
        }
        isLoading = false
    }

    private func apply(_ overview: DashboardReadyOverview) {
        semesterLine = Self.formatSemesterLine(
            classCount: Int(overview.classCount),
            totalCredits: Int(overview.totalCredits),
            semesterCode: overview.semesterCode,
        )
        if let info = overview.nextClass {
            nextClass = ReadyNextClass(
                disciplineName: info.disciplineName,
                startTime: Self.trimTime(info.startTime),
                spaceLocation: info.spaceLocation,
                teacherName: info.teacherName,
                startsInLabel: Self.formatStartsIn(Int(info.startsInMinutes)),
            )
        } else {
            nextClass = nil
        }
    }

    // Upstream ships HH:mm or HH:mm:ss — trim to five chars so the preview
    // card renders minutes only, matching ScheduleFocusedViewModel.
    private static func trimTime(_ value: String) -> String {
        String(value.prefix(5))
    }

    private static func formatSemesterLine(classCount: Int, totalCredits: Int, semesterCode: String?) -> String {
        var parts: [String] = []
        parts.append("\(classCount) \(classCount == 1 ? "turma" : "turmas")")
        parts.append("\(totalCredits) créditos")
        if let code = semesterCode, !code.isEmpty {
            parts.append("semestre \(code)")
        }
        return parts.joined(separator: " · ")
    }

    // "em 1h 12min" / "em 45min" / "em 2d 3h". Mirrors the placeholder copy.
    private static func formatStartsIn(_ minutes: Int) -> String {
        let clamped = max(minutes, 0)
        let days = clamped / (60 * 24)
        let hours = (clamped / 60) % 24
        let mins = clamped % 60

        if days > 0 {
            return hours > 0 ? "em \(days)d \(hours)h" : "em \(days)d"
        }
        if hours > 0 {
            return mins > 0 ? "em \(hours)h \(mins)min" : "em \(hours)h"
        }
        return "em \(mins)min"
    }
}
