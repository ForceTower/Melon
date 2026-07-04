import ComposableArchitecture
import Foundation
#if os(iOS) && canImport(WatchConnectivity)
import WatchConnectivity
#endif

private let log = Log.scoped("WatchSyncClient")

#if os(iOS) && canImport(WatchConnectivity)

extension WatchSyncClient: DependencyKey {
    static let liveValue = WatchSyncClient(
        run: {
            guard WCSession.isSupported() else {
                log.debug("run skipped: WCSession unsupported")
                return
            }
            @Dependency(\.database) var database
            @Dependency(\.date) var date
            let mirror = MirrorStore(writer: database)

            // Outer nil: nothing emitted yet. Inner nil: signed out → clear.
            let latest = LockIsolated<Data??>(nil)
            let push: @Sendable () -> Void = {
                guard let payload = latest.value else { return }
                let session = WCSession.default
                guard session.activationState == .activated else { return }
                do {
                    try session.updateApplicationContext(payload.map { ["snapshot": $0] } ?? [:])
                    log.debug("push ok hasSnapshot=\(payload != nil)")
                } catch {
                    // Heals on the next mirror emission or (re)activation.
                    log.warn("push failed", error: error)
                }
            }

            let bridge = WatchSessionBridge(onSessionReady: push)
            WCSession.default.delegate = bridge
            WCSession.default.activate()
            log.debug("run subscribed")

            do {
                for try await snapshot in mirror.watchSnapshotUpdates(now: { date.now }) {
                    // Signed out (or encode failure): clear the watch.
                    latest.setValue(.some(snapshot.flatMap { try? JSONEncoder().encode($0) }))
                    push()
                }
            } catch {
                log.error("run failed", error: error)
            }
            withExtendedLifetime(bridge) {}
        }
    )
}

/// The session needs a delegate for its whole lifetime; pushes are retried
/// from `onSessionReady` whenever the session becomes usable again.
private final class WatchSessionBridge: NSObject, WCSessionDelegate, Sendable {
    private let onSessionReady: @Sendable () -> Void

    init(onSessionReady: @escaping @Sendable () -> Void) {
        self.onSessionReady = onSessionReady
    }

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: (any Error)?
    ) {
        if let error {
            log.warn("activation failed", error: error)
            return
        }
        log.info("activated state=\(activationState.rawValue) paired=\(session.isPaired) installed=\(session.isWatchAppInstalled)")
        onSessionReady()
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// A new watch was paired — reactivate so it gets the payload too.
    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }

    func sessionWatchStateDidChange(_ session: WCSession) {
        guard session.isPaired, session.isWatchAppInstalled else { return }
        onSessionReady()
    }
}

#else

extension WatchSyncClient: DependencyKey {
    static let liveValue = WatchSyncClient(run: {})
}

#endif
