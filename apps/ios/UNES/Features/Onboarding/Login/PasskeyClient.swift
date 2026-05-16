import AuthenticationServices
import Foundation
import UIKit
@preconcurrency import Umbrella

enum PasskeyError: LocalizedError {
    case notSupported
    case cancelled
    case invalidChallenge
    case noCredentialReturned
    case failed(String)

    var errorDescription: String? {
        switch self {
        case .notSupported: return "Passkey não é suportada neste dispositivo."
        case .cancelled: return nil
        case .invalidChallenge: return "Desafio inválido recebido do servidor."
        case .noCredentialReturned: return "Nenhuma credencial foi retornada."
        case .failed(let message): return message
        }
    }
}

/// Drives a single ASAuthorization assertion request. Owned per-call (one
/// instance per `assert(...)`) so the delegate continuation is never reused.
@MainActor
final class PasskeyAuthenticator: NSObject {
    private var continuation: CheckedContinuation<AuthPasskeyAssertion, Error>?
    private weak var anchor: ASPresentationAnchor?

    func assert(challenge: AuthPasskeyChallenge, anchor: ASPresentationAnchor) async throws -> AuthPasskeyAssertion {
        guard let challengeData = Data(base64URLEncoded: challenge.challenge) else {
            throw PasskeyError.invalidChallenge
        }

        self.anchor = anchor

        let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(
            relyingPartyIdentifier: challenge.rpId
        )
        let request = provider.createCredentialAssertionRequest(challenge: challengeData)

        let allowed = challenge.allowCredentials.compactMap { allowed -> ASAuthorizationPlatformPublicKeyCredentialDescriptor? in
            guard let id = Data(base64URLEncoded: allowed.id) else { return nil }
            return ASAuthorizationPlatformPublicKeyCredentialDescriptor(credentialID: id)
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
                continuation?.resume(throwing: PasskeyError.noCredentialReturned)
                continuation = nil
                return
            }

            let assertion = AuthPasskeyAssertion(
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
                continuation?.resume(throwing: PasskeyError.cancelled)
            } else {
                continuation?.resume(throwing: PasskeyError.failed(error.localizedDescription))
            }
            continuation = nil
        }
    }
}

extension PasskeyAuthenticator: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated {
            anchor ?? UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow } ?? ASPresentationAnchor()
        }
    }
}

// MARK: - Base64URL helpers

extension Data {
    init?(base64URLEncoded string: String) {
        var base64 = string
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let padding = (4 - base64.count % 4) % 4
        base64 += String(repeating: "=", count: padding)
        self.init(base64Encoded: base64)
    }

    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}
