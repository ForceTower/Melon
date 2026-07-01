import SwiftUI

/// The living gradient mesh behind hero surfaces: heavily blurred color
/// blobs drifting on slow eased loops, with a faint radial highlight on top.
struct MeshView: View {
    enum Variant {
        /// Deep plum → coral → amber (hero / splash).
        case warm
        /// Schedule — teal/blue.
        case cool
        /// Grades — amber/gold.
        case sun
        /// Messages — magenta/plum.
        case rose
        /// Success — green/teal.
        case fresh
    }

    var variant: Variant
    var intensity: Double = 1

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1 / 30, paused: reduceMotion)) { timeline in
            let time = timeline.date.timeIntervalSinceReferenceDate
            Canvas { context, size in
                for blob in variant.blobs {
                    let drift = reduceMotion ? MeshDrift() : blob.drift(at: time)
                    let center = CGPoint(
                        x: blob.x * size.width + blob.size / 2 + drift.offset.width,
                        y: blob.y * size.height + blob.size / 2 + drift.offset.height
                    )
                    fillBlob(blob, at: center, scale: drift.scale, in: &context)
                }
                // Film grain highlight for warmth.
                context.blendMode = .overlay
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .radialGradient(
                        Gradient(colors: [.white.opacity(0.08), .clear]),
                        center: CGPoint(x: size.width * 0.3, y: size.height * 0.2),
                        startRadius: 0,
                        endRadius: max(size.width, size.height) * 0.6
                    )
                )
            }
        }
        .allowsHitTesting(false)
    }

    /// A soft blob as a radial gradient approximating a solid circle under a
    /// 48pt gaussian blur — a per-frame blur filter is far too expensive here.
    private func fillBlob(_ blob: MeshBlob, at center: CGPoint, scale: CGFloat, in context: inout GraphicsContext) {
        let sigma: CGFloat = 48
        let radius = blob.size / 2 * scale
        let outer = radius + 2 * sigma
        let base = blob.color.opacity(0.85 * intensity)

        let rect = CGRect(x: center.x - outer, y: center.y - outer, width: outer * 2, height: outer * 2)
        context.fill(
            Ellipse().path(in: rect),
            with: .radialGradient(
                Gradient(stops: [
                    .init(color: base, location: 0),
                    .init(color: base, location: max(0, (radius - sigma) / outer)),
                    .init(color: base.opacity(0.5), location: radius / outer),
                    .init(color: base.opacity(0.16), location: (radius + sigma) / outer),
                    .init(color: base.opacity(0), location: 1),
                ]),
                center: center,
                startRadius: 0,
                endRadius: outer
            )
        )
    }
}

private struct MeshDrift {
    var offset: CGSize = .zero
    var scale: CGFloat = 1
}

private struct MeshBlob {
    var color: Color
    /// Resting top-left corner as a fraction of the container size.
    var x: CGFloat
    var y: CGFloat
    /// Fixed diameter in points, independent of container size (clipped by it).
    var size: CGFloat
    var path: MeshDriftPath
    var duration: Double
    var reversed = false

    func drift(at time: TimeInterval) -> MeshDrift {
        var phase = (time / duration).truncatingRemainder(dividingBy: 1)
        if reversed { phase = 1 - phase }
        return path.drift(at: phase)
    }
}

/// The three drift loops. Each keyframe segment eases in-out, like the CSS originals.
private enum MeshDriftPath {
    case a, b, c

