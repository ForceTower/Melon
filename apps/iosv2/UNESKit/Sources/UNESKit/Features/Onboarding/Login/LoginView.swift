import ComposableArchitecture
import SwiftUI

struct LoginView: View {
    @Bindable var store: StoreOf<LoginFeature>
    @FocusState private var focus: Field?

    private enum Field {
        case username, password
    }

    var body: some View {
        ZStack(alignment: .top) {
            UNESColor.surface.ignoresSafeArea()
            ambientWash

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Eyebrow(text: "UEFS · SAGRES")
                        .fadeUp(delay: 0.04, duration: 0.5)

                    title
                        .padding(.top, 10)
                        .fadeUp(delay: 0.12, duration: 0.6)

                    Text("As mesmas credenciais do SAGRES.")
                        .font(.system(size: 15.5))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 8)
                        .fadeUp(delay: 0.2, duration: 0.6)

                    fieldsCard
                        .padding(.top, 26)
                        .fadeUp(delay: 0.28, duration: 0.6)

                    if let error = store.errorMessage {
                        Text(error)
                            .font(.system(size: 13, weight: .medium))
                            .foregroundStyle(UNESColor.coral)
                            .padding(.top, 10)
                            .transition(.opacity.combined(with: .move(edge: .top)))
                    }

                    Button("Esqueci minha senha") {
                        store.send(.forgotPasswordTapped)
                    }
                    .font(.system(size: 15, weight: .semibold))
                    .tint(UNESColor.accent)
                    .padding(.top, 12)
                    .fadeUp(delay: 0.34, duration: 0.6)
                }
                .padding(.horizontal, 24)
                .padding(.top, 12)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
        .animation(.easeInOut(duration: 0.2), value: store.errorMessage)
        .safeAreaInset(edge: .bottom) { bottomBar }
        .bareNavigationBar()
    }

    // MARK: Chrome

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
            titleLine("Entre com seu", color: UNESColor.ink)
            titleLine("usuário.", color: UNESColor.accent)
        }
    }

    private func titleLine(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 40, weight: .heavy))
            .tracking(-1.6)
            .foregroundStyle(color)
    }

    // MARK: Fields

    private var fieldsCard: some View {
        VStack(spacing: 0) {
            fieldRow(
                icon: "person",
                label: "Usuário",
                isFocused: focus == .username
            ) {
                TextField("seu.usuario", text: $store.username)
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
                label: "Senha",
                isFocused: focus == .password
            ) {
                Group {
                    if store.showPassword {
                        TextField("••••••••", text: $store.password)
                            .noAutocapitalization()
                            .autocorrectionDisabled()
                    } else {
                        SecureField("••••••••", text: $store.password)
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

    // MARK: Bottom bar

    private var bottomBar: some View {
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
                    UNESButtonLabel(text: "Entrar")
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
                    Text("Entrar com passkey").tracking(-0.17)
                }
            }
            .buttonStyle(.unesNeutral)
            .disabled(store.isLoading)
            .fadeUp(delay: 0.52, duration: 0.6)

            terms
                .padding(.top, 16)
                .fadeUp(delay: 0.6, duration: 0.5)
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 12)
        .background(UNESColor.surface)
    }

    private var divider: some View {
        HStack(spacing: 12) {
            UNESColor.line.frame(height: 1)
            Text("ou")
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .semibold))
                .tracking(1.1)
                .foregroundStyle(UNESColor.ink4)
            UNESColor.line.frame(height: 1)
        }
    }

    private var terms: some View {
        (
            Text("Ao continuar, você concorda com nossos ")
                + Text("Termos").fontWeight(.medium).foregroundStyle(UNESColor.ink2)
                + Text(" e ")
                + Text("Privacidade").fontWeight(.medium).foregroundStyle(UNESColor.ink2)
                + Text(".")
        )
        .font(.system(size: 12.5))
        .foregroundStyle(UNESColor.ink4)
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
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
