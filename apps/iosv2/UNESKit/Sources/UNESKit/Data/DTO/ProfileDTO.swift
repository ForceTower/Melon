import Foundation

struct ProfileDTO: Decodable {
    let id: String
    let name: String
    let email: String?
    let course: String?
}

extension ProfileDTO {
    var domain: Profile {
        Profile(id: id, name: name, email: email, course: course)
    }
}