    private var keyframes: [(time: Double, drift: MeshDrift)] {
        switch self {
        case .a:
            [
                (0, MeshDrift()),
                (0.33, MeshDrift(offset: CGSize(width: 40, height: -30), scale: 1.15)),
                (0.66, MeshDrift(offset: CGSize(width: -25, height: 35), scale: 0.92)),
                (1, MeshDrift()),
            ]
        case .b:
            [
                (0, MeshDrift()),
                (0.5, MeshDrift(offset: CGSize(width: -45, height: 25), scale: 1.2)),
                (1, MeshDrift()),
            ]
        case .c:
            [
                (0, MeshDrift()),
                (0.4, MeshDrift(offset: CGSize(width: 30, height: 40), scale: 0.88)),
                (0.8, MeshDrift(offset: CGSize(width: -35, height: -25), scale: 1.1)),
                (1, MeshDrift()),
            ]
        }
    }

    func drift(at phase: Double) -> MeshDrift {
        let frames = keyframes
        guard let next = frames.firstIndex(where: { $0.time >= phase }), next > 0 else {
            return frames.first!.drift
        }
        let lo = frames[next - 1]
        let hi = frames[next]
        let u = (phase - lo.time) / (hi.time - lo.time)
        let eased = u * u * (3 - 2 * u)
        return MeshDrift(
            offset: CGSize(
                width: lo.drift.offset.width + (hi.drift.offset.width - lo.drift.offset.width) * eased,
                height: lo.drift.offset.height + (hi.drift.offset.height - lo.drift.offset.height) * eased
            ),
            scale: lo.drift.scale + (hi.drift.scale - lo.drift.scale) * eased
        )
    }
}

extension MeshView.Variant {
    fileprivate var blobs: [MeshBlob] {
        switch self {
        case .warm:
            [
                MeshBlob(color: UNESColor.plum, x: -0.15, y: -0.10, size: 340, path: .a, duration: 14),
                MeshBlob(color: UNESColor.coral, x: 0.50, y: 0.30, size: 300, path: .b, duration: 11),
                MeshBlob(color: UNESColor.amber, x: -0.20, y: 0.65, size: 280, path: .c, duration: 17),
                MeshBlob(color: UNESColor.magenta, x: 0.55, y: 0.60, size: 240, path: .a, duration: 13, reversed: true),
            ]
        case .cool:
            [
                MeshBlob(color: Color(hex: 0x1E3A5F), x: -0.10, y: -0.10, size: 320, path: .a, duration: 13),
                MeshBlob(color: Color(hex: 0x3B9EAE), x: 0.55, y: 0.40, size: 280, path: .b, duration: 15),
                MeshBlob(color: Color(hex: 0x88D4C1), x: -0.15, y: 0.60, size: 260, path: .c, duration: 12),
            ]
        case .sun:
            [
                MeshBlob(color: Color(hex: 0xC94538), x: -0.10, y: -0.15, size: 320, path: .a, duration: 15),
                MeshBlob(color: UNESColor.amber, x: 0.50, y: 0.35, size: 300, path: .b, duration: 12),
                MeshBlob(color: UNESColor.peach, x: -0.05, y: 0.55, size: 280, path: .c, duration: 14),
            ]
        case .rose:
            [
                MeshBlob(color: Color(hex: 0x3D1B3E), x: -0.10, y: -0.10, size: 300, path: .a, duration: 14),
                MeshBlob(color: UNESColor.magenta, x: 0.50, y: 0.30, size: 290, path: .b, duration: 13),
                MeshBlob(color: UNESColor.coral, x: -0.15, y: 0.65, size: 260, path: .c, duration: 16),
            ]
        case .fresh:
            [
                MeshBlob(color: Color(hex: 0x0F4D3A), x: -0.10, y: -0.10, size: 320, path: .a, duration: 13),
                MeshBlob(color: Color(hex: 0x4AA679), x: 0.50, y: 0.35, size: 290, path: .b, duration: 14),
                MeshBlob(color: UNESColor.amber, x: -0.10, y: 0.65, size: 240, path: .c, duration: 15),
            ]
        }
    }
}

#Preview {
    VStack(spacing: 0) {
        MeshView(variant: .warm)
        MeshView(variant: .cool)
        MeshView(variant: .fresh)
    }
    .ignoresSafeArea()
}
