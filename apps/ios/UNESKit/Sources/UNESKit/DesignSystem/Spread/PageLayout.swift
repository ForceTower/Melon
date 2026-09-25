import SwiftUI

enum PageLayout: Equatable, Sendable {
    case stack
    case spread

    static let spreadMinWidth: CGFloat = 700

    /// Regular width alone isn't enough: an open iPhone Duo held upright is
    /// regular but only 669pt across, too narrow for two pages.
    static func resolve(width: CGFloat, sizeClass: UserInterfaceSizeClass?) -> PageLayout {
        sizeClass == .regular && width >= spreadMinWidth ? .spread : .stack
    }
}

extension EnvironmentValues {
    @Entry var pageLayout = PageLayout.stack
}

extension View {
    func resolvesPageLayout() -> some View {
        modifier(PageLayoutResolver())
    }
}

private struct PageLayoutResolver: ViewModifier {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var width: CGFloat = 0

    func body(content: Content) -> some View {
        content
            .environment(\.pageLayout, PageLayout.resolve(width: width, sizeClass: sizeClass))
            .onGeometryChange(for: CGFloat.self) { proxy in
                proxy.size.width
            } action: { newWidth in
                width = newWidth
            }
    }
}
