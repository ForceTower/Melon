import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("melon.kmp-library")
}

kotlin {
    val xcf = XCFramework("SharedKmp")
    iosX64 { binaries.framework { baseName = "SharedKmp"; xcf.add(this) } }
    iosArm64 { binaries.framework { baseName = "SharedKmp"; xcf.add(this) } }
    iosSimulatorArm64 { binaries.framework { baseName = "SharedKmp"; xcf.add(this) } }

    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:network"))
            api(project(":packages:shared-kmp:core:database"))
            api(project(":packages:shared-kmp:features:auth"))
            api(project(":packages:shared-kmp:features:dashboard"))
        }
    }
}
