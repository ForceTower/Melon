import ComposableArchitecture
import Foundation
import Security

/// The network re-signs the API's TLS (campus Fortinet, antivirus HTTPS
/// inspection). Repositories surface it as a plain transport error, so this is
/// what lets Hoje explain why nothing syncs. `APIClient` clears it on the next
/// response.
struct TLSInterception: Equatable, Sendable {
    /// Known product name, else the issuing CA's subject; nil when the server
    /// only presented its own certificate.
    var issuerName: String?
}

extension SharedKey where Self == InMemoryKey<TLSInterception?>.Default {
    static var tlsInterception: Self {
        Self[.inMemory("tlsInterception"), default: nil]
    }
}

extension TLSInterception {
    /// Only an untrusted chain counts: a bad date points at the device clock,
    /// and other handshake failures don't say who is in the middle.
    init?(_ error: any Error) {
        guard let error = error as? URLError, Self.isUntrustedChain(error) else { return nil }
        self.init(issuerName: error.failureURLPeerTrust.flatMap(Self.issuerName(in:)))
    }

    private static func isUntrustedChain(_ error: URLError) -> Bool {
        switch error.code {
        case .serverCertificateUntrusted, .serverCertificateHasUnknownRoot:
            return true
        case .secureConnectionFailed:
            // App Transport Security reports a rejected chain as a generic
            // handshake failure (-9802), so ask the chain why it was rejected.
            guard let trust = error.failureURLPeerTrust else { return false }
            var rejection: CFError?
            guard !SecTrustEvaluateWithError(trust, &rejection), let rejection else { return false }
            let dateFailures = [errSecCertificateExpired, errSecCertificateNotValidYet].map(Int.init)
            return !dateFailures.contains(CFErrorGetCode(rejection))
        default:
            return false
        }
    }

    /// Mirrors the Android matcher so both apps name the same products.
    static func productName(forIssuer issuer: String) -> String? {
        products.first { product in
            product.needles.contains { issuer.localizedCaseInsensitiveContains($0) }
        }?.name
    }

    private static func issuerName(in trust: SecTrust) -> String? {
        // Index 0 is the server's own certificate; index 1 is whoever signed it.
        guard let chain = SecTrustCopyCertificateChain(trust) as? [SecCertificate],
              chain.count > 1,
              let subject = SecCertificateCopySubjectSummary(chain[1]) as String?
        else { return nil }
        return productName(forIssuer: subject) ?? subject
    }

    private static let products: [(name: String, needles: [String])] = [
        ("Fortinet", ["Fortinet", "FortiGate", "FortiGuard"]),
        ("Kaspersky", ["Kaspersky"]),
        ("Bitdefender", ["Bitdefender"]),
        ("ESET", ["ESET"]),
        ("Avast", ["avast"]),
        ("AVG", ["AVG "]),
        ("Sophos", ["Sophos"]),
        ("Webroot", ["Webroot"]),
        ("Cisco Umbrella", ["Cisco Umbrella"]),
        ("Zscaler", ["Zscaler"]),
        ("Blue Coat", ["Blue Coat", "BlueCoat", "Symantec ProxySG"]),
        ("Check Point", ["Check Point"]),
        ("McAfee", ["McAfee"]),
        ("Charles Proxy", ["Charles Proxy"]),
        ("mitmproxy", ["mitmproxy"]),
        ("Squid Proxy", ["Squid Proxy"]),
    ]
}
