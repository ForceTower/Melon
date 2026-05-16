plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":packages:shared-kmp:core:common"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
    }
}
