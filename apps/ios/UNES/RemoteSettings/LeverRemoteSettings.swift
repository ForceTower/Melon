import Foundation
import Lever
import UNESKit

/// The top layer: our own remote config (https://github.com/ForceTower/lever).
///
/// Deliberately not a `RemoteSettings` — this source is allowed to have no
/// opinion. `lookup` returns `nil` when lever has not published a key (or
/// published something the key cannot read), which is what lets the composite
/// fall through to Firebase instead of committing to a default. A key lever
/// *has* published wins outright, `false` included.
///
/// The defaults handed to `LeverKey` are never served: `lookup` reports absence
/// rather than substituting them. They exist because the SDK's key type carries
/// one.
nonisolated struct LeverRemoteSettings: Sendable {
    private let client: LeverClient
    private let log = Log.scoped("LeverRemoteSettings")

    init(client: LeverClient) {
        self.client = client
    }

    func lookup(_ key: RemoteBoolKey) -> Bool? {
        client.lookup(LeverKey(key.rawValue, default: false))
    }

    func lookup(_ key: RemoteStringKey) -> String? {
        client.lookup(LeverKey(key.rawValue, default: ""))
    }

    /// The client fetches from the moment it is constructed, so there is
    /// nothing to kick off — only its activations to listen for. One stream
    /// carries the launch fetch, the foreground refresh, the polling floor, and
    /// the SSE nudge that lands a console publish while the app is open.
    func start(onChange: @escaping @Sendable () -> Void) {
        Task { [client, log] in
            // Reading `updates` registers the continuation, so taking it first
            // and reporting once covers anything that activated between the
            // client being constructed and this task running.
            let updates = client.updates
            onChange()
            for await update in updates {
                log.debug("lever activated version=\(update.version) changed=\(update.changedKeys.count)")
                onChange()
            }
        }
    }
}
