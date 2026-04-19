import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
    alias(libs.plugins.skie)
}

kotlin {
    val xcf = XCFramework("Umbrella")
    val bundleId = "dev.forcetower.melon.umbrella"
    iosX64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            xcf.add(this)
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:common"))
            api(project(":packages:shared-kmp:core:network"))
            api(project(":packages:shared-kmp:core:database"))
            api(project(":packages:shared-kmp:core:storage"))
            api(project(":packages:shared-kmp:core:session"))
            api(project(":packages:shared-kmp:core:sync"))
            api(project(":packages:shared-kmp:features:auth"))
            api(project(":packages:shared-kmp:features:dashboard"))
            api(project(":packages:shared-kmp:features:sync"))
        }
    }
}
