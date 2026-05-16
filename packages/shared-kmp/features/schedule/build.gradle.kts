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
            implementation(project(":packages:shared-kmp:core:database"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
