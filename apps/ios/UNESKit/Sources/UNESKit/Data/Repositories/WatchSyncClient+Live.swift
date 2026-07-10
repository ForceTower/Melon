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
            @Dependency(\.messagesRepository) var wrappedMessagesRepository
            let messagesRepository = wrappedMessagesRepository
            let now = { @Sendable in date.now }
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

            // Read receipts from the watch land in the same markRead flow the
            // inbox uses (mirror overlay + server ack); the mirror write then
            // re-emits the watch payload, confirming the flip back to it.
            let bridge = WatchSessionBridge(
                onSessionReady: push,
                onReadReceipts: { ids in
                    log.info("read receipts received count=\(ids.count)")
                    Task {
                        for id in ids {
                            do {
                                try await messagesRepository.markRead(id, now())
                            } catch {
                                log.warn("read receipt markRead failed id=\(id)", error: error)
                            }
                        }
                    }
                },
                onSecretIconUnlocks: { raws in
                    let icons = raws.compactMap(AppIcon.init(rawValue:)).filter(\.isSecret)
                    guard !icons.isEmpty else { return }
                    log.info("secret icons unlocked from watch \(icons.map(\.rawValue))")
                    @Shared(.unlockedSecretIcons) var unlockedSecretIcons
                    $unlockedSecretIcons.withLock { $0.formUnion(icons) }
                }
            )
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
    private let onReadReceipts: @Sendable ([String]) -> Void
    private let onSecretIconUnlocks: @Sendable ([String]) -> Void

    init(
        onSessionReady: @escaping @Sendable () -> Void,
        onReadReceipts: @escaping @Sendable ([String]) -> Void,
        onSecretIconUnlocks: @escaping @Sendable ([String]) -> Void
    ) {
        self.onSessionReady = onSessionReady
        self.onReadReceipts = onReadReceipts
        self.onSecretIconUnlocks = onSecretIconUnlocks
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

    /// Live path — the watch prefers `sendMessage` while the phone is reachable.
    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        deliver(payload: message)
    }

    /// Queued path — the watch falls back to `transferUserInfo` when unreachable.
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String: Any] = [:]) {
        deliver(payload: userInfo)
    }

    private func deliver(payload: [String: Any]) {
        if let ids = payload["readMessageIds"] as? [String], !ids.isEmpty {
            onReadReceipts(ids)
        }
        if let raws = payload["unlockedAppIcons"] as? [String], !raws.isEmpty {
            onSecretIconUnlocks(raws)
        }
    }
}

#else

extension WatchSyncClient: DependencyKey {
    static let liveValue = WatchSyncClient(run: {})
}

#endif
