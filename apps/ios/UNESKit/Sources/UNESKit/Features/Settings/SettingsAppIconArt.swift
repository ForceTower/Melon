import SwiftUI

/// A rounded miniature of one app icon, rendered from its `icon.*` imageset
/// (compiled `.icon` bundles can't be loaded as images at runtime).
struct SettingsAppIconArt: View {
    var icon: AppIcon
    var size: CGFloat

    var body: some View {
        Image(icon.assetName)
            .resizable()
            .scaledToFill()
            .frame(width: size, height: size)
            .clipShape(shape)
            .overlay {
                shape.strokeBorder(.black.opacity(0.06), lineWidth: 0.5)
            }
    }

    private var shape: RoundedRectangle {
        RoundedRectangle(cornerRadius: size * 0.25, style: .continuous)
    }
}

#Preview {
    HStack(spacing: 12) {
        ForEach(AppIcon.allCases) { icon in
            SettingsAppIconArt(icon: icon, size: 60)
        }
    }
    .padding(24)
    .background(UNESColor.surface)
}
