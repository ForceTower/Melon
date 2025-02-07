/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import dev.forcetower.gradle.utils.buildVersion
import dev.forcetower.gradle.utils.runCommand
import java.nio.file.Files

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.androidx.navigation.safe.args)
    alias(libs.plugins.hilt.android.gradle)
    alias(libs.plugins.kotlinter.gradle)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.firebase.crashlytics.gradle)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.google.services)
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "com.forcetower.uefs"
        minSdk = 21
        targetSdk = 35
        val (code, name) = buildVersion()
        versionCode = code
        versionName = name
        multiDexEnabled = true

        buildConfigField("String", "SIECOMP_TIMEZONE", "\"America/Bahia\"")
        buildConfigField("String", "SIECOMP_DAY1_START", "\"2019-10-16T08:00:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY1_END", "\"2019-10-16T17:30:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY2_START", "\"2019-10-17T08:00:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY2_END", "\"2019-10-17T17:30:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY3_START", "\"2019-10-18T08:00:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY3_END", "\"2019-10-18T17:30:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY4_START", "\"2019-10-21T08:00:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY4_END", "\"2019-10-21T17:30:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY5_START", "\"2019-10-22T08:00:00-03:00\"")
        buildConfigField("String", "SIECOMP_DAY5_END", "\"2019-10-22T17:30:00-03:00\"")
        buildConfigField("String", "UEFS_DEFAULT_PROXY", "\"10.65.16.2:3128\"")
        buildConfigField("String", "DATADOG_PUBLIC_KEY", "\"${System.getenv("UNES_DATADOG_CLIENT_KEY")}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi", "armeabi-v7a", "mips", "mips64", "x86", "x86_64")
        }

        ndkVersion = "21.3.6528147"

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    signingConfigs {
        create("release") {
            var password = System.getenv("UNES_KEYSTORE_PASSWORD")
            if (password == null)
                password = "android"

            var alias = System.getenv("UNES_KEYSTORE_ALIAS")
            if (alias == null)
                alias = "androiddebugkey"

            var keyPass = System.getenv("UNES_KEYSTORE_PRIVATE_KEY_PASSWORD")
            if (keyPass == null)
                keyPass = "android"

            var signFile = rootProject.file("sign.jks")
            if (!signFile.exists())
                signFile = rootProject.file("debug.keystore")

            storeFile = signFile
            storePassword = password
            keyAlias = alias
            keyPassword = keyPass
        }
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        var mapsKey = System.getenv("UNES_MAPS_KEY")
        if (mapsKey == null) {
            mapsKey = "AIzaSyAIb0g7GrjLgOwRqmKHhBxbxWKjct8IF8Y"
        }
        getByName("release") {
            manifestPlaceholders += mapOf("crashlyticsEnabled" to true)
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            isMinifyEnabled = true
            resValue("string", "google_maps_key", mapsKey)
        }
        getByName("debug") {
            manifestPlaceholders += mapOf("crashlyticsEnabled" to false)
            applicationIdSuffix = ".debug"
            resValue("string", "google_maps_key", "AIzaSyAIb0g7GrjLgOwRqmKHhBxbxWKjct8IF8Y")
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    kapt {
        correctErrorTypes = true
        javacOptions {
            option("-Xmaxerrs", "1000")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    dynamicFeatures += setOf(":dynamic-features:aeri", ":dynamic-features:dashboard", ":dynamic-features:conference", ":dynamic-features:event", ":dynamic-features:map", ":dynamic-features:disciplines", ":dynamic-features:enrollment")

    testOptions.unitTests.isReturnDefaultValues = true

    lint {
        abortOnError = true
        checkDependencies = true
        disable += setOf("MissingTranslation", "InvalidPackage", "NullSafeMutableLiveData")
        ignoreTestSources = true
    }
    namespace = "com.forcetower.uefs"
}

play {
    val branch = "git rev-parse --abbrev-ref HEAD".runCommand(project.rootDir).trim()
    var publishTrack = "internal"
    if (branch == "main") publishTrack = "production"

    serviceAccountCredentials.set(rootProject.file("unes_uefs_publisher.json"))
    track.set(publishTrack)
    defaultToAppBundles.set(true)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.android.tools.desugar)
    implementation(project(":bypass"))
    implementation(project(":core"))
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.autostarter)
    implementation(libs.juice)
    implementation(libs.snowpiercer)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.flexbox)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.listenablefuture)
    api(libs.androidx.viewmodel)
    api(libs.androidx.lifecycle.livedata.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.common.java8)
    api(libs.androidx.lifecycle.reactivestreams.ktx)
    api(libs.androidx.lifecycle.service)
    api(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    implementation(libs.firebase.ui.storage)
    // Remove direct guava dependency when https://github.com/firebase/firebase-android-sdk/issues/6232 resolves. or don't.
    implementation(libs.guava)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)
    implementation(libs.timber)
    implementation(libs.datadog)
    implementation(libs.play.services.games.v2)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation(libs.review.ktx)
    implementation(libs.app.update.ktx)
    implementation(libs.feature.delivery.ktx)
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    implementation(libs.lottie)
    implementation(libs.aboutlibraries)
    implementation(libs.aboutlibraries.core)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.mpandroidchart)
    implementation(libs.card.slider)
    implementation(libs.floatingsearchview)
    implementation(libs.rxkotlin)
    implementation(libs.taptargetview)
    implementation(libs.play.services.maps)
    implementation(libs.materialdatetimepicker)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.latex)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.image.glide)
    implementation(libs.markwon.linkify)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val buildNotesTask = tasks.create("buildNotes") {
    doFirst {
        try {
            val branch = "git rev-parse --abbrev-ref HEAD".runCommand(project.rootDir).trim()
            if (branch == "main") {
                val lastTag = "git describe --abbrev=0".runCommand(project.rootDir).trim()
                val tagMessage = "git tag -l --format=%(contents) $lastTag".runCommand(project.rootDir).trim()
                val production = File(project.rootDir, "app/src/main/play/release-notes/pt-BR/production.txt")
                production.writeText(tagMessage)
            } else if (branch == "development") {
                val message = "git log -1 --pretty=%B".runCommand(project.rootDir).trim().split(":").reversed()[0]
                val internal = File(project.rootDir, "app/src/main/play/release-notes/pt-BR/internal.txt")
                internal.writeText(message)
            }
        } catch (ignored: Exception) {
            System.err.println("No git installed on the machine or not on a git repo. UNES will not generate release notes")
        }
    }
}

afterEvaluate {
    val publish = tasks.findByName("publishReleaseBundle")
    publish?.dependsOn(buildNotesTask)
}

val googleServices = file("google-services.json")
if (!googleServices.exists()) {
    Files.copy(file("google-services-mock.json").toPath(), googleServices.toPath())
}

