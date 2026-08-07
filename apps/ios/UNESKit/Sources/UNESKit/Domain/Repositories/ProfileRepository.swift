import ComposableArchitecture
import Foundation

@DependencyClient
struct ProfileRepository: Sendable {
    var current: @Sendable () async throws -> Profile
    /// `PATCH api/me/name` — nil clears the alternate name so the portal
    /// name takes over. The caller re-pulls `current` afterwards so state
    /// picks up whatever the server normalized (re-typing the official name
    /// stores null).
    var updateName: @Sendable (String?) async throws -> Void
    /// `POST api/me/picture` — multipart avatar upload. `mimeType` must be
    /// the real encoding of the bytes: the server sniffs magic numbers
    /// against it (jpeg/png/webp, ≤ 5 MB) and rejects a mismatch.
    var uploadPicture: @Sendable (_ data: Data, _ mimeType: String) async throws -> Void
    /// `DELETE api/me/picture` — back to the monogram everywhere.
    var deletePicture: @Sendable () async throws -> Void
}

extension ProfileRepository: TestDependencyKey {
    static let testValue = ProfileRepository()
    static let previewValue = ProfileRepository(
        current: { .preview },
        updateName: { _ in },
        uploadPicture: { _, _ in },
        deletePicture: {}
    )
}

extension DependencyValues {
    var profileRepository: ProfileRepository {
        get { self[ProfileRepository.self] }
        set { self[ProfileRepository.self] = newValue }
    }
}
