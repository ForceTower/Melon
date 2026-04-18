import SwiftUI

enum MeshVariant {
    case warm, cool, sun, rose, fresh
}

private struct MeshBlob {
    let color: Color
    /// Fractional top-left position within the container (matches CSS
    /// `top`/`left` percentages — negative values push off-canvas).
    let origin: CGPoint
    let size: CGFloat
    let amplitude: CGSize
    let period: Double
    let phase: Double
    let ySpeed: Double
}

private extension MeshVariant {
    var blobs: [MeshBlob] {
        switch self {
        case .warm:
            return [
                MeshBlob(color: UNESColor.plum,    origin: CGPoint(x: -0.15, y: -0.10), size: 340,
                         amplitude: CGSize(width: 40, height: 30),  period: 14, phase: 0,   ySpeed: 1.1),
                MeshBlob(color: UNESColor.coral,   origin: CGPoint(x:  0.50, y:  0.30), size: 300,
                         amplitude: CGSize(width: 45, height: 25),  period: 11, phase: 1.3, ySpeed: 0.9),
                MeshBlob(color: UNESColor.amber,   origin: CGPoint(x: -0.20, y:  0.65), size: 280,
                         amplitude: CGSize(width: 35, height: 40),  period: 17, phase: 2.6, ySpeed: 1.2),
                MeshBlob(color: UNESColor.magenta, origin: CGPoint(x:  0.55, y:  0.60), size: 240,
                         amplitude: CGSize(width: 40, height: 30),  period: 13, phase: 0.8, ySpeed: -1.0),
            ]
        case .cool:
            return [
                MeshBlob(color: Color(red: 0x1E / 255, green: 0x3A / 255, blue: 0x5F / 255),
                         origin: CGPoint(x: -0.10, y: -0.10), size: 320,
                         amplitude: CGSize(width: 40, height: 30), period: 13, phase: 0, ySpeed: 1),
                MeshBlob(color: Color(red: 0x3B / 255, green: 0x9E / 255, blue: 0xAE / 255),
                         origin: CGPoint(x: 0.55, y: 0.40), size: 280,
                         amplitude: CGSize(width: 45, height: 25), period: 15, phase: 1.5, ySpeed: 0.8),
                MeshBlob(color: Color(red: 0x88 / 255, green: 0xD4 / 255, blue: 0xC1 / 255),
                         origin: CGPoint(x: -0.15, y: 0.60), size: 260,
                         amplitude: CGSize(width: 35, height: 40), period: 12, phase: 2.2, ySpeed: 1.1),
            ]
        case .sun:
            return [
                MeshBlob(color: UNESColor.accentPress, origin: CGPoint(x: -0.10, y: -0.15), size: 320,
                         amplitude: CGSize(width: 40, height: 30), period: 15, phase: 0, ySpeed: 1),
                MeshBlob(color: UNESColor.amber,       origin: CGPoint(x: 0.50, y: 0.35),  size: 300,
                         amplitude: CGSize(width: 45, height: 25), period: 12, phase: 1.3, ySpeed: 0.9),
                MeshBlob(color: UNESColor.peach,       origin: CGPoint(x: -0.05, y: 0.55), size: 280,
                         amplitude: CGSize(width: 35, height: 40), period: 14, phase: 2.4, ySpeed: 1.15),
            ]
        case .rose:
            return [
                MeshBlob(color: Color(red: 0x3D / 255, green: 0x1B / 255, blue: 0x3E / 255),
                         origin: CGPoint(x: -0.10, y: -0.10), size: 300,
                         amplitude: CGSize(width: 40, height: 30), period: 14, phase: 0, ySpeed: 1),
                MeshBlob(color: UNESColor.magenta, origin: CGPoint(x: 0.50, y: 0.30), size: 290,
                         amplitude: CGSize(width: 45, height: 25), period: 13, phase: 1.3, ySpeed: 0.9),
                MeshBlob(color: UNESColor.coral,   origin: CGPoint(x: -0.15, y: 0.65), size: 260,
                         amplitude: CGSize(width: 35, height: 40), period: 16, phase: 2.5, ySpeed: 1.1),
            ]
        case .fresh:
            return [
                MeshBlob(color: Color(red: 0x0F / 255, green: 0x4D / 255, blue: 0x3A / 255),
                         origin: CGPoint(x: -0.10, y: -0.10), size: 320,
                         amplitude: CGSize(width: 40, height: 30), period: 13, phase: 0, ySpeed: 1),
                MeshBlob(color: Color(red: 0x4A / 255, green: 0xA6 / 255, blue: 0x79 / 255),
                         origin: CGPoint(x: 0.50, y: 0.35), size: 290,
                         amplitude: CGSize(width: 45, height: 25), period: 14, phase: 1.3, ySpeed: 0.9),
                MeshBlob(color: UNESColor.amber,
                         origin: CGPoint(x: -0.10, y: 0.65), size: 240,
                         amplitude: CGSize(width: 35, height: 40), period: 15, phase: 2.5, ySpeed: 1.1),
            ]
        }
    }
}

struct MeshGradientView: View {
    var variant: MeshVariant = .warm
    var intensity: Double = 1.0

    var body: some View {
        GeometryReader { geo in
            TimelineView(.animation) { context in
                let t = context.date.timeIntervalSince1970
                Canvas { ctx, size in
                    // subtle film grain highlight
                    let highlight = GraphicsContext.Shading.radialGradient(
                        Gradient(colors: [Color.white.opacity(0.08), .clear]),
                        center: CGPoint(x: size.width * 0.3, y: size.height * 0.2),
                        startRadius: 0,
                        endRadius: size.width * 0.7
                    )

                    for blob in variant.blobs {
                        let phase = t / blob.period * 2 * .pi + blob.phase
                        let dx = cos(phase) * Double(blob.amplitude.width)
                        let dy = sin(phase * blob.ySpeed) * Double(blob.amplitude.height)
                        let scale = 1 + sin(phase * 0.7) * 0.15

                        // Origin is top-left as a % of container (matches CSS).
                        // Scale around the blob's own center so motion feels natural.
                        let baseLeft = size.width * blob.origin.x + CGFloat(dx)
                        let baseTop  = size.height * blob.origin.y + CGFloat(dy)
                        let centerX  = baseLeft + blob.size / 2
                        let centerY  = baseTop  + blob.size / 2
                        let w = blob.size * scale
                        let rect = CGRect(x: centerX - w / 2, y: centerY - w / 2, width: w, height: w)

                        var blobCtx = ctx
                        blobCtx.addFilter(.blur(radius: 48))
                        blobCtx.opacity = 0.85 * intensity
                        blobCtx.fill(
                            Path(ellipseIn: rect),
                            with: .color(blob.color)
                        )
                    }

                    ctx.fill(
                        Path(CGRect(origin: .zero, size: size)),
                        with: highlight
                    )
                }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .clipped()
        .allowsHitTesting(false)
    }
}

struct MeshChip: View {
    var variant: MeshVariant = .warm
    var size: CGFloat = 48
    var radius: CGFloat = 14

    var body: some View {
        MeshGradientView(variant: variant)
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}

#Preview {
    ZStack {
        MeshGradientView(variant: .warm)
    }
    .frame(width: 390, height: 844)
    .background(UNESColor.darkBg)
}
