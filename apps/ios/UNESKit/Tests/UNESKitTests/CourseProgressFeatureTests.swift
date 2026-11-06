import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct CourseProgressFeatureTests {
    /// The picker hosts the failure alert, so a failed switch must leave it up.
    @Test
    func aFailedSwitchAlertsOverTheStillOpenPicker() async {
        struct Offline: Error {}
        let store = TestStore(initialState: CourseProgressFeature.State(progress: .preview())) {
            CourseProgressFeature()
        } withDependencies: {
            $0.courseProgressRepository.selectVersion = { _ in throw Offline() }
        }
        store.exhaustivity = .off

        await store.send(.versionPickerTapped) {
            $0.isVersionPickerPresented = true
        }
        await store.send(.versionSelected("cur-psi-20122")) {
            $0.versionSwitch = .version("cur-psi-20122")
        }
        await store.receive(\.versionSwitchFinished)

        #expect(store.state.versionSwitch == nil)
        #expect(store.state.isVersionPickerPresented)
        #expect(store.state.alert != nil)
    }

    @Test
    func aSuccessfulSwitchClosesThePicker() async {
        let store = TestStore(initialState: CourseProgressFeature.State(progress: .preview())) {
            CourseProgressFeature()
        } withDependencies: {
            $0.courseProgressRepository.selectVersion = { _ in }
        }

        await store.send(.versionPickerTapped) {
            $0.isVersionPickerPresented = true
        }
        await store.send(.versionSelected("cur-psi-20122")) {
            $0.versionSwitch = .version("cur-psi-20122")
        }
        await store.receive(\.versionSwitchFinished) {
            $0.versionSwitch = nil
            $0.isVersionPickerPresented = false
        }
    }
}
