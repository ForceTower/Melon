import CryptoKit
import Foundation

/// Stable per-install device identifier — the legacy apps' recipe: a UUID
/// minted on first launch and persisted, exposed only as its MD5 hex digest.
/// Shown on the Me screen (`AppInfo`) and sent as the `X-Machine-Id` header
/// so the backend can attribute records to a device without trusting client
/// payloads.
enum MachineIdentity {
    static var id: String {
        let defaults = UserDefaults.standard
        let installationId = defaults.string(forKey: "installationId") ?? {
            let minted = UUID().uuidString
            defaults.set(minted, forKey: "installationId")
            return minted
        }()
        return Insecure.MD5.hash(data: Data(installationId.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}
