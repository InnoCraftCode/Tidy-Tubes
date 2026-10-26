pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "sortpuzzle"

include(":core")
include(":lwjgl3")
// Added in step 5. Requires an Android SDK path in local.properties.
if (file("android/build.gradle.kts").exists()) {
    include(":android")
}
