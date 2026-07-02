import Foundation

private let log = Log.scoped("FeatureFlags")

/// Remote Config flags, pushed by the app target after each fetch. Values
/// land in UserDefaults so `@Shared(.appStorage(...))` readers update
/// reactively and the last-known value gates the next launch.
public enum FeatureFlags {
    /// Gates the "Matrícula" hub entry — the `enable_enrollment` key.
    public static let enrollmentEnabledKey = "flag_enable_enrollment"

    public static func update(enrollmentEnabled: Bool) {
        UserDefaults.standard.set(enrollmentEnabled, forKey: enrollmentEnabledKey)
        log.info("feature flags updated enrollment=\(enrollmentEnabled)")
    }
}
