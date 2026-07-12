import SwiftUI

/// The per-key detail bottom sheet: identity, a bordered info card, and the
/// rename (inline) / delete actions. Delete confirmation is a native alert.
struct PasskeyDetailSheet: View {
    var credential: PasskeyCredential
    var isEditingName: Bool
    @Binding var renameText: String
    var isMutating: Bool
    var error: String?
    var onRename: () -> Void
    var onRenameCancel: () -> Void
    var onRenameSave: () -> Void
    var onDelete: () -> Void

    @State private var height: CGFloat = 380
    @FocusState private var nameFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            PasskeyTile(credential: credential, size: 62, radius: 16)
                .padding(.bottom, 14)

            if isEditingName {
                renameField
            } else {
                identity
            }

            if !isEditingName {
                infoCard
                    .padding(.top, 18)

                if let error {
                    Text(error)
                        .font(.system(size: 12.5, weight: .medium))
                        .foregroundStyle(UNESColor.alertRed)
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)
                }

                actions
                    .padding(.top, 14)
            }
        }
        .padding(EdgeInsets(top: 26, leading: 20, bottom: 16, trailing: 20))
        .frame(maxWidth: .infinity)
        .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height = $0 }
        .onChange(of: isEditingName) { _, editing in nameFocused = editing }
        .presentationBackground(UNESColor.card)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
        .presentationCornerRadiusCompat(30)
    }

    private var identity: some View {
        VStack(spacing: 6) {
            Text(credential.deviceName ?? String.localized(.passkeysUnnamed))
                .font(.system(size: 22, weight: .bold))
                .tracking(-0.66)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.center)

            HStack(spacing: 5) {
                Image(systemName: credential.isSynced ? "icloud" : "lock.shield")
                    .font(.system(size: 12, weight: .semibold))
                Text(credential.isSynced ? .passkeysSyncedFull : .passkeysDeviceBound)
                    .font(.system(size: 12.5, weight: .semibold))
            }
            .foregroundStyle(credential.isSynced ? UNESColor.teal : UNESColor.ink4)
        }
    }

    private var renameField: some View {
        VStack(spacing: 12) {
            TextField(String.localized(.passkeysRenamePlaceholder), text: $renameText)
                .font(.system(size: 16, weight: .semibold))
                .tracking(-0.16)
                .foregroundStyle(UNESColor.ink)
                .multilineTextAlignment(.center)
                .textFieldStyle(.plain)
                .focused($nameFocused)
                .submitLabel(.done)
                .onSubmit(onRenameSave)
                .padding(EdgeInsets(top: 11, leading: 14, bottom: 11, trailing: 14))
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(UNESColor.accent, lineWidth: 1.5)
                }

            HStack(spacing: 10) {
                Button(action: onRenameCancel) {
                    Text(.commonCancel)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(UNESColor.ink)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .overlay {
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .strokeBorder(UNESColor.line, lineWidth: 1.5)
                        }
                }
                .buttonStyle(TilePressStyle())

                Button(action: onRenameSave) {
                    Text(.commonSave)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(UNESColor.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .buttonStyle(TilePressStyle())
            }
        }
    }

    // The info card carries the design's hairline-bordered surface treatment.
    private var infoCard: some View {
        VStack(spacing: 0) {
            infoRow(label: String.localized(.passkeysDetailSync), value: syncValue)
            Rectangle()
                .fill(UNESColor.line)
                .frame(height: 0.5)
            infoRow(label: String.localized(.passkeysDetailCreated), value: createdValue)
        }
        .background(UNESColor.surface3)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(UNESColor.line)
        }
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack(spacing: 12) {
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
            Spacer(minLength: 12)
            Text(value)
                .font(.system(size: 14, weight: .semibold))
                .tracking(-0.14)
                .foregroundStyle(UNESColor.ink)
        }
        .padding(EdgeInsets(top: 12, leading: 14, bottom: 12, trailing: 14))
    }

    private var actions: some View {
        HStack(spacing: 10) {
            Button(action: onRename) {
                actionLabel(.passkeysRename, systemImage: "pencil", tint: UNESColor.ink)
                    .overlay {
                        RoundedRectangle(cornerRadius: 15, style: .continuous)
                            .strokeBorder(UNESColor.line, lineWidth: 1.5)
                    }
            }
            .buttonStyle(TilePressStyle())

            Button(action: onDelete) {
                actionLabel(.passkeysDelete, systemImage: "trash", tint: UNESColor.alertRed)
                    .background(UNESColor.alertRed.opacity(0.12), in: RoundedRectangle(cornerRadius: 15, style: .continuous))
            }
            .buttonStyle(TilePressStyle())
        }
        .disabled(isMutating)
        .opacity(isMutating ? 0.5 : 1)
    }

    private func actionLabel(_ title: LocalizedStringResource, systemImage: String, tint: Color) -> some View {
        HStack(spacing: 7) {
            Image(systemName: systemImage)
                .font(.system(size: 15, weight: .semibold))
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .tracking(-0.15)
        }
        .foregroundStyle(tint)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 13)
    }

    private var syncValue: String {
        String.localized(credential.isSynced ? .passkeysSynced : .passkeysDeviceBound)
    }

    private var createdValue: String {
        credential.createdAt.formatted(date: .abbreviated, time: .omitted)
    }
}

#Preview {
    Color.clear.sheet(isPresented: .constant(true)) {
        PasskeyDetailSheet(
            credential: .preview,
            isEditingName: false,
            renameText: .constant("iPhone 15 Pro"),
            isMutating: false,
            error: nil,
            onRename: {},
            onRenameCancel: {},
            onRenameSave: {},
            onDelete: {}
        )
    }
}
