plugins {
    id("melon.android-library")
}

android {
    namespace = "dev.forcetower.unes.mvi"
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    api(composeBom)

    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.kotlinx.coroutines.core)
}
