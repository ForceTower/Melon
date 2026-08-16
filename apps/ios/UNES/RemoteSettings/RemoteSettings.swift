import Foundation

/// A remote-config source that always answers. Reads are synchronous and
/// total: a key nothing has published resolves to the type's empty value
/// (`false` / `""`), which is the floor every gate is written against — a
/// feature stays off and the captcha pair stays empty until something says
/// otherwise.
///
/// Only the *base* source conforms. Layers stacked on top of it report absence
/// instead (see `LeverRemoteSettings`), so they can decline a key and let the
/// layer below answer.
nonisolated protocol RemoteSettings: Sendable {
    func bool(_ key: RemoteBoolKey) -> Bool
    func string(_ key: RemoteStringKey) -> String

    /// Begins fetching, and calls `onChange` whenever the serving values
    /// changed. Called once, from `AppDelegate`; the callback arrives off the
    /// main actor.
    func start(onChange: @escaping @Sendable () -> Void)
}

/// Parameter names are the un-prefixed keys shared with Android. Both platforms
/// resolve them from the same lever environment, so a gate that should differ
/// between the two is a platform condition on the parameter — not a second key.
nonisolated enum RemoteBoolKey: String {
    case enrollment = "enable_enrollment"
    case enrollmentCertificate = "enable_enrollment_certificate"
    case academicHistory = "enable_academic_history"
    case paradoxo = "enable_paradoxo"
    case materials = "enable_materials"
    case library = "enable_library"
    case campusEvent = "enable_campus_event"
    case evaluationReminders = "enable_evaluation_reminders"
    case retrospective = "enable_retrospective"
    case courseProgress = "enable_course_progress"
}

nonisolated enum RemoteStringKey: String {
    case documentCaptchaSiteKey = "document_captcha_site_key"
    case documentCaptchaBaseURL = "document_captcha_base_url"
}
