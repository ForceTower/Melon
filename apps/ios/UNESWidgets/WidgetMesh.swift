import SwiftUI

enum WidgetMeshVariant {
    case warm, cool, sun, rose, fresh
}

private struct StaticMeshBlob {
    let color: Color
    /// Fractional top-left of the blob within the container (matches CSS).
    let origin: CGPoint
    let size: CGFloat
}

private extension WidgetMeshVariant {
    var blobs: [StaticMeshBlob] {
        switch self {
        case .warm:
            return [
                .init(color: WidgetColor.plum,    origin: .init(x: -0.15, y: -0.10), size: 340),
                .init(color: WidgetColor.coral,   origin: .init(x:  0.50, y:  0.30), size: 300),
                .init(color: WidgetColor.amber,   origin: .init(x: -0.20, y:  0.65), size: 280),
                .init(color: WidgetColor.magenta, origin: .init(x:  0.55, y:  0.60), size: 240),
            ]
        case .cool:
            return [
                .init(color: Color(red: 0x1E / 255, green: 0x3A / 255, blue: 0x5F / 255),
                      origin: .init(x: -0.10, y: -0.10), size: 320),
                .init(color: Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255),
                      origin: .init(x: 0.55, y: 0.40),   size: 280),
                .init(color: Color(red: 0x88 / 255, green: 0xD4 / 255, blue: 0xC1 / 255),
                      origin: .init(x: -0.15, y: 0.60),  size: 260),
            ]
        case .sun:
            return [
                .init(color: Color(red: 0xC9 / 255, green: 0x45 / 255, blue: 0x38 / 255),
                      origin: .init(x: -0.10, y: -0.15), size: 320),
                .init(color: WidgetColor.amber, origin: .init(x: 0.50, y: 0.35),  size: 300),
                .init(color: WidgetColor.peach, origin: .init(x: -0.05, y: 0.55), size: 280),
            ]
        case .rose:
            return [
                .init(color: Color(red: 0x3D / 255, green: 0x1B / 255, blue: 0x3E / 255),
                      origin: .init(x: -0.10, y: -0.10), size: 300),
                .init(color: WidgetColor.magenta, origin: .init(x: 0.50, y: 0.30), size: 290),
                .init(color: WidgetColor.coral,   origin: .init(x: -0.15, y: 0.65), size: 260),
            ]
        case .fresh:
            return [
                .init(color: Color(red: 0x0F / 255, green: 0x4D / 255, blue: 0x3A / 255),
                      origin: .init(x: -0.10, y: -0.10), size: 320),
                .init(color: Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255),
                      origin: .init(x: 0.50, y: 0.35), size: 290),
                .init(color: WidgetColor.amber,
                      origin: .init(x: -0.10, y: 0.65), size: 240),
            ]
        }
    }
}

/// Static mesh for widgets — WidgetKit only re-renders on timeline ticks, so
/// the in-app `TimelineView` animation isn't useful here. Same blob layout
/// matches the app's `MeshGradientView` at `t = 0`.
struct WidgetMeshView: View {
    var variant: WidgetMeshVariant = .warm
    var intensity: Double = 1.0

    var body: some View {
        GeometryReader { geo in
            Canvas { ctx, size in
                let highlight = GraphicsContext.Shading.radialGradient(
                    Gradient(colors: [Color.white.opacity(0.08), .clear]),
                    center: CGPoint(x: size.width * 0.3, y: size.height * 0.2),
                    startRadius: 0,
                    endRadius: size.width * 0.7
                )

                for blob in variant.blobs {
                    let baseLeft = size.width * blob.origin.x
                    let baseTop  = size.height * blob.origin.y
                    let rect = CGRect(x: baseLeft, y: baseTop, width: blob.size, height: blob.size)

                    var blobCtx = ctx
                    blobCtx.addFilter(.blur(radius: 48))
                    blobCtx.opacity = 0.85 * intensity
                    blobCtx.fill(Path(ellipseIn: rect), with: .color(blob.color))
                }

                ctx.fill(Path(CGRect(origin: .zero, size: size)), with: highlight)
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .clipped()
        .allowsHitTesting(false)
    }
}
