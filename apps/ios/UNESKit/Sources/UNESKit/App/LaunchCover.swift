import SwiftUI

/// Signed-in cold start: holds the system launch image over the tab shell
/// while it mounts, then dissolves into it. The mesh blooms in behind the
/// tile, which lifts slightly as the cover fades — the same exit the Android
/// splash plays. `onFinished` removes the cover from the hierarchy so the
/// mesh stops drawing once it is gone.
struct LaunchCover: View {
    var onFinished: () -> Void

    @State private var awake = false
    @State private var dissolved = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ZStack {
            UNESColor.darkBg
            LaunchBackdrop()
                .opacity(awake ? 1 : 0)
            LaunchTile(shadowOpacity: awake ? 1 : 0)
                .scaleEffect(dissolved && !reduceMotion ? 1.06 : 1)
        }
        .ignoresSafeArea()
        .opacity(dissolved ? 0 : 1)
        .allowsHitTesting(false)
        // Chained on the bloom's completion rather than a clock: the clock
        // would start before the first frame is on screen, and a slow cold
        // start would then eat into the bloom.
        .onAppear {
            let bloom: Animation = reduceMotion ? .linear(duration: 0.3) : UNESMotion.ease(0.45)
            withAnimation(bloom) { awake = true } completion: {
                withAnimation(.easeInOut(duration: 0.4)) { dissolved = true } completion: { onFinished() }
            }
        }
    }
}

#Preview {
    LaunchCover {}
}
