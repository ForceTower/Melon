// swift-tools-version: 5.9
// Gradle-free consumption path for the shared KMP umbrella. The UNES app itself
// links the framework via an Xcode Run Script phase (fast dev loop); this
// package exists so other Swift consumers can link the prebuilt XCFramework
// without needing Gradle at build time.
//
// Bootstrap once (and after any Kotlin change):
//   ./gradlew :packages:shared-kmp:umbrella:assembleUmbrellaReleaseXCFramework
//
// SPM caches local binaryTargets aggressively; after rebuilding the xcframework,
// run "Product > Clean Build Folder" in the consuming Xcode project.

import PackageDescription

let package = Package(
    name: "Umbrella",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "Umbrella", targets: ["Umbrella"])
    ],
    targets: [
        .binaryTarget(
            name: "Umbrella",
            path: "./build/XCFrameworks/release/Umbrella.xcframework"
        )
    ]
)
