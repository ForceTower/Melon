import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
    alias(libs.plugins.skie)
}

kotlin {
    val xcf = XCFramework("Umbrella")
    val bundleId = "dev.forcetower.melon.umbrella"
    // kermit-crashlytics cinterop references FirebaseCrashlytics Obj-C classes
    // (FIRCrashlytics, FIRExceptionModel, FIRStackFrame, FIRCLSExceptionRecordNSException).
    // Those live in the host iOS app (added via SPM in apps/ios), not in the KMP
    // framework's dependency closure, so we mark them as undefined and defer
    // resolution to the final app link step.
    val crashlyticsUndefined = listOf(
        "-U", "_FIRCLSExceptionRecordNSException",
        "-U", "_OBJC_CLASS_\$_FIRCrashlytics",
        "-U", "_OBJC_CLASS_\$_FIRExceptionModel",
        "-U", "_OBJC_CLASS_\$_FIRStackFrame",
    )
    iosX64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            linkerOpts(crashlyticsUndefined)
            xcf.add(this)
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            linkerOpts(crashlyticsUndefined)
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "Umbrella"
            binaryOption("bundleId", bundleId)
            linkerOpts(crashlyticsUndefined)
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
            api(project(":packages:shared-kmp:features:dashboard"))
            api(project(":packages:shared-kmp:features:disciplines"))
            api(project(":packages:shared-kmp:features:me"))
            api(project(":packages:shared-kmp:features:messages"))
            api(project(":packages:shared-kmp:features:notifications"))
            api(project(":packages:shared-kmp:features:overview"))
            api(project(":packages:shared-kmp:features:schedule"))
            api(project(":packages:shared-kmp:features:sync"))
        }
    }
}
