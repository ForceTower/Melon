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

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = mapOf("room.schemaLocation" to "$projectDir/sagres_schemas")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        isAbortOnError = false
    }
}

dependencies {
    implementation (fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation (Dependencies.Kotlin.stdlib)

    implementation (Dependencies.AndroidX.annotations)

    implementation (Dependencies.Architecture.lifecycleExtensions)
    implementation (Dependencies.Architecture.lifecycleJava8)
    implementation (Dependencies.Architecture.room)
    kapt (Dependencies.Architecture.roomCompiler)

    implementation (Dependencies.Network.okhttp)
    implementation (Dependencies.Network.cookieJar)

    implementation (Dependencies.Common.timber)
    implementation (Dependencies.Common.gson)
    implementation (Dependencies.Common.jsoup)
}

repositories {
    mavenCentral()
}
