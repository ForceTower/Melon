import ComposableArchitecture
import Foundation

#if os(iOS)
import UIKit
#endif

/// Chaves de acesso — lists the account's passkeys and drives create, rename
/// and delete. The one step we don't own is authentication: choosing a target
/// hands off to the system passkey sheet (Face ID / key tap), and we enroll
/// whatever it returns.
@Reducer
struct PasskeysFeature {
    @ObservableState
    struct State: Equatable {
        /// Seeded from the host so the add sheet's account card isn't empty.
        var accountName: String?
        var credentials: [PasskeyCredential] = []
        /// Distinguishes "still loading" from "loaded, and empty".
        var hasLoaded = false

        // Add sheet
        var isAddPresented = false
        var addTarget: PasskeyTarget = .thisDevice
        var addStep: AddStep = .choose
        /// The system sheet is up / the attestation is being verified.
        var isCreating = false
        var createError: String?

        // Detail sheet
        var detail: PasskeyCredential?
        var isEditingName = false
        var renameText = ""
        /// A rename or delete request is in flight.
        var isMutating = false
        var mutationError: String?

        // Delete confirmation (native alert)
        var pendingDelete: PasskeyCredential?

        // Feedback
        /// The just-created credential, briefly highlighted in the list.
        var newCredentialID: String?
        var toast: String?

        var avatarInitial: String {
            accountName?.first.map { String($0).uppercased() } ?? "•"
        }
    }

    enum AddStep: Equatable, Sendable { case choose, success }

    enum Action: Equatable {
        case task
        case credentialsResponse([PasskeyCredential])
        case credentialsLoadFailed

        // Add
        case addTapped
        case addTargetSelected(PasskeyTarget)
        case addContinueTapped
        case addDismissed
        case created(id: String, credentials: [PasskeyCredential])
        case addSuccessCompleted
        case createCancelled
        case createFailed(message: String?)

        // Detail + rename
        case rowTapped(PasskeyCredential)
        case detailDismissed
        case renameTapped
        case renameTextChanged(String)
        case renameCancelled
        case renameSubmitted
        case renameResponse(id: String, name: String)

        // Delete
        case deleteTapped
        case deleteDismissed
        case deleteConfirmed
        case deleteResponse(id: String)

        case mutationFailed(message: String?)

        // Feedback
        case highlightExpired
        case toastExpired
    }

    @Dependency(\.passkeyRepository) var passkeyRepository
    @Dependency(\.passkeyClient) var passkeyClient
    @Dependency(\.continuousClock) var clock

    private let log = Log.scoped("PasskeysFeature")

    private static let successLinger: Duration = .milliseconds(1050)
    private static let toastWindow: Duration = .milliseconds(2600)
    private static let highlightWindow: Duration = .seconds(2)

    private enum CancelID { case create, mutate, highlight, toast, success }

