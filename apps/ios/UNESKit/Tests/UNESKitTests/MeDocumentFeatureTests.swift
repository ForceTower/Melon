import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct MeDocumentFeatureTests {
    static nonisolated let referenceDate = Date(timeIntervalSince1970: 1_776_000_000)

    static nonisolated let fetched = FetchedAcademicDocument(
        fileURL: URL(fileURLWithPath: "/tmp/downloads/comprovante-matricula.pdf"),
        isFresh: true,
        generatedAt: referenceDate
    )

    static nonisolated let stored = StoredAcademicDocument(
        fileURL: URL(fileURLWithPath: "/tmp/documents/comprovante-matricula.pdf"),
        version: 2,
        savedAt: referenceDate
    )

    @Test
    func firstDownloadSavesTheOfflineCopy() async {
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .enrollmentCertificate)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { document, token in
                #expect(document == .enrollmentCertificate)
                #expect(token == nil)
                return Self.fetched
            }
            $0.localDocuments.save = { _, fileURL in
                #expect(fileURL == Self.fetched.fileURL)
                return Self.stored
            }
        }

        #expect(store.state.stage == .intro)
        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentReady(Self.fetched)) {
            $0.stored = Self.stored
            $0.stage = .fresh
        }
    }

    @Test
    func siteKeyGatesTheDownloadAndCancelReturnsToSaved() async {
        let initialState = MeDocumentFeature.State(document: .academicHistory, stored: Self.stored)
        initialState.$captchaSiteKey.withLock { $0 = "site-key" }

        let store = TestStore(initialState: initialState) {
            MeDocumentFeature()
        }

        #expect(store.state.stage == .saved)
        await store.send(.downloadTapped) {
            $0.stage = .captcha
        }
        await store.send(.captchaCanceled) {
            $0.stage = .saved
        }
    }

    @Test
    func serverFallbackLandsStaleWithTheServerDate() async {
        let fallback = FetchedAcademicDocument(
            fileURL: URL(fileURLWithPath: "/tmp/downloads/historico-escolar.pdf"),
            isFresh: false,
            generatedAt: Date(timeIntervalSince1970: 1_770_000_000)
        )
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .academicHistory, stored: Self.stored)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { _, _ in fallback }
            $0.localDocuments.save = { _, _ in Self.stored }
        }

        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentReady(fallback)) {
            $0.stage = .stale(savedAt: fallback.generatedAt)
        }
    }

    @Test
    func refreshFailureFallsBackToTheOfflineCopy() async {
        struct Failure: Error {}
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .enrollmentCertificate, stored: Self.stored)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { _, _ in throw Failure() }
        }

        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentFailed(.connection)) {
            $0.stage = .stale(savedAt: Self.stored.savedAt)
        }
    }

    @Test
    func failureWithoutACopyShowsTheError() async {
        struct Failure: Error {}
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .enrollmentCertificate)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { _, _ in throw Failure() }
        }

        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentFailed(.connection)) {
            $0.stage = .failed(.connection)
        }
    }

    @Test
    func notFoundReadsAsUnavailableNotConnection() async {
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .enrollmentCertificate)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { _, _ in
                throw APIError.server(status: 404, message: "The portal did not produce this document")
            }
        }

        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentFailed(.unavailable)) {
            $0.stage = .failed(.unavailable)
        }
    }
}
