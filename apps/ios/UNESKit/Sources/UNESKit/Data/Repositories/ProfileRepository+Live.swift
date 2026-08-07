import ComposableArchitecture
import Foundation

private let log = Log.scoped("ProfileRepository")

/// The `name` key is required but nullable server-side: null (or blank, which
/// the API also nulls) clears the alternate name, while a missing key is a
/// 400. Synthesized encoding would drop a nil through `encodeIfPresent`, so
/// the key is written by hand to guarantee `{"name":null}` goes over the wire.
private struct UpdateNameBody: Encodable {
    let name: String?

    private enum CodingKeys: String, CodingKey { case name }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(name, forKey: .name)
    }
}

extension ProfileRepository: DependencyKey {
    static let liveValue = ProfileRepository(
        current: {
            @Dependency(\.apiClient) var apiClient
            log.debug("current start")
            do {
                let dto = try await apiClient.get(ProfileDTO.self, from: "api/sync/profile")
                log.info("current ok userId=\(dto.user.id)")
                return dto.domain
            } catch {
                switch error {
                case APIError.server(401, _):
                    log.warn("current unauthorized")
                case let APIError.server(status, message):
                    log.warn("current server \(status) message=\(message ?? "<none>")")
                case APIError.emptyEnvelope:
                    log.warn("current 2xx envelope had null data")
                case is URLError:
                    log.warn("current transport failure", error: error)
                default:
                    log.error("current failed", error: error)
                }
                throw error
            }
        },
        updateName: { name in
            @Dependency(\.apiClient) var apiClient
            log.debug("updateName start clearing=\(name == nil)")
            do {
                try await apiClient.patch(at: "api/me/name", body: UpdateNameBody(name: name))
                log.info("updateName ok")
            } catch {
                log.warn("updateName failed", error: error)
                throw error
            }
        },
        uploadPicture: { data, mimeType in
            @Dependency(\.apiClient) var apiClient
            log.debug("uploadPicture start bytes=\(data.count) mime=\(mimeType)")
            do {
                let boundary = "unes-\(UUID().uuidString)"
                _ = try await apiClient.send(APIRequest(
                    method: "POST",
                    path: "api/me/picture",
                    body: multipartBody(data: data, mimeType: mimeType, boundary: boundary),
                    contentType: "multipart/form-data; boundary=\(boundary)"
                ))
                log.info("uploadPicture ok")
            } catch {
                log.warn("uploadPicture failed", error: error)
                throw error
            }
        },
        deletePicture: {
            @Dependency(\.apiClient) var apiClient
            log.debug("deletePicture start")
            do {
                try await apiClient.delete("api/me/picture")
                log.info("deletePicture ok")
            } catch {
                log.warn("deletePicture failed", error: error)
                throw error
            }
        }
    )

    /// Single-part `multipart/form-data` payload carrying the avatar under
    /// the `file` field, the shape `POST api/me/picture` expects. The part's
    /// Content-Type must be the image's real MIME — the server verifies magic
    /// numbers against it.
    private static func multipartBody(data: Data, mimeType: String, boundary: String) -> Data {
        var body = Data()
        body.append(Data("--\(boundary)\r\n".utf8))
        body.append(Data("Content-Disposition: form-data; name=\"file\"; filename=\"avatar\"\r\n".utf8))
        body.append(Data("Content-Type: \(mimeType)\r\n\r\n".utf8))
        body.append(data)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        return body
    }
}
