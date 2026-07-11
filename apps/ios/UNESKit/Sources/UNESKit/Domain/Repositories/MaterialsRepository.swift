import ComposableArchitecture
import Foundation

/// Student-contributed study materials. The feature is online-only: every
/// screen fetches live and nothing is persisted on device — "Salvos" is a
/// server-synced bookmark, and `open` streams the file into a temp URL that
/// QuickLook consumes and the system discards.
@DependencyClient
struct MaterialsRepository: Sendable {
    var overview: @Sendable () async throws -> MaterialsOverview
    var discipline: @Sendable (_ id: String) async throws -> MaterialsDisciplineDetails
    var saved: @Sendable () async throws -> [Material]
    /// Toggles the "útil" vote; returns the new vote count.
    var setUseful: @Sendable (_ materialId: String, _ isUseful: Bool) async throws -> Int
    var setSaved: @Sendable (_ materialId: String, _ isSaved: Bool) async throws -> Void
    var report: @Sendable (_ materialId: String, _ reason: MaterialReportReason) async throws -> Void
    /// Resolves the file into a local temp URL for QuickLook (counts as a
    /// download server-side).
    var open: @Sendable (_ material: Material) async throws -> URL
    /// Sends a new upload into moderation; returns it as `pending`.
    var submit: @Sendable (_ submission: MaterialSubmission) async throws -> Material
}

extension MaterialsRepository: TestDependencyKey {
    static let testValue = MaterialsRepository()

    static let previewValue = MaterialsRepository(
        overview: { .preview() },
        discipline: { id in
            id == "d5" ? .previewEmpty() : .preview()
        },
        saved: { Material.preview().filter(\.isSaved) },
        setUseful: { _, isUseful in isUseful ? 129 : 128 },
        setSaved: { _, _ in },
        report: { _, _ in },
        open: { _ in URL(fileURLWithPath: "/dev/null") },
        submit: { submission in
            var material = Material.preview()[6]
            material.title = submission.title
            material.type = submission.type
            material.semester = submission.semester
            return material
        }
    )
}

extension DependencyValues {
    var materialsRepository: MaterialsRepository {
        get { self[MaterialsRepository.self] }
        set { self[MaterialsRepository.self] = newValue }
    }
}
