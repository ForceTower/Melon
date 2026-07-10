import Foundation

/// Authenticated session against apps/api. Access token is a long-lived JWT;
/// the refresh token is single-use (rotated by `api/auth/token/refresh`).
struct Session: Equatable, Sendable, Codable {
    var accessToken: String
    var refreshToken: String
    var user: SessionUser
}

struct SessionUser: Equatable, Sendable, Codable, Identifiable {
    let id: String
    var name: String
    var imageUrl: String?
}

extension Session {
    static let preview = Session(
        accessToken: "preview-token",
        refreshToken: "preview-refresh",
        user: SessionUser(id: "preview", name: "Mariana Souza", imageUrl: nil)
    )
}
