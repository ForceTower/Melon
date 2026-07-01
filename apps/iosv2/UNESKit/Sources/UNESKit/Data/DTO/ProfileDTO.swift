import Foundation

/// Wire shape of `GET api/sync/profile` (subset — decoding ignores the rest).
struct ProfileDTO: Decodable {
    let user: UserDTO
    let course: CourseDTO?

    struct UserDTO: Decodable {
        let id: String
        let name: String
        let email: String?
        let imageUrl: String?
    }

    struct CourseDTO: Decodable {
        let name: String
        let resumedName: String?
    }
}

extension ProfileDTO {
    var domain: Profile {
        Profile(
            id: user.id,
            name: user.name,
            email: user.email,
            course: course.map { $0.resumedName ?? $0.name }
        )
    }
}
