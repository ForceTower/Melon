import ComposableArchitecture
import Foundation

extension PasskeyClient: DependencyKey {
    static let liveValue = PasskeyClient(
        assert: { challenge in
            #if os(iOS)
            try await PasskeyAuthenticator().assert(challenge: challenge)
            #else
            throw AuthError.passkeyUnavailable
            #endif
        }
    )
}

#if os(iOS)
import AuthenticationServices
import UIKit

/// Drives a single ASAuthorization assertion request. Owned per-call (one
/// instance per `assert(...)`) so the delegate continuation is never reused.
@MainActor
private final class PasskeyAuthenticator: NSObject {
    private var continuation: CheckedContinuation<PasskeyAssertion, Error>?

    func assert(challenge: PasskeyChallenge) async throws -> PasskeyAssertion {
        guard let challengeData = Data(base64URLEncoded: challenge.challenge) else {
            throw AuthError.server("Desafio inválido recebido do servidor.")
        }

        let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: challenge.rpId
        )
        let request = provider.createCredentialAssertionRequest(challenge: challengeData)

        let allowed = challenge.allowedCredentialIds.compactMap { id in
            Data(base64URLEncoded: id).map(ASAuthorizationPlatformPublicKeyCredentialDescriptor.init(credentialID:))
        }
        if !allowed.isEmpty {
            request.allowedCredentials = allowed
        }

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            controller.performRequests()
        }
    }
}

extension PasskeyAuthenticator: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        Task { @MainActor in
            guard let credential = authorization.credential as? ASAuthorizationPlatformPublicKeyCredentialAssertion else {
                continuation?.resume(throwing: AuthError.passkeyUnavailable)
                continuation = nil
                return
            }

            let assertion = PasskeyAssertion(
                id: credential.credentialID.base64URLEncodedString(),
                rawId: credential.credentialID.base64URLEncodedString(),
                authenticatorAttachment: "platform",
                clientDataJSON: credential.rawClientDataJSON.base64URLEncodedString(),
                authenticatorData: credential.rawAuthenticatorData.base64URLEncodedString(),
                signature: credential.signature.base64URLEncodedString(),
                userHandle: credential.userID.base64URLEncodedString()
            )
            continuation?.resume(returning: assertion)
            continuation = nil
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        Task { @MainActor in
            if let authError = error as? ASAuthorizationError, authError.code == .canceled {
                continuation?.resume(throwing: AuthError.cancelled)
            } else {
                continuation?.resume(throwing: AuthError.server(error.localizedDescription))
            }
            continuation = nil
        }
    }
}

extension PasskeyAuthenticator: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated {
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)
                .first { $0.isKeyWindow } ?? ASPresentationAnchor()
        }
    }
}
#endif

// MARK: - Base64URL helpers

extension Data {
    init?(base64URLEncoded string: String) {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        base64 += String(repeating: "=", count: (4 - base64.count % 4) % 4)
        self.init(base64Encoded: base64)
    }

    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
