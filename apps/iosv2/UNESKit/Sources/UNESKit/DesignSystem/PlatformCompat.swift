import SwiftUI

// The package also compiles for macOS so `swift test` runs on the host.
// These shims no-op the iOS-only chrome there.

extension View {
    /// Swipeable pages without the system page dots.
    @ViewBuilder
    func pagedTabViewStyle() -> some View {
        #if os(iOS)
        tabViewStyle(.page(indexDisplayMode: .never))
        #else
        self
        #endif
    }

    /// Transparent, inline navigation bar.
    @ViewBuilder
    func bareNavigationBar() -> some View {
        #if os(iOS)
        toolbarBackground(.hidden, for: .navigationBar)
            .navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    /// Inline navigation bar keeping the system's scroll-edge glass.
    @ViewBuilder
    func inlineNavigationBar() -> some View {
        #if os(iOS)
        navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    /// Fully hidden navigation bar (root screens with their own chrome).
    @ViewBuilder
    func hiddenNavigationBar() -> some View {
        #if os(iOS)
        toolbar(.hidden, for: .navigationBar)
        #else
        self
        #endif
    }

    @ViewBuilder
    func noAutocapitalization() -> some View {
        #if os(iOS)
        textInputAutocapitalization(.never)
        #else
        self
        #endif
    }
}

extension ToolbarItemPlacement {
    static var trailingCompat: ToolbarItemPlacement {
        #if os(iOS)
        .topBarTrailing
        #else
        .automatic
        #endif
    }
}
