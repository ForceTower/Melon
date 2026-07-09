import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

@MainActor
struct MeDocumentFeatureTests {
    @Test
    func downloadWithoutSiteKeySkipsTheCaptcha() async {
        let url = URL(fileURLWithPath: "/tmp/comprovante-matricula.pdf")
        let store = TestStore(
            initialState: MeDocumentFeature.State(document: .enrollmentCertificate)
        ) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { document, token in
                #expect(document == .enrollmentCertificate)
                #expect(token == nil)
                return url
            }
        }

        await store.send(.downloadTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentReady(url)) {
            $0.stage = .ready(url)
        }
    }

    @Test
    func siteKeyGatesTheDownloadBehindTheCaptcha() async {
        let url = URL(fileURLWithPath: "/tmp/historico-escolar.pdf")
        let initialState = MeDocumentFeature.State(document: .academicHistory)
        initialState.$captchaSiteKey.withLock { $0 = "site-key" }

        let store = TestStore(initialState: initialState) {
            MeDocumentFeature()
        } withDependencies: {
            $0.documentsRepository.fetch = { _, token in
                #expect(token == "captcha-token")
                return url
            }
        }

        await store.send(.downloadTapped) {
            $0.stage = .captcha
        }
        await store.send(.captchaSolved(token: "captcha-token")) {
            $0.stage = .generating
        }
        await store.receive(.documentReady(url)) {
            $0.stage = .ready(url)
        }
    }

    @Test
    func fetchFailureLandsOnRetry() async {
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
        await store.receive(.documentFailed) {
            $0.stage = .failed
        }
        await store.send(.retryTapped) {
            $0.stage = .generating
        }
        await store.receive(.documentFailed) {
            $0.stage = .failed
        }
    }
}
