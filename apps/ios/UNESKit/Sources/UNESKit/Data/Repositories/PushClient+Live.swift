import ComposableArchitecture
import Foundation
import UserNotifications
#if canImport(UIKit)
import UIKit
#endif

private let log = Log.scoped("PushClient")

extension PushClient: DependencyKey {
    static let liveValue = PushClient(
        requestAuthorization: {
            log.debug("requestAuthorization start")
            do {
                let granted = try await UNUserNotificationCenter.current()
                    .requestAuthorization(options: [.alert, .badge, .sound])
                log.info("requestAuthorization ok granted=\(granted)")
            } catch {
                log.warn("requestAuthorization failed", error: error)
            }
        },
        fcmTokenReceived: { token in
            await registrar.fcmTokenReceived(token)
        },
        reconcile: {
            await registrar.reconcile()
        },
        unregister: {
            await registrar.unregister()
        },
        dataNotificationReceived: { data in
            guard let kind = data["kind"] else {
                log.debug("dataNotificationReceived ignored: no kind key")
                return
            }
            log.info("dataNotificationReceived kind=\(kind) -> publishing")
            hub.send(PushDataEvent(kind: kind))
        },
        dataEvents: { hub.stream() }
    )
}

private let registrar = PushRegistrar()

/// Owns the device's push registration: the FCM registration token is
/// registered with apps/api on every app open, and any Firebase Installation
/// ID row left behind by the retired FID-targeting builds is deleted
/// (FID-targeted sends stopped reaching some devices, so the clients are
/// fully back on tokens until FID push matures). The backend dedups rows by
/// identifier value, so a stale identifier's row stays live next to the token
/// row — and every push would arrive twice — until the delete lands; failed
/// deletes are queued and retried on later reconciles.
private actor PushRegistrar {
    /// Written by the retired FID-targeting builds; read once so the FID row
    /// a previous app version registered can be deleted, then cleared for
    /// good.
    private static let legacyFidKey = "messaging_installation_id"
    /// Same key the pre-FID builds wrote, so upgraded installs already have a
    /// token cached; Firebase re-delivers it on every launch either way.
    private static let tokenKey = "messaging_notification_token"
    private static let pendingDeletesKey = "messaging_pending_deletes"

    /// Token rotation: retire the previous token's row once the new one is
    /// registered, never before — deleting first would leave a push gap.
    func fcmTokenReceived(_ token: String) async {
        let previous = UserDefaults.standard.string(forKey: Self.tokenKey)
        UserDefaults.standard.set(token, forKey: Self.tokenKey)
        log.debug("fcm token stored length=\(token.count)")
        if let previous, previous != token {
            addPendingDelete(previous)
        }
        await reconcile()
    }

    func reconcile() async {
        @Dependency(\.sessionStore) var sessionStore
        guard sessionStore.current() != nil else {
            log.debug("reconcile deferred: no session")
            return
        }
        guard let token = UserDefaults.standard.string(forKey: Self.tokenKey) else {
            log.debug("reconcile: no fcm token available yet")
            await flushPendingDeletes()
            return
        }
        guard await register(token) else { return }
        // The FID builds queued this token's delete when they took over —
        // it's canonical again, so the queued delete would strand the device.
        removePendingDelete(token)
        // Queue the FID row's delete only after the token is registered —
        // deleting first would leave a push gap.
        if let fid = UserDefaults.standard.string(forKey: Self.legacyFidKey), fid != token {
            addPendingDelete(fid)
            UserDefaults.standard.removeObject(forKey: Self.legacyFidKey)
        }
        await flushPendingDeletes()
    }

    /// Logout teardown — best-effort: after logout there is nothing to retry
    /// with; the backend's invalid-token prune eventually collects what this
    /// misses.
    func unregister() async {
        var identifiers = Set(pendingDeletes())
        for key in [Self.legacyFidKey, Self.tokenKey] {
            if let value = UserDefaults.standard.string(forKey: key) {
                identifiers.insert(value)
            }
        }
        for identifier in identifiers {
            let deleted = await delete(identifier)
            if !deleted {
                log.warn("logout unregister failed for a push identifier")
            }
        }
        for key in [Self.legacyFidKey, Self.tokenKey, Self.pendingDeletesKey] {
            UserDefaults.standard.removeObject(forKey: key)
        }
    }

    /// Best-effort: Firebase re-delivers the token on every launch and
    /// reconcile re-runs on every foreground, so a failed registration heals
    /// on the next one.
    private func register(_ token: String) async -> Bool {
        @Dependency(\.apiClient) var apiClient
        #if canImport(UIKit) && !os(watchOS)
        let deviceName: String? = await UIDevice.current.name
        #else
        let deviceName: String? = nil
        #endif
        let body = RegisterTokenBody(
            token: token,
            identifierType: "fcm_token",
            platform: "ios",
            deviceName: deviceName,
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String,
            locale: Locale.current.identifier
        )
        log.debug("register start length=\(token.count)")
        do {
            try await apiClient.post(to: "api/notifications/token", body: body)
            log.info("register ok")
            return true
        } catch {
            switch error {
            case APIError.server(401, _):
                log.warn("register unauthorized")
            case let APIError.server(status, message):
                log.warn("register server \(status) message=\(message ?? "<none>")")
            case is URLError:
                log.warn("register transport failure", error: error)
            default:
                log.error("register failed", error: error)
            }
            return false
        }
    }

    private func delete(_ identifier: String) async -> Bool {
        @Dependency(\.apiClient) var apiClient
        do {
            try await apiClient.delete("api/notifications/token", body: UnregisterTokenBody(token: identifier))
            log.info("unregister ok")
            return true
        } catch {
            log.warn("unregister failed", error: error)
            return false
        }
    }

    private func flushPendingDeletes() async {
        for identifier in pendingDeletes() {
            if await delete(identifier) {
                removePendingDelete(identifier)
            } else {
                log.warn("stale push identifier delete failed — will retry")
            }
        }
    }

    private func pendingDeletes() -> [String] {
        UserDefaults.standard.stringArray(forKey: Self.pendingDeletesKey) ?? []
    }

    private func addPendingDelete(_ identifier: String) {
        var pending = pendingDeletes()
        guard !pending.contains(identifier) else { return }
        pending.append(identifier)
        UserDefaults.standard.set(pending, forKey: Self.pendingDeletesKey)
    }

    private func removePendingDelete(_ identifier: String) {
        let pending = pendingDeletes().filter { $0 != identifier }
        UserDefaults.standard.set(pending, forKey: Self.pendingDeletesKey)
    }
}

private let hub = PushEventHub()

/// Fans data pushes out from the app-delegate hand-off to whichever reducers
/// are subscribed at the time; pushes with no subscriber are dropped (the
/// next foreground sync picks the data up anyway).
private struct PushEventHub: Sendable {
    private let subscribers = LockIsolated<[UUID: AsyncStream<PushDataEvent>.Continuation]>([:])

    func stream() -> AsyncStream<PushDataEvent> {
        let (stream, continuation) = AsyncStream<PushDataEvent>.makeStream()
        let id = UUID()
        subscribers.withValue { $0[id] = continuation }
        continuation.onTermination = { [subscribers] _ in
            subscribers.withValue { $0[id] = nil }
        }
        return stream
    }

    func send(_ event: PushDataEvent) {
        subscribers.withValue { active in
            for continuation in active.values {
                continuation.yield(event)
            }
        }
    }
}

/// `token` carries whichever push identifier is canonical; `identifierType`
/// ("fcm_token" | "fid") tells the backend which one it is.
private struct RegisterTokenBody: Encodable {
    let token: String
    let identifierType: String
    let platform: String
    let deviceName: String?
    let appVersion: String?
    let locale: String
}

private struct UnregisterTokenBody: Encodable {
    let token: String
}
