import ComposableArchitecture
import Foundation
import Security
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
    func aChainRejectedBehindAGenericHandshakeFailureIsAnInterception() {
        // App Transport Security reports a re-signed chain as -1200, not -1202.
        let rejected = ProxyChain.trust(on: "2026-06-01T00:00:00Z")
        #expect(TLSInterception(ProxyChain.handshakeFailure(rejected)) == TLSInterception(issuerName: nil))
    }

    @Test
    func aHandshakeFailureWithAnAcceptableChainIsNot() {
        // Trusted but out of date: the device clock, not the network.
        let expired = ProxyChain.trust(on: "2027-06-01T00:00:00Z", trustingProxyCA: true)
        #expect(TLSInterception(ProxyChain.handshakeFailure(expired)) == nil)
        // Trusted and in date: the handshake failed for another reason.
        let trusted = ProxyChain.trust(on: "2026-06-01T00:00:00Z", trustingProxyCA: true)
        #expect(TLSInterception(ProxyChain.handshakeFailure(trusted)) == nil)
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
            let rejected = ProxyChain.trust(on: "2026-06-01T00:00:00Z")
            client?.urlProtocol(self, didFailWithError: ProxyChain.handshakeFailure(rejected))
            return
        }
        let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: "HTTP/1.1", headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(#"{"ok":true,"message":"","data":{}}"#.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

/// `melon.test` as a re-signing proxy presents it: a leaf valid through 2026,
/// signed by the proxy's own CA and sent without it, like a campus FortiGate.
private enum ProxyChain {
    /// The leaf as the device judges it at `date`; `trustingProxyCA` stands in
    /// for a phone that installed the network's CA.
    static func trust(on date: String, trustingProxyCA: Bool = false) -> SecTrust {
        var trust: SecTrust?
        SecTrustCreateWithCertificates(certificate(leaf), SecPolicyCreateSSL(true, "melon.test" as CFString), &trust)
        SecTrustSetVerifyDate(trust!, try! Date(date, strategy: .iso8601) as CFDate)
        if trustingProxyCA {
            SecTrustSetAnchorCertificates(trust!, [certificate(proxyCA)] as CFArray)
        }
        return trust!
    }

    /// What URLSession throws when App Transport Security rejects the chain.
    static func handshakeFailure(_ trust: SecTrust) -> URLError {
        URLError(.secureConnectionFailed, userInfo: [NSURLErrorFailingURLPeerTrustErrorKey: trust])
    }

    private static func certificate(_ base64: String) -> SecCertificate {
        SecCertificateCreateWithData(nil, Data(base64Encoded: base64)! as CFData)!
    }

    /// CN=melon.test, issued by `proxyCA`, valid 2026-01-01 through 2026-12-31.
    private static let leaf = """
    MIIB7jCCAZSgAwIBAgICEAEwCgYIKoZIzj0EAwIwWzELMAkGA1UEBhMCVVMxETAPBgNVBAoMCEZvcnRpbmV0MR4wHAYDVQQLDBVD\
    ZXJ0aWZpY2F0ZSBBdXRob3JpdHkxGTAXBgNVBAMMEEZHMTAwRlRLMDAwMDAwMDAwHhcNMjYwMTAxMDAwMDAwWhcNMjYxMjMxMjM1\
    OTU5WjAVMRMwEQYDVQQDDAptZWxvbi50ZXN0MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2GObfa4yTZRcthIfkWq2Hmmy0jO0\
    PESJGueyPpQOSIO3GWpN3Rz2wI7fD4ShZr8jgqG/9Mj7yGyCvCzv2d9BbaOBjTCBijAMBgNVHRMBAf8EAjAAMA4GA1UdDwEB/wQE\
    AwIHgDATBgNVHSUEDDAKBggrBgEFBQcDATAVBgNVHREEDjAMggptZWxvbi50ZXN0MB0GA1UdDgQWBBS4nh3AOO6JJbO0SZ8lXyyb\
    KWzNyTAfBgNVHSMEGDAWgBR1VEpkCW5J+3HxPvaQCkgmBDYb3jAKBggqhkjOPQQDAgNIADBFAiBMMLwn6JSAtWfyLcQ5s7lmfggu\
    kW5B5TcE5xXzbenv+wIhALE4EKBE46fxsfm3OYLNeqkejAdmaRnBj+4QTguBXVZg
    """

    /// Self-signed `O=Fortinet, CN=FG100FTK00000000`, valid 2026 through 2035.
    private static let proxyCA = """
    MIICGjCCAcGgAwIBAgIUPHTZa/wLr44iL3LMmjvc25yl+1kwCgYIKoZIzj0EAwIwWzELMAkGA1UEBhMCVVMxETAPBgNVBAoMCEZv\
    cnRpbmV0MR4wHAYDVQQLDBVDZXJ0aWZpY2F0ZSBBdXRob3JpdHkxGTAXBgNVBAMMEEZHMTAwRlRLMDAwMDAwMDAwHhcNMjYwMTAx\
    MDAwMDAwWhcNMzYwMTAxMDAwMDAwWjBbMQswCQYDVQQGEwJVUzERMA8GA1UECgwIRm9ydGluZXQxHjAcBgNVBAsMFUNlcnRpZmlj\
    YXRlIEF1dGhvcml0eTEZMBcGA1UEAwwQRkcxMDBGVEswMDAwMDAwMDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABCatDxpZo92+\
    wHZeuaDR9EG0I9FxvmSB1a4aO3NYpJ6zz/owDrZDdyULrpbfIsmSaOfoE2JfX9LmbQZChKfT5myjYzBhMB0GA1UdDgQWBBR1VEpk\
    CW5J+3HxPvaQCkgmBDYb3jAfBgNVHSMEGDAWgBR1VEpkCW5J+3HxPvaQCkgmBDYb3jAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB\
    /wQEAwIBBjAKBggqhkjOPQQDAgNHADBEAh92yNO+Zffcq0f1/bg4SoE/YVBU6YzPGI3BG5pmSkfyAiEAnF81gmVD2Su6RsbHcawZ\
    C3oonc9J8N/eWrLt/edZpEY=
    """
}
