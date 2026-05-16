# Melon

Melon is the client side of the UNES app:

- **`apps/ios`** — Native iOS app (Swift + SwiftUI).
- **`apps/android`** — Native Android app (Kotlin + Jetpack Compose).
- **`apps/landing`** — Marketing site (Astro, deployed to Cloudflare Pages).
- **`packages/shared-kmp`** — Shared business logic via Kotlin Multiplatform, packaged as an XCFramework for iOS and a library for Android.

## Tooling

- [`mise`](https://mise.jdx.dev/) manages tool versions (`bun`, `gradle`, `java`, `license-plist`). Run `mise install` once.
- `bun install` for Node dependencies.
- `./gradlew` for the JVM/Android side; the Gradle composite build wires in `build-logic/` and `packages/shared-kmp/`.
- iOS is a standard Xcode project (`apps/ios`). The iOS app depends on the KMP umbrella XCFramework — build it with `bun run kmp:xcframework`.

## License

See [`LICENSE`](./LICENSE).
