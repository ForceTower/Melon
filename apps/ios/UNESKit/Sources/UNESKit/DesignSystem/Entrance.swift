import SwiftUI

/// Staggered entrance effects: the view stays hidden until `delay`
/// elapses, then animates to identity — the CSS `animation … both` behavior.
private struct Entrance: ViewModifier {
    var delay: Double
    var duration: Double
    var offsetX: CGFloat = 0
    var offsetY: CGFloat = 0
    var scale: CGFloat = 1
    var overshoot: Double = 1

    @State private var shown = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(x: shown ? 0 : offsetX, y: shown ? 0 : offsetY)
            .scaleEffect(shown ? 1 : scale)
            .onAppear {
                guard !shown else { return }
                if reduceMotion {
                    shown = true
                } else {
                    withAnimation(UNESMotion.ease(duration, overshoot: overshoot).delay(delay)) {
                        shown = true
                    }
                }
            }
    }
}

extension View {
    /// Fade in while rising 16pt.
    func fadeUp(delay: Double = 0, duration: Double = 0.6) -> some View {
        modifier(Entrance(delay: delay, duration: duration, offsetY: 16))
    }

    /// Fade in, in place.
    func fadeIn(delay: Double = 0, duration: Double = 0.6) -> some View {
        modifier(Entrance(delay: delay, duration: duration))
    }

    /// Fade in while sliding 28pt from the right — the schedule v2 entrance.
    func slideIn(delay: Double = 0, duration: Double = 0.55) -> some View {
        modifier(Entrance(delay: delay, duration: duration, offsetX: 28))
    }

    /// Fade in rising 12pt from 98% scale — used by hero cards.
    func scaleIn(delay: Double = 0, duration: Double = 0.55) -> some View {
        modifier(Entrance(delay: delay, duration: duration, offsetY: 12, scale: 0.98))
    }

    /// Scale pop with overshoot — splash icon, checkmarks, accent dots.
    func popIn(
        delay: Double = 0,
        duration: Double = 0.6,
        from scale: CGFloat,
        offsetY: CGFloat = 0,
        overshoot: Double = 1.5
    ) -> some View {
        modifier(Entrance(delay: delay, duration: duration, offsetY: offsetY, scale: scale, overshoot: overshoot))
    }
}
