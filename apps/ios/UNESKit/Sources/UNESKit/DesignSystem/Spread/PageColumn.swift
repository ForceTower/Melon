import SwiftUI

extension View {
    /// Keeps a single-column screen one page wide on a spread: on the leading
    /// page beside a spine fold, centered where there is none.
    func pageColumn(maxWidth: CGFloat = 460) -> some View {
        modifier(PageColumn(maxWidth: maxWidth))
    }
}

private struct PageColumn: ViewModifier {
    @Environment(\.pageLayout) private var layout
    @Environment(\.deviceFold) private var fold

    let maxWidth: CGFloat

    func body(content: Content) -> some View {
        let spine = fold?.spineX
        content
            .frame(maxWidth: layout == .spread ? spine ?? maxWidth : .infinity)
            .frame(maxWidth: .infinity, alignment: spine == nil ? .center : .leading)
    }
}
