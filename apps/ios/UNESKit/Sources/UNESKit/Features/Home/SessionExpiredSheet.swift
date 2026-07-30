import ComposableArchitecture
import SwiftUI

/// Sign back in without losing the app: the refresh token was spent, but the
/// mirror is intact, so this swaps the tokens in place instead of routing
/// through the farewell/onboarding flow. Reuses `LoginFeature` at sheet scale.
struct SessionExpiredSheet: View {
    @Bindable var store: StoreOf<LoginFeature>

    @FocusState private var focus: Field?
    @State private var height: CGFloat = 420

    private enum Field {
        case username, password
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            fieldsCard
                .padding(.top, 18)
            if let message = store.errorMessage {
                Text(message)
                    .font(.system(size: 12.5, weight: .medium))
                    .foregroundStyle(UNESBannerTone.danger)
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
        .task { await store.send(.task).finish() }
    }

    // MARK: Header

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(.white)
                .frame(width: 34, height: 34)
                .background(UNESBannerTone.danger, in: RoundedRectangle(cornerRadius: 11, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(.sessionExpiredSheetTitle)
                    .font(.system(size: 19, weight: .bold))
                    .tracking(-0.4)
                    .foregroundStyle(UNESColor.ink)
                Text(.sessionExpiredSheetSubtitle)
                    .font(.system(size: 13, weight: .medium))
                    .lineSpacing(2)
                    .foregroundStyle(UNESColor.ink3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    // MARK: Fields

    private var fieldsCard: some View {
        VStack(spacing: 0) {
            fieldRow(icon: "person", label: String.localized(.commonUsername), isFocused: focus == .username) {
                TextField(String(""), text: $store.username, prompt: Text(.onboardingLoginUsernamePlaceholder))
                    .textContentType(.username)
                    .noAutocapitalization()
                    .autocorrectionDisabled()
                    .submitLabel(.next)
                    .focused($focus, equals: .username)
                    .onSubmit { focus = .password }
            }

            UNESColor.line
                .frame(height: 1)
                .padding(.leading, 46)

            fieldRow(icon: "lock", label: String.localized(.commonPassword), isFocused: focus == .password) {
                Group {
                    if store.showPassword {
                        TextField(String("••••••••"), text: $store.password)
                            .noAutocapitalization()
                            .autocorrectionDisabled()
                    } else {
                        SecureField(String("••••••••"), text: $store.password)
                    }
                }
                .textContentType(.password)
                .submitLabel(.go)
                .focused($focus, equals: .password)
                .onSubmit { store.send(.submitTapped) }
            } accessory: {
                Button {
                    store.send(.toggleShowPassword)
                } label: {
                    Image(systemName: store.showPassword ? "eye.slash" : "eye")
                        .font(.system(size: 15))
                        .foregroundStyle(UNESColor.ink4)
                }
            }
        }
        .background(UNESColor.card)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(UNESColor.cardLine)
        }
    }

    private func fieldRow(
        icon: String,
        label: String,
        isFocused: Bool,
        @ViewBuilder field: () -> some View,
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

                field()
                    .font(.system(size: 17, weight: .medium))
                    .foregroundStyle(UNESColor.ink)
            }

            accessory()
        }
        .padding(.horizontal, 16)
        .frame(minHeight: 62)
        .background(isFocused ? UNESColor.surface2 : .clear)
        .animation(.easeInOut(duration: 0.18), value: isFocused)
    }

    // MARK: Actions

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
                    UNESButtonLabel(text: .commonSignIn)
                }
            }
            .buttonStyle(.unesDark)
            .disabled(!store.canSubmit)

            Button {
                store.send(.passkeyTapped)
            } label: {
                HStack(spacing: 9) {
                    Image(systemName: "person.badge.key")
                        .font(.system(size: 17, weight: .medium))
                    Text(.onboardingLoginPasskey).tracking(-0.17)
                }
            }
            .buttonStyle(.unesNeutral)
            .disabled(store.isLoading)

            Button {
                store.send(.closeTapped)
            } label: {
                Text(.sessionExpiredSheetCancel)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(UNESColor.ink3)
            }
            .buttonStyle(.plain)
            .disabled(store.isLoading)
        }
    }
}

#Preview {
    SessionExpiredSheet(
        store: Store(initialState: LoginFeature.State(analyticsScreen: Screens.sessionExpired)) {
            LoginFeature()
        }
    )
}
