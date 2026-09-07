import SwiftUI

/// Signed-out cold start. Frame zero is the system launch image — flat
/// `darkBg` ground with `LaunchTile` at the screen centre — and everything
/// grows out of it: the mesh blooms in, the tile pulses and lifts, and the
/// wordmark and footer rise beneath it before the flow crossfades to Welcome.
struct SplashView: View {
    @State private var awake = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// About half the wordmark block, so the lifted group ends up centred.
    private let lift: CGFloat = 52

    var body: some View {
        ZStack {
            UNESColor.darkBg
            LaunchBackdrop()
                .opacity(awake ? 1 : 0)

            VStack(spacing: 20) {
                tile
                wordmark
                    .fadeUp(delay: 0.25, duration: 0.7)
            }
            // The ZStack centres this guide, so frame zero puts the tile's
            // centre — not the group's — at the screen centre, where the
            // launch image sits; the wordmark then hangs below it.
            .alignmentGuide(VerticalAlignment.center) { d in d[.top] + LaunchTile.size / 2 }
            .offset(y: awake && !reduceMotion ? -lift : 0)

            VStack {
                Spacer()
                footer
                    .fadeUp(delay: 0.9, duration: 0.6)
                    .padding(.bottom, 46)
            }
        }
        .ignoresSafeArea()
        .animation(reduceMotion ? nil : UNESMotion.ease(0.9), value: awake)
        .onAppear { awake = true }
    }

    /// Settles with a single pulse as the mesh blooms behind it.
    private var tile: some View {
        LaunchTile(shadowOpacity: awake ? 1 : 0)
            .keyframeAnimator(initialValue: CGFloat(1), trigger: awake && !reduceMotion) { content, scale in
                content.scaleEffect(scale)
            } keyframes: { _ in
                CubicKeyframe(1.08, duration: 0.3)
                CubicKeyframe(1, duration: 0.5)
            }
    }

    private var wordmark: some View {
        VStack(spacing: 14) {
            HStack(alignment: .bottom, spacing: 3) {
                Text(verbatim: "unes")
                    .font(.system(size: 46, weight: .heavy))
                    .tracking(-2.3)
                    .foregroundStyle(UNESColor.paper)
                Circle()
                    .fill(UNESColor.accent)
                    .frame(width: 9, height: 9)
                    .padding(.bottom, 6)
                    .popIn(delay: 0.7, duration: 0.5, from: 0, overshoot: 1.6)
            }
            Text(.onboardingSplashTagline)
                .font(.system(size: 12.5, weight: .semibold))
                .tracking(0.25)
                .foregroundStyle(UNESColor.paper.opacity(0.6))
        }
    }

    private var footer: some View {
        VStack(spacing: 16) {
            SpinnerRing(
                size: 26,
                color: UNESColor.paper.opacity(0.75),
                trackColor: UNESColor.paper.opacity(0.2)
            )
            (
                Text(.onboardingSplashForPrefix).foregroundStyle(UNESColor.paper.opacity(0.5))
                    + Text(verbatim: "UEFS").fontWeight(.semibold).foregroundStyle(UNESColor.paper.opacity(0.82))
                    + Text(verbatim: " · Feira de Santana").foregroundStyle(UNESColor.paper.opacity(0.5))
            )
            .font(.system(size: 13, weight: .medium))
        }
    }
}

#Preview {
    SplashView()
}
