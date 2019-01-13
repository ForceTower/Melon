plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("android.extensions")
    id("io.fabric")
    id("com.github.triplet.play") version Versions.playPublisher
    id("com.google.gms.google-services") apply false
    id("org.jmailen.kotlinter")
}

android {
    dataBinding.isEnabled = true

    compileSdkVersion(Versions.Android.compileSdkVersion)

    defaultConfig {
        applicationId = Config.applicationId
        minSdkVersion(Versions.Android.minSdkVersion)
        targetSdkVersion(Versions.Android.targetSdkVersion)
        versionCode = Config.buildVersionCode()
        versionName = Config.buildVersionName(findProperty("version") as String)
        multiDexEnabled = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

        ndk?.abiFilters("arm64-v8a", "armeabi", "armeabi-v7a", "mips", "mips64", "x86", "x86_64")
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("sign.jks") // Can also be a .keystore file, there's no difference
            storePassword = System.getenv("UNES_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("UNES_KEYSTORE_ALIAS")
            keyPassword = System.getenv("UNES_KEYSTORE_PRIVATE_KEY_PASSWORD")
        }
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    kapt {
        useBuildCache = true
        javacOptions {
            option("-Xmaxerrs", 1000)
        }
    }
    dexOptions {
        preDexLibraries = true
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions {
        isAbortOnError = true
    }
}

play {
    serviceAccountCredentials = rootProject.file("unes_uefs_publisher.json")
    track = "internal"
    defaultToAppBundles = true
}

dependencies {
    // Any .jar at the libs folder
    implementation (fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation (Dependencies.Kotlin.stdlib)

    implementation (Dependencies.AndroidX.coreKtx)
    implementation (Dependencies.AndroidX.annotations)
    implementation (Dependencies.AndroidX.appCompat)
    implementation (Dependencies.AndroidX.constraintLayout)
    implementation (Dependencies.AndroidX.material)
    implementation (Dependencies.AndroidX.pallete)
    implementation (Dependencies.AndroidX.browser)
    implementation (Dependencies.AndroidX.preference)
    implementation (Dependencies.AndroidX.flexbox)

    implementation (Dependencies.Architecture.lifecycleExtensions)
    implementation (Dependencies.Architecture.lifecycleJava8)
    implementation (Dependencies.Architecture.navigationFragment)
    implementation (Dependencies.Architecture.navigationFragmentKtx)
    implementation (Dependencies.Architecture.navigationUi)
    implementation (Dependencies.Architecture.navigationUiKtx)
    implementation (Dependencies.Architecture.paging)
    implementation (Dependencies.Architecture.room)
    kapt (Dependencies.Architecture.roomCompiler)
    implementation (Dependencies.Architecture.workManager)

    implementation (Dependencies.Billing.client)

    implementation (Dependencies.Dagger.core)
    implementation (Dependencies.Dagger.android)
    implementation (Dependencies.Dagger.androidSupport)
    kapt (Dependencies.Dagger.compiler)
    kapt (Dependencies.Dagger.androidProcessor)

    implementation (Dependencies.Firebase.core)
    implementation (Dependencies.Firebase.auth)
    implementation (Dependencies.Firebase.messaging)
    implementation (Dependencies.Firebase.firestore)
    implementation (Dependencies.Firebase.storage)
    implementation (Dependencies.Firebase.config)
    implementation (Dependencies.Firebase.UI.storage)

    implementation (Dependencies.Crashlytics.core) { isTransitive = true }

    implementation (Dependencies.GooglePlayServices.auth)
    implementation (Dependencies.GooglePlayServices.games)
    implementation (Dependencies.GooglePlayServices.location)

    implementation (Dependencies.Network.okhttp)
    implementation (Dependencies.Network.retrofit)
    implementation (Dependencies.Network.retrofitGson)
    implementation (Dependencies.Network.cookieJar)

    implementation (Dependencies.Glide.core)
    kapt (Dependencies.Glide.compiler)

    implementation (Dependencies.Common.gson)
    implementation (Dependencies.Common.jsoup)
    implementation (Dependencies.Common.chartView)
    implementation (Dependencies.Common.timber)
    implementation (Dependencies.Common.lottie)
    implementation (Dependencies.Common.aboutLibraries)
    implementation (Dependencies.Common.easyPermissions)
    implementation (Dependencies.Common.cardSlider)
    implementation (Dependencies.Common.imageCropper)

    implementation(project(":sagres"))
    implementation(project(":bypass"))
}

apply(mapOf("plugin" to "com.google.gms.google-services"))