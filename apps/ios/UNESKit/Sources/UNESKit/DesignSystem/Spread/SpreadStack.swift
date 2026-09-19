import ComposableArchitecture
import SwiftUI

/// A tab's navigation shell: a plain stack, or on a `.spread` layout a split
/// view where `root` stays on the leading page while the trailing page starts
/// on `overview` and takes every push.
struct SpreadStack<State: ObservableState, Action, Root: View, Overview: View, Destination: View>: View {
    @Environment(\.pageLayout) private var layout

    @Binding private var path: Store<StackState<State>, StackAction<State, Action>>
    private let root: Root
    private let overview: Overview
    private let destination: (Store<State, Action>) -> Destination

    init(
        path: Binding<Store<StackState<State>, StackAction<State, Action>>>,
        @ViewBuilder root: () -> Root,
        @ViewBuilder overview: () -> Overview,
        @ViewBuilder destination: @escaping (Store<State, Action>) -> Destination
    ) {
        _path = path
        self.root = root()
        self.overview = overview()
        self.destination = destination
    }

    var body: some View {
        switch layout {
        case .stack:
            NavigationStack(path: $path) {
                root
            } destination: { store in
                page(store)
            }
        case .spread:
            NavigationSplitView(columnVisibility: .constant(.all)) {
                root
                    .toolbar(removing: .sidebarToggle)
                    .largeNavigationBar()
                    .navigationSplitViewColumnWidth(min: 340, ideal: 400, max: 480)
            } detail: {
                NavigationStack(path: $path) {
                    // Untitled, the page gets no bar; pushed pages do.
                    overview
                        .safeAreaPadding(.top, 24)
                } destination: { store in
                    page(store)
                }
            }
            .navigationSplitViewStyle(.balanced)
        }
    }

    /// `open` swaps the page at a depth for another; without its own identity
    /// SwiftUI would update the old page in place and never run the new one's `.task`.
    private func page(_ store: Store<State, Action>) -> some View {
        destination(store)
            .id(ObjectIdentifier(store))
    }
}

extension StackState {
    /// For pushes made from a tab's root. On a stack the root is only tappable
    /// while the path is empty, so replacing equals pushing; on a spread the
    /// root stays on screen and must replace whatever is open beside it.
    mutating func open(_ element: Element) {
        self = StackState([element])
    }
}
