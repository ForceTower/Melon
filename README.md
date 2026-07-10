# Melon

Open-source clients for **UNES**, a companion app for students at UEFS (Universidade
Estadual de Feira de Santana). UNES connects to your official academic portal (SAGRES)
and organizes grades, schedule, classes, messages, and enrollment into a fast, native
experience.

This repository holds the **client applications** — Android, iOS, and the marketing
site. The backend service they talk to is a separate, closed-source project.

Its history runs continuously from the original single-app Android project (2018) through
the current multi-platform rewrite, so the whole lineage is browsable here even though the
current tree is the v2 clients.

## Apps & packages

| Path                   | What                                                                          |
| ---------------------- | ----------------------------------------------------------------------------- |
| `apps/android`         | Native Android app (Kotlin + Jetpack Compose)                                 |
| `apps/iosv2`           | Native iOS app (Swift + SwiftUI + The Composable Architecture)                |
| `apps/landing`         | Marketing site (Astro)                                                        |
| `packages/shared-kmp`  | Shared business logic (Kotlin Multiplatform) — XCFramework for iOS, lib for Android |
| `build-logic`          | Gradle convention plugins shared across the JVM projects                       |

## Building

Tooling versions are pinned with [mise](https://mise.jdx.dev):

```bash
mise install        # JDK, Gradle, Bun
```

- **Android:** `./gradlew :apps:android:app:assembleDebug`
- **Shared KMP XCFramework:** `./gradlew :packages:shared-kmp:umbrella:assembleUmbrellaReleaseXCFramework`
- **iOS:** open `apps/iosv2` in Xcode
- **Landing:** `cd apps/landing && bun install && bun run dev`

The apps expect their own Firebase configuration and a backend endpoint. If you are
building your own fork, supply your own `google-services.json` /
`GoogleService-Info.plist` and point the apps at your own server.

## Legacy

The original single-app Android incarnation is also preserved as standalone refs:

- Tag: **`v1-legacy-android`**
- Branch: **`legacy/android-v1`**

## License

[GNU General Public License v3.0](./LICENSE).
