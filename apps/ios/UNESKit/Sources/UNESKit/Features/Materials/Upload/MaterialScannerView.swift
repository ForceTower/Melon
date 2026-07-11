#if os(iOS)
import SwiftUI
import UIKit
import VisionKit

/// The system document scanner, flattening the captured pages into one PDF.
struct MaterialScannerView: UIViewControllerRepresentable {
    var onComplete: (MaterialPickedFile) -> Void
    var onCancel: () -> Void
    var onFail: () -> Void

    static var isSupported: Bool {
        VNDocumentCameraViewController.isSupported
    }

    func makeUIViewController(context: Context) -> VNDocumentCameraViewController {
        let controller = VNDocumentCameraViewController()
        controller.delegate = context.coordinator
        return controller
    }

    func updateUIViewController(_ controller: VNDocumentCameraViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(onComplete: onComplete, onCancel: onCancel, onFail: onFail)
    }

    final class Coordinator: NSObject, VNDocumentCameraViewControllerDelegate {
        private let onComplete: (MaterialPickedFile) -> Void
        private let onCancel: () -> Void
        private let onFail: () -> Void

        init(
            onComplete: @escaping (MaterialPickedFile) -> Void,
            onCancel: @escaping () -> Void,
            onFail: @escaping () -> Void
        ) {
            self.onComplete = onComplete
            self.onCancel = onCancel
            self.onFail = onFail
        }

        func documentCameraViewController(
            _ controller: VNDocumentCameraViewController,
            didFinishWith scan: VNDocumentCameraScan
        ) {
            guard scan.pageCount > 0 else {
                onCancel()
                return
            }
            let pages = (0..<scan.pageCount).map { scan.imageOfPage(at: $0) }
            let data = Self.pdf(from: pages)
            onComplete(MaterialPickedFile(
                fileName: "digitalizacao.pdf",
                byteCount: data.count,
                pages: pages.count,
                data: data,
                isScan: true
            ))
        }

        func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) {
            onCancel()
        }

        func documentCameraViewController(
            _ controller: VNDocumentCameraViewController,
            didFailWithError error: Error
        ) {
            Log.scoped("MaterialScannerView").warn("scan failed", error: error)
            onFail()
        }

        /// One PDF page per captured sheet, at the capture's pixel size.
        private static func pdf(from pages: [UIImage]) -> Data {
            let bounds = CGRect(origin: .zero, size: pages[0].size)
            let renderer = UIGraphicsPDFRenderer(bounds: bounds)
            return renderer.pdfData { context in
                for page in pages {
                    context.beginPage(withBounds: CGRect(origin: .zero, size: page.size), pageInfo: [:])
                    page.draw(at: .zero)
                }
            }
        }
    }
}
#endif
