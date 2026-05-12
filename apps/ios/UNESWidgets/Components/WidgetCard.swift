import SwiftUI

/// Mirrors `WidgetCard` from `screens-widgets.jsx`. Dark mesh hero is the
/// canonical "next class" surface; the light variant is reused by the
/// "dia concluído" state.
struct WidgetCardBackground: View {
    var dark: Bool = true
    var mesh: Bool = false
    var meshVariant: WidgetMeshVariant = .cool

    var body: some View {
        ZStack {
            if dark {
                Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255)
            } else {
                WidgetColor.surfaceLight
            }

            if mesh {
                WidgetMeshView(variant: meshVariant, intensity: 1)
                LinearGradient(
                    stops: [
                        .init(color: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.08), location: 0),
                        .init(color: Color(red: 0x1A / 255, green: 0x0F / 255, blue: 0x28 / 255).opacity(0.55), location: 1),
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

    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: systemImage)
                .font(.system(size: 9, weight: .medium))
                .foregroundStyle(foreground.opacity(0.7))
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
