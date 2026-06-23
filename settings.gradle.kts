pluginManagement {
    repositories {
        maven { url = uri("file://${System.getProperty("user.home")}/local-maven-repo") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("file://${System.getProperty("user.home")}/local-maven-repo") }
        google()
        mavenCentral()
    }
}
rootProject.name = "RouletteWatchApp"
include(":app")
