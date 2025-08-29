pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // vagy FAIL_ON_PROJECT_REPOS, ha szigorú akarsz maradni
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "android_gemma"
include(":app")

