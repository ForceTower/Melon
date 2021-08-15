// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.38.1")
    }
}

plugins {
    id("com.diffplug.spotless") version "5.13.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots") }
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    val ktLintVersion = "0.42.1"
    spotless {
        kotlin {
            target("**/*.kt")
            targetExclude("$buildDir/**/*.kt", "bin/**/*.kt")
            ktlint(ktLintVersion)

            licenseHeaderFile(project.rootProject.file("copyright.kt"))
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(ktLintVersion)
            licenseHeaderFile(project.rootProject.file("copyright.kt"), "(plugins |import |include)")
        }
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
        kotlinOptions {
            // Treat all Kotlin warnings as errors
            allWarningsAsErrors = true

            // Enable experimental coroutines APIs, including Flow
            val compilerArgs = listOf<String>(
//                "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
//                "-Xopt-in=kotlinx.coroutines.FlowPreview",
//                "-Xopt-in=kotlin.Experimental"
            )
            freeCompilerArgs = freeCompilerArgs + compilerArgs
            jvmTarget = "1.8"
        }
    }
}