import AuthenticationServices
import SwiftUI
import UIKit
import Umbrella

struct LoginView: View {
    let onSubmit: (String) -> Void

    @State private var viewModel: LoginViewModel
    @FocusState private var focusedField: Field?
    @State private var forgotPasswordPresented: Bool = false
    // Mirrors the intrinsic-height pattern used by `LogoutConfirmationSheet`
    // / `AboutSheet` — the sheet reports its measured height back so the
    // detent never reserves empty space below the buttons.
    @State private var forgotPasswordSheetHeight: CGFloat = 440

    private static let forgotPasswordURL = URL(string: "https://academico.uefs.br/PortalSagres/Acesso.aspx")!

    init(
        loginUseCase: AuthLoginUseCase?,
        beginPasskeyLogin: AuthBeginPasskeyLoginUseCase?,
        completePasskeyLogin: AuthCompletePasskeyLoginUseCase?,
        onSubmit: @escaping (String) -> Void
    ) {
        _viewModel = State(initialValue: LoginViewModel(
            loginUseCase: loginUseCase,
            beginPasskeyLogin: beginPasskeyLogin,
            completePasskeyLogin: completePasskeyLogin
        ))
        self.onSubmit = onSubmit
    }

    enum Field { case id, password }

    var body: some View {
        ZStack(alignment: .topLeading) {
            UNESColor.surface.ignoresSafeArea()

            // soft mesh behind hero
            MeshGradientView(variant: .warm, intensity: 0.55)
                .frame(height: 340)
                .mask(
                    LinearGradient(
                        colors: [.black, .black.opacity(0)],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .ignoresSafeArea(edges: .top)
                .allowsHitTesting(false)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("◦ UEFS · UNES")
                        .font(UNESFont.sans(12, weight: .medium))
                        .tracking(1.4)
                        .textCase(.uppercase)
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 16)
                        .fadeUpOnAppear(delay: 0.05)

                    titleText
                        .font(UNESFont.serif(42))
                        .tracking(-1.05)
                        .foregroundStyle(UNESColor.ink)
                        .padding(.top, 10)
                        .padding(.bottom, 28)
                        .fadeUpOnAppear(delay: 0.15)

                    inputGroup
                        .fadeUpOnAppear(delay: 0.35)

                    Button("Esqueci minha senha") {
                        focusedField = nil
                        forgotPasswordPresented = true
                    }
                        .font(UNESFont.sans(14, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 10)
                        .fadeUpOnAppear(delay: 0.45)

                    if let errorMessage = viewModel.errorMessage {
                        Text(errorMessage)
                            .font(UNESFont.sans(13, weight: .medium))
                            .foregroundStyle(UNESColor.accent)
                            .padding(.top, 14)
                            .transition(.opacity)
                    }

                    Spacer().frame(height: 28)

                    PrimaryButton(
                        title: "Entrar",
                        isLoading: viewModel.isLoading,
                        action: submit
                    )
                    .opacity(viewModel.canSubmit || viewModel.isLoading ? 1 : 0.3)
                    .disabled(!viewModel.canSubmit)
                    .fadeUpOnAppear(delay: 0.5)

                    dividerRow
                        .padding(.vertical, 18)
                        .fadeInOnAppear(delay: 0.55)

                    GlassButton(
                        title: "Entrar com passkey",
                        foreground: UNESColor.ink,
                        tint: UNESColor.ink.opacity(0.05),
                        stroke: UNESColor.line,
                        leading: { passkeyIcon },
                        action: passkey
                    )
                    .fadeUpOnAppear(delay: 0.6)

                    termsFooter
                        .padding(.top, 16)
                        .fadeInOnAppear(delay: 0.7)
                }
                .padding(.horizontal, 28)
                .padding(.top, 13)
                .padding(.bottom, 6)
            }
        }
        .toolbarBackground(.hidden, for: .navigationBar)
        .tint(UNESColor.ink)
        .animation(.easeInOut(duration: 0.2), value: viewModel.errorMessage)
        .sheet(isPresented: $forgotPasswordPresented) {
            forgotPasswordSheet
        }
    }

