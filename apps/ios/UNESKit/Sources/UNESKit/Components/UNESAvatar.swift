import SwiftUI

/// The user avatar circle: profile picture when one is set, the gradient
/// monogram otherwise. The monogram stays underneath as the loading/error
/// fallback — the photo simply paints over it once it arrives.
struct UNESAvatar: View {
    var name: String?
    var imageUrl: String?
    var size: CGFloat
    var fontSize: CGFloat
    /// Monogram backdrop; defaults to the identity gradient the Eu hero uses.
    var stops: [Gradient.Stop] = [
        .init(color: UNESColor.amber, location: 0),
        .init(color: UNESColor.coral, location: 0.55),
        .init(color: UNESColor.magenta, location: 1),
    ]

    var body: some View {
        Text(initial)
            .font(.system(size: fontSize, weight: .bold))
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(LinearGradient.css(stops: stops, angle: 135), in: Circle())
            .overlay {
                if let url = imageUrl.flatMap(URL.init(string:)) {
                    AsyncImage(url: url) { image in
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: size, height: size)
                            .clipShape(Circle())
                    } placeholder: {
                        EmptyView()
                    }
                }
            }
    }

    private var initial: String {
        name?.first.map { String($0).uppercased() } ?? "•"
    }
}

#Preview {
    VStack(spacing: 16) {
        UNESAvatar(name: "Mariana", size: 62, fontSize: 28)
        UNESAvatar(name: "Mariana", size: 32, fontSize: 14)
        UNESAvatar(name: nil, size: 40, fontSize: 18)
    }
    .padding()
    .background(UNESColor.surface)
}
