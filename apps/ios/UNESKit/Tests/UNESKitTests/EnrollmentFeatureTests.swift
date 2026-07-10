import ComposableArchitecture
import Foundation
import Sharing
import Testing

@testable import UNESKit

// Repository stubs that resolve instantly would mutate the shared session
// before `store.send` finishes asserting, so the success stubs await a gate
// the test opens right before the matching `receive`.

@MainActor
struct EnrollmentFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    @Test
    func taskLoadsTheWindowThenOffersAndPreseeds() async {
        let (windowGate, openWindow) = AsyncStream.makeStream(of: Void.self)
        let (offersGate, openOffers) = AsyncStream.makeStream(of: Void.self)
        let store = TestStore(initialState: EnrollmentFeature.State(profile: .preview)) {
            EnrollmentFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.enrollmentRepository.window = {
                for await _ in windowGate { break }
                return .preview
            }
            $0.enrollmentRepository.offers = {
                for await _ in offersGate { break }
                return .previewCatalogue
            }
        }

        await store.send(.task) {
            $0.referenceDate = Self.referenceDate
            $0.hasLoaded = true
            $0.isLoading = true
        }

        openWindow.yield(())
        await store.receive(.windowLoaded(.preview)) {
            $0.$session.withLock { $0.window = .preview }
        }

        openOffers.yield(())
        await store.receive(.offersLoaded(.previewCatalogue)) {
            $0.isLoading = false
            $0.$session.withLock {
                $0.disciplines = .previewCatalogue
                // Literal, not a preseedFromSavedProposal() mirror — the
                // expectation must fail if the preseed rules regress.
                $0.picks = [
                    EnrollmentPick(disciplineId: 201, sectionId: 30101, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 203, sectionId: 30302, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 204, sectionId: 30402, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 205, sectionId: 30501, allowsOther: true, waitlist: false),
                ]
            }
        }
        #expect(store.state.session.picks.count == 4)
    }

    @Test
    func taskWithoutAWindowLandsOnTheEmptyState() async {
        let store = TestStore(initialState: EnrollmentFeature.State(profile: .preview)) {
            EnrollmentFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.enrollmentRepository.window = { nil }
        }

        await store.send(.task) {
            $0.referenceDate = Self.referenceDate
            $0.hasLoaded = true
            $0.isLoading = true
        }
        await store.receive(.windowLoaded(nil)) {
            $0.isLoading = false
        }
    }

    @Test
    func loadFailureSurfacesTheMessageAndRetryRecovers() async {
        let failing = LockIsolated(true)
        let (windowGate, openWindow) = AsyncStream.makeStream(of: Void.self)
        let (offersGate, openOffers) = AsyncStream.makeStream(of: Void.self)
        let store = TestStore(initialState: EnrollmentFeature.State(profile: .preview)) {
            EnrollmentFeature()
        } withDependencies: {
            $0.date = .constant(Self.referenceDate)
            $0.enrollmentRepository.window = {
                if failing.value { throw EnrollmentFailure.network }
                for await _ in windowGate { break }
                return .preview
            }
            $0.enrollmentRepository.offers = {
                for await _ in offersGate { break }
                return .previewCatalogue
            }
        }

        await store.send(.task) {
            $0.referenceDate = Self.referenceDate
            $0.hasLoaded = true
            $0.isLoading = true
        }
        await store.receive(.loadFailed("Sem conexão. Verifique sua internet e tente de novo.")) {
            $0.isLoading = false
            $0.errorMessage = "Sem conexão. Verifique sua internet e tente de novo."
        }

        failing.setValue(false)
        await store.send(.retryTapped) {
            $0.isLoading = true
            $0.errorMessage = nil
        }

        openWindow.yield(())
        await store.receive(.windowLoaded(.preview)) {
            $0.$session.withLock { $0.window = .preview }
        }

        openOffers.yield(())
        await store.receive(.offersLoaded(.previewCatalogue)) {
            $0.isLoading = false
            $0.$session.withLock {
                $0.disciplines = .previewCatalogue
                // Literal, not a preseedFromSavedProposal() mirror — the
                // expectation must fail if the preseed rules regress.
                $0.picks = [
                    EnrollmentPick(disciplineId: 201, sectionId: 30101, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 203, sectionId: 30302, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 204, sectionId: 30402, allowsOther: true, waitlist: false),
                    EnrollmentPick(disciplineId: 205, sectionId: 30501, allowsOther: true, waitlist: false),
                ]
            }
        }
    }

    @Test
    func reappearanceOnlyRepinsTheReferenceDate() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let later = Self.referenceDate.addingTimeInterval(3_600)
        let store = TestStore(initialState: EnrollmentFeature.State(profile: .preview, session: .preview)) {
            EnrollmentFeature()
        } withDependencies: {
            $0.date = .constant(later)
        }

        // The seeded session marks the state as loaded — no fetch fires.
        await store.send(.task) {
            $0.referenceDate = later
        }
    }

    @Test
    func tapsRouteThroughDelegates() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentFeature.State(profile: .preview, session: .preview)) {
            EnrollmentFeature()
        }

        await store.send(.startTapped)
        await store.receive(.delegate(.openOffers))
        await store.send(.reviewTapped)
        await store.receive(.delegate(.openReview))
        await store.send(.proposalRowTapped(203))
        await store.receive(.delegate(.openDiscipline(203)))
    }
}

