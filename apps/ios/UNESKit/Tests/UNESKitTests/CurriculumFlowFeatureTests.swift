import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct CurriculumFlowFeatureTests {
    @Test
    func togglingABlockedEntryMarksItCompleted() async {
        let calls = LockIsolated<[String]>([])
        let store = makeStore(.preview(), calls: calls)

        await store.send(.completionToggled("CHF345")) {
            $0.togglingCompletionCode = "CHF345"
        }
        await store.receive(\.completionToggleFinished) {
            $0.togglingCompletionCode = nil
        }
        #expect(calls.value == ["mark CHF345"])
    }

    @Test
    func togglingAManualCompletionRemovesTheMark() async {
        var progress = CourseProgress.preview()
        progress.periods[0].entries[0].isManuallyCompleted = true
        let code = progress.periods[0].entries[0].code
        let calls = LockIsolated<[String]>([])
        let store = makeStore(progress, calls: calls)

        await store.send(.completionToggled(code)) {
            $0.togglingCompletionCode = code
        }
        await store.receive(\.completionToggleFinished) {
            $0.togglingCompletionCode = nil
        }
        #expect(calls.value == ["unmark \(code)"])
    }

    @Test
    func observedEntriesIgnoreTheToggle() async {
        let calls = LockIsolated<[String]>([])
        let store = makeStore(.preview(), calls: calls)

        await store.send(.completionToggled("CHF289"))
        await store.send(.completionToggled("CHF344"))
        #expect(calls.value.isEmpty)
    }

    @Test
    func aFailedToggleAlerts() async {
        struct Offline: Error {}
        let store = TestStore(initialState: CurriculumFlowFeature.State(progress: .preview())) {
            CurriculumFlowFeature()
        } withDependencies: {
            $0.courseProgressRepository.markCompleted = { _ in throw Offline() }
        }
        store.exhaustivity = .off

        await store.send(.completionToggled("CHF345"))
        await store.receive(\.completionToggleFinished)
        #expect(store.state.togglingCompletionCode == nil)
        #expect(store.state.alert != nil)
    }

    private func makeStore(
        _ progress: CourseProgress,
        calls: LockIsolated<[String]>
    ) -> TestStoreOf<CurriculumFlowFeature> {
        TestStore(initialState: CurriculumFlowFeature.State(progress: progress)) {
            CurriculumFlowFeature()
        } withDependencies: {
            $0.courseProgressRepository.markCompleted = { code in calls.withValue { $0.append("mark \(code)") } }
            $0.courseProgressRepository.unmarkCompleted = { code in calls.withValue { $0.append("unmark \(code)") } }
        }
    }
}
