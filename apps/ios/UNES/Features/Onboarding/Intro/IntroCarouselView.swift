import SwiftUI
import UserNotifications

private enum IntroSlide: Int, CaseIterable, Identifiable {
    case schedule, grades, messages, notifications
    var id: Int { rawValue }

    var eyebrow: String {
        switch self {
        case .schedule:      return "horário"
        case .grades:        return "notas"
        case .messages:      return "recados"
        case .notifications: return "notificações"
        }
    }

    var variant: MeshVariant {
        switch self {
        case .schedule:      return .cool
        case .grades:        return .sun
        case .messages:      return .rose
        case .notifications: return .warm
        }
    }

    var accentColor: Color {
        UNESColor.amber
    }

    var body: String {
        switch self {
        case .schedule:      return "A grade da UEFS puxada direto do Portal. Aulas canceladas, salas trocadas e provas — tudo em tempo real."
        case .grades:        return "Notas parciais, coeficiente e histórico. Sem precisar entrar no Portal pelo navegador toda semana."
        case .messages:      return "Recados de professores, coordenação e DCE — sem perder prazos nem assembleias importantes."
        case .notifications: return "Nota nova, recado de professor, material publicado, sala trocada — um toque no bolso antes de você abrir o app."
        }
    }
}

struct IntroCarouselView: View {
    let onDone: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var index: Int = 0
    @State private var contentKey: Int = 0

    private var slide: IntroSlide { IntroSlide.allCases[index] }

    var body: some View {
        ZStack {
            UNESColor.surface.ignoresSafeArea()

            VStack(spacing: 0) {
                topBar
                    .padding(.top, 8)
                    .padding(.horizontal, 20)

                illustrationArea
                    .frame(height: 340)
                    .frame(maxWidth: .infinity)

                Spacer(minLength: 0)

                copyAndCTA
                    .padding(.horizontal, 28)
                    .padding(.bottom, 16)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    @ViewBuilder
    private var topBar: some View {
        HStack {
            Button(action: goBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(UNESColor.ink3)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)

            Spacer()

            HStack(spacing: 6) {
                ForEach(IntroSlide.allCases) { s in
                    Capsule()
                        .fill(s.rawValue == index ? UNESColor.ink : UNESColor.ink4.opacity(0.35))
                        .frame(width: s.rawValue == index ? 24 : 6, height: 6)
                        .animation(.spring(response: 0.45, dampingFraction: 0.7), value: index)
                }
            }

            Spacer()

            Button("Pular", action: onDone)
                .font(UNESFont.sans(14, weight: .medium))
                .foregroundStyle(UNESColor.ink3)
                .frame(width: 48, height: 40)
        }
    }

    @ViewBuilder
    private var illustrationArea: some View {
        ZStack {
            // mesh backdrop with warm surface tint
            RoundedRectangle(cornerRadius: 40, style: .continuous)
                .fill(Color.clear)
                .overlay(
                    MeshGradientView(variant: slide.variant, intensity: 0.65)
                        .overlay(UNESColor.surface.opacity(0.82))
                        .clipShape(RoundedRectangle(cornerRadius: 40, style: .continuous))
                )
                .padding(30)
                .id("mesh-\(contentKey)")
                .scaleInOnAppear()

            Group {
                switch slide {
                case .schedule:      ScheduleIllustration()
                case .grades:        GradesIllustration()
                case .messages:      MessagesIllustration()
                case .notifications: NotificationsIllustration()
                }
            }
            .id("illust-\(contentKey)")
        }
    }

    @ViewBuilder
    private var copyAndCTA: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("◦ \(slide.eyebrow)")
                .font(UNESFont.sans(12, weight: .medium))
                .tracking(1.4)
                .textCase(.uppercase)
                .foregroundStyle(UNESColor.ink3)
                .id("eyebrow-\(contentKey)")
                .fadeUpOnAppear(delay: 0.05)

            titleText
                .font(UNESFont.serif(44))
                .tracking(-1.1)
                .lineSpacing(-2)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 12)
                .id("title-\(contentKey)")
                .fadeUpOnAppear(delay: 0.2)

            Text(slide.body)
                .font(UNESFont.sans(16))
                .tracking(-0.08)
                .lineSpacing(3)
                .foregroundStyle(UNESColor.ink3)
                .padding(.top, 14)
                .id("body-\(contentKey)")
                .fadeUpOnAppear(delay: 0.3)

            Spacer().frame(height: 28)

            PrimaryButton(
                title: index == IntroSlide.allCases.count - 1 ? "Entrar na conta" : "Continuar",
                action: advance
            )
            .id("cta-\(contentKey)")
            .fadeUpOnAppear(delay: 0.4)
        }
    }

    @ViewBuilder
    private var titleText: some View {
        VStack(alignment: .leading, spacing: 0) {
            switch slide {
            case .schedule:
                Text("Sua semana,")
                    .foregroundStyle(UNESColor.ink)
                Text("organizada.")
                    .italic()
                    .foregroundStyle(slide.accentColor)
            case .grades:
                Text("Acompanhe")
                    .foregroundStyle(UNESColor.ink)
                Text("\(Text("seu ").foregroundStyle(UNESColor.ink))\(Text("desempenho.").italic().foregroundStyle(slide.accentColor))")
            case .messages:
                Text("Tudo o que")
                    .foregroundStyle(UNESColor.ink)
                Text("\(Text("você ").foregroundStyle(UNESColor.ink))\(Text("precisa saber.").italic().foregroundStyle(slide.accentColor))")
            case .notifications:
                Text("\(Text("Avisa no ").foregroundStyle(UNESColor.ink))\(Text("instante").italic().foregroundStyle(slide.accentColor))")
                Text("que acontece.")
                    .foregroundStyle(UNESColor.ink)
            }
        }
    }

    private func advance() {
        if index < IntroSlide.allCases.count - 1 {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                index += 1
                contentKey += 1
            }
        } else {
            Task { await requestNotificationsAndFinish() }
        }
    }

    private func requestNotificationsAndFinish() async {
        _ = try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .badge, .sound])
        await MainActor.run { onDone() }
    }

    private func goBack() {
        if index > 0 {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.8)) {
                index -= 1
                contentKey += 1
            }
        } else {
            dismiss()
        }
    }
}

#Preview {
    NavigationStack {
        IntroCarouselView(onDone: {})
    }
}
