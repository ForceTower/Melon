@preconcurrency import Umbrella

// KMP use cases the calendar leans on. `observeEvents` feeds the agenda;
// `observeActiveSemesterCode` powers the eyebrow's "SEMESTRE 2026.1" label.
struct CalendarUseCases {
    let observeEvents: CalendarObserveCalendarEventsUseCase
    let observeActiveSemesterCode: CalendarObserveActiveSemesterCodeUseCase
}

@MainActor
struct CalendarFactory {
    let useCases: CalendarUseCases

    func makeViewModel() -> CalendarViewModel {
        CalendarViewModel(useCases: useCases)
    }
}
