import ComposableArchitecture
import SwiftUI

/// Portal-password sheet: the SAGRES username is read-only, only the password
/// is editable. Sibling of `SessionExpiredSheet`, deliberately kept separate —
/// that one re-runs the Melon login, this one replaces the credential the
/// server syncs with.
struct ReauthSheet: View {
    @Bindable var store: StoreOf<ReauthFeature>

    @FocusState private var passwordFocused: Bool
    @State private var height: CGFloat = 380

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            fieldsCard
                .padding(.top, 18)
            if let message = store.errorMessage {
                Text(message)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESBannerTone.danger)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, 10)
            }
            actions
                .padding(.top, 18)
        }
        .padding(EdgeInsets(top: 24, leading: 18, bottom: 16, trailing: 18))
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height
        } action: { measured in
            height = measured
            // Detent updates issued mid-presentation get dropped, and a
            // same-value write is a no-op, so re-send with a hidden nudge.
            Task { @MainActor in
                try? await Task.sleep(for: .milliseconds(700))
                if height == measured {
                    height = measured + 0.001
                }
            }
        }
        .presentationBackground(UNESColor.surface)
        .presentationDetents([.height(height)])
        .presentationDragIndicator(.visible)
        .task {
            passwordFocused = true
            await store.send(.task).finish()
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "key.horizontal.fill")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .background(UNESBannerTone.warn, in: RoundedRectangle(cornerRadius: 11, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(.reauthSheetTitle)
                    .font(.system(size: 19, weight: .bold))
                    .tracking(-0.4)
                    .foregroundStyle(UNESColor.ink)
                Text(.reauthSheetSubtitle)
                    .font(.system(size: 13, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    private var fieldsCard: some View {
        VStack(spacing: 0) {
            if let username = store.username {
                row(icon: "person", label: String.localized(.commonUsername)) {
                    // Read-only: this sheet updates a password, it can't move
                    // the account to a different student.
                    Text(username)
                        .font(.system(size: 17, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                UNESColor.line
                    .frame(height: 1)
                    .padding(.leading, 46)
            }

            row(icon: "lock", label: String.localized(.commonPassword)) {
                Group {
                    if store.showPassword {
                        TextField(String("••••••••"), text: $store.password)
                            .noAutocapitalization()
                            .autocorrectionDisabled()
                    } else {
                        SecureField(String("••••••••"), text: $store.password)
                    }
                }
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(UNESColor.ink)
                .textContentType(.password)
                .submitLabel(.go)
                .focused($passwordFocused)
                .onSubmit { store.send(.submitTapped) }
            } accessory: {
                Button {
                    store.send(.toggleShowPassword)
                } label: {
                    Image(systemName: store.showPassword ? "eye.slash" : "eye")
                        .font(.system(size: 15))
                        .foregroundStyle(UNESColor.ink4)
                }
                .buttonStyle(.plain)
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }

    private func row(
        icon: String,
        label: String,
        @ViewBuilder content: () -> some View,
        @ViewBuilder accessory: () -> some View = { EmptyView() }
    ) -> some View {
        HStack(spacing: 0) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundStyle(UNESColor.ink4)
                .frame(width: 30, alignment: .leading)

            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .textCase(.uppercase)
                    .font(.system(size: 11, weight: .semibold))
                    .tracking(0.44)
                    .foregroundStyle(UNESColor.ink3)
                content()
            }

            accessory()
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 62)
    }

    private var actions: some View {
        VStack(spacing: 12) {
            Button {
                store.send(.submitTapped)
            } label: {
                if store.isLoading {
                    SpinnerRing(
                        size: 20,
                        color: UNESColor.surface,
                        trackColor: UNESColor.paper.opacity(0.3),
                        speed: 0.7
                    )
                } else {
                    UNESButtonLabel(text: .reauthSheetConfirm)
                }
            }
            .buttonStyle(.unesDark)
            .disabled(!store.canSubmit)

            Button {
                store.send(.closeTapped)
            } label: {
                Text(.reauthSheetCancel)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .buttonStyle(.plain)
            .disabled(store.isLoading)
        }
    }
}

#Preview {
    ReauthSheet(
        store: Store(initialState: ReauthFeature.State(username: "20191234")) {
            ReauthFeature()
        }
    )
}
