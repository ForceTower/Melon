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
            implementation(project(":packages:shared-kmp:core:database"))
            implementation(project(":packages:shared-kmp:core:session"))
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
