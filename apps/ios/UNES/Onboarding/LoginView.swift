import SwiftUI

struct LoginView: View {
    let onSubmit: (String) -> Void
    let onBack: () -> Void

    @State private var studentId = ""
    @State private var password  = ""
    @State private var showPassword = false
    @FocusState private var focusedField: Field?
    @State private var isLoading = false

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
                    Text("◦ UEFS · SAGRES")
                        .font(UNESFont.sans(12, weight: .medium))
                        .tracking(1.4)
                        .textCase(.uppercase)
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 60)
                        .fadeUpOnAppear(delay: 0.05)

                    titleText
                        .font(UNESFont.serif(42))
                        .tracking(-1.05)
                        .foregroundStyle(UNESColor.ink)
                        .padding(.top, 10)
                        .fadeUpOnAppear(delay: 0.15)

                    Text("As mesmas credenciais que você usa pra entrar no Portal. Nada fica no nosso servidor.")
                        .font(UNESFont.sans(15))
                        .lineSpacing(3)
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 8)
                        .padding(.bottom, 28)
                        .fadeUpOnAppear(delay: 0.25)

                    inputGroup
                        .fadeUpOnAppear(delay: 0.35)

                    Button("Esqueci minha senha") {}
                        .font(UNESFont.sans(14, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .padding(.top, 10)
                        .fadeUpOnAppear(delay: 0.45)

                    Spacer().frame(height: 28)

                    PrimaryButton(
                        title: "Entrar",
                        isLoading: isLoading,
                        action: submit
                    )
                    .opacity((studentId.isEmpty || password.isEmpty) && !isLoading ? 0.3 : 1)
                    .disabled(studentId.isEmpty || password.isEmpty || isLoading)
                    .fadeUpOnAppear(delay: 0.5)

                    dividerRow
                        .padding(.vertical, 18)
                        .fadeInOnAppear(delay: 0.55)

                    GhostButton(
                        title: "Entrar com passkey",
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

            backButton
        }
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
                text: $studentId,
                field: .id,
                keyboard: .numberPad,
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
                text: $password,
                field: .password,
                keyboard: .default,
                isSecure: !showPassword,
                trailing: {
                    Button {
                        showPassword.toggle()
                    } label: {
                        Image(systemName: showPassword ? "eye.slash" : "eye")
                            .font(.system(size: 14, weight: .regular))
                    }
                }
            )
        }
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Color.white)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(
                    focusedField != nil ? UNESColor.ink : UNESColor.line,
                    lineWidth: focusedField != nil ? 1.5 : 1
                )
        )
        .shadow(
            color: focusedField != nil ? UNESColor.ink.opacity(0.06) : .clear,
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

    @ViewBuilder
    private var backButton: some View {
        Button(action: onBack) {
            Image(systemName: "chevron.left")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(UNESColor.ink)
                .frame(width: 40, height: 40)
                .background(
                    Circle()
                        .fill(UNESColor.surface.opacity(0.6))
                        .background(.ultraThinMaterial, in: Circle())
                        .overlay(Circle().stroke(UNESColor.ink.opacity(0.06), lineWidth: 1))
                )
        }
        .padding(.leading, 14)
        .padding(.top, 11)
        .buttonStyle(.plain)
    }

    private func submit() {
        guard !isLoading else { return }
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.9) {
            onSubmit(studentId)
        }
    }

    private func passkey() {
        isLoading = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
            onSubmit("passkey")
        }
    }
}

#Preview {
    LoginView(onSubmit: { _ in }, onBack: {})
}
