import ComposableArchitecture
import Foundation

private let log = Log.scoped("MaterialsRepository")

extension MaterialsRepository: DependencyKey {
    static let liveValue = MaterialsRepository(
        overview: {
            @Dependency(\.apiClient) var apiClient
            log.debug("overview start")
            do {
                let dto: MaterialsOverviewDTO = try await apiClient.get(from: "api/materials/overview")
                let overview = dto.domain
                log.info("overview ok disciplines=\(overview.disciplines.count) total=\(overview.totalCount)")
                return overview
            } catch {
                logFailure("overview", error: error)
                throw error
            }
        },
        discipline: { id in
            @Dependency(\.apiClient) var apiClient
            log.debug("discipline start id=\(id)")
            do {
                let dto: MaterialsDisciplineDTO = try await apiClient.get(
                    from: "api/materials/discipline",
                    query: [URLQueryItem(name: "id", value: id)]
                )
                let details = dto.domain
                log.info("discipline ok id=\(id) materials=\(details.materials.count)")
                return details
            } catch {
                logFailure("discipline", error: error)
                throw error
            }
        },
        saved: {
            @Dependency(\.apiClient) var apiClient
            log.debug("saved start")
            do {
                let dto: MaterialsListDTO = try await apiClient.get(from: "api/materials/saved")
                return dto.materials.compactMap(\.domain)
            } catch {
                logFailure("saved", error: error)
                throw error
            }
        },
        setUseful: { materialId, isUseful in
            @Dependency(\.apiClient) var apiClient
            log.info("set useful id=\(materialId) useful=\(isUseful)")
            do {
                let dto: MaterialUsefulDTO = try await apiClient.post(
                    to: "api/materials/useful",
                    query: [URLQueryItem(name: "id", value: materialId)],
                    body: MaterialUsefulRequest(useful: isUseful)
                )
                return dto.count
            } catch {
                logFailure("setUseful", error: error)
                throw error
            }
        },
        setSaved: { materialId, isSaved in
            @Dependency(\.apiClient) var apiClient
            log.info("set saved id=\(materialId) saved=\(isSaved)")
            do {
                try await apiClient.post(
                    to: "api/materials/save",
                    query: [URLQueryItem(name: "id", value: materialId)],
                    body: MaterialSavedRequest(saved: isSaved)
                )
            } catch {
                logFailure("setSaved", error: error)
                throw error
            }
        },
        report: { materialId, reason in
            @Dependency(\.apiClient) var apiClient
            log.info("report id=\(materialId) reason=\(reason.rawValue)")
            do {
                try await apiClient.post(
                    to: "api/materials/report",
                    query: [URLQueryItem(name: "id", value: materialId)],
                    body: MaterialReportRequest(reason: reason.rawValue)
                )
            } catch {
                logFailure("report", error: error)
                throw error
            }
        },
        open: { material in
            @Dependency(\.apiClient) var apiClient
            log.info("open start id=\(material.id)")
            do {
                let dto: MaterialDownloadDTO = try await apiClient.post(
                    to: "api/materials/open",
                    query: [URLQueryItem(name: "id", value: material.id)],
                    body: EmptyBody()
                )
                let url = try await downloadFile(from: dto.url, materialId: material.id, filename: dto.filename)
                log.info("open ok id=\(material.id)")
                return url
            } catch {
                logFailure("open", error: error)
                throw error
            }
        },
        submit: { submission in
            @Dependency(\.apiClient) var apiClient
            log.info("""
            submit start discipline=\(submission.disciplineId) type=\(submission.type.rawValue) \
            bytes=\(submission.data.count)
            """)
            do {
                // The file goes straight to storage through a presigned slot;
                // only the metadata rides the API envelope.
                let slot: MaterialUploadSlotDTO = try await apiClient.post(
                    to: "api/materials/uploads",
                    body: MaterialUploadSlotRequest(
                        fileName: submission.fileName,
                        byteCount: submission.data.count,
                        contentType: submission.fileKind == .pdf ? "application/pdf" : "image/jpeg"
                    )
                )
                try await uploadFile(submission.data, to: slot.url, fileKind: submission.fileKind)
                let dto: MaterialDTO = try await apiClient.post(
                    to: "api/materials",
                    body: MaterialSubmitRequest(
                        uploadId: slot.uploadId,
                        disciplineId: submission.disciplineId,
                        type: submission.type.rawValue,
                        title: submission.title,
                        semester: submission.semester,
                        teacherName: submission.teacherName,
                        fileKind: submission.fileKind.rawValue,
                        pages: submission.pages
                    )
                )
                guard let material = dto.domain else { throw APIError.emptyEnvelope }
                log.info("submit ok id=\(material.id)")
                return material
            } catch {
                logFailure("submit", error: error)
                throw error
            }
        }
    )

    private static func logFailure(_ operation: String, error: Error) {
        switch error {
        case APIError.server(401, _):
            log.warn("\(operation) unauthorized")
        case let APIError.server(status, message):
            log.warn("\(operation) server \(status) message=\(message ?? "<none>")")
        case APIError.emptyEnvelope:
            log.warn("\(operation) 2xx envelope had null data")
        case is URLError:
            log.warn("\(operation) transport failure", error: error)
        default:
            log.error("\(operation) failed", error: error)
        }
    }
}

