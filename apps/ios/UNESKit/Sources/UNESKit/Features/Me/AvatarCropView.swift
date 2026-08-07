#if os(iOS)
import SwiftUI
import UIKit

/// Fullscreen circular crop — dc `EuScreen` "Ajustar foto" step, the same
/// math as Android's `ProfilePhotoCrop`. Pinch/drag position the photo under
/// a fixed circle mask, the slider mirrors the pinch zoom, and Concluir bakes
/// the circle's content into a square JPEG handed back to the reducer.
struct AvatarCropView: View {
    let sourceData: Data
    var onCancel: () -> Void
    var onDone: (Data) -> Void

    @State private var image: UIImage?
    @State private var zoom: CGFloat = Self.initialZoom
    @State private var offset: CGSize = .zero
    @State private var stageSize: CGSize = .zero
    @State private var dragStart: CGSize?
    @State private var pinchStart: (zoom: CGFloat, offset: CGSize)?

    private static let minZoom: CGFloat = 1
    private static let maxZoom: CGFloat = 2.6
    private static let initialZoom: CGFloat = 1.24
    private static let circleFraction: CGFloat = 0.72
    private static let outputSide: CGFloat = 640
    private static let backdrop = Color(hex: 0x08060C)

    var body: some View {
        VStack(spacing: 0) {
            Text(.meCropTitle)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white)
                .padding(.top, 18)

            stage

            controls
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Self.backdrop.ignoresSafeArea())
        .task {
            // Force-decoded up front so the first gesture doesn't pay the
            // JPEG decode. An unreadable pick (corrupt file) just backs out.
            let decoded = UIImage(data: sourceData)
            image = await decoded?.byPreparingForDisplay() ?? decoded
            if image == nil { onCancel() }
        }
    }

    // MARK: Stage

    /// The mask's diameter in px and the zoom-1 "cover" scale (photo's
    /// shorter side spans the circle) — shared by the gestures and the export.
    private var circle: CGFloat {
        min(stageSize.width, stageSize.height) * Self.circleFraction
    }

    private var stage: some View {
        ZStack {
            if let image {
                // Laid out once at the zoom-1 "cover" size; zoom and pan are
                // pure transforms so gestures never re-layout or re-rasterize
                // the bitmap.
                let base = baseScale(for: image)
                Image(uiImage: image)
                    .resizable()
                    .frame(width: image.size.width * base, height: image.size.height * base)
                    .scaleEffect(zoom)
                    .offset(offset)
            }
            scrim
            VStack {
                Spacer()
                Text(.meCropHint)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.6))
                    .padding(.bottom, 18)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .contentShape(Rectangle())
        .onGeometryChange(for: CGSize.self) { proxy in
            proxy.size
        } action: { size in
            stageSize = size
            offset = clampedOffset(offset, atZoom: zoom)
        }
        .gesture(dragGesture.simultaneously(with: pinchGesture))
    }

    /// Veil with the circle punched out, plus the hairline ring.
    private var scrim: some View {
        ZStack {
            Rectangle()
                .fill(Self.backdrop.opacity(0.74))
                .overlay {
                    Circle()
                        .frame(width: circle, height: circle)
                        .blendMode(.destinationOut)
                }
                .compositingGroup()
            Circle()
                .strokeBorder(.white.opacity(0.9), lineWidth: 1.5)
                .frame(width: circle, height: circle)
        }
        .allowsHitTesting(false)
    }

    // MARK: Gestures

    private var dragGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                let start = dragStart ?? offset
                dragStart = start
                offset = clampedOffset(
                    CGSize(width: start.width + value.translation.width,
                           height: start.height + value.translation.height),
                    atZoom: zoom
                )
            }
            .onEnded { _ in dragStart = nil }
    }

    private var pinchGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                let start = pinchStart ?? (zoom, offset)
                pinchStart = start
                let newZoom = (start.zoom * value).clamped(to: Self.minZoom...Self.maxZoom)
                // Keep the on-screen anchor put: offsets scale with zoom.
                let scaled = CGSize(
                    width: start.offset.width * (newZoom / start.zoom),
                    height: start.offset.height * (newZoom / start.zoom)
                )
                zoom = newZoom
                offset = clampedOffset(scaled, atZoom: newZoom)
            }
            .onEnded { _ in pinchStart = nil }
    }

    private var zoomBinding: Binding<CGFloat> {
        Binding(
            get: { zoom },
            set: { value in
                let scaled = zoom > 0
                    ? CGSize(width: offset.width * (value / zoom), height: offset.height * (value / zoom))
                    : offset
                zoom = value
                offset = clampedOffset(scaled, atZoom: value)
            }
        )
    }

    private func baseScale(for image: UIImage) -> CGFloat {
        guard circle > 0 else { return 0 }
        return circle / min(image.size.width, image.size.height)
    }

    /// Never lets the circle see past the photo's edges.
    private func clampedOffset(_ candidate: CGSize, atZoom: CGFloat) -> CGSize {
        guard let image else { return candidate }
        let scale = baseScale(for: image) * atZoom
        let maxX = max(0, (image.size.width * scale - circle) / 2)
        let maxY = max(0, (image.size.height * scale - circle) / 2)
        return CGSize(
            width: candidate.width.clamped(to: -maxX...maxX),
            height: candidate.height.clamped(to: -maxY...maxY)
        )
    }

    // MARK: Controls

    private var controls: some View {
        VStack(spacing: 16) {
            HStack(spacing: 14) {
                Image(systemName: "photo")
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.5))
                Slider(value: zoomBinding, in: Self.minZoom...Self.maxZoom)
                    .tint(.accentColor)
                Image(systemName: "photo")
                    .font(.system(size: 19))
                    .foregroundStyle(.white.opacity(0.5))
            }
            HStack {
                Button {
                    onCancel()
                } label: {
                    Text(.meEditCancel)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(.white.opacity(0.8))
                }
                Spacer()
                Button {
                    if let data = exportCrop() {
                        onDone(data)
                    } else {
                        onCancel()
                    }
                } label: {
                    Text(.meCropDone)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(EdgeInsets(top: 12, leading: 26, bottom: 12, trailing: 26))
                        .background(Color.accentColor, in: Capsule())
                }
            }
        }
        .padding(EdgeInsets(top: 12, leading: 22, bottom: 16, trailing: 22))
    }

    // MARK: Export

    /// The circle's content baked into a square JPEG. The circle's center
    /// sits at `imageCenter - offset/scale` in image coordinates; its
    /// diameter covers `circle/scale` source points — same recipe as
    /// Android's `exportCrop`.
    private func exportCrop() -> Data? {
        guard let image, circle > 0 else { return nil }
        let scale = baseScale(for: image) * zoom
        let srcHalf = (circle / scale) / 2
        let cx = image.size.width / 2 - offset.width / scale
        let cy = image.size.height / 2 - offset.height / scale
        let k = Self.outputSide / (srcHalf * 2)

        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        let out = UIGraphicsImageRenderer(
            size: CGSize(width: Self.outputSide, height: Self.outputSide),
            format: format
        ).image { _ in
            image.draw(in: CGRect(
                x: -(cx - srcHalf) * k,
                y: -(cy - srcHalf) * k,
                width: image.size.width * k,
                height: image.size.height * k
            ))
        }
        return out.jpegData(compressionQuality: 0.88)
    }
}

extension Comparable {
    fileprivate func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

#Preview {
    AvatarCropView(
        sourceData: UIGraphicsImageRenderer(size: CGSize(width: 900, height: 600)).image { context in
            UIColor.systemTeal.setFill()
            context.fill(CGRect(x: 0, y: 0, width: 900, height: 600))
            UIColor.systemPink.setFill()
            context.fill(CGRect(x: 300, y: 150, width: 300, height: 300))
        }.jpegData(compressionQuality: 0.9) ?? Data(),
        onCancel: {},
        onDone: { _ in }
    )
}
#endif
