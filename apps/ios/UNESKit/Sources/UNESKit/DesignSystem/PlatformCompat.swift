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

    /// Forces the large title even when pushed from an inline screen.
    @ViewBuilder
    func largeNavigationBar() -> some View {
        #if os(iOS)
        navigationBarTitleDisplayMode(.large)
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

    /// Hides the tab bar while a pushed flow owns the whole screen
    /// (bottom-docked actions would collide with it).
    @ViewBuilder
    func hiddenTabBar() -> some View {
        #if os(iOS)
        toolbar(.hidden, for: .tabBar)
        #else
        self
        #endif
    }

    /// `presentationCornerRadius` is iOS-only; the package also builds for macOS.
    @ViewBuilder
    func presentationCornerRadiusCompat(_ radius: CGFloat) -> some View {
        #if os(iOS)
        presentationCornerRadius(radius)
        #else
        self
        #endif
    }

    /// Hides the status bar and home indicator for an immersive full-screen
    /// scene. iOS-only; the package also builds for macOS.
    @ViewBuilder
    func immersiveChromeHidden() -> some View {
        #if os(iOS)
        statusBarHidden(true)
            .persistentSystemOverlays(.hidden)
        #else
        self
        #endif
    }

    /// A full-screen modal on iOS; falls back to a sheet on macOS, which has
    /// no `fullScreenCover`.
    @ViewBuilder
    func fullScreenCoverCompat(
        isPresented: Binding<Bool>,
        @ViewBuilder content: @escaping () -> some View
    ) -> some View {
        #if os(iOS)
        fullScreenCover(isPresented: isPresented, content: content)
        #else
        sheet(isPresented: isPresented, content: content)
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

    /// `.segmented` picker style doesn't exist on watchOS.
    @ViewBuilder
    func segmentedPickerCompat() -> some View {
        #if os(watchOS)
        self
        #else
        pickerStyle(.segmented)
        #endif
    }

    /// `textSelection` doesn't exist on watchOS.
    @ViewBuilder
    func textSelectionCompat() -> some View {
        #if os(watchOS)
        self
        #else
        textSelection(.enabled)
        #endif
    }

    /// Decimal-pad keyboard for grade inputs.
    @ViewBuilder
    func decimalKeyboard() -> some View {
        #if os(iOS)
        keyboardType(.decimalPad)
        #else
        self
        #endif
    }

    /// The iOS grouped-list look; `.insetGrouped`/`.grouped` are unavailable
    /// outside iOS/macOS.
    @ViewBuilder
    func insetGroupedListStyle() -> some View {
        #if os(iOS)
        listStyle(.insetGrouped)
        #elseif os(macOS)
        listStyle(.grouped)
        #else
        self
        #endif
    }
}

extension TabContent {
    /// `Tab.badge` doesn't exist on watchOS.
    func badgeCompat(_ count: Int) -> some TabContent<TabValue> {
        #if os(watchOS)
        self
        #else
        badge(count)
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

    /// `.principal` doesn't exist on watchOS.
    static var principalCompat: ToolbarItemPlacement {
        #if os(watchOS)
        .automatic
        #else
        .principal
        #endif
    }
}
