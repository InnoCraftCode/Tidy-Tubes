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

rootProject.name = "gametemplate"

include(":core")
include(":lwjgl3")
// The Android module needs an SDK path (local.properties or ANDROID_HOME). Pass -Pskip.android=true
// to build core + desktop on a machine without one.
if (providers.gradleProperty("skip.android").orNull != "true") {
    include(":android")
}
