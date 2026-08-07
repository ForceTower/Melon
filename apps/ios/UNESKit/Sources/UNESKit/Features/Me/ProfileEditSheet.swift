import ComposableArchitecture
import SwiftUI

/// The "Editar perfil" sheet — native form chrome: grouped sections, a
/// confirmation dialog for the photo source, and the system picker's square
/// crop behind it.
struct ProfileEditSheet: View {
    @Bindable var store: StoreOf<ProfileEditFeature>

    var body: some View {
        NavigationStack {
            Form {
                avatarSection
                nameSection
            }
            .navigationTitle(Text(.meEditTitle))
            .inlineNavigationBar()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        store.send(.cancelTapped)
                    } label: {
                        Text(.meEditCancel)
                    }
                    .disabled(store.isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if store.isSaving {
                        ProgressView()
                    } else {
                        Button {
                            store.send(.saveTapped)
                        } label: {
                            Text(.meEditSave)
                                .fontWeight(.semibold)
                        }
                    }
                }
            }
        }
        .interactiveDismissDisabled(store.isSaving)
        .presentationDetents([.medium, .large])
        .fullScreenCoverCompat(isPresented: pickerBinding) {
            picker
        }
    }

    // MARK: Avatar

    private var avatarSection: some View {
        Section {
            VStack(spacing: 12) {
                Button {
                    store.send(.changePhotoTapped)
                } label: {
                    avatar
                }
                .buttonStyle(.plain)
                Button {
                    store.send(.changePhotoTapped)
                } label: {
                    Text(store.hasPhoto ? .meEditChangePhoto : .meEditAddPhoto)
                        .font(.subheadline.weight(.medium))
                }
            }
            .frame(maxWidth: .infinity)
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
            .accessibilityElement(children: .combine)
            .accessibilityLabel(Text(store.hasPhoto ? .meEditChangePhoto : .meEditAddPhoto))
            // Anchored here on purpose: iOS 26 draws confirmation dialogs as
            // a bubble pointing at the attached view — on the sheet root it
            // would poke out of the sheet's top edge.
            .confirmationDialog(
                Text(.meEditPhotoDialogTitle),
                isPresented: $store.isSourceDialogPresented,
                titleVisibility: .visible
            ) {
                sourceDialogActions
            }
        }
    }

    private var avatar: some View {
        ZStack {
            monogram
            if let draft = draftImage {
                draft
                    .resizable()
                    .scaledToFill()
            } else if let url = store.visibleServerImageUrl.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    monogram
                }
            }
        }
        .frame(width: 104, height: 104)
        .clipShape(Circle())
        .overlay(alignment: .bottomTrailing) {
            Image(systemName: "camera.fill")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 32, height: 32)
                .background(Color.accentColor, in: Circle())
                .overlay {
                    Circle()
                        .strokeBorder(UNESColor.surface, lineWidth: 3)
                }
        }
    }

    /// The gradient initial underneath — the loading/error fallback the
    /// photo simply paints over, same recipe as the hero.
    private var monogram: some View {
        Text(initial)
            .font(.system(size: 46, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: 104, height: 104)
            .background(
                LinearGradient.css(
                    stops: [
                        .init(color: UNESColor.amber, location: 0),
                        .init(color: UNESColor.coral, location: 0.55),
                        .init(color: UNESColor.magenta, location: 1),
                    ],
                    angle: 135
                ),
                in: Circle()
            )
    }

    private var initial: String {
        store.previewName.first.map { String($0).uppercased() } ?? "•"
    }

    private var draftImage: Image? {
        #if canImport(UIKit)
        store.draftPhoto.flatMap(UIImage.init(data:)).map(Image.init(uiImage:))
        #else
        nil
        #endif
    }

    // MARK: Name

    private var nameSection: some View {
        Section {
            TextField(
                String.localized(.meEditNameLabel),
                text: $store.draftName,
                prompt: Text(.meEditNamePlaceholder)
            )
            .textContentType(.nickname)
            .autocorrectionDisabled()
            if !store.trimmedDraftName.isEmpty {
                Button {
                    store.send(.restoreNameTapped)
                } label: {
                    Label {
                        Text(.meEditRestorePortalName)
                    } icon: {
                        Image(systemName: "arrow.uturn.backward")
                    }
                }
            }
        } header: {
            Text(.meEditNameLabel)
        } footer: {
            VStack(alignment: .leading, spacing: 8) {
                Text(.meEditLockNote(store.portalName))
                if store.saveFailed {
                    Text(.meEditError)
                        .foregroundStyle(.red)
                }
            }
        }
    }

    // MARK: Photo source + picker

    @ViewBuilder
    private var sourceDialogActions: some View {
        #if os(iOS)
        if AvatarImagePicker.isCameraAvailable {
            Button {
                store.send(.photoSourcePicked(.camera))
            } label: {
                Text(.meEditTakePhoto)
            }
        }
        #endif
        Button {
            store.send(.photoSourcePicked(.library))
        } label: {
            Text(.meEditChooseFromLibrary)
        }
        if store.hasPhoto {
            Button(role: .destructive) {
                store.send(.removePhotoTapped)
            } label: {
                Text(.meEditRemovePhoto)
            }
        }
    }

    /// Only reports a dismissal while state still shows the picker: SwiftUI
    /// re-writes the binding when the dismiss animation completes, by which
    /// point a pick may have already cleared it.
    private var pickerBinding: Binding<Bool> {
        Binding(
            get: { store.pickerSource != nil },
            set: { value in
                if !value, store.pickerSource != nil { store.send(.pickerCanceled) }
            }
        )
    }

    @ViewBuilder
    private var picker: some View {
        #if os(iOS)
        if let source = store.pickerSource {
            AvatarImagePicker(source: source) { data in
                store.send(.photoPicked(data))
            } onCancel: {
                store.send(.pickerCanceled)
            }
            .ignoresSafeArea()
        }
        #endif
    }
}

#Preview {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            ProfileEditSheet(
                store: Store(initialState: ProfileEditFeature.State(profile: .preview)) {
                    ProfileEditFeature()
                }
            )
        }
}
