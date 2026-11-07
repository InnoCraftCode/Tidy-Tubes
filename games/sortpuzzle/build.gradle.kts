plugins {
    base
    // All three on the ROOT build classpath (apply false): the Google services and Crashlytics
    // plugins link against AGP classes, so AGP must be loaded by the same classloader or they
    // fail with "com/android/build/api/variant/Variant". :android applies AGP always and the
    // other two only when android/google-services.json exists.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

allprojects {
    group = "com.ben.laama"
    version = "1.2.1"
}

/**
 * Convenience: `./gradlew play` == `./gradlew :lwjgl3:run`
 */
tasks.register("play") {
    group = "application"
    description = "Runs the desktop (LWJGL3) build of the game."
    dependsOn(":lwjgl3:run")
}
