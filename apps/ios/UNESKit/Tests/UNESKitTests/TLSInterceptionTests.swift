import ComposableArchitecture
import Foundation
import Testing

@testable import UNESKit

/// Serialized because the stub transport reads a shared flag.
@Suite(.serialized)
struct TLSInterceptionTests {
    @Test
    func anUntrustedChainIsAnInterception() {
        #expect(TLSInterception(URLError(.serverCertificateUntrusted)) == TLSInterception(issuerName: nil))
        #expect(TLSInterception(URLError(.serverCertificateHasUnknownRoot)) == TLSInterception(issuerName: nil))
    }

    @Test
    func otherFailuresAreNot() {
        // A bad date points at the device clock, not at the network.
        #expect(TLSInterception(URLError(.serverCertificateHasBadDate)) == nil)
        #expect(TLSInterception(URLError(.serverCertificateNotYetValid)) == nil)
        #expect(TLSInterception(URLError(.secureConnectionFailed)) == nil)
        #expect(TLSInterception(URLError(.notConnectedToInternet)) == nil)
        #expect(TLSInterception(APIError.invalidResponse) == nil)
    }

    @Test
    func knownIssuersAreNamedAfterTheirProduct() {
        #expect(TLSInterception.productName(forIssuer: "FortiGate CA") == "Fortinet")
        #expect(TLSInterception.productName(forIssuer: "Kaspersky Anti-Virus Personal Root Certificate") == "Kaspersky")
        #expect(TLSInterception.productName(forIssuer: "Acme Corp Root") == nil)
    }

    @Test
    func theAPIClientRaisesItAndTheNextResponseClearsIt() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [InterceptingURLProtocol.self]
        let apiClient = APIClient.live(
            baseURL: URL(string: "https://melon.test")!,
            session: URLSession(configuration: configuration)
        )
        let request = APIRequest(path: "api/me/ping", authorization: .unauthenticated)

        try await withDependencies {
            $0.defaultInMemoryStorage = InMemoryStorage()
        } operation: {
            @Shared(.tlsInterception) var interception

            InterceptingURLProtocol.intercepting.setValue(true)
            await #expect(throws: URLError.self) { try await apiClient.send(request) }
            #expect(interception == TLSInterception(issuerName: nil))

            InterceptingURLProtocol.intercepting.setValue(false)
            _ = try await apiClient.send(request)
            #expect(interception == nil)
        }
    }
}

/// Fails the handshake like a re-signing middlebox while `intercepting` is set.
private final class InterceptingURLProtocol: URLProtocol {
    static let intercepting = LockIsolated(false)

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        if Self.intercepting.value {
            client?.urlProtocol(self, didFailWithError: URLError(.serverCertificateUntrusted))
            return
        }
        let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: "HTTP/1.1", headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(#"{"ok":true,"message":"","data":{}}"#.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}
