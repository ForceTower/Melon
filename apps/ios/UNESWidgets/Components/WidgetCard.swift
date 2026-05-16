import SwiftUI

/// Card background. Both themes share the same structure: tonal surface,
/// optional mesh, and a top→bottom veil that lifts contrast on the inner
/// text. Mirrors `WidgetCard` in `screens-widgets.jsx` after the handoff
/// added per-mode palettes.
struct WidgetCardBackground: View {
    let theme: WidgetTheme
    var mesh: Bool = false

    var body: some View {
        ZStack {
            theme.surface

            if mesh {
                WidgetMeshView(variant: theme.meshVariant, intensity: theme.meshIntensity)
                LinearGradient(
                    stops: [
                        .init(color: theme.veilTop, location: 0),
                        .init(color: theme.veilBottom, location: 1),
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
            }
        }
    }
}

/// Small monospace pill used to badge the subject code (CALC II / ALGI / ...).
struct CodePill: View {
    enum Size { case sm, lg }
    let code: String
    let color: Color
    var size: Size = .sm

    var body: some View {
        let fs: CGFloat = size == .lg ? 11 : 9.5
        let pad: EdgeInsets = size == .lg
            ? .init(top: 4, leading: 9, bottom: 4, trailing: 9)
            : .init(top: 3, leading: 7, bottom: 3, trailing: 7)
        Text(code)
            .font(WidgetFont.mono(fs, weight: .semibold))
            .tracking(size == .lg ? 1.3 : 0.95)
            .foregroundStyle(color)
            .padding(pad)
            .background(color.opacity(0.13))
            .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

/// Pulsing dot used as the "live" indicator next to countdown labels.
struct LiveDot: View {
    var color: Color = WidgetColor.amber
    var size: CGFloat = 5

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .overlay(
                Circle()
                    .fill(color.opacity(0.2))
                    .frame(width: size * 2, height: size * 2)
            )
    }
}

/// Meta row: small icon + label (e.g. "Sala MT-14", "Adriana Matos"). Mirrors
/// the bottom strip on Medium / Large.
struct MetaItem: View {
    let systemImage: String
    let label: String
    var shrinks: Bool = false
    var foreground: Color
    /// Icon often reads lighter than the label in the design — pass the row's
    /// secondary color here if you want the iconography dimmed.
    var iconForeground: Color?

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: systemImage)
                .font(.system(size: 9, weight: .medium))
                .foregroundStyle(iconForeground ?? foreground.opacity(0.7))
            Text(label)
                .font(WidgetFont.sans(11))
                .foregroundStyle(foreground)
                .lineLimit(1)
                .truncationMode(.tail)
                .fixedSize(horizontal: !shrinks, vertical: false)
        }
        .layoutPriority(shrinks ? 0 : 1)
    }
}
