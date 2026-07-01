import ComposableArchitecture
import SwiftUI

public struct RootView: View {
    @State private var store = Store(initialState: AppFeature.State()) {
        AppFeature()
    }

    public init() {}

    public var body: some View {
        AppView(store: store)
    }
}
