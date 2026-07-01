import ComposableArchitecture
import Testing

@testable import UNESKit

@MainActor
struct MeFeatureTests {
    @Test
    func loadsProfileOnAppear() async {
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.profileRepository.current = { .preview }
        }

        await store.send(.onAppear) {
            $0.isLoading = true
        }
        await store.receive(.profileLoaded(.preview)) {
            $0.isLoading = false
            $0.profile = .preview
        }
    }

    @Test
    func surfacesFailureMessage() async {
        struct Boom: Error {}
        let store = TestStore(initialState: MeFeature.State()) {
            MeFeature()
        } withDependencies: {
            $0.profileRepository.current = { throw Boom() }
        }

        await store.send(.onAppear) {
            $0.isLoading = true
        }
        await store.receive(\.profileFailed) {
            $0.isLoading = false
            $0.errorMessage = Boom().localizedDescription
        }
    }
}
