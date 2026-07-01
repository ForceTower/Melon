import Foundation

enum APIError: Error, Equatable {
    case invalidResponse
    /// Non-2xx status; `message` is the envelope message when the body had one.
    case server(status: Int, message: String?)
    /// 2xx response whose envelope had `ok: false` or no `data`.
    case emptyEnvelope
}
