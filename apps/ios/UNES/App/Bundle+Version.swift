import Foundation

extension Bundle {
    /// Marketing version, e.g. "0.9.0". Falls back to "0.0.0" so UI never
    /// has to branch on a missing Info.plist key.
    var appVersion: String {
        infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }

    /// Build number (CFBundleVersion), e.g. "1". Falls back to "0".
    var buildNumber: String {
        infoDictionary?["CFBundleVersion"] as? String ?? "0"
    }
}