@MainActor
struct EnrollmentDisciplineFeatureTests {
    @Test
    func tappingASectionPicksItAndTappingAgainRemovesIt() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentDisciplineFeature.State(disciplineId: 203)) {
            EnrollmentDisciplineFeature()
        }

        // Move EXA866 to its full T01: the pick queues automatically.
        await store.send(.sectionTapped(30301)) {
            $0.$session.withLock {
                $0.picks[3] = EnrollmentPick(disciplineId: 203, sectionId: 30301, allowsOther: true, waitlist: true)
            }
        }

        await store.send(.sectionTapped(30301)) {
            $0.$session.withLock { $0.remove(disciplineId: 203) }
        }
    }

    @Test
    func clashingSectionsCannotBePicked() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentDisciplineFeature.State(disciplineId: 204)) {
            EnrollmentDisciplineFeature()
        }

        // TEC502 T01 clashes with the picked EXA427 T01 — nothing changes.
        await store.send(.sectionTapped(30401))
    }

    @Test
    func allowsOtherTogglesThePick() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentDisciplineFeature.State(disciplineId: 201)) {
            EnrollmentDisciplineFeature()
        }

        await store.send(.allowsOtherChanged(false)) {
            $0.$session.withLock { $0.setAllowsOther(false, disciplineId: 201) }
        }
    }
}

@MainActor
struct EnrollmentReviewFeatureTests {
    @Test
    func submitSendsTheFullSelectionSetAndClosesTheWindow() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let submitted = LockIsolated<[EnrollmentSelection]>([])
        let (submitGate, openSubmit) = AsyncStream.makeStream(of: Void.self)
        let store = TestStore(initialState: EnrollmentReviewFeature.State()) {
            EnrollmentReviewFeature()
        } withDependencies: {
            $0.enrollmentRepository.submit = { selections in
                submitted.setValue(selections)
                for await _ in submitGate { break }
            }
        }

        await store.send(.submitTapped) {
            $0.isSubmitting = true
        }

        openSubmit.yield(())
        await store.receive(.submitSucceeded) {
            $0.isSubmitting = false
            $0.$session.withLock { $0.window?.state = .closed }
        }
        await store.receive(.delegate(.submitted))

        submitted.withValue {
            #expect($0 == EnrollmentSession.preview.selections)
        }
        #expect(store.state.isReadonly)
    }

    @Test
    func submitFailureAlertsAndStaysEditable() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentReviewFeature.State()) {
            EnrollmentReviewFeature()
        } withDependencies: {
            $0.enrollmentRepository.submit = { _ in throw EnrollmentFailure.server("O SAGRES recusou a proposta") }
        }

        await store.send(.submitTapped) {
            $0.isSubmitting = true
        }
        await store.receive(.submitFailed("O SAGRES recusou a proposta")) {
            $0.isSubmitting = false
            $0.alert = AlertState {
                TextState("Não deu para enviar a proposta")
            } message: {
                TextState("O SAGRES recusou a proposta")
            }
        }
        #expect(!store.state.isReadonly)
    }

    @Test
    func blockedProposalsCannotSubmit() async {
        var blocked = EnrollmentSession.preview
        blocked.picks = []
        @Shared(.enrollmentSession) var session = blocked
        let store = TestStore(initialState: EnrollmentReviewFeature.State()) {
            EnrollmentReviewFeature()
        }

        #expect(!store.state.canSubmit)
        await store.send(.submitTapped)
    }

    @Test
    func editsFlowIntoTheSharedSession() async {
        @Shared(.enrollmentSession) var session = EnrollmentSession.preview
        let store = TestStore(initialState: EnrollmentReviewFeature.State()) {
            EnrollmentReviewFeature()
        }

        await store.send(.waitlistChanged(201, true)) {
            $0.$session.withLock { $0.setWaitlist(true, disciplineId: 201) }
        }
        await store.send(.removeTapped(203)) {
            $0.$session.withLock { $0.remove(disciplineId: 203) }
        }
    }
}
