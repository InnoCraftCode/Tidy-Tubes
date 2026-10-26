plugins {
    base
}

allprojects {
    group = "com.example.gametemplate"
    version = "1.0.0"
}

/** `gradlew play` == `gradlew :lwjgl3:run` */
tasks.register("play") {
    group = "application"
    description = "Runs the desktop (LWJGL3) build of the game."
    dependsOn(":lwjgl3:run")
}
