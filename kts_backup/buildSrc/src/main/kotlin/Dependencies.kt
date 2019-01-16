/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

object Dependencies {
    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
        const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
        const val annotations = "androidx.annotation:annotation:${Versions.annotation}"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
        const val material = "com.google.android.material:material:${Versions.googleMaterial}"
        const val pallete = "androidx.palette:palette:${Versions.pallete}"
        const val browser = "androidx.browser:browser:${Versions.browser}"
        const val preference = "androidx.preference:preference:${Versions.preference}"
        const val flexbox = "com.google.android:flexbox:${Versions.flexbox}"
    }

    object Architecture {
        const val lifecycleExtensions = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
        const val lifecycleJava8 = "androidx.lifecycle:lifecycle-common-java8:${Versions.lifecycle}"

        const val navigationFragment = "android.arch.navigation:navigation-fragment:${Versions.navigation}"
        const val navigationUi = "android.arch.navigation:navigation-ui:${Versions.navigation}"
        const val navigationFragmentKtx = "android.arch.navigation:navigation-fragment-ktx:${Versions.navigation}"
        const val navigationUiKtx = "android.arch.navigation:navigation-ui-ktx:${Versions.navigation}"

        const val paging = "androidx.paging:paging-runtime:${Versions.paging}"

        const val room = "androidx.room:room-runtime:${Versions.room}"
        const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"

        const val workManager = "android.arch.work:work-runtime-ktx:${Versions.workManager}"
    }

    object Dagger {
        const val core = "com.google.dagger:dagger:${Versions.dagger}"
        const val compiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"
        const val android = "com.google.dagger:dagger-android:${Versions.dagger}"
        const val androidSupport = "com.google.dagger:dagger-android-support:${Versions.dagger}"
        const val androidProcessor =  "com.google.dagger:dagger-android-processor:${Versions.dagger}"
    }

    object Firebase {
        const val core = "com.google.firebase:firebase-core:${Versions.firebaseCore}"
        const val auth = "com.google.firebase:firebase-auth:${Versions.firebaseAuth}"
        const val messaging = "com.google.firebase:firebase-messaging:${Versions.firebaseMessaging}"
        const val firestore = "com.google.firebase:firebase-firestore:${Versions.firebaseFirestore}"
        const val storage = "com.google.firebase:firebase-storage:${Versions.firebaseStorage}"
        const val config = "com.google.firebase:firebase-config:${Versions.firebaseRemoteConfig}"

        object UI {
            const val storage = "com.firebaseui:firebase-ui-storage:${Versions.firebaseUiStorage}"
        }
    }

    object Crashlytics {
        const val core = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}"
    }

    object GooglePlayServices {
        const val games = "com.google.android.gms:play-services-games:${Versions.GooglePlayServices.games}"
        const val auth = "com.google.android.gms:play-services-auth:${Versions.GooglePlayServices.auth}"
        const val location = "com.google.android.gms:play-services-location:${Versions.GooglePlayServices.location}"
    }

    object Billing {
        const val client = "com.android.billingclient:billing:${Versions.billingClient}"
    }

    object Network {
        const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.Network.okhttp}"
        const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.Network.retrofit}"
        const val retrofitGson = "com.squareup.retrofit2:converter-gson:${Versions.Network.retrofit}"
        const val cookieJar = "com.github.franmontiel:PersistentCookieJar:${Versions.Network.cookieJar}"
    }

    object Glide {
        const val core = "com.github.bumptech.glide:glide:${Versions.glide}"
        const val compiler = "com.github.bumptech.glide:compiler:${Versions.glide}"
    }

    object Common {
        const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
        const val gson = "com.google.code.gson:gson:${Versions.Common.gson}"
        const val lottie = "com.airbnb.android:lottie:${Versions.lottie}"

        const val jsoup = "org.jsoup:jsoup:${Versions.Common.jsoup}"
        const val chartView = "com.github.PhilJay:MPAndroidChart:${Versions.Common.chartView}"
        const val aboutLibraries = "com.mikepenz:aboutlibraries:${Versions.Common.aboutLibraries}"
        const val easyPermissions = "pub.devrel:easypermissions:${Versions.Common.easyPermissions}"
        const val cardSlider = "com.ramotion.cardslider:card-slider:${Versions.Common.cardSlider}"
        const val imageCropper = "com.theartofdev.edmodo:android-image-cropper:${Versions.Common.imageCropper}"
    }

}