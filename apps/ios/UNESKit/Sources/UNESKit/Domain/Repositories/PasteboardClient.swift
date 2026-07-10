import ComposableArchitecture

/// System clipboard — write-only, for the credential copy buttons.
@DependencyClient
struct PasteboardClient: Sendable {
    var copy: @Sendable (String) async -> Void
}

extension PasteboardClient: TestDependencyKey {
    static let testValue = PasteboardClient()
    static let previewValue = PasteboardClient(copy: { _ in })
}

extension DependencyValues {
    var pasteboard: PasteboardClient {
        get { self[PasteboardClient.self] }
        set { self[PasteboardClient.self] = newValue }
    }
}
