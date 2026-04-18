import SwiftUI

struct PrimaryButton: View {
    let title: String
    var showsArrow: Bool = true
    var isLoading: Bool = false
    var background: Color = UNESColor.ink
    var foreground: Color = UNESColor.surface
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading {
                    SpinnerView(color: foreground)
                        .frame(width: 20, height: 20)
                } else {
                    Text(title)
                        .font(UNESFont.sans(17, weight: .medium))
                        .tracking(-0.17)
                    if showsArrow {
                        ArrowRightGlyph()
                    }
                }
            }
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(background, in: Capsule(style: .continuous))
        }
        .buttonStyle(PressScaleStyle())
        .disabled(isLoading)
    }
}

struct GhostButton<Leading: View>: View {
    let title: String
    @ViewBuilder var leading: () -> Leading
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                leading()
                Text(title)
                    .font(UNESFont.sans(17, weight: .medium))
                    .tracking(-0.17)
            }
            .foregroundStyle(UNESColor.ink)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .overlay(
                Capsule(style: .continuous)
                    .strokeBorder(UNESColor.line, lineWidth: 1.5)
            )
        }
        .buttonStyle(PressScaleStyle())
    }
}

extension GhostButton where Leading == EmptyView {
    init(title: String, action: @escaping () -> Void) {
        self.init(title: title, leading: { EmptyView() }, action: action)
    }
}

struct GlassButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(UNESFont.sans(17, weight: .medium))
                .tracking(-0.17)
                .foregroundStyle(UNESColor.surface)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .modifier(LiquidGlassCapsule())
        }
        .buttonStyle(PressScaleStyle())
    }
}

/// Applies native iOS 26+ Liquid Glass when available, with a blurred-material
/// fallback on older runtimes.
private struct LiquidGlassCapsule: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.tint(.white.opacity(0.08)), in: Capsule(style: .continuous))
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(.white.opacity(0.14), lineWidth: 1)
                )
        } else {
            content.background(
                Capsule(style: .continuous)
                    .fill(.white.opacity(0.08))
                    .background(.ultraThinMaterial, in: Capsule(style: .continuous))
                    .overlay(
                        Capsule(style: .continuous)
                            .strokeBorder(.white.opacity(0.14), lineWidth: 1)
                    )
            )
        }
    }
}

struct PressScaleStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.25, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

struct ArrowRightGlyph: View {
    var size: CGFloat = 18
    var body: some View {
        Image(systemName: "arrow.right")
            .font(.system(size: size * 0.75, weight: .semibold))
    }
}

struct SpinnerView: View {
    var color: Color = UNESColor.surface
    @State private var rotating = false

    var body: some View {
        Circle()
            .trim(from: 0, to: 0.75)
            .stroke(color, style: StrokeStyle(lineWidth: 2, lineCap: .round))
            .rotationEffect(.degrees(rotating ? 360 : 0))
            .animation(.linear(duration: 0.7).repeatForever(autoreverses: false), value: rotating)
            .onAppear { rotating = true }
    }
}
