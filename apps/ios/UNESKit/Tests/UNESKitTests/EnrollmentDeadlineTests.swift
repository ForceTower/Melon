import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
@Suite(.serialized)
struct EnrollmentDeadlineTests {
    private nonisolated static let deadline = Date(timeIntervalSince1970: 1_790_996_400)

    @Test(arguments: [false, true])
    func expiredWindowCannotSubmitEvenAfterReopening(reopened: Bool) async {
        var proposal = Self.proposal()
        if reopened {
            proposal.window?.state = .closed
            proposal.reopened = true
        }
        let attempts = LockIsolated(0)
        let store = TestStore(initialState: EnrollmentReviewFeature.State(session: proposal)) {
            EnrollmentReviewFeature()
        } withDependencies: {
            $0.date = .constant(Self.deadline.addingTimeInterval(0.001))
            $0.enrollmentRepository.submit = { _ in attempts.withValue { $0 += 1 } }
        }
        store.exhaustivity = .off

        await store.send(.task)
        #expect(!store.state.canSubmit)
        #expect(store.state.blockers == [.deadlinePassed])
        await store.send(.submitTapped)
        await store.finish()
        #expect(attempts.value == 0)
        #expect(!store.state.isSubmitting)
    }

    @Test
    func crossingDeadlineWhileReviewingNeverCallsSubmit() async {
        let now = LockIsolated(Self.deadline.addingTimeInterval(-1))
        let attempts = LockIsolated(0)
        let store = TestStore(initialState: EnrollmentReviewFeature.State(session: Self.proposal())) {
            EnrollmentReviewFeature()
        } withDependencies: {
            $0.date = DateGenerator { now.value }
            $0.enrollmentRepository.submit = { _ in attempts.withValue { $0 += 1 } }
        }
        store.exhaustivity = .off

        await store.send(.task)
        #expect(store.state.canSubmit)
        now.setValue(Self.deadline.addingTimeInterval(0.001))
        await store.send(.submitTapped)
        await store.finish()
        #expect(attempts.value == 0)
        #expect(!store.state.canSubmit)
        #expect(store.state.blockers == [.deadlinePassed])
        #expect(!store.state.isSubmitting)
    }

    @Test(arguments: [0.0, -3_600.0])
    func exactDeadlineAndAlreadyOpenPortalBeforeStartAllowSubmission(offset: TimeInterval) async {
        let attempts = LockIsolated(0)
        let store = TestStore(initialState: EnrollmentReviewFeature.State(session: Self.proposal())) {
            EnrollmentReviewFeature()
        } withDependencies: {
            $0.date = .constant(Self.deadline.addingTimeInterval(offset))
            $0.enrollmentRepository.submit = { _ in attempts.withValue { $0 += 1 } }
        }
        store.exhaustivity = .off

        await store.send(.task)
        #expect(store.state.canSubmit)
        await store.send(.submitTapped)
        await store.receive(.submitSucceeded)
        await store.receive(.delegate(.submitted))
        await store.finish()
        #expect(attempts.value == 1)
        #expect(store.state.isReadonly)
    }

    private static func proposal() -> EnrollmentSession {
        var session = EnrollmentSession.preview
        session.window?.startDate = deadline.addingTimeInterval(-1_800)
        session.window?.endDate = deadline
        return session
    }
}
