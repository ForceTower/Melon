plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.agp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}
