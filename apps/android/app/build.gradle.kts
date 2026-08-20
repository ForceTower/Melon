import app.cash.licensee.LicenseeTask

plugins {
    id("melon.android-application")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.licensee)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

fun gitOutput(vararg args: String): String? {
    val execOutput =
        providers.exec {
            commandLine("git", *args)
            workingDir = rootDir
            isIgnoreExitValue = true
        }
    return if (execOutput.result.get().exitValue == 0) {
        execOutput.standardOutput.asText.get().trim().ifEmpty { null }
    } else {
        null
    }
}

// Bumped by hand on every release.
val marketingVersion = "12.7.0"

val gitVersionCode = gitOutput("rev-list", "--count", "main")?.toIntOrNull() ?: 1

android {
    namespace = "dev.forcetower.unes"

    defaultConfig {
        applicationId = "com.forcetower.uefs"
        versionCode = 2130000 + gitVersionCode
        versionName = marketingVersion

        // API origin the UmbrellaGraph is built with. Override for a local
        // mock/proxy via `-Pmelon.apiBaseUrl=http://127.0.0.1:8787` (pair
        // with `adb reverse tcp:8787 tcp:8787` on a device/emulator).
        val apiBaseUrl = providers.gradleProperty("melon.apiBaseUrl")
            .getOrElse("https://melon.forcetower.dev")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // PostHog product analytics. The `phc_` project key is a public client
        // token by design; host is our first-party proxy in front of PostHog
        // Cloud EU.
        buildConfigField(
            "String",
            "POSTHOG_API_KEY",
            "\"phc_uhYjeNJg9RpdEknMM8mNxdRqeiEtr4jqEm6zsv9TETqu\"",
        )
        buildConfigField("String", "POSTHOG_HOST", "\"https://a.forcetower.dev\"")

        // lever — our self-hosted remote config, the layer in front of Firebase
        // Remote Config. This is the API origin; the dashboard is a separate
        // deployment at rc.forcetower.dev and is not what clients talk to.
        //
        // The `pk_` key is a public client identifier by design: it authorizes
        // reading one environment's resolved values, which every user of the
        // app can see anyway. Never put a secret in a config value.
        buildConfigField("String", "LEVER_BASE_URL", "\"https://rc-api.forcetower.dev\"")
        buildConfigField(
            "String",
            "LEVER_CLIENT_KEY",
            "\"pk_kwXERv2Rt6NjY6pd52A2gMG0SHJLJrlu\"",
        )
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

// Licensee scans the runtime classpath at build time and emits an
// `artifacts.json` listing every dependency's coordinates, license, and
// declared homepage. The Licenças screen reads that JSON from assets at
// runtime so the credits stay honest — anything bundled in the APK is
// represented, anything missing from the JSON isn't bundled. The allowlist
// keeps the build from failing on the typical OSS license set we ship.
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-3-Clause")

    // Play Services and Google identity libraries declare their license via
    // URL only (no SPDX id), so allow the Android SDK terms URL explicitly —
    // the bundled libraries that ship under it are first-party Google.
    allowUrl("https://developer.android.com/studio/terms.html")
    // ML Kit (document scanner) ships under Google's ML Kit terms, URL-only.
    allowUrl("https://developers.google.com/ml-kit/terms")
    // Play Core (in-app updates) declares the Play Core SDK ToS URL-only.
    allowUrl("https://developer.android.com/guide/playcore/license")
    // slf4j declares MIT by URL form rather than SPDX id.
    allowUrl("https://opensource.org/license/mit")
    // lever-android's POM declares the SPDX id `MIT` alongside its own
    // repository's LICENSE URL, and Licensee still resolves it by URL — so the
    // id on its own is not enough to satisfy `allow("MIT")` above.
    allowUrl("https://github.com/ForceTower/lever-android/blob/main/LICENSE")
}

// Bundle the per-variant `artifacts.json` into the APK as an asset. AGP picks
// up the Licensee task's output directory as a generated assets root, so the
// JSON ends up at `assets/artifacts.json` for the runtime loader to read.
// The Android-flavoured Licensee task is named `licenseeAndroid<Variant>`
// (one per debug/release), distinct from the catch-all `licensee` task.
androidComponents {
    onVariants { variant ->
        val capitalized = variant.name.replaceFirstChar(Char::titlecase)
        val licenseeTask = tasks.named("licenseeAndroid$capitalized", LicenseeTask::class.java)
        variant.sources.assets?.addGeneratedSourceDirectory(
            licenseeTask,
            LicenseeTask::outputDir,
        )
    }
}

dependencies {
    implementation(project(":apps:android:design-system"))
    implementation(project(":apps:android:mvi"))
    implementation(project(":packages:shared-kmp:umbrella"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.timber)

    // Coil — remote avatar rendering (profile picture on the Eu hero card).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    implementation(libs.androidx.biometric)

    // Glance — backs the home-screen widget. Compiles to RemoteViews so layouts
    // run in the system widget host process, but the receiver + snapshot writer
    // live alongside the rest of the app code.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.play.services.mlkit.document.scanner)

    // Play In-App Updates — flexible/immediate update flows (docs/in-app-update.md).
    implementation(libs.play.app.update.ktx)

    // Play In-App Reviews — the rating sheet (`ReviewPrompter`).
    implementation(libs.play.review.ktx)

    // lever — our own remote config (github.com/ForceTower/lever), the first
    // layer of `CompositeRemoteSettings` in front of Firebase Remote Config.
    implementation(libs.lever.android)

    // Firebase BoM pins all SDK versions in lockstep — Analytics for usage
    // tracking, Crashlytics for crash reporting, Messaging for FCM push,
    // Remote Config as the fallback layer under lever (same parameter keys as
    // iOS).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    // `await()` extension for FirebaseMessaging.getToken() / Tasks.
    implementation(libs.kotlinx.coroutines.play.services)

    // PostHog — product analytics, routed to our first-party proxy (a.forcetower.dev).
    implementation(libs.posthog.android)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
}
