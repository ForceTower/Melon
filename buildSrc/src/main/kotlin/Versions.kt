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

object Versions {
    object Android {
        const val compileSdkVersion = 28
        const val buildToolsVersion = "28.0.3"
        const val minSdkVersion = 21
        const val targetSdkVersion = 28
    }

    const val gradleBuildTool = "3.3.0-rc03"
    const val kotlin = "1.3.11"
    const val googleServices = "4.2.0"
    const val ktlint = "0.15.0"
    const val ktlintGradle = "3.0.0"
    const val fabricGradleTool = "1.26.1"
    const val playPublisher = "2.0.0"

    // Kotlin Extensions
    const val coreKtx = "1.0.1"

    // Draw
    const val exifInterface = "1.0.0"
    const val pallete = "1.0.0"

    // Android X / (Old Support Library)
    const val annotation = "1.0.1"
    const val appCompat = "1.0.2"
    const val constraintLayout = "1.1.3"
    const val googleMaterial = "1.0.0"
    const val flexbox = "1.1.0"
    const val browser = "1.0.0"
    const val preference = "1.0.0"

    // Android Architecture
    const val paging = "2.0.0"
    const val room = "2.0.0"
    const val lifecycle = "2.0.0"
    const val navigation = "1.0.0-alpha09"
    const val workManager = "1.0.0-beta01"

    // Dagger
    const val dagger = "2.20"

    // Firebase
    const val firebaseCore = "16.0.6"
    const val firebaseAuth = "16.1.0"
    const val firebaseMessaging = "17.3.4"
    const val firebaseFirestore = "17.1.5"
    const val firebaseStorage = "16.0.5"
    const val firebaseUiStorage = "4.2.0"


    const val crashlytics = "2.9.8@aar"
    const val timber = "4.7.1"
    const val glide = "4.8.0"
    const val lottie = "2.8.0"

    const val billingClient = "1.2"

    object GooglePlayServices {
        const val auth = "16.0.1"
        const val games = "16.0.0"
        const val location = "16.0.0"
    }

    object Network {
        const val okhttp = "3.11.0"
        const val retrofit = "2.4.0"
        const val cookieJar = "v1.0.1"
    }

    object Common {
        const val gson = "2.8.5"
        const val jsoup = "1.11.3"
        const val chartView = "v3.0.3"
        const val aboutLibraries = "6.2.0"
        const val easyPermissions = "0.3.0"
        const val cardSlider = "0.3.0"
        const val imageCropper = "2.8.0"
    }
}