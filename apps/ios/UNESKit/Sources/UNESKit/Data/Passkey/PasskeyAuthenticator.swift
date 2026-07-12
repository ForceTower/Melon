import ComposableArchitecture
import Foundation

private let log = Log.scoped("PasskeyAuthenticator")

extension PasskeyClient: DependencyKey {
    static let liveValue = PasskeyClient(
        assert: { challenge in
            #if os(iOS)
            log.info("assert attempt rpId=\(challenge.rpId)")
            do {
                let assertion = try await PasskeyAuthenticator().assert(challenge: challenge)
                log.info("assert ok")
                return assertion
            } catch {
                if case AuthError.cancelled = error {
                    log.info("assert cancelled")
                } else {
                    log.warn("assert failed", error: error)
                }
                throw error
            }
            #else
            log.warn("assert unavailable: platform not supported")
            throw AuthError.passkeyUnavailable
            #endif
        },
        register: { options, target in
            #if os(iOS)
            log.info("register attempt rpId=\(options.rpId) target=\(target)")
            do {
                let attestation = try await PasskeyRegistrar().register(options: options, target: target)
                log.info("register ok")
                return attestation
            } catch {
                if case AuthError.cancelled = error {
                    log.info("register cancelled")
                } else {
                    log.warn("register failed", error: error)
                }
                throw error
            }
            #else
            log.warn("register unavailable: platform not supported")
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
            log.warn("assert failed: invalid challenge encoding")
            throw AuthError.server(String.localized(.dataErrorInvalidChallenge))
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
                log.warn("assertion completion failed: unexpected credential type")
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
            log.debug("assertion completion ok")
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
                log.info("assertion cancelled")
                continuation?.resume(throwing: AuthError.cancelled)
            } else {
                log.warn("assertion completion error", error: error)
                continuation?.resume(throwing: AuthError.server(error.localizedDescription))
            }
            continuation = nil
        }
    }
}

extension PasskeyAuthenticator: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated { passkeyPresentationAnchor() }
    }
}

/// Drives a single ASAuthorization registration request. Owned per-call, like
/// `PasskeyAuthenticator`. The system presents its own creation sheet — Face ID
/// (platform) or key-tap (security key) is confirmed there, not by us.
@MainActor
private final class PasskeyRegistrar: NSObject {
    private var continuation: CheckedContinuation<PasskeyAttestation, Error>?
    private var attachment = "platform"

    func register(options: PasskeyRegistrationOptions, target: PasskeyTarget) async throws -> PasskeyAttestation {
        guard let challengeData = Data(base64URLEncoded: options.challenge),
              let userIDData = Data(base64URLEncoded: options.userId) else {
            log.warn("register failed: invalid options encoding")
            throw AuthError.server(String.localized(.dataErrorInvalidChallenge))
        }

        let request = makeRequest(options: options, target: target, challenge: challengeData, userID: userIDData)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation
            controller.performRequests()
        }
    }

    private func makeRequest(
        options: PasskeyRegistrationOptions,
        target: PasskeyTarget,
        challenge: Data,
        userID: Data
    ) -> ASAuthorizationRequest {
        switch target {
        case .thisDevice:
            attachment = "platform"
            let provider = ASAuthorizationPlatformPublicKeyCredentialProvider(relyingPartyIdentifier: options.rpId)
            let request = provider.createCredentialRegistrationRequest(
                challenge: challenge,
                name: options.userName,
                userID: userID
            )
            request.userVerificationPreference = .required
            request.excludedCredentials = options.excludedCredentialIds.compactMap { id in
                Data(base64URLEncoded: id).map(ASAuthorizationPlatformPublicKeyCredentialDescriptor.init(credentialID:))
            }
            return request

        case .securityKey:
            attachment = "cross-platform"
            let provider = ASAuthorizationSecurityKeyPublicKeyCredentialProvider(relyingPartyIdentifier: options.rpId)
            let request = provider.createCredentialRegistrationRequest(
                challenge: challenge,
                displayName: options.userDisplayName,
                name: options.userName,
                userID: userID
            )
            request.credentialParameters = options.algorithms.map {
                ASAuthorizationPublicKeyCredentialParameters(algorithm: ASCOSEAlgorithmIdentifier(rawValue: $0))
            }
            request.residentKeyPreference = .required
            request.userVerificationPreference = .required
            request.excludedCredentials = options.excludedCredentialIds.compactMap { id in
                Data(base64URLEncoded: id).map {
                    ASAuthorizationSecurityKeyPublicKeyCredentialDescriptor(credentialID: $0, transports: [])
                }
            }
            return request
        }
    }
}

extension PasskeyRegistrar: ASAuthorizationControllerDelegate {
    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        Task { @MainActor in
            guard let credential = authorization.credential as? ASAuthorizationPublicKeyCredentialRegistration,
                  let attestationObject = credential.rawAttestationObject else {
                log.warn("registration completion failed: unexpected credential type")
                continuation?.resume(throwing: AuthError.passkeyUnavailable)
                continuation = nil
                return
            }

            let attestation = PasskeyAttestation(
                id: credential.credentialID.base64URLEncodedString(),
                rawId: credential.credentialID.base64URLEncodedString(),
                authenticatorAttachment: attachment,
                clientDataJSON: credential.rawClientDataJSON.base64URLEncodedString(),
                attestationObject: attestationObject.base64URLEncodedString()
            )
            log.debug("registration completion ok")
            continuation?.resume(returning: attestation)
            continuation = nil
        }
    }

    nonisolated func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: Error
    ) {
        Task { @MainActor in
            if let authError = error as? ASAuthorizationError, authError.code == .canceled {
                log.info("registration cancelled")
                continuation?.resume(throwing: AuthError.cancelled)
            } else {
                log.warn("registration completion error", error: error)
                continuation?.resume(throwing: AuthError.server(error.localizedDescription))
            }
            continuation = nil
        }
    }
}

extension PasskeyRegistrar: ASAuthorizationControllerPresentationContextProviding {
    nonisolated func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        MainActor.assumeIsolated { passkeyPresentationAnchor() }
    }
}

/// The key window that hosts the system authorization sheet.
@MainActor
private func passkeyPresentationAnchor() -> ASPresentationAnchor {
    UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap(\.windows)
        .first { $0.isKeyWindow } ?? ASPresentationAnchor()
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
