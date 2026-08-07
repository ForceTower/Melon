import Foundation

struct Profile: Equatable, Sendable, Identifiable {
    let id: String
    /// Upstream registry name — documents and everything issued by the
    /// university keep rendering it.
    var name: String
    var email: String?
    var course: String?
    /// Profile-customization display name; nil while the portal name is in
    /// charge.
    var alternateName: String?
    var imageUrl: String?

    /// What the UI calls the user: the customization when one is set, the
    /// registry name otherwise.
    var displayName: String { alternateName ?? name }
}

extension Profile {
    static let preview = Profile(
        id: "preview",
        name: "João Sena",
        email: "joao@example.com",
        course: "Ciência da Computação"
    )
}
