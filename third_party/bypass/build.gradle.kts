plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("android.extensions")
    id("org.jmailen.kotlinter")
}

android {
    compileSdkVersion(Versions.Android.compileSdkVersion)
    defaultConfig {
        minSdkVersion(Versions.Android.minSdkVersion)
        targetSdkVersion(Versions.Android.targetSdkVersion)
        versionCode = 2
        versionName = "1.2.0"

        ndk?.abiFilters("arm64-v8a", "armeabi", "armeabi-v7a", "mips", "mips64", "x86", "x86_64")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

repositories {
    google()
    jcenter()
}

dependencies {
    implementation (fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation (Dependencies.Kotlin.stdlib)
    implementation (Dependencies.AndroidX.annotations)
}
