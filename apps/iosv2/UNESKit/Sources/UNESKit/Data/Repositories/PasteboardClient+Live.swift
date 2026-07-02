import ComposableArchitecture

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

extension PasteboardClient: DependencyKey {
    static let liveValue = PasteboardClient(
        copy: { text in
            await MainActor.run {
                #if canImport(UIKit)
                UIPasteboard.general.string = text
                #else
                NSPasteboard.general.clearContents()
                NSPasteboard.general.setString(text, forType: .string)
                #endif
            }
        }
    )
}
