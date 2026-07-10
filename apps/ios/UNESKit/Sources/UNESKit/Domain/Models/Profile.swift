import Foundation

struct Profile: Equatable, Sendable, Identifiable {
    let id: String
    var name: String
    var email: String?
    var course: String?
}

extension Profile {
    static let preview = Profile(
        id: "preview",
        name: "João Sena",
        email: "joao@example.com",
        course: "Ciência da Computação"
    )
}
