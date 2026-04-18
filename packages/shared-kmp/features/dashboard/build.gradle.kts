plugins {
    id("melon.kmp-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":packages:shared-kmp:core:network"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
