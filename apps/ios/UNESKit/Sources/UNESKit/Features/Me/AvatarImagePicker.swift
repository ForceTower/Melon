#if os(iOS)
import SwiftUI
import UIKit

/// Camera capture for the avatar — plain shot, no system editing: the crop
/// happens in `AvatarCropView`, which the reducer presents with the returned
/// bytes. (`allowsEditing` was tried first and silently returned no
/// `editedImage` on device, so the uncropped original went up — never again.)
struct AvatarCameraPicker: UIViewControllerRepresentable {
    var onCaptured: (Data) -> Void
    var onCancel: () -> Void

    static var isCameraAvailable: Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.sourceType = .camera
        if UIImagePickerController.isCameraDeviceAvailable(.front) {
            picker.cameraDevice = .front
        }
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ controller: UIImagePickerController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onCaptured: onCaptured, onCancel: onCancel)
    }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        private let onCaptured: (Data) -> Void
        private let onCancel: () -> Void

        init(onCaptured: @escaping (Data) -> Void, onCancel: @escaping () -> Void) {
            self.onCaptured = onCaptured
            self.onCancel = onCancel
        }

        func imagePickerController(
            _ picker: UIImagePickerController,
            didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
        ) {
            guard let image = info[.originalImage] as? UIImage,
                  let data = Self.cropSourceJPEG(from: image) else {
                onCancel()
                return
            }
            onCaptured(data)
        }

        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            onCancel()
        }

        /// Downscales the shot to at most 2048pt per side — plenty of
        /// headroom for the crop's zoom while keeping gesture math cheap.
        private static func cropSourceJPEG(from image: UIImage) -> Data? {
            let maxSide: CGFloat = 2048
            let scale = min(1, maxSide / max(image.size.width, image.size.height))
            guard scale < 1 else { return image.jpegData(compressionQuality: 0.92) }
            let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
            let format = UIGraphicsImageRendererFormat.default()
            format.scale = 1
            let scaled = UIGraphicsImageRenderer(size: size, format: format).image { _ in
                image.draw(in: CGRect(origin: .zero, size: size))
            }
            return scaled.jpegData(compressionQuality: 0.92)
        }
    }
}
#endif
