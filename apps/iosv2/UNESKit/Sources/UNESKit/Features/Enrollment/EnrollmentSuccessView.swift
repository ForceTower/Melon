import ComposableArchitecture
import SwiftUI

/// The always-dark confirmation: the proposal is registered upstream; seat
/// confirmation arrives later by message. No back navigation — "Concluir"
/// returns to the entry screen.
struct EnrollmentSuccessView: View {
    let store: StoreOf<EnrollmentSuccessFeature>

    var body: some View {
        ZStack {
            Color(hex: 0x0F1A14)
            MeshView(variant: .fresh, intensity: 0.85)
                .opacity(0.9)
            LinearGradient(
                stops: [
                    .init(color: Color(hex: 0x0F1A14, opacity: 0.2), location: 0),
                    .init(color: Color(hex: 0x0F1A14, opacity: 0.78), location: 1),
                ],
                startPoint: .top,
                endPoint: .bottom
            )

            content
        }
        .ignoresSafeArea()
        .environment(\.colorScheme, .dark)
        .navigationBarBackButtonHidden(true)
        .bareNavigationBar()
        .hiddenTabBar()
    }

    private var content: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 70)

            checkSeal
                .popIn(delay: 0.05, duration: 0.5, from: 0.7, offsetY: 10, overshoot: 1.3)
                .padding(.bottom, 26)

            Text("Matrícula · \(store.session.window?.semester ?? "")")
                .textCase(.uppercase)
                .font(.system(size: 11, weight: .semibold))
                .tracking(0.66)
                .foregroundStyle(UNESColor.paper.opacity(0.7))
                .fadeUp(delay: 0.2)
                .padding(.bottom, 12)

            Text("Proposta enviada")
                .font(.system(size: 36, weight: .bold))
                .tracking(-1.26)
                .foregroundStyle(UNESColor.paper)
                .fadeUp(delay: 0.26)
                .padding(.bottom, 12)

            message
                .fadeUp(delay: 0.32)
                .padding(.bottom, 28)

            stats
                .fadeUp(delay: 0.38)
                .padding(.bottom, 30)

            Button("Concluir") {
                store.send(.doneTapped)
            }
            .buttonStyle(.unesLight)
            .frame(maxWidth: 320)
            .fadeUp(delay: 0.44)

            Spacer(minLength: 40)
        }
        .padding(.horizontal, 26)
        .frame(maxWidth: .infinity)
    }

    private var checkSeal: some View {
        EnrollmentCheckmark()
            .frame(width: 42, height: 42)
            .frame(width: 88, height: 88)
            .background(.white.opacity(0.14), in: Circle())
            .overlay {
                Circle().strokeBorder(.white.opacity(0.25))
            }
    }

    private var message: some View {
        var hours = AttributedString("\(store.session.totalHours)h")
        hours.font = .system(size: 15, weight: .bold)
        var count = AttributedString(DisciplinesFormat.disciplineCountLabel(store.session.picks.count))
        count.font = .system(size: 15, weight: .bold)

        var text = AttributedString("Sua matrícula de ")
        text += hours
        text += AttributedString(" em ")
        text += count
        text += AttributedString(" foi registrada. A confirmação das vagas chega por mensagem.")

        return Text(text)
            .font(.system(size: 15, weight: .medium))
            .lineSpacing(4)
            .foregroundStyle(UNESColor.paper.opacity(0.82))
            .multilineTextAlignment(.center)
            .frame(maxWidth: 300)
    }

    private var stats: some View {
        HStack(spacing: 10) {
            stat(store.session.picks.count, "disciplinas")
            stat(store.session.waitlistedCount, "em fila")
            stat(store.allowsOtherCount, "aceitam troca")
        }
        .frame(maxWidth: 320)
    }

    private func stat(_ value: Int, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("\(value)")
                .font(.system(size: 26, weight: .bold))
                .tracking(-0.91)
                .monospacedDigit()
                .foregroundStyle(UNESColor.paper)
            Text(label)
                .textCase(.uppercase)
                .font(.system(size: 9.5, weight: .semibold))
                .tracking(0.38)
                .foregroundStyle(UNESColor.paper.opacity(0.7))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(EdgeInsets(top: 13, leading: 12, bottom: 13, trailing: 8))
        .background(.white.opacity(0.1), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .strokeBorder(.white.opacity(0.16))
        }
    }
}

/// The success check, stroke-drawn on appearance.
private struct EnrollmentCheckmark: View {
    @State private var drawn = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        CheckmarkShape()
            .trim(from: 0, to: drawn ? 1 : 0)
            .stroke(.white, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
            .onAppear {
                guard !drawn else { return }
                if reduceMotion {
                    drawn = true
                } else {
                    withAnimation(UNESMotion.draw(0.45).delay(0.3)) {
                        drawn = true
                    }
                }
            }
    }

    private struct CheckmarkShape: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            path.move(to: CGPoint(x: rect.width * 0.1, y: rect.height * 0.55))
            path.addLine(to: CGPoint(x: rect.width * 0.42, y: rect.height * 0.85))
            path.addLine(to: CGPoint(x: rect.width * 0.95, y: rect.height * 0.18))
            return path
        }
    }
}

#Preview {
    NavigationStack {
        EnrollmentSuccessView(
            store: Store(initialState: EnrollmentSuccessFeature.State(session: .preview)) {
                EnrollmentSuccessFeature()
            }
        )
    }
}
