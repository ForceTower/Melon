import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

// Serialized: the single-flight latch is process-global, so parallel tests
// could coalesce into each other's in-flight refresh.
@Suite(.serialized)
struct PushBackgroundSyncTests {
    @Test
    func aPayloadWithoutKindDoesNotRefresh() async {
        let refreshes = LockIsolated(0)
        let result = await withDependencies {
            $0.homeRepository.refresh = { _ in refreshes.withValue { $0 += 1 } }
            $0.date = .constant(Date(timeIntervalSince1970: 0))
        } operation: {
            await PushEvents.backgroundSync(["url": "unes://classes"])
        }
        #expect(result == .noData)
        #expect(refreshes.value == 0)
    }

    @Test
    func aWakeRefreshesTheMirror() async {
        let refreshes = LockIsolated(0)
        let result = await withDependencies {
            $0.homeRepository.refresh = { _ in refreshes.withValue { $0 += 1 } }
            $0.date = .constant(Date(timeIntervalSince1970: 0))
        } operation: {
            await PushEvents.backgroundSync(["kind": "grade_added"])
        }
        #expect(result == .newData)
        #expect(refreshes.value == 1)
    }

    @Test
    func overlappingWakesCoalesceIntoOneRefresh() async {
        let refreshes = LockIsolated(0)
        let results = await withDependencies {
            $0.homeRepository.refresh = { _ in
                refreshes.withValue { $0 += 1 }
                try await Task.sleep(nanoseconds: 100_000_000)
            }
            $0.date = .constant(Date(timeIntervalSince1970: 0))
        } operation: {
            async let first = PushEvents.backgroundSync(["kind": "grade_added"])
            async let second = PushEvents.backgroundSync(["kind": "message"])
            return await [first, second]
        }
        #expect(results == [.newData, .newData])
        #expect(refreshes.value == 1)
    }

    @Test
    func aFailedRefreshReportsFailed() async {
        struct RefreshError: Error {}
        let result = await withDependencies {
            $0.homeRepository.refresh = { _ in throw RefreshError() }
            $0.date = .constant(Date(timeIntervalSince1970: 0))
        } operation: {
            await PushEvents.backgroundSync(["kind": "message"])
        }
        #expect(result == .failed)
    }
}
