import SwiftUI

/// Fade + slide up on appear, with an optional delay.
struct FadeUpOnAppear: ViewModifier {
    let delay: Double
    let distance: CGFloat
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : distance)
            .onAppear {
                withAnimation(.timingCurve(0.2, 0.8, 0.2, 1, duration: 0.7).delay(delay)) {
                    shown = true
                }
            }
    }
}

/// Fade-in only.
struct FadeInOnAppear: ViewModifier {
    let delay: Double
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .onAppear {
                withAnimation(.easeOut(duration: 0.6).delay(delay)) { shown = true }
            }
    }
}

/// Spring scale-in.
struct ScaleInOnAppear: ViewModifier {
    let delay: Double
    @State private var shown = false

    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .scaleEffect(shown ? 1 : 0.92)
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.7).delay(delay)) {
                    shown = true
                }
            }
    }
}

/// Pulsing opacity/scale — used on active dots.
struct PulseAnimation: ViewModifier {
    @State private var pulsing = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(pulsing ? 0.95 : 1)
            .opacity(pulsing ? 0.6 : 1)
            .animation(.easeInOut(duration: 1).repeatForever(autoreverses: true), value: pulsing)
            .onAppear { pulsing = true }
    }
}

extension View {
    func fadeUpOnAppear(delay: Double = 0, distance: CGFloat = 12) -> some View {
        modifier(FadeUpOnAppear(delay: delay, distance: distance))
    }

    func fadeInOnAppear(delay: Double = 0) -> some View {
        modifier(FadeInOnAppear(delay: delay))
    }

    func scaleInOnAppear(delay: Double = 0) -> some View {
        modifier(ScaleInOnAppear(delay: delay))
    }

    func pulseForever() -> some View {
        modifier(PulseAnimation())
    }
}

/// A checkmark circle that draws itself stroke-by-stroke.
struct DrawingCheckmark: View {
    var size: CGFloat = 54
    var strokeColor: Color = UNESColor.surface
    var drawCircle: Bool = true

    @State private var circleProgress: CGFloat = 0
    @State private var checkProgress: CGFloat = 0

    var body: some View {
        ZStack {
            if drawCircle {
                Circle()
                    .trim(from: 0, to: circleProgress)
                    .stroke(strokeColor, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                    .rotationEffect(.degrees(-90))
            }
            CheckmarkShape()
                .trim(from: 0, to: checkProgress)
                .stroke(strokeColor, style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 0.7)) {
                circleProgress = 1
            }
            withAnimation(.timingCurve(0.4, 0, 0.2, 1, duration: 0.4).delay(0.6)) {
                checkProgress = 1
            }
        }
    }
}

private struct CheckmarkShape: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        // Match the 54×54 design: from (17,27) → (24,34) → (38,20), scaled to rect.
        let scaleX = rect.width / 54
        let scaleY = rect.height / 54
        p.move(to: CGPoint(x: 17 * scaleX, y: 27 * scaleY))
        p.addLine(to: CGPoint(x: 24 * scaleX, y: 34 * scaleY))
        p.addLine(to: CGPoint(x: 38 * scaleX, y: 20 * scaleY))
        return p
    }
}
