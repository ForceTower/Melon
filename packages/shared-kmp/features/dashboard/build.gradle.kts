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
            implementation(project(":packages:shared-kmp:core:network"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
