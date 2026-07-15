plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

android {
    compileSdk = 37
    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    lint {
        warningsAsErrors = true
        abortOnError = true
        // Typos: false positives on base64 font-cert hashes and pt-BR copy.
        // The version checks flag upstream releases, not code issues.
        disable += setOf(
            "Typos",
            "NewerVersionAvailable",
            "GradleDependency",
            "AndroidGradlePluginVersion",
        )
    }
}