    @ViewBuilder
    private var forgotPasswordSheet: some View {
        if #available(iOS 16.4, *) {
            ForgotPasswordSheet(
                onOpenPortal: openForgotPasswordPortal,
                measuredHeight: $forgotPasswordSheetHeight
            )
            .presentationDetents([.height(forgotPasswordSheetHeight)])
            .presentationDragIndicator(.visible)
            .presentationBackground(UNESColor.surface)
        } else {
            ForgotPasswordSheet(
                onOpenPortal: openForgotPasswordPortal,
                measuredHeight: $forgotPasswordSheetHeight
            )
            .presentationDetents([.height(forgotPasswordSheetHeight)])
            .presentationDragIndicator(.visible)
        }
    }

    private func openForgotPasswordPortal() {
        UIApplication.shared.open(Self.forgotPasswordURL)
        forgotPasswordPresented = false
    }

    private var titleText: Text {
        Text("\(Text("Entre com seu\n").foregroundStyle(UNESColor.ink))\(Text("usuário.").italic().foregroundStyle(UNESColor.accent))")
    }

    @ViewBuilder
    private var inputGroup: some View {
        VStack(spacing: 0) {
            inputRow(
                label: "Usuário",
                placeholder: "nome123",
                text: $viewModel.studentId,
                field: .id,
                keyboard: .default,
                isSecure: false,
                trailing: {
                    Image(systemName: "person.text.rectangle")
                        .font(.system(size: 14, weight: .regular))
                }
            )

            Divider().background(UNESColor.line)

            inputRow(
                label: "Senha",
                placeholder: "••••••••",
                text: $viewModel.password,
                field: .password,
                keyboard: .default,
                isSecure: !viewModel.showPassword,
                trailing: {
                    Button {
                        viewModel.showPassword.toggle()
                    } label: {
                        Image(systemName: viewModel.showPassword ? "eye.slash" : "eye")
                            .font(.system(size: 14, weight: .regular))
                    }
                }
            )
        }
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(UNESColor.card)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(
                    focusedField != nil ? UNESColor.ink : UNESColor.line,
                    lineWidth: focusedField != nil ? 1.5 : 1
                )
        )
        .shadow(
            color: focusedField != nil ? Color.black.opacity(0.06) : .clear,
            radius: 4
        )
        .animation(.easeInOut(duration: 0.2), value: focusedField)
    }

    @ViewBuilder
    private func inputRow<Trailing: View>(
        label: String,
        placeholder: String,
        text: Binding<String>,
        field: Field,
        keyboard: UIKeyboardType,
        isSecure: Bool,
        @ViewBuilder trailing: () -> Trailing
    ) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(label)
                    .font(UNESFont.sans(11, weight: .medium))
                    .tracking(0.9)
                    .textCase(.uppercase)
                    .foregroundStyle(UNESColor.ink3)

                Group {
                    if isSecure {
                        SecureField(placeholder, text: text)
                    } else {
                        TextField(placeholder, text: text)
                            .keyboardType(keyboard)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    }
                }
                .font(UNESFont.sans(17))
                .foregroundStyle(UNESColor.ink)
                .focused($focusedField, equals: field)
                .submitLabel(field == .password ? .done : .next)
                .onSubmit {
                    switch field {
                    case .id: focusedField = .password
                    case .password: submit()
                    }
                }
            }

            Spacer()

            trailing()
                .frame(width: 32, height: 32)
                .foregroundStyle(UNESColor.ink3)
                .background(UNESColor.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .padding(.horizontal, 18)
        .frame(minHeight: 60)
    }

    @ViewBuilder
    private var dividerRow: some View {
        HStack(spacing: 12) {
            Rectangle().fill(UNESColor.line).frame(height: 1)
            Text("ou")
                .font(UNESFont.mono(10))
                .tracking(1.5)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink4)
            Rectangle().fill(UNESColor.line).frame(height: 1)
        }
    }

    @ViewBuilder
    private var passkeyIcon: some View {
        Image(systemName: "key.horizontal")
            .font(.system(size: 18, weight: .regular))
    }

    private var termsFooter: Text {
        Text("\(Text("Ao continuar, você concorda com nossos ").foregroundStyle(UNESColor.ink4))\(Text("Termos").foregroundStyle(UNESColor.ink2).underline())\(Text(" e ").foregroundStyle(UNESColor.ink4))\(Text("Privacidade").foregroundStyle(UNESColor.ink2).underline())\(Text(".").foregroundStyle(UNESColor.ink4))")
    }

    private func submit() {
        focusedField = nil
        Task {
            if let user = await viewModel.submit() {
                onSubmit(user.name)
            }
        }
    }

    private func passkey() {
        focusedField = nil
        let anchor = Self.keyWindow() ?? ASPresentationAnchor()
        Task {
            if let user = await viewModel.loginWithPasskey(anchor: anchor) {
                onSubmit(user.name)
            }
        }
    }

    @MainActor
    private static func keyWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }
}

#Preview {
    NavigationStack {
        LoginView(
            loginUseCase: nil,
            beginPasskeyLogin: nil,
            completePasskeyLogin: nil,
            onSubmit: { _ in }
        )
    }
}
