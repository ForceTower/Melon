package dev.forcetower.gradle.utils

import org.gradle.api.Project

fun Project.buildVersion(): Pair<Int, String> {
    try {
//        val tagCount = Integer.parseInt("git tag | wc -l".runCommand(project.rootDir).trim())
        val commitCount = Integer.parseInt("git rev-list --count HEAD".runCommand(project.rootDir).trim())
        val branch = "git rev-parse --abbrev-ref HEAD".runCommand(project.rootDir).trim()
        val lastTagCommit = "git rev-list --tags --max-count=1".runCommand(project.rootDir).trim()
        val lastTag = "git describe --tags $lastTagCommit".runCommand(project.rootDir).trim()
        val lastCommit = "git rev-parse HEAD".runCommand(project.rootDir).trim().substring(0, 7)

        val (majorStr, minorStr, patchStr) = lastTag.lowercase().replace('-', '.').split('.')

        var postfix = "main"
        var adder = 20000
        if (branch == "development") {
            adder = 30000
            postfix = "beta"
        } else if (branch.startsWith("feature/")) {
            adder = 30000
            postfix = branch.replace("feature/", "")
        }

        val (major, minor, patch) = listOf(majorStr, minorStr, patchStr.replace(Regex("[^0-9]"), "")).map { it.toInt() }

        val name = "${major}.${minor}.${patch}.${commitCount}.rev-${postfix} [${lastCommit}]"
        val code = (21 * 100000) + commitCount + adder
        return code to name
    } catch (ignored: Exception) {
        System.err.println("No git installed on the machine or not on a git repo. UNES will not automate version name and code")
        return 1 to "1.0.0-rev0.no.git"
    }
}