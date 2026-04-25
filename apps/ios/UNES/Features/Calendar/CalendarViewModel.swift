import Foundation
import Observation
@preconcurrency import Umbrella

// Drives `CalendarView`. Subscribes to the events flow and the active-semester
// code in parallel. Fixture mode (useCases == nil) is retained for `#Preview`
// so the canvas keeps rendering offline against `CalendarFixtures`.
@MainActor
@Observable
final class CalendarViewModel {
    private(set) var events: [CalendarEvent]
    private(set) var semesterCode: String?

    @ObservationIgnored private let useCases: CalendarUseCases?
    @ObservationIgnored private let log = Log.scoped("CalendarViewModel")

    init(useCases: CalendarUseCases?) {
        self.useCases = useCases
        self.events = useCases == nil ? CalendarFixtures.events : []
        self.semesterCode = useCases == nil ? CalendarFixtures.semesterLabel : nil
    }

    // Factory-less init for `#Preview`.
    convenience init() {
        self.init(useCases: nil)
    }

    func observe() async {
        guard let useCases else { return }
        log.info("subscribing to calendar feeds")
        async let a: Void = observeEvents(useCases)
        async let b: Void = observeSemester(useCases)
        _ = await (a, b)
    }

    private func observeEvents(_ useCases: CalendarUseCases) async {
        for await items in useCases.observeEvents.invoke() {
            events = items.map(CalendarMapping.map)
        }
    }

    private func observeSemester(_ useCases: CalendarUseCases) async {
        for await code in useCases.observeActiveSemesterCode.invoke() {
            semesterCode = code
        }
    }
}
