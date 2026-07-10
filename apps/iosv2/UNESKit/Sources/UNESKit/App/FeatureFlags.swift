import Foundation

private let log = Log.scoped("FeatureFlags")

/// Remote Config flags, pushed by the app target after each fetch. Values
/// land in UserDefaults so `@Shared(.appStorage(...))` readers update
/// reactively and the last-known value gates the next launch.
public enum FeatureFlags {
    /// Gates the "Matrícula" hub entry — the `enable_enrollment` key.
    public static let enrollmentEnabledKey = "flag_enable_enrollment"
    /// Gates the "Comprovante" hub entry — the `enable_enrollment_certificate` key.
    public static let certificateEnabledKey = "flag_enable_enrollment_certificate"
    /// Gates the "Histórico" hub entry — the `enable_academic_history` key.
    public static let historyEnabledKey = "flag_enable_academic_history"
    /// Gates the "Paradoxo" hub entry — the `enable_paradoxo` key.
    public static let paradoxoEnabledKey = "flag_enable_paradoxo"
    /// reCAPTCHA site key for document requests — the `document_captcha_site_key`
    /// key. Empty means the portal isn't demanding a captcha right now.
    public static let documentCaptchaSiteKeyKey = "flag_document_captcha_site_key"
    /// Page origin the captcha widget renders under — the
    /// `document_captcha_base_url` key. Must be the portal login URL: the
    /// site key belongs to the portal and its domain allow-list is checked
    /// against this origin.
    public static let documentCaptchaBaseURLKey = "flag_document_captcha_base_url"

    public static func update(
        enrollmentEnabled: Bool,
        certificateEnabled: Bool,
        historyEnabled: Bool,
        paradoxoEnabled: Bool,
        documentCaptchaSiteKey: String,
        documentCaptchaBaseURL: String
    ) {
        let defaults = UserDefaults.standard
        defaults.set(enrollmentEnabled, forKey: enrollmentEnabledKey)
        defaults.set(certificateEnabled, forKey: certificateEnabledKey)
        defaults.set(historyEnabled, forKey: historyEnabledKey)
        defaults.set(paradoxoEnabled, forKey: paradoxoEnabledKey)
        defaults.set(documentCaptchaSiteKey, forKey: documentCaptchaSiteKeyKey)
        defaults.set(documentCaptchaBaseURL, forKey: documentCaptchaBaseURLKey)
        log.info("""
        feature flags updated enrollment=\(enrollmentEnabled) \
        certificate=\(certificateEnabled) history=\(historyEnabled) \
        paradoxo=\(paradoxoEnabled) captcha=\(!documentCaptchaSiteKey.isEmpty)
        """)
    }
}
