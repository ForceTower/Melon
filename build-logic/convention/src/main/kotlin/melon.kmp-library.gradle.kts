plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "dev.forcetower.unes.shared" +
            project.path.removePrefix(":packages:shared-kmp").replace(":", ".")
        compileSdk = 37
        minSdk = 28
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}
