import Foundation

/// One open-source dependency the app ships with. Hydrated from the
/// `com.mono0926.LicensePlist.plist` artifact `license-plist` writes into
/// `Resources/Licenses/` during the build. The plist exposes a
/// `PreferenceSpecifiers` array shaped for the iOS Settings.bundle format —
/// we reuse it for in-app rendering instead of mounting a Settings bundle.
struct LicenseEntry: Identifiable, Hashable {
    /// `Title` from the plist — the library name as it should appear in the
    /// list (e.g. "Firebase", "abseil"). Stable enough to use as identity.
    let title: String
    /// `License` from the plist — short SPDX-ish identifier such as
    /// `Apache-2.0` or `MIT`. Optional because some entries omit it.
    let identifier: String?
    /// `FooterText` from the plist — full license body. Always rendered in
    /// the detail view; the list shows a short preview line by stripping it
    /// down to its first non-empty line.
    let body: String

    var id: String { title }

    /// First non-empty trimmed line of the license body — the closest thing
    /// to a one-line summary the plist gives us. Used as a row subtitle so
    /// the list reads as more than a column of identifiers.
    var summaryLine: String {
        body
            .split(whereSeparator: \.isNewline)
            .lazy
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .first(where: { !$0.isEmpty }) ?? ""
    }
}

/// Loads the `com.mono0926.LicensePlist.plist` produced by `license-plist`.
/// The file is bundled at build time by a Run Script phase that points at
/// the repo's `Package.resolved` — see the `UNES` target's "Generate
/// licenses" phase. Failures are non-fatal: a missing plist (e.g. running a
/// debug build before the script has fired) just renders an empty list.
enum LicensePlistLoader {
    /// Top-level shape of the plist. `license-plist` writes a single key,
    /// `PreferenceSpecifiers`, holding one entry per dependency.
    private struct PlistRoot: Decodable {
        let preferenceSpecifiers: [PlistEntry]

        enum CodingKeys: String, CodingKey {
            case preferenceSpecifiers = "PreferenceSpecifiers"
        }
    }

    private struct PlistEntry: Decodable {
        let title: String?
        let license: String?
        let footerText: String?

        enum CodingKeys: String, CodingKey {
            case title       = "Title"
            case license     = "License"
            case footerText  = "FooterText"
        }
    }

    /// Resource name written by `license-plist` — keep in sync with the Run
    /// Script phase's `--output-path` value (the file name itself isn't
    /// configurable, only its parent directory).
    private static let resourceName = "com.mono0926.LicensePlist"

    static func load(bundle: Bundle = .main) -> [LicenseEntry] {
        guard let url = bundle.url(forResource: resourceName, withExtension: "plist"),
              let data = try? Data(contentsOf: url)
        else { return [] }

        let decoder = PropertyListDecoder()
        guard let root = try? decoder.decode(PlistRoot.self, from: data) else {
            return []
        }

        return root.preferenceSpecifiers.compactMap { entry in
            guard let title = entry.title, !title.isEmpty else { return nil }
            return LicenseEntry(
                title: title,
                identifier: entry.license?.trimmingCharacters(in: .whitespaces).nilIfEmpty,
                body: entry.footerText ?? ""
            )
        }
    }
}

private extension String {
    var nilIfEmpty: String? { isEmpty ? nil : self }
}
