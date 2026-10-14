plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    compileOnly(libs.android.lint.api)
    testImplementation(libs.android.lint.api)
    testImplementation(libs.android.lint.tests)
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    maxHeapSize = "2g"
}

tasks.processTestResources {
    from(tasks.named("testFixturesJar")) {
        rename { "compose-fixtures.jar" }
    }
}
