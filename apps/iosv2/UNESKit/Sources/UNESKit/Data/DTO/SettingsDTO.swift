import Foundation

/// Wire shape of the `settings` object on `GET api/sync/profile` and on the
/// `PATCH api/me/settings` response.
struct UserSettingsDTO: Decodable {
    let gradeSpoiler: Int
    let notifMsgBroadcast: Bool
    let notifMsgClass: Bool
    let notifMsgDirect: Bool
    let notifGradePosted: Bool
    let notifGradeChanged: Bool
    let notifGradeDateChanged: Bool
    let notifClassLocation: Bool
    let notifClassMaterial: Bool
    let notifClassSubject: Bool
}

extension UserSettingsDTO {
    var domain: UserSettings {
        UserSettings(
            gradeSpoiler: GradeSpoiler(rawValue: gradeSpoiler) ?? .summary,
            messageBroadcast: notifMsgBroadcast,
            messageClass: notifMsgClass,
            messageDirect: notifMsgDirect,
            gradePosted: notifGradePosted,
            gradeChanged: notifGradeChanged,
            gradeDateChanged: notifGradeDateChanged,
            classLocation: notifClassLocation,
            classMaterial: notifClassMaterial,
            classSubject: notifClassSubject
        )
    }
}

/// `GET api/sync/profile`, the slice Settings needs: identity + settings.
struct SettingsProfileDTO: Decodable {
    let user: ProfileDTO.UserDTO
    let course: ProfileDTO.CourseDTO?
    let settings: UserSettingsDTO
}

extension SettingsProfileDTO {
    var domain: SettingsAccount {
        SettingsAccount(
            profile: ProfileDTO(user: user, course: course).domain,
            settings: settings.domain
        )
    }
}

/// `GET api/me/credentials` — `credentials` is null when nothing is on file.
struct CredentialsDTO: Decodable {
    let credentials: Entry?

    struct Entry: Decodable {
        let username: String
        let password: String
    }
}

/// `PATCH api/me/settings` body. Only the changed field is non-nil; the
/// synthesized encoder omits nil fields, which is what the API expects.
struct UserSettingsPatchDTO: Encodable {
    var gradeSpoiler: Int?
    var notifMsgBroadcast: Bool?
    var notifMsgClass: Bool?
    var notifMsgDirect: Bool?
    var notifGradePosted: Bool?
    var notifGradeChanged: Bool?
    var notifGradeDateChanged: Bool?
    var notifClassLocation: Bool?
    var notifClassMaterial: Bool?
    var notifClassSubject: Bool?
}

extension UserSettingsPatchDTO {
    init(_ change: SettingsChange) {
        self.init()
        switch change {
        case let .gradeSpoiler(spoiler):
            gradeSpoiler = spoiler.rawValue
        case let .notification(toggle, isOn):
            switch toggle {
            case .messageBroadcast: notifMsgBroadcast = isOn
            case .messageClass: notifMsgClass = isOn
            case .messageDirect: notifMsgDirect = isOn
            case .gradePosted: notifGradePosted = isOn
            case .gradeChanged: notifGradeChanged = isOn
            case .gradeDateChanged: notifGradeDateChanged = isOn
            case .classLocation: notifClassLocation = isOn
            case .classMaterial: notifClassMaterial = isOn
            case .classSubject: notifClassSubject = isOn
            }
        }
    }
}

/// `PATCH api/me/settings` response payload.
struct UpdateSettingsDTO: Decodable {
    let settings: UserSettingsDTO
}
