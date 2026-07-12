@file:OptIn(dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi::class)

plugins {
    id("melon.kmp-library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

metro {
    generateContributionProviders.set(true)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:common"))
            implementation(project(":packages:shared-kmp:core:network"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
    }
}
