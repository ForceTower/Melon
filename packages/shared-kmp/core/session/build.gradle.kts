@file:OptIn(dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi::class)

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
}

metro {
    generateContributionProviders.set(true)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:common"))
            api(project(":packages:shared-kmp:core:network"))
            implementation(project(":packages:shared-kmp:core:storage"))
            implementation(project(":packages:shared-kmp:core:database"))
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