    var body: some ReducerOf<Self> {
        Reduce { state, action in
            switch action {
            case .task:
                return loadCredentials()

            case let .credentialsResponse(credentials):
                state.credentials = credentials
                state.hasLoaded = true
                return .none

            case .credentialsLoadFailed:
                state.hasLoaded = true
                return .none

            // MARK: Add

            case .addTapped:
                guard !state.isCreating else { return .none }
                state.isAddPresented = true
                state.addStep = .choose
                state.addTarget = .thisDevice
                state.createError = nil
                return .none

            case let .addTargetSelected(target):
                state.addTarget = target
                return .none

            case .addContinueTapped:
                guard !state.isCreating else { return .none }
                state.isCreating = true
                state.createError = nil
                let target = state.addTarget
                log.info("create passkey target=\(target)")
                return createPasskey(target)

            case .addDismissed:
                state.isAddPresented = false
                state.addStep = .choose
                state.createError = nil
                return .cancel(id: CancelID.create)

            case let .created(id, credentials):
                state.isCreating = false
                state.credentials = credentials
                state.newCredentialID = id
                state.addStep = .success
                log.info("passkey created")
                return .run { send in
                    try await clock.sleep(for: Self.successLinger)
                    await send(.addSuccessCompleted)
                }
                .cancellable(id: CancelID.success, cancelInFlight: true)

            case .addSuccessCompleted:
                state.isAddPresented = false
                state.addStep = .choose
                return .merge(
                    showToast(String.localized(.passkeysToastCreated), in: &state),
                    startHighlight()
                )

            case .createCancelled:
                state.isCreating = false
                return .none

            case let .createFailed(message):
                state.isCreating = false
                state.createError = message ?? String.localized(.passkeysCreateErrorMessage)
                return .none

            // MARK: Detail + rename

            case let .rowTapped(credential):
                state.detail = credential
                state.isEditingName = false
                state.mutationError = nil
                return .none

            case .detailDismissed:
                state.detail = nil
                state.isEditingName = false
                state.pendingDelete = nil
                state.mutationError = nil
                return .none

            case .renameTapped:
                guard let detail = state.detail else { return .none }
                state.isEditingName = true
                state.renameText = detail.deviceName ?? ""
                state.mutationError = nil
                return .none

            case let .renameTextChanged(text):
                state.renameText = text
                return .none

            case .renameCancelled:
                state.isEditingName = false
                return .none

            case .renameSubmitted:
                guard let detail = state.detail else { return .none }
                let name = state.renameText.trimmingCharacters(in: .whitespacesAndNewlines)
                state.isEditingName = false
                guard !name.isEmpty, name != detail.deviceName else { return .none }
                state.isMutating = true
                state.mutationError = nil
                log.info("rename passkey")
                return .run { send in
                    try await passkeyRepository.rename(detail.id, name)
                    await send(.renameResponse(id: detail.id, name: name))
                } catch: { [log] error, send in
                    log.warn("passkey rename failed", error: error)
                    await send(.mutationFailed(message: (error as? AuthError)?.message))
                }
                .cancellable(id: CancelID.mutate, cancelInFlight: true)

            case let .renameResponse(id, name):
                state.isMutating = false
                if let index = state.credentials.firstIndex(where: { $0.id == id }) {
                    state.credentials[index] = state.credentials[index].renamed(to: name)
                }
                if state.detail?.id == id {
                    state.detail = state.detail?.renamed(to: name)
                }
                log.info("passkey renamed")
                return showToast(String.localized(.passkeysToastRenamed), in: &state)

            // MARK: Delete

            case .deleteTapped:
                guard let detail = state.detail else { return .none }
                state.pendingDelete = detail
                return .none

            case .deleteDismissed:
                state.pendingDelete = nil
                return .none

            case .deleteConfirmed:
                guard let target = state.pendingDelete else { return .none }
                state.pendingDelete = nil
                state.isMutating = true
                state.mutationError = nil
                log.info("delete passkey")
                return .run { send in
                    try await passkeyRepository.delete(target.id)
                    await send(.deleteResponse(id: target.id))
                } catch: { [log] error, send in
                    log.warn("passkey delete failed", error: error)
                    await send(.mutationFailed(message: (error as? AuthError)?.message))
                }
                .cancellable(id: CancelID.mutate, cancelInFlight: true)

            case let .deleteResponse(id):
                state.isMutating = false
                state.credentials.removeAll { $0.id == id }
                if state.detail?.id == id {
                    state.detail = nil
                }
                log.info("passkey deleted")
                return showToast(String.localized(.passkeysToastDeleted), in: &state)

            case let .mutationFailed(message):
                state.isMutating = false
                state.mutationError = message ?? String.localized(.passkeysMutationErrorMessage)
                return .none

            // MARK: Feedback

            case .highlightExpired:
                state.newCredentialID = nil
                return .none

            case .toastExpired:
                state.toast = nil
                return .none
            }
        }
    }

    private func loadCredentials() -> Effect<Action> {
        .run { [log] send in
            do {
                let credentials = try await passkeyRepository.credentials()
                await send(.credentialsResponse(credentials))
            } catch {
                log.debug("passkey list load failed")
                await send(.credentialsLoadFailed)
            }
        }
    }

    private func createPasskey(_ target: PasskeyTarget) -> Effect<Action> {
        .run { send in
            let options = try await passkeyRepository.registrationOptions()
            let attestation = try await passkeyClient.register(options, target)
            try await passkeyRepository.register(attestation, passkeyDeviceName(for: target))
            let credentials = try await passkeyRepository.credentials()
            await send(.created(id: attestation.id, credentials: credentials))
        } catch: { [log] error, send in
            if case AuthError.cancelled = error {
                log.info("passkey create cancelled")
                await send(.createCancelled)
            } else {
                log.warn("passkey create failed", error: error)
                await send(.createFailed(message: (error as? AuthError)?.message))
            }
        }
        .cancellable(id: CancelID.create, cancelInFlight: true)
    }

    private func startHighlight() -> Effect<Action> {
        .run { send in
            try await clock.sleep(for: Self.highlightWindow)
            await send(.highlightExpired)
        }
        .cancellable(id: CancelID.highlight, cancelInFlight: true)
    }

    private func showToast(_ message: String, in state: inout State) -> Effect<Action> {
        state.toast = message
        return .run { send in
            try await clock.sleep(for: Self.toastWindow)
            await send(.toastExpired)
        }
        .cancellable(id: CancelID.toast, cancelInFlight: true)
    }
}

/// A friendly label for the new credential: this device's name for a platform
/// passkey, a generic tag for a roaming security key.
private func passkeyDeviceName(for target: PasskeyTarget) async -> String? {
    switch target {
    case .securityKey:
        return String.localized(.passkeysDeviceSecurityKey)
    case .thisDevice:
        #if os(iOS)
        return await MainActor.run { UIDevice.current.name }
        #else
        return nil
        #endif
    }
}
