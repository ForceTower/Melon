import ComposableArchitecture
import SwiftUI

struct LoginView: View {
    @Bindable var store: StoreOf<LoginFeature>
    @FocusState private var focus: Field?

    private enum Field {
        case username, password
    }

    /// A phone held sideways: too short for the column with its pinned bar.
    @Environment(\.verticalSizeClass) private var verticalSizeClass

    var body: some View {
        Group {
            if verticalSizeClass == .compact {
                sideBySide
            } else {
                column
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        // Behind the content rather than beside it: as a sibling its fixed
        // height was the least the screen could shrink to, and whatever didn't
        // fit was cut off at both ends.
        .background(alignment: .top) { ambientWash }
        .background { UNESColor.surface.ignoresSafeArea() }
        .animation(.easeInOut(duration: 0.2), value: store.errorMessage)
        .bareNavigationBar()
        .task { await store.send(.task).finish() }
    }

    // MARK: Layouts

    private var column: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                heading
                form
                    .padding(.top, 26)
            }
            .padding(.horizontal, 24)
            .padding(.top, 12)
        }
        .scrollBounceBehavior(.basedOnSize)
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 16) {
                actions
                terms(centered: true)
                    .fadeUp(delay: 0.6, duration: 0.5)
            }
            .padding(EdgeInsets(top: 20, leading: 24, bottom: 12, trailing: 24))
            .background(UNESColor.surface)
        }
    }

    /// Heading on one side, everything to fill in and tap on the other.
    private var sideBySide: some View {
        HStack(alignment: .top, spacing: 32) {
            VStack(alignment: .leading, spacing: 16) {
                heading
                Spacer(minLength: 0)
                terms(centered: false)
                    .fadeUp(delay: 0.6, duration: 0.5)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.bottom, 12)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    form
                    actions
                        .padding(.top, 20)
                }
                .padding(.bottom, 12)
            }
            .scrollBounceBehavior(.basedOnSize)
            .scrollIndicators(.hidden)
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 24)
        .padding(.top, 12)
    }

    // MARK: Chrome

    private var heading: some View {
        VStack(alignment: .leading, spacing: 0) {
            Eyebrow(text: "UEFS · SAGRES")
                .fadeUp(delay: 0.04, duration: 0.5)

            title
                .padding(.top, 10)
                .fadeUp(delay: 0.12, duration: 0.6)

            Text(.onboardingLoginSubtitle)
                .font(.system(size: 15.5))
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 8)
                .fadeUp(delay: 0.2, duration: 0.6)
        }
    }

    private var ambientWash: some View {
        // Fading the mesh's own alpha (instead of painting surface over it)
        // guarantees a seamless blend into the surface background.
        MeshView(variant: .warm, intensity: 0.55)
            .frame(height: 300)
            .padding(.horizontal, -50)
            .mask {
                LinearGradient(
                    stops: [
                        .init(color: .white, location: 0),
                        .init(color: .clear, location: 0.88),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
            .opacity(0.4)
            .offset(y: -70)
            .ignoresSafeArea()
    }

    private var title: some View {
        VStack(alignment: .leading, spacing: -6) {
            titleLine(String.localized(.onboardingLoginTitleLine1), color: UNESColor.ink)
            titleLine(String.localized(.onboardingLoginTitleLine2), color: UNESColor.accent)
        }
    }

    private func titleLine(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 40, weight: .heavy))
            .tracking(-1.6)
            .foregroundStyle(color)
    }

    // MARK: Fields

    private var form: some View {
        VStack(alignment: .leading, spacing: 0) {
            fieldsCard
                .fadeUp(delay: 0.28, duration: 0.6)

            if let error = store.errorMessage {
                Text(error)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(UNESColor.coral)
                    .padding(.top, 10)
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }

            Button {
                store.send(.forgotPasswordTapped)
            } label: {
                Text(.onboardingLoginForgotPassword)
            }
            .font(.system(size: 15, weight: .semibold))
            .tint(UNESColor.accent)
            .padding(.top, 12)
            .fadeUp(delay: 0.34, duration: 0.6)
        }
    }

    private var fieldsCard: some View {
        VStack(spacing: 0) {
            fieldRow(
                icon: "person",
                label: String.localized(.commonUsername),
                isFocused: focus == .username
            ) {
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

            fieldRow(
                icon: "lock",
                label: String.localized(.commonPassword),
                isFocused: focus == .password
            ) {
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
        .shadow(color: Color(hex: 0x141020, opacity: 0.06), radius: 12, y: 8)
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
        VStack(spacing: 0) {
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
            .fadeUp(delay: 0.4, duration: 0.6)

            divider
                .padding(.vertical, 18)
                .fadeUp(delay: 0.46, duration: 0.5)

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
            .fadeUp(delay: 0.52, duration: 0.6)
        }
    }

    private var divider: some View {
        HStack(spacing: 12) {
            UNESColor.line.frame(height: 1)
            Text(.commonOr)
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .semibold))
                .tracking(1.1)
                .foregroundStyle(UNESColor.ink4)
            UNESColor.line.frame(height: 1)
        }
    }

    private func terms(centered: Bool) -> some View {
        (
            Text(.onboardingLoginTermsPrefix)
                + Text(.onboardingLoginTermsLink).fontWeight(.medium).foregroundStyle(UNESColor.ink2)
                + Text(.onboardingLoginTermsConnector)
                + Text(.onboardingLoginPrivacyLink).fontWeight(.medium).foregroundStyle(UNESColor.ink2)
                + Text(verbatim: ".")
        )
        .font(.system(size: 12.5))
        .foregroundStyle(UNESColor.ink4)
        .multilineTextAlignment(centered ? .center : .leading)
        .frame(maxWidth: .infinity, alignment: centered ? .center : .leading)
    }
}

#Preview {
    NavigationStack {
        LoginView(
            store: Store(initialState: LoginFeature.State()) {
                LoginFeature()
            }
        )
    }
}
