import Foundation
import GRDB

extension MirrorStore {
    /// Semester labels for the materials upload sheet's quick-picks, newest
    /// first. The semester list is already mirrored for Turmas, so this needs
    /// no network and works offline.
    ///
    /// Suggestions only — the upload field is free text, since a student can
    /// legitimately tag something from a term that never reached the mirror.
    func uploadSemesterLabels() async throws -> [String] {
        let codes = try await writer.read { db in
            try SemesterRecord
                .order(Column("startDate").desc)
                .fetchAll(db)
                .map(\.code)
        }
        var seen = Set<String>()
        return codes.map(DisciplinesFormat.semesterLabel).filter { seen.insert($0).inserted }
    }
}
