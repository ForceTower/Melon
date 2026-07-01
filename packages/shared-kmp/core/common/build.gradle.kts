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
            implementation(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
        }
    }
}
