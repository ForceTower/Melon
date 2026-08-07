import ComposableArchitecture
import Foundation

/// The "Editar perfil" sheet: alternate display name + avatar. Nothing is
/// written locally — a successful save re-pulls `api/sync/profile` through
/// the parent, so state picks up whatever the server normalized (re-typing
/// the portal name stores null, a fresh picture mints a new URL).
@Reducer
struct ProfileEditFeature {
    /// Where a replacement avatar comes from.
    enum PickerSource: Equatable, Sendable {
        case camera, library
    }

    /// What happens to the picture on save. `keep` leaves the server photo
    /// alone; `remove` deletes it; `replace` uploads the freshly cropped
    /// bytes.
    enum PhotoAction: Equatable, Sendable {
        case keep, remove, replace
    }

    /// dc `EuScreen` caps the field at 24 chars (the API allows 60) —
    /// enforced on the binding so paste can't sneak past it.
    static let nameMaxLength = 24

    @ObservableState
    struct State: Equatable {
        /// Upstream registry name — the placeholder and the lock note.
        let portalName: String
        /// Server-side alternate name when the sheet opened, "" for none.
        let savedAlternateName: String
        let serverImageUrl: String?
        /// Alternate-name draft — empty means "portal name in charge".
        var draftName: String
        var photoAction: PhotoAction = .keep
        /// Cropped JPEG staged for upload while `photoAction == .replace`.
        var draftPhoto: Data?
        var isSaving = false
        var saveFailed = false
        var isSourceDialogPresented = false
        var pickerSource: PickerSource?

        init(profile: Profile) {
            portalName = profile.name
            savedAlternateName = profile.alternateName ?? ""
            serverImageUrl = profile.imageUrl
            draftName = profile.alternateName ?? ""
        }

        /// Whether the sheet currently shows a photo (drives the action
        /// sheet's remove entry and the add/change label).
        var hasPhoto: Bool {
            switch photoAction {
            case .replace: true
            case .remove: false
            case .keep: serverImageUrl != nil
            }
        }

        /// The server avatar still on display — nil once removed or covered
        /// by a draft.
        var visibleServerImageUrl: String? {
            photoAction == .keep ? serverImageUrl : nil
        }

        var trimmedDraftName: String {
            draftName.trimmingCharacters(in: .whitespacesAndNewlines)
        }

        /// The name the avatar monogram previews.
        var previewName: String {
            trimmedDraftName.isEmpty ? portalName : trimmedDraftName
        }
    }

    enum Action: BindableAction, Equatable {
        case binding(BindingAction<State>)
        case changePhotoTapped
        case photoSourcePicked(PickerSource)
        case removePhotoTapped
        case photoPicked(Data)
        case pickerCanceled
        case restoreNameTapped
        case cancelTapped
        case saveTapped
        case saveCompleted(Bool)
        case delegate(Delegate)

        enum Delegate: Equatable {
            case saved
        }
    }

    @Dependency(\.profileRepository) var profileRepository
    @Dependency(\.dismiss) var dismiss

    private let log = Log.scoped("ProfileEditFeature")

    var body: some ReducerOf<Self> {
        BindingReducer()
        Reduce { state, action in
            switch action {
            case .binding(\.draftName):
                state.draftName = String(state.draftName.prefix(Self.nameMaxLength))
                state.saveFailed = false
                return .none

            case .binding:
                return .none

            case .changePhotoTapped:
                state.isSourceDialogPresented = true
                return .none

            case let .photoSourcePicked(source):
                state.isSourceDialogPresented = false
                state.pickerSource = source
                return .none

            case .removePhotoTapped:
                state.isSourceDialogPresented = false
                state.photoAction = .remove
                state.draftPhoto = nil
                state.saveFailed = false
                return .none

            case let .photoPicked(data):
                state.pickerSource = nil
                state.photoAction = .replace
                state.draftPhoto = data
                state.saveFailed = false
                return .none

            case .pickerCanceled:
                state.pickerSource = nil
                return .none

            case .restoreNameTapped:
                state.draftName = ""
                state.saveFailed = false
                return .none

            case .cancelTapped:
                return .run { _ in await dismiss() }

            case .saveTapped:
                return save(&state)

            case .saveCompleted(true):
                log.info("profile save ok")
                state.isSaving = false
                return .run { send in
                    await send(.delegate(.saved))
                    await dismiss()
                }

            case .saveCompleted(false):
                log.warn("profile save failed")
                state.isSaving = false
                state.saveFailed = true
                return .none

            case .delegate:
                return .none
            }
        }
    }

    private func save(_ state: inout State) -> Effect<Action> {
        guard !state.isSaving else { return .none }
        let trimmed = state.trimmedDraftName
        let nameChanged = trimmed != state.savedAlternateName
        let photo = state.draftPhoto
        let uploadsPhoto = state.photoAction == .replace && photo != nil
        // Removing when no server photo exists is a visual no-op — skip the call.
        let removesPhoto = state.photoAction == .remove && state.serverImageUrl != nil

        guard nameChanged || uploadsPhoto || removesPhoto else {
            return .run { _ in await dismiss() }
        }

        log.info("begin profile save nameChanged=\(nameChanged) uploadsPhoto=\(uploadsPhoto) removesPhoto=\(removesPhoto)")
        state.isSaving = true
        state.saveFailed = false
        return .run { send in
            do {
                if nameChanged {
                    try await profileRepository.updateName(trimmed.isEmpty ? nil : trimmed)
                }
                if uploadsPhoto, let photo {
                    try await profileRepository.uploadPicture(photo, "image/jpeg")
                } else if removesPhoto {
                    try await profileRepository.deletePicture()
                }
                await send(.saveCompleted(true))
            } catch {
                await send(.saveCompleted(false))
            }
        }
    }
}
