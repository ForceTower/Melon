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
        /// Profile-customization display name; absent on an older API.
        let alternateName: String?
    }

    struct CourseDTO: Decodable {
        // `resumedName` is intentionally ignored: SAGRES appends the
        // curriculum code to it ("Engenharia de Computação-614").
        let name: String
    }
}

extension ProfileDTO {
    var domain: Profile {
        Profile(
            id: user.id,
            name: user.name,
            email: user.email,
            course: course?.name,
            alternateName: user.alternateName,
            imageUrl: user.imageUrl
        )
    }
}
