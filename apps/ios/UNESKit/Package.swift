// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "UNESKit",
    defaultLocalization: "pt-BR",
    platforms: [.iOS(.v18), .macOS(.v15), .watchOS(.v11)],
    products: [
        .library(name: "UNESKit", targets: ["UNESKit"]),
    ],
    dependencies: [
        // Exact: 1.26.0 breaks the watchOS 27 SDK build and 1.26.2 moves to
        // swift-issue-reporting 2.x, which clashes with the other pointfree pins.
        .package(
            url: "https://github.com/pointfreeco/swift-composable-architecture",
            exact: "1.26.1"
        ),
        .package(
            url: "https://github.com/groue/GRDB.swift",
            from: "7.11.0"
        ),
    ],
    targets: [
        .target(
            name: "UNESKit",
            dependencies: [
                .product(name: "ComposableArchitecture", package: "swift-composable-architecture"),
                .product(name: "GRDB", package: "GRDB.swift"),
            ],
            resources: [
                .process("Resources"),
            ]
        ),
        .testTarget(
            name: "UNESKitTests",
            dependencies: ["UNESKit"]
        ),
    ],
    swiftLanguageModes: [.v6]
)
