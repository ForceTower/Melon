// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "UNESKit",
    defaultLocalization: "pt-BR",
    platforms: [.iOS(.v18), .macOS(.v15)],
    products: [
        .library(name: "UNESKit", targets: ["UNESKit"]),
    ],
    dependencies: [
        .package(
            url: "https://github.com/pointfreeco/swift-composable-architecture",
            from: "1.26.0"
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
