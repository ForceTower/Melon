import SwiftUI

/// The filled v2 CTA: 54pt tall, continuous 16pt corners, presses to 97%.
struct UNESButtonStyle: ButtonStyle {
    enum Tone {
        /// Ink on surface — the standard CTA (flips in dark mode).
        case dark
        /// Fixed light paper on always-dark screens.
        case light
        case accent
        /// Translucent blur chip on always-dark screens.
        case glass
        /// Quiet secondary on surface screens.
        case neutral
    }

    var tone: Tone = .dark

    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background { background }
            .clipShape(shape)
            .overlay {
                if let borderColor {
                    shape.strokeBorder(borderColor)
                }
            }
            .opacity(isEnabled ? 1 : 0.4)
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(UNESMotion.ease(0.15, overshoot: 1.2), value: configuration.isPressed)
    }

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: 16, style: .continuous)
    }

    @ViewBuilder
    private var background: some View {
        switch tone {
        case .dark: UNESColor.ink
        case .light: UNESColor.paper
        case .accent: UNESColor.accent
        case .glass: Color.white.opacity(0.1).background(.ultraThinMaterial)
        case .neutral: UNESColor.surface2
        }
    }

    private var foreground: Color {
        switch tone {
        case .dark: UNESColor.surface
        case .light: Color(hex: 0x1A1420)
        case .accent: .white
        case .glass: UNESColor.paper
        case .neutral: UNESColor.ink
        }
    }

    private var borderColor: Color? {
        switch tone {
        case .glass: .white.opacity(0.16)
        case .neutral: UNESColor.line
        case .dark, .light, .accent: nil
        }
    }
}

extension ButtonStyle where Self == UNESButtonStyle {
    static var unesDark: UNESButtonStyle { UNESButtonStyle(tone: .dark) }
    static var unesLight: UNESButtonStyle { UNESButtonStyle(tone: .light) }
    static var unesAccent: UNESButtonStyle { UNESButtonStyle(tone: .accent) }
    static var unesGlass: UNESButtonStyle { UNESButtonStyle(tone: .glass) }
    static var unesNeutral: UNESButtonStyle { UNESButtonStyle(tone: .neutral) }
}

/// Standard CTA label: text with the trailing arrow.
struct UNESButtonLabel: View {
    var text: String

    var body: some View {
        HStack(spacing: 8) {
            Text(text).tracking(-0.17)
            Image(systemName: "arrow.right")
                .font(.system(size: 15, weight: .semibold))
        }
    }
}

#Preview {
    VStack(spacing: 12) {
        Button {} label: { UNESButtonLabel(text: "Continuar") }
            .buttonStyle(.unesDark)
        Button {} label: { UNESButtonLabel(text: "Conhecer o app") }
            .buttonStyle(.unesLight)
        Button("Já tenho matrícula") {}
            .buttonStyle(.unesGlass)
        Button("Entrar") {}
            .buttonStyle(.unesDark)
            .disabled(true)
    }
    .padding(28)
    .background(UNESColor.darkBg)
}
