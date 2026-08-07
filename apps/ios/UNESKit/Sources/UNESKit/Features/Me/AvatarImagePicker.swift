#if os(iOS)
import SwiftUI
import UIKit

/// The classic system picker with `allowsEditing`, chosen for its native
/// square "Mover e Redimensionar" step — the avatar needs a crop and neither
/// `PhotosPicker` nor `PHPickerViewController` offers one. The deprecation on
/// the `.photoLibrary` source is accepted for that trade.
struct AvatarImagePicker: UIViewControllerRepresentable {
    var source: ProfileEditFeature.PickerSource
    var onPicked: (Data) -> Void
    var onCancel: () -> Void

    static var isCameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = source == .camera ? .camera : .photoLibrary
        if source == .camera, UIImagePickerController.isCameraDeviceAvailable(.front) {
            picker.cameraDevice = .front
        }
        picker.allowsEditing = true
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ controller: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onPicked: onPicked, onCancel: onCancel)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let onPicked: (Data) -> Void
        private let onCancel: () -> Void

        init(onPicked: @escaping (Data) -> Void, onCancel: @escaping () -> Void) {
            self.onPicked = onPicked
            self.onCancel = onCancel
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            let image = (info[.editedImage] ?? info[.originalImage]) as? UIImage
            guard let jpeg = image.flatMap(Self.avatarJPEG(from:)) else {
                onCancel()
                return
            }
            onPicked(jpeg)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCancel()
        }

        /// Downscales the crop to at most 512pt per side and JPEG-encodes it —
        /// far below the API's 5 MB cap and plenty for avatar rendering.
        private static func avatarJPEG(from image: UIImage) -> Data? {
            let maxSide: CGFloat = 512
            let scale = min(1, maxSide / max(image.size.width, image.size.height))
            let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
            let format = UIGraphicsImageRendererFormat.default()
            format.scale = 1
            let scaled = UIGraphicsImageRenderer(size: size, format: format).image { _ in
                image.draw(in: CGRect(origin: .zero, size: size))
            }
            return scaled.jpegData(compressionQuality: 0.85)
        }
    }
}
#endif
