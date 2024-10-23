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

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlinter.gradle)
    alias(libs.plugins.google.ksp)
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "com.forcetower.core"
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
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
    api(libs.androidx.lifecycle.reactivestreams)
    api(libs.androidx.lifecycle.service)
    implementation(libs.timber)
    api(libs.androidx.navigation.ui.ktx)
    api(libs.androidx.navigation.fragment.ktx)
    api(libs.androidx.navigation.dynamic.features.fragment)
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.work.gcm)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.paging)
    api(libs.androidx.room.ktx)
    api(libs.glide)
    ksp(libs.glide.compiler)
    api(libs.android.image.cropper)
    api(libs.okhttp)
    api(libs.logging.interceptor)
    api(libs.retrofit)
    api(libs.converter.gson)
    api(libs.persistentcookiejar)
    api(libs.dagger)
    ksp(libs.dagger.compiler)
}
