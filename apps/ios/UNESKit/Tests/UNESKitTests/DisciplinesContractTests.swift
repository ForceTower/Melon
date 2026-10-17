import Foundation
import Testing

@testable import UNESKit

/// `contracts/v1/disciplines.json`, the semesters behind the Android
/// Disciplines journeys. Its past semester's Probabilidade was taken as a 60h
/// offer, while the catalog row carries the 75h synced later.
private struct DisciplinesContract {
    let current: SemesterPayloadDTO
    let past: SemesterPayloadDTO
    let now = Date(timeIntervalSince1970: 1_790_946_000)

    init() throws {
        var root = URL(fileURLWithPath: #filePath)
        for _ in 0..<6 { root.deleteLastPathComponent() }
        let data = try Data(contentsOf: root.appending(path: "contracts/v1/disciplines.json"))
        struct Contract: Decodable {
            let current: SemesterPayloadDTO
            let past: SemesterPayloadDTO
        }
        let contract = try JSONDecoder().decode(Contract.self, from: data)
        current = contract.current
        past = contract.past
    }
}

struct DisciplinesContractTests {
    @Test("A past discipline counts the hours of the offer taken")
    func pastDisciplineUsesOfferHours() throws {
        let fixture = try DisciplinesContract()
        let snapshot = fixture.past.snapshot
        let summary = try #require(
            snapshot.disciplineSummaries(now: fixture.now).first { $0.name == "Probabilidade de Exemplo" }
        )
        #expect(summary.hours == 60)
        #expect(summary.allowedMissedHours == 15)
        let detail = try #require(snapshot.disciplineDetail(disciplineId: summary.id, now: fixture.now))
        #expect(detail.hours == 60)
    }

    @Test("The CR weighs each closed grade by the offer hours, a failure without a mean as 0")
    func coefficientUsesOfferHours() throws {
        let fixture = try DisciplinesContract()
        let snapshots = [fixture.past.snapshot, fixture.current.snapshot]
        let history = CoefficientHistory(
            semesters: snapshots.map(\.semester),
            disciplines: snapshots.flatMap(\.disciplines),
            disciplineOffers: snapshots.flatMap(\.disciplineOffers),
            classes: snapshots.flatMap(\.classes),
            studentClasses: snapshots.flatMap(\.studentClasses)
        )
        // 2026.1: (8.5×60 + 3.0×60 + 0×45) / 165, Desenho failing for
        // absences without a mean. 2026.2 adds Cálculo's 4.5×60, which counts
        // while its Prova Final is pending, as on Android.
        let spark = history.checkpoints(track: nil).map(\.value)
        #expect(spark.count == 2)
        #expect(abs((spark.first ?? 0) - 690.0 / 165.0) < 1e-9)
        #expect(abs((spark.last ?? 0) - 960.0 / 225.0) < 1e-9)
    }
}
