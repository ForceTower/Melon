buildscript {
    repositories {
        google()
        jcenter()
        maven("https://maven.fabric.io/public")
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.gradleBuildTool}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
        classpath("com.google.gms:google-services:4.2.0")
        classpath("io.fabric.tools:gradle:1.26.1")
        classpath("org.jmailen.gradle:kotlinter-gradle:1.20.1")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
    }

    afterEvaluate  {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "10000", "-Xmaxwarns", "10000"))
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
