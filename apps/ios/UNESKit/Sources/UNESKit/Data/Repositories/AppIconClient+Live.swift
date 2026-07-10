import ComposableArchitecture

#if canImport(UIKit) && !os(watchOS)
import UIKit
#endif

private let log = Log.scoped("AppIconClient")

extension AppIconClient: DependencyKey {
    static let liveValue = AppIconClient(
        current: {
            #if canImport(UIKit) && !os(watchOS)
            await MainActor.run { AppIcon(alternateIconName: UIApplication.shared.alternateIconName) }
            #else
            .aurora
            #endif
        },
        set: { icon in
            #if canImport(UIKit) && !os(watchOS)
            try await applyAlternateIcon(icon.alternateIconName)
            log.info("app icon set to \(icon.rawValue)")
            #else
            _ = icon
            #endif
        }
    )
}

#if canImport(UIKit) && !os(watchOS)
/// The `String? → NSString` bridge into the main-actor UIKit call must happen
/// inside main-actor isolation, or strict concurrency flags it as a send.
@MainActor
private func applyAlternateIcon(_ name: String?) async throws {
    guard UIApplication.shared.supportsAlternateIcons else { return }
    try await UIApplication.shared.setAlternateIconName(name)
}
#endif
