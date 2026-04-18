plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(21)

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}
