#if os(watchOS)
import SwiftUI

/// Mesh hero container — the one place the watch draws custom chrome; rows
/// and navigation stay native.
struct WatchMeshCard<Content: View>: View {
    var variant: MeshView.Variant
    /// Per-discipline color wash blended over the mesh.
    var wash: Color?
    @ViewBuilder var content: () -> Content

    var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 12, leading: 13, bottom: 12, trailing: 13))
            .background {
                ZStack {
                    UNESColor.darkBg
                    MeshView(variant: variant, intensity: 0.95)
                    LinearGradient(
                        colors: [
                            wash?.opacity(0.27) ?? Color(hex: 0x050A0E, opacity: 0.1),
                            Color(hex: 0x050A0E, opacity: 0.62),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(Color.white.opacity(0.06))
            }
    }
}

/// Thin progress ring with a centered label.
struct WatchRing<Label: View>: View {
    var fraction: Double
    var size: CGFloat
    var stroke: CGFloat
    var color: Color = .white
    var track: Color = .white.opacity(0.18)
    @ViewBuilder var label: () -> Label

    var body: some View {
        ZStack {
            Circle()
                .stroke(track, lineWidth: stroke)
            Circle()
                .trim(from: 0, to: min(1, max(0, fraction)))
                .stroke(color, style: StrokeStyle(lineWidth: stroke, lineCap: .round))
                .rotationEffect(.degrees(-90))
            label()
        }
        .frame(width: size, height: size)
    }
}

/// Compact evaluation/discipline code chip.
struct WatchCodeChip: View {
    var text: String
    var color: Color

    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .bold))
            .tracking(0.4)
            .foregroundStyle(color)
            .padding(EdgeInsets(top: 3, leading: 6, bottom: 3, trailing: 6))
            .background(color.opacity(0.16), in: RoundedRectangle(cornerRadius: 6, style: .continuous))
    }
}

/// One schedule row: time gutter, discipline color bar, title + detail.
struct WatchClassRow: View {
    var time: String
    var title: String
    var subtitle: String?
    var color: Color
    var isDone = false
    var isNow = false

    var body: some View {
        HStack(spacing: 8) {
            Text(time)
                .font(.system(size: 13, weight: isNow ? .bold : .medium))
                .monospacedDigit()
                .foregroundStyle(isNow ? UNESColor.ink : UNESColor.ink3)
                .frame(width: 40, alignment: .trailing)
            Capsule()
                .fill(color)
                .frame(width: 4, height: 28)
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 5) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .tracking(-0.2)
                        .foregroundStyle(UNESColor.ink)
                        .strikethrough(isDone)
                        .lineLimit(1)
                    if isNow {
                        Text(String.localized(.commonNow).uppercased())
                            .font(.system(size: 9, weight: .bold))
                            .tracking(0.3)
                            .foregroundStyle(.white)
                            .padding(EdgeInsets(top: 2, leading: 4, bottom: 2, trailing: 4))
                            .background(UNESColor.coral, in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                    }
                }
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(UNESColor.ink3)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .opacity(isDone ? 0.45 : 1)
    }
}

extension MeshView.Variant {
    /// Every discipline gets a mesh family, cycled the same way its color is.
    static func discipline(_ colorIndex: Int) -> MeshView.Variant {
        let variants: [MeshView.Variant] = [.warm, .cool, .sun, .rose, .fresh]
        return variants[abs(colorIndex) % variants.count]
    }
}

extension MessageItem {
    /// The mesh family behind a message hero, following its origin the way
    /// the accent color does.
    var meshVariant: MeshView.Variant {
        switch origin {
        case .discipline: .discipline(disciplineColorIndex ?? 0)
        case .secretariat: .cool
        case .campus: .sun
        case .app: .rose
        case .direct: .warm
        }
    }
}

/// The red "now" marker threaded between schedule rows. When given a
/// long-press action, the little ship flying at its tip launches the
/// Space Impact easter egg.
struct WatchNowLine: View {
    var now: Date
    var onShipLongPress: (() -> Void)?

    var body: some View {
        HStack(spacing: 6) {
            Text(now.formatted(date: .omitted, time: .shortened))
                .font(.system(size: 11, weight: .bold))
                .monospacedDigit()
                .foregroundStyle(UNESColor.alertRed)
            Circle()
                .fill(UNESColor.alertRed)
                .frame(width: 6, height: 6)
            Rectangle()
                .fill(UNESColor.alertRed)
                .frame(height: 1.5)
                .clipShape(Capsule())
            if let onShipLongPress {
                SI2ShipTrigger(color: UNESColor.alertRed, action: onShipLongPress)
            }
        }
    }
}
#endif
