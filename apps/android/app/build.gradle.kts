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

val gitVersionName =
    gitOutput(
        "for-each-ref",
        "--sort=-creatordate",
        "--count=1",
        "--format=%(refname:short)",
        "refs/tags/",
    ) ?: "0.1.0"

val gitVersionCode = gitOutput("rev-list", "--count", "next")?.toIntOrNull() ?: 1

android {
    namespace = "dev.forcetower.unes"

    defaultConfig {
        applicationId = "com.forcetower.uefs"
        versionCode = 2130000 + gitVersionCode
        versionName = gitVersionName
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
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
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("ISC")
    allow("MPL-2.0")
    allow("EPL-1.0")
    allow("CC0-1.0")
    allow("Unlicense")

    // Play Services and Google identity libraries declare their license via
    // URL only (no SPDX id), so allow the Android SDK terms URL explicitly —
    // the bundled libraries that ship under it are first-party Google.
    allowUrl("https://developer.android.com/studio/terms.html")
    // slf4j declares MIT by URL form rather than SPDX id.
    allowUrl("https://opensource.org/license/mit")
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.datastore.preferences)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.timber)

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

    // Firebase BoM pins all SDK versions in lockstep — Analytics for usage
    // tracking, Crashlytics for crash reporting, Messaging for FCM push,
    // Remote Config for feature gates (same parameter keys as iOS).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    // `await()` extension for FirebaseMessaging.getToken() / Tasks.
    implementation(libs.kotlinx.coroutines.play.services)
}
