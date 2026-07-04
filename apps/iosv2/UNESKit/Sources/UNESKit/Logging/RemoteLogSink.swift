import Foundation
import os

/// Ships log records to the Melon API's `/api/logs`, which re-emits them to
/// the OTel backend server-side under this app's service name and stamps the
/// device's machine id from the `X-Machine-Id` header (see
/// `apps/api/src/api/logs.ts`; the v1 pipeline is `ApiLogWriter` in
/// `packages/shared-kmp`). The endpoint is unauthenticated by design, so the
/// watch ships its own logs too — as `melon-watchos`, under the watch's own
/// machine id.
///
/// Records buffer in memory and flush `flushInterval` after the first record
/// of an idle window arrives — no timers run while nothing is logged. The
/// buffer is bounded and drops the newest records when full: logging must
/// never block the caller or pile up unbounded. Transport failures drop the
/// batch silently — reporting them through this same logger would recurse.
final class RemoteLogSink: Sendable {

    private struct Shipment: Encodable {
        let service: String
        let records: [Record]
    }

    /// Wire format of one record — mirrors `logRecordSchema` in the API.
    private struct Record: Encodable, Sendable {
        let timestamp: Int64
        let severity: String
        let message: String
        let attributes: [String: String]
    }

    private struct State {
        var buffer: [Record] = []
        var flushScheduled = false
    }

    private let endpoint: URL
    private let service: String
    private let session: URLSession
    private let flushInterval: Duration
    private let maxBatchSize: Int
    private let bufferCapacity: Int
    private let state = OSAllocatedUnfairLock(initialState: State())

    init(
        baseURL: URL = MelonAPI.baseURL,
        service: String = RemoteLogSink.defaultService,
        session: URLSession = .shared,
        flushInterval: Duration = .seconds(5),
        maxBatchSize: Int = 50,
        bufferCapacity: Int = 1024
    ) {
        self.endpoint = baseURL.appending(path: "api/logs")
        self.service = service
        self.session = session
        self.flushInterval = flushInterval
        self.maxBatchSize = maxBatchSize
        self.bufferCapacity = bufferCapacity
    }

    func enqueue(_ level: LogLevel, tag: String, message: String, error: (any Error)?) {
        var attributes = [
            "log.tag": tag,
            "deployment.environment": Self.environment,
        ]
        if let error {
            attributes["exception.type"] = String(reflecting: type(of: error))
            attributes["exception.message"] = String(describing: error)
        }
        let record = Record(
            timestamp: Int64(Date().timeIntervalSince1970 * 1000),
            severity: level.wireSeverity,
            message: message,
            attributes: attributes
        )
        let shouldScheduleFlush = state.withLock { state in
            guard state.buffer.count < bufferCapacity else { return false }
            state.buffer.append(record)
            guard !state.flushScheduled else { return false }
            state.flushScheduled = true
            return true
        }
        if shouldScheduleFlush {
            Task {
                try? await Task.sleep(for: flushInterval)
                await self.flush()
            }
        }
    }

    private func flush() async {
        let records = state.withLock { state in
            let drained = state.buffer
            state.buffer.removeAll()
            state.flushScheduled = false
            return drained
        }
        for start in stride(from: 0, to: records.count, by: maxBatchSize) {
            await post(Array(records[start..<min(start + maxBatchSize, records.count)]))
        }
    }

    private func post(_ records: [Record]) async {
        guard let body = try? JSONEncoder().encode(Shipment(service: service, records: records)) else { return }
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.httpBody = body
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(MachineIdentity.id, forHTTPHeaderField: "X-Machine-Id")
        _ = try? await session.data(for: request)
    }

    static let defaultService: String = {
        #if os(watchOS)
        "melon-watchos"
        #else
        "melon-iosv2"
        #endif
    }()

    private static let environment: String = {
        #if DEBUG
        "debug"
        #else
        "release"
        #endif
    }()
}

private extension LogLevel {
    var wireSeverity: String {
        switch self {
        case .debug: "debug"
        case .info: "info"
        case .warn: "warn"
        case .error: "error"
        }
    }
}
