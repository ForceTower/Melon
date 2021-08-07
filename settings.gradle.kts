dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Noct"
include(":app")
include(":commons:storage")
include(":commons:design")
include(":feature:auth")
