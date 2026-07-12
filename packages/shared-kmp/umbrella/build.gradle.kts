import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
    alias(libs.plugins.skie)
}

kotlin {
    val xcf = XCFramework("Umbrella")
    val bundleId = "dev.forcetower.melon.umbrella"
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
            api(project(":packages:shared-kmp:core:logging"))
            api(project(":packages:shared-kmp:core:network"))
            api(project(":packages:shared-kmp:core:database"))
            api(project(":packages:shared-kmp:core:storage"))
            api(project(":packages:shared-kmp:core:session"))
            api(project(":packages:shared-kmp:core:sync"))
            api(project(":packages:shared-kmp:features:auth"))
            api(project(":packages:shared-kmp:features:calendar"))
            api(project(":packages:shared-kmp:features:campusevent"))
            api(project(":packages:shared-kmp:features:dashboard"))
            api(project(":packages:shared-kmp:features:disciplines"))
            api(project(":packages:shared-kmp:features:enrollment"))
            api(project(":packages:shared-kmp:features:materials"))
            api(project(":packages:shared-kmp:features:me"))
            api(project(":packages:shared-kmp:features:messages"))
            api(project(":packages:shared-kmp:features:notifications"))
            api(project(":packages:shared-kmp:features:overview"))
            api(project(":packages:shared-kmp:features:paradoxo"))
            api(project(":packages:shared-kmp:features:schedule"))
            api(project(":packages:shared-kmp:features:settings"))
            api(project(":packages:shared-kmp:features:sync"))
        }
    }
}
