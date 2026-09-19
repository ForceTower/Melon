import SwiftUI

/// `frame` is in global coordinates.
struct DeviceFold: Equatable, Sendable {
    enum Axis: Sendable {
        /// Runs top to bottom, between a leading and a trailing page.
        case spine
        /// Runs side to side, between an upright half and one lying flat.
        case waist
    }

    var frame: CGRect
    var axis: Axis
    /// The system only reserves the fold while the hinge is bent; flat, it
    /// still reports where the fold is.
    var isBent: Bool

    var spineX: CGFloat? { axis == .spine ? frame.midX : nil }

    static let minPage: CGFloat = 300

    /// A fold only counts when it crosses the whole scene with a page's worth
    /// of room on both sides — not one at the edge of a Split View half.
    static func make(frame: CGRect, isBent: Bool, in size: CGSize) -> DeviceFold? {
        if frame.height >= size.height * 0.9, frame.height > frame.width {
            guard frame.midX >= minPage, size.width - frame.midX >= minPage else { return nil }
            return DeviceFold(frame: frame, axis: .spine, isBent: isBent)
        }
        if frame.width >= size.width * 0.9, frame.width > frame.height {
            guard frame.midY >= minPage, size.height - frame.midY >= minPage else { return nil }
            return DeviceFold(frame: frame, axis: .waist, isBent: isBent)
        }
        return nil
    }
}

extension EnvironmentValues {
    @Entry var deviceFold: DeviceFold?
}

extension View {
    /// The only place that names the fold API; everything else reads `deviceFold`.
    @ViewBuilder
    func readsDeviceFold() -> some View {
        #if os(iOS)
        if #available(iOS 27.1, *) {
            modifier(DeviceFoldReader())
        } else {
            self
        }
        #else
        self
        #endif
    }
}

#if os(iOS)
@available(iOS 27.1, *)
private struct DeviceFoldReader: ViewModifier {
    @State private var fold: DeviceFold?

    func body(content: Content) -> some View {
        content
            .environment(\.deviceFold, fold)
            .onGeometryChange(for: DeviceFold?.self) { proxy in
                guard let region = proxy.reservedRegions(kind: .division, options: [.includeInactive]).first,
                      var fold = DeviceFold.make(frame: region.frame, isBent: region.isActive, in: proxy.size)
                else { return nil }
                // Regions come in this view's space, which starts below the
                // safe area; readers compare against global frames.
                let origin = proxy.frame(in: .global).origin
                fold.frame = fold.frame.offsetBy(dx: origin.x, dy: origin.y)
                return fold
            } action: { newFold in
                fold = newFold
            }
    }
}
#endif
