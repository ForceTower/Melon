import SwiftUI

/// One license family the app's dependencies fall under.
enum LicenseFamily: String, CaseIterable, Identifiable, Sendable {
    case mit = "MIT"
    case apache2 = "Apache-2.0"

    var id: String { rawValue }

    /// The group card subtitle.
    var blurb: LocalizedStringResource {
        switch self {
        case .mit: .licensesFamilyMitBlurb
        case .apache2: .licensesFamilyApache2Blurb
        }
    }

    /// Canonical license text — the "Texto" pill opens it in the browser.
    var textURL: URL {
        switch self {
        case .mit: URL(string: "https://opensource.org/license/mit")!
        case .apache2: URL(string: "https://www.apache.org/licenses/LICENSE-2.0")!
        }
    }

    /// Lifted for dark-mode legibility on cards, chips and row dots.
    var tone: Color { UNESColor.readable(toneHex) }

    /// The fixed light value — the hero is always dark, so its bar and
    /// legend never take the lifted variant.
    var heroTone: Color { Color(hex: toneHex) }

    private var toneHex: UInt32 {
        switch self {
        case .mit: 0xE8894E
        case .apache2: 0x2AA5B8
        }
    }
}

/// One open-source dependency the app ships with.
struct LicensePackage: Equatable, Identifiable, Sendable {
    let name: String
    let version: String
    let family: LicenseFamily
    let author: String
    let category: LocalizedStringResource
    /// Repository, scheme-less — rows show it verbatim.
    let homepage: String

    var id: String { name }

    var homepageURL: URL? { URL(string: "https://\(homepage)") }

    /// "swift-sharing@2.9.1" — what the copy pill puts on the pasteboard.
    var pin: String { "\(name)@\(version)" }
}

/// The dependency graph, pinned by hand from `Package.resolved`: it only
/// changes when the package manifest does, and there is no license-plist
/// build phase to hydrate from at runtime.
enum LicenseCatalog {
    static let packages: [LicensePackage] = [
        LicensePackage(
            name: "swift-composable-architecture",
            version: "1.26.0",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryArchitecture,
            homepage: "github.com/pointfreeco/swift-composable-architecture"
        ),
        LicensePackage(
            name: "GRDB.swift",
            version: "7.11.1",
            family: .mit,
            author: "Gwendal Roué",
            category: .licensesCategoryDatabase,
            homepage: "github.com/groue/GRDB.swift"
        ),
        LicensePackage(
            name: "combine-schedulers",
            version: "1.2.0",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryConcurrency,
            homepage: "github.com/pointfreeco/combine-schedulers"
        ),
        LicensePackage(
            name: "swift-case-paths",
            version: "1.8.0",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryArchitecture,
            homepage: "github.com/pointfreeco/swift-case-paths"
        ),
        LicensePackage(
            name: "swift-clocks",
            version: "1.1.0",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryConcurrency,
            homepage: "github.com/pointfreeco/swift-clocks"
        ),
        LicensePackage(
            name: "swift-concurrency-extras",
            version: "1.4.0",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryConcurrency,
            homepage: "github.com/pointfreeco/swift-concurrency-extras"
        ),
        LicensePackage(
            name: "swift-custom-dump",
            version: "1.6.1",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryDiagnostics,
            homepage: "github.com/pointfreeco/swift-custom-dump"
        ),
        LicensePackage(
            name: "swift-dependencies",
            version: "1.14.1",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryArchitecture,
            homepage: "github.com/pointfreeco/swift-dependencies"
        ),
        LicensePackage(
            name: "swift-identified-collections",
            version: "1.1.1",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryDataStructures,
            homepage: "github.com/pointfreeco/swift-identified-collections"
        ),
        LicensePackage(
            name: "swift-navigation",
            version: "2.10.2",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryNavigation,
            homepage: "github.com/pointfreeco/swift-navigation"
        ),
        LicensePackage(
            name: "swift-perception",
            version: "2.0.10",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryArchitecture,
            homepage: "github.com/pointfreeco/swift-perception"
        ),
        LicensePackage(
            name: "swift-sharing",
            version: "2.9.1",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryArchitecture,
            homepage: "github.com/pointfreeco/swift-sharing"
        ),
        LicensePackage(
            name: "xctest-dynamic-overlay",
            version: "1.10.1",
            family: .mit,
            author: "Point-Free, Inc.",
            category: .licensesCategoryDiagnostics,
            homepage: "github.com/pointfreeco/xctest-dynamic-overlay"
        ),
        LicensePackage(
            name: "swift-collections",
            version: "1.6.0",
            family: .apache2,
            author: "Apple & projeto Swift",
            category: .licensesCategoryDataStructures,
            homepage: "github.com/apple/swift-collections"
        ),
        LicensePackage(
            name: "swift-syntax",
            version: "601.0.1",
            family: .apache2,
            author: "Apple & projeto Swift",
            category: .licensesCategoryMacros,
            homepage: "github.com/swiftlang/swift-syntax"
        ),
    ]

    /// Families ordered by share, largest first — the hero bar, legend,
    /// filter chips, and group cards all follow it.
    static let breakdown: [LicenseShare] = LicenseFamily.allCases
        .map { family in
            LicenseShare(family: family, count: packages.count { $0.family == family })
        }
        .filter { $0.count > 0 }
        .sorted { $0.count > $1.count }

    static func package(_ id: LicensePackage.ID) -> LicensePackage? {
        packages.first { $0.id == id }
    }
}

/// How many packages a family covers.
struct LicenseShare: Equatable, Identifiable, Sendable {
    let family: LicenseFamily
    let count: Int

    var id: LicenseFamily { family }
}
