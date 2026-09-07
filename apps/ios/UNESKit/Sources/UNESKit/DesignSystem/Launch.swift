import SwiftUI

/// The tile the system launch screen shows: Info.plist `UILaunchScreen`
/// centres the app target's `LaunchIcon` imageset on the `LaunchBackground`
/// ground. In-app launch surfaces draw the same imageset at the same point
/// size on `UNESColor.darkBg`, so their first frame matches the static launch
/// image and the handoff from the system snapshot to live SwiftUI is invisible.
///
/// The imageset lives in the app target's catalog because the launch screen
/// can only read the main bundle, so package previews render the tile empty.
struct LaunchTile: View {
    /// The imageset's native point size — what the launch screen draws it at.
    static let size: CGFloat = 76

    /// 0 on the launch frame, where the flat ground has nothing to cast a
    /// shadow onto; raised together with the mesh blooming in behind.
    var shadowOpacity: Double = 0

    var body: some View {
        Image("LaunchIcon")
            .frame(width: Self.size, height: Self.size)
            .shadow(color: .black.opacity(0.4 * shadowOpacity), radius: 17, y: 12)
    }
}

/// What blooms in behind the tile once the app is live: the warm mesh under
/// the splash scrim. Layered over `UNESColor.darkBg`, which the launch
/// screen's `LaunchBackground` color mirrors.
struct LaunchBackdrop: View {
    var body: some View {
        ZStack {
            MeshView(variant: .warm)
            LinearGradient.css(
                stops: [
                    .init(color: UNESColor.scrim.opacity(0.2), location: 0),
                    .init(color: UNESColor.scrim.opacity(0.55), location: 1),
                ],
                angle: 160
            )
        }
    }
}

#Preview {
    ZStack {
        UNESColor.darkBg
        LaunchBackdrop()
        LaunchTile(shadowOpacity: 1)
    }
    .ignoresSafeArea()
}