/// The presigned URL is its own bearer token — plain requests, no session
/// header (same recipe as `DocumentsRepository`).
///
/// Every scan uploads as "digitalizacao.pdf", so the id keys the directory to
/// stop distinct materials overwriting each other.
private func downloadFile(from url: URL, materialId: String, filename: String) async throws -> URL {
    let (data, response) = try await URLSession.shared.data(from: url)
    guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
        throw APIError.invalidResponse
    }
    let directory = FileManager.default.temporaryDirectory
        .appendingPathComponent("materials", isDirectory: true)
        .appendingPathComponent(safeFileName(materialId), isDirectory: true)
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    let destination = directory.appendingPathComponent(safeFileName(filename))
    try data.write(to: destination, options: .atomic)
    return destination
}

private func uploadFile(_ data: Data, to url: URL, fileKind: MaterialFileKind) async throws {
    var request = URLRequest(url: url)
    request.httpMethod = "PUT"
    request.setValue(
        fileKind == .pdf ? "application/pdf" : "image/jpeg",
        forHTTPHeaderField: "Content-Type"
    )
    let (_, response) = try await URLSession.shared.upload(for: request, from: data)
    guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
        throw APIError.invalidResponse
    }
}

private struct EmptyBody: Encodable {}

// MARK: - DTOs (`api/materials/*`)

private struct MaterialsOverviewDTO: Decodable {
    struct Discipline: Decodable {
        var id: String
        var code: String
        var name: String
        var teacherName: String? = nil
        var counts: [String: Int]? = nil
    }

    var semester: String
    var disciplines: [Discipline]
    var savedCount: Int? = nil

    var domain: MaterialsOverview {
        MaterialsOverview(
            semester: semester,
            disciplines: disciplines.enumerated().map { index, discipline in
                MaterialsDiscipline(
                    id: discipline.id,
                    code: discipline.code,
                    name: discipline.name,
                    teacherName: discipline.teacherName,
                    colorIndex: index,
                    counts: materialCounts(from: discipline.counts ?? [:])
                )
            },
            savedCount: savedCount ?? 0
        )
    }
}

/// Unknown type keys are newer server categories — dropped, not failed.
private func materialCounts(from wire: [String: Int]) -> [MaterialType: Int] {
    wire.reduce(into: [:]) { counts, entry in
        guard let type = MaterialType(rawValue: entry.key) else { return }
        counts[type] = entry.value
    }
}

private struct MaterialUploaderDTO: Decodable {
    var course: String
    var entryYear: Int

    var domain: MaterialUploader { MaterialUploader(course: course, entryYear: entryYear) }
}

private struct MaterialDTO: Decodable {
    struct Discipline: Decodable {
        var id: String
        var code: String
        var name: String
    }

    var id: String
    var discipline: Discipline
    var type: String
    var title: String
    var teacherName: String? = nil
    var semester: String
    var pages: Int? = nil
    var fileKind: String
    var usefulCount: Int? = nil
    var downloadCount: Int? = nil
    var uploader: MaterialUploaderDTO
    var note: String? = nil
    var mine: Bool? = nil
    var status: String? = nil
    var rejectionReason: String? = nil
    var useful: Bool? = nil
    var saved: Bool? = nil

    /// Unknown types/statuses are newer server vocabulary — dropped, not
    /// failed.
    var domain: Material? {
        guard let type = MaterialType(rawValue: type),
              let status = MaterialStatus(rawValue: status ?? "published")
        else { return nil }
        return Material(
            id: id,
            discipline: MaterialDisciplineRef(
                id: discipline.id,
                code: discipline.code,
                name: discipline.name,
                colorIndex: MaterialsFormat.stableColorIndex(for: discipline.code)
            ),
            type: type,
            title: title,
            teacherName: teacherName,
            semester: semester,
            pages: pages ?? 1,
            fileKind: MaterialFileKind(rawValue: fileKind) ?? .pdf,
            usefulCount: usefulCount ?? 0,
            downloadCount: downloadCount ?? 0,
            uploader: uploader.domain,
            note: note,
            isMine: mine ?? false,
            status: status,
            rejectionReason: rejectionReason,
            isUseful: useful ?? false,
            isSaved: saved ?? false
        )
    }
}

private struct MaterialsDisciplineDTO: Decodable {
    struct Discipline: Decodable {
        var id: String
        var code: String
        var name: String
        var teacherName: String? = nil
        var counts: [String: Int]? = nil
    }

    var discipline: Discipline
    var materials: [MaterialDTO]

    var domain: MaterialsDisciplineDetails {
        MaterialsDisciplineDetails(
            discipline: MaterialsDiscipline(
                id: discipline.id,
                code: discipline.code,
                name: discipline.name,
                teacherName: discipline.teacherName,
                colorIndex: MaterialsFormat.stableColorIndex(for: discipline.code),
                counts: materialCounts(from: discipline.counts ?? [:])
            ),
            materials: materials.compactMap(\.domain)
        )
    }
}

private struct MaterialsListDTO: Decodable {
    var materials: [MaterialDTO]
}

private struct MaterialUsefulRequest: Encodable {
    var useful: Bool
}

private struct MaterialUsefulDTO: Decodable {
    var count: Int
}

private struct MaterialSavedRequest: Encodable {
    var saved: Bool
}

private struct MaterialReportRequest: Encodable {
    var reason: String
}

private struct MaterialDownloadDTO: Decodable {
    var url: URL
    var filename: String
}

private struct MaterialUploadSlotRequest: Encodable {
    var fileName: String
    var byteCount: Int
    var contentType: String
}

private struct MaterialUploadSlotDTO: Decodable {
    var uploadId: String
    var url: URL
}

private struct MaterialSubmitRequest: Encodable {
    var uploadId: String
    var disciplineId: String
    var type: String
    var title: String
    var semester: String
    var teacherName: String?
    var fileKind: String
    var pages: Int
}
