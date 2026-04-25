plugins {
    id("melon.kmp-library")
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kermit)
        }
    }
}
