import ComposableArchitecture
import SwiftUI

/// A tab's navigation shell: a plain stack, or on a `.spread` layout a split
/// view where `root` stays on the leading page while the trailing page starts
/// on `overview` and takes every push.
struct SpreadStack<State: ObservableState, Action, Root: View, Overview: View, Destination: View>: View {
    typealias PathStore = Store<StackState<State>, StackAction<State, Action>>

    @Environment(\.pageLayout) private var layout
    @SwiftUI.State private var gate = PathGate<State, Action>()
    /// Bumped once a new shell may read the live path, so it re-reads it.
    @SwiftUI.State private var adoptions = 0

    @Binding private var path: PathStore
    private let root: Root
    private let overview: Overview
    private let destination: (Store<State, Action>) -> Destination

    init(
        path: Binding<PathStore>,
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
        let _ = gate.close(unless: layout)
        let _ = adoptions
        shell
            .task(id: layout) {
                guard gate.shell != layout else { return }
                try? await Task.sleep(for: adoptionDelay)
                guard !Task.isCancelled else { return }
                gate.shell = layout
                // The pages were already open; they shouldn't slide in again.
                var transaction = Transaction()
                transaction.disablesAnimations = true
                withTransaction(transaction) { adoptions += 1 }
            }
    }

    @ViewBuilder
    private var shell: some View {
        switch layout {
        case .stack:
            pages(for: .stack) { root }
        case .spread:
            NavigationSplitView(columnVisibility: .constant(.all)) {
                root
                    .toolbar(removing: .sidebarToggle)
                    .largeNavigationBar()
                    .navigationSplitViewColumnWidth(min: 340, ideal: 400, max: 480)
            } detail: {
                pages(for: .spread) {
                    // Untitled, the page gets no bar; pushed pages do.
                    overview
                        .safeAreaPadding(.top, 24)
                }
            }
            .navigationSplitViewStyle(.balanced)
        }
    }

    /// The stack keeps the store it is built with for its pages' lifetime, so
    /// building it always sees the live one; only later reads are gated.
    private func pages(for shell: PageLayout, @ViewBuilder root: () -> some View) -> some View {
        gate.isBuilding = true
        defer { gate.isBuilding = false }
        return NavigationStack(path: path(for: shell), root: root) { store in
            page(store)
        }
    }

    /// `open` swaps the page at a depth for another; without its own identity
    /// SwiftUI would update the old page in place and never run the new one's `.task`.
    private func page(_ store: Store<State, Action>) -> some View {
        destination(store)
            .id(ObjectIdentifier(store))
    }

    /// Rotating or folding swaps one shell for the other. The shell on its way
    /// out empties its path as it is torn down, and a split view empties a
    /// path it is mounted with. Both get a parked path to empty instead; the
    /// new shell adopts the live one once it is up, and the open pages carry over.
    private func path(for shell: PageLayout) -> Binding<PathStore> {
        Binding {
            gate.isBuilding || gate.shell == shell ? path : gate.parked
        } set: { _ in
        }
    }
}

/// Long enough for a new split view to have emptied the path it mounted with.
private let adoptionDelay = Duration.milliseconds(100)

/// A reference, so that the binding a shell was built with sees the swap.
@MainActor
private final class PathGate<State, Action> {
    /// The shell reading the live path; none while one replaces the other.
    var shell: PageLayout?
    var isBuilding = false
    let parked = Store<StackState<State>, StackAction<State, Action>>(initialState: StackState()) {
        EmptyReducer()
    }

    private var hasOpened = false

    /// The first shell has nothing to take over, so it opens right away.
    func close(unless layout: PageLayout) {
        guard shell != layout else { return }
        shell = hasOpened ? nil : layout
        hasOpened = true
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
