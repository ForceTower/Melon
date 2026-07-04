import ComposableArchitecture
import Foundation

private let log = Log.scoped("WatchRepository")

/// The watch's structured cache as a dependency; the WatchConnectivity
/// receiver is the only writer, screens and widget timelines the readers.
extension WatchStore: DependencyKey {
    static let liveValue: WatchStore = {
        do {
            return try WatchStore.onDisk()
        } catch {
            log.error("store open failed, falling back to memory", error: error)
            return try! WatchStore.inMemory()
        }
    }()

    static var testValue: WatchStore { try! .inMemory() }
    static var previewValue: WatchStore { try! .inMemory() }
}

extension DependencyValues {
    var watchStore: WatchStore {
        get { self[WatchStore.self] }
        set { self[WatchStore.self] = newValue }
    }
}

#if os(watchOS)
import WatchConnectivity
#if canImport(WidgetKit)
import WidgetKit
#endif

/// The watch app's data surface. `activate` brings the WatchConnectivity
/// session up so phone pushes land in the store; `observe` replays the cached
/// dataset and re-emits after every landed push (nil = signed out / never
/// synced), keeping every screen reactive to the underlying data.
@DependencyClient
struct WatchRepository: Sendable {
    var activate: @Sendable () -> Void
    var observe: @Sendable () -> AsyncStream<WatchSnapshot?> = { .finished }
}

extension WatchRepository: DependencyKey {
    static let liveValue = WatchRepository(
        activate: {
            WatchSessionReceiver.shared.activate()
        },
        observe: {
            @Dependency(\.watchStore) var wrappedStore
            let store = wrappedStore
            log.debug("observe subscribed")
            return AsyncStream { continuation in
                let task = Task {
                    // Observation only fails if the database itself is gone;
                    // ending the stream is all there is to do.
                    do {
                        for try await snapshot in store.updates() {
                            continuation.yield(snapshot)
                        }
                    } catch {
                        log.error("observe failed", error: error)
                    }
                    continuation.finish()
                }
                continuation.onTermination = { _ in task.cancel() }
            }
        }
    )

    static let testValue = WatchRepository()

    static let previewValue = WatchRepository(
        activate: {},
        observe: { AsyncStream { $0.yield(.preview()) } }
    )
}

extension DependencyValues {
    var watchRepository: WatchRepository {
        get { self[WatchRepository.self] }
        set { self[WatchRepository.self] = newValue }
    }
}

/// Owns the watch side of the WCSession and lands every received payload in
/// the store, which fans the change out to the app's observations and the
/// widget timelines.
private final class WatchSessionReceiver: NSObject, WCSessionDelegate, Sendable {
    static let shared = WatchSessionReceiver()

    func activate() {
        let session = WCSession.default
        guard session.delegate !== self else { return }
        session.delegate = self
        session.activate()
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
        log.info("activated state=\(activationState.rawValue)")
        // A payload pushed while the app wasn't running is delivered here.
        guard !session.receivedApplicationContext.isEmpty else { return }
        apply(context: session.receivedApplicationContext)
    }

    func session(_ session: WCSession, didReceiveApplicationContext context: [String: Any]) {
        apply(context: context)
    }

    private func apply(context: [String: Any]) {
        let snapshot: WatchSnapshot?
        if let data = context["snapshot"] as? Data {
            guard let decoded = try? JSONDecoder().decode(WatchSnapshot.self, from: data) else {
                log.warn("apply failed reason=decode")
                return
            }
            snapshot = decoded
        } else {
            // The phone pushed an empty context — signed out.
            snapshot = nil
        }

        Task {
            @Dependency(\.watchStore) var store
            do {
                try await store.apply(snapshot)
            } catch {
                log.error("apply failed reason=store", error: error)
                return
            }
            #if canImport(WidgetKit)
            WidgetCenter.shared.reloadAllTimelines()
            #endif
        }
    }
}
#endif
