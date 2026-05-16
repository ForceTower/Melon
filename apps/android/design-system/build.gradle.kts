plugins {
    id("melon.android-library")
}

android {
    namespace = "dev.forcetower.unes.designsystem"
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    api(composeBom)

    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui.text.google.fonts)

    debugApi(libs.androidx.compose.ui.tooling)
    api(libs.androidx.compose.ui.tooling.preview)
}
