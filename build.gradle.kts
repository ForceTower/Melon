plugins {
    base
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.licensee) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

val ktlint = configurations.create("ktlint") {
    isCanBeConsumed = false
}

dependencies {
    ktlint(libs.ktlint.cli) {
        // ktlint-cli also publishes a shaded variant; take the plain jar with its transitive dependencies.
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintIncludes = listOf("**/*.kt", "**/*.kts")
val ktlintExcludes = listOf(
    "**/build/**",
    "**/.gradle/**",
    "**/.kotlin/**",
    "**/node_modules/**",
    ".claude/**",
)
val ktlintArgs = listOf("--relative") + ktlintIncludes + ktlintExcludes.map { "!$it" }

val ktlintCheck = tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Checks Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    val report = layout.buildDirectory.file("reports/ktlint/ktlint.xml")
    inputs.files(
        fileTree(layout.projectDirectory) {
            include(ktlintIncludes + "**/.editorconfig")
            exclude(ktlintExcludes)
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(report)
    args(ktlintArgs + "--reporter=plain" + "--reporter=checkstyle,output=${report.get().asFile}")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Formats Kotlin code with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(listOf("--format") + ktlintArgs)
}

tasks.check {
    dependsOn(ktlintCheck)
}
