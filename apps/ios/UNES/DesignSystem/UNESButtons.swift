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

/// Liquid Glass pill button. Generic over a leading view so callers can pass
/// an icon (matches `GhostButton`). Colors default to the `onDark` palette
/// (white text + whisper-white stroke + subtle surface tint) so it reads
/// well over the welcome mesh; pass `foreground/tint/stroke` for screens
/// with light surfaces — see `LoginView` for a light-scheme example.
struct GlassButton<Leading: View>: View {
    let title: String
    var foreground: Color = UNESColor.surfaceLight
    var tint: Color = UNESColor.surface.opacity(0.08)
    var stroke: Color = .white.opacity(0.14)
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
            .foregroundStyle(foreground)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .modifier(LiquidGlassCapsule(tint: tint, stroke: stroke))
        }
        .buttonStyle(PressScaleStyle())
    }
}

extension GlassButton where Leading == EmptyView {
    init(
        title: String,
        foreground: Color = UNESColor.surfaceLight,
        tint: Color = UNESColor.surface.opacity(0.08),
        stroke: Color = .white.opacity(0.14),
        action: @escaping () -> Void
    ) {
        self.init(
            title: title,
            foreground: foreground,
            tint: tint,
            stroke: stroke,
            leading: { EmptyView() },
            action: action
        )
    }
}

/// Applies native iOS 26+ Liquid Glass when available, with a blurred-material
/// fallback on older runtimes. Tint is reused as the fallback fill so both
/// branches share a single visual parameter.
private struct LiquidGlassCapsule: ViewModifier {
    var tint: Color = UNESColor.surface.opacity(0.08)
    var stroke: Color = .white.opacity(0.14)

    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular.tint(tint), in: Capsule(style: .continuous))
                .overlay(
                    Capsule(style: .continuous)
                        .strokeBorder(stroke, lineWidth: 1)
                )
        } else {
            content.background(
                Capsule(style: .continuous)
                    .fill(tint)
                    .background(.ultraThinMaterial, in: Capsule(style: .continuous))
                    .overlay(
                        Capsule(style: .continuous)
                            .strokeBorder(stroke, lineWidth: 1)
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
