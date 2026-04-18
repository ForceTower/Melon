pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "melon"

include(":packages:shared-kmp:core:network")
include(":packages:shared-kmp:core:database")
include(":packages:shared-kmp:features:auth")
include(":packages:shared-kmp:features:dashboard")
include(":packages:shared-kmp:umbrella")
