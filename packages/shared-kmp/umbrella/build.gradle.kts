import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
}

kotlin {
    val xcf = XCFramework("Umbrella")
    iosX64 { binaries.framework { baseName = "Umbrella"; xcf.add(this) } }
    iosArm64 { binaries.framework { baseName = "Umbrella"; xcf.add(this) } }
    iosSimulatorArm64 { binaries.framework { baseName = "Umbrella"; xcf.add(this) } }

    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:common"))
            api(project(":packages:shared-kmp:core:network"))
            api(project(":packages:shared-kmp:core:database"))
            api(project(":packages:shared-kmp:core:storage"))
            api(project(":packages:shared-kmp:core:session"))
            api(project(":packages:shared-kmp:features:auth"))
            api(project(":packages:shared-kmp:features:dashboard"))
        }
    }
}
