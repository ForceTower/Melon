import Foundation

/// The semester half of a refresh, shared by the Home, Turmas, and Horário
/// repositories: pulls the list, then the payloads that warrant it — the
/// active semester plus any already-mirrored semester whose server `dirtyAt`
/// moved since its payload was applied — and mirrors everything.
/// Never-downloaded semesters are not pulled here; they keep the opt-in
/// "Baixar" flow.
enum SemesterRefresh {
    struct Summary {
        var semesterCount: Int
        var scopeCount: Int
    }

    static func run(apiClient: APIClient, mirror: MirrorStore, now: Date) async throws -> Summary {
        let list: SemesterListDTO = try await apiClient.get(from: "api/sync/semesters")
        let records = list.semesters.map(\.record)

        var targetIds: [String] = []
        if let active = list.semesters.map(\.domain).active(today: now.dayStamp) {
            targetIds.append(active.id)
        }
        for id in try await mirror.staleMirroredSemesterIds(in: records) where !targetIds.contains(id) {
            targetIds.append(id)
        }

        var snapshots: [SemesterSnapshot] = []
        for id in targetIds {
            let payload: SemesterPayloadDTO = try await apiClient.get(from: "api/sync/semesters/\(id)")
            snapshots.append(payload.snapshot)
        }
        try await mirror.apply(semesters: records, snapshots: snapshots, syncedAt: now)
        return Summary(semesterCount: records.count, scopeCount: snapshots.count)
    }
}
