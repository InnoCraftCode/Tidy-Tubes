plugins {
    application
}

val gdxVersion = libs.versions.gdx.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("javaToolchainVersion").get().toInt()))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.ben.laama.sortpuzzle.lwjgl3.Lwjgl3Launcher")
}

/**
 * Desktop and Android read the same asset folder, so a sound dropped in once is audible in both
 * builds. The game ships with none, but the wiring has to exist before it is useful.
 */
val assetsDir = rootProject.file("android/src/main/assets")

sourceSets {
    named("main") {
        resources.srcDir(assetsDir)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.lwjgl3)
    runtimeOnly("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    runtimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

    // ANGLE translates OpenGL ES to Direct3D, which gets a window on machines with no usable
    // OpenGL driver — Remote Desktop sessions and bare VMs, mostly. The launcher only reaches
    // for it after native OpenGL has failed. Windows and macOS only; it is a no-op on Linux.
    runtimeOnly("com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion")
}

tasks.named<JavaExec>("run") {
    // Gdx.files.internal resolves against the working directory first, so point it at the
    // shared assets folder; the classpath copy is the fallback for the packaged jar.
    setWorkingDir(assetsDir)
    isIgnoreExitValue = true
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

/**
 * Google Play captures (see StoreCapture):
 *   gradlew :lwjgl3:storeCapture                                   -> play-store/sortpuzzle/screenshots/phone/ (1080x1920)
 *   gradlew :lwjgl3:storeCapture -PcaptureDevice=tablet-7 -PcaptureSize=1200x1920   -> .../screenshots/tablet-7/
 *   gradlew :lwjgl3:storeCapture -PcaptureDevice=tablet-10 -PcaptureSize=1600x2560  -> .../screenshots/tablet-10/
 *   gradlew :lwjgl3:storeCapture -PcaptureMode=video               -> build/capture/frames/ (then make-promo-video.ps1)
 */
tasks.register<JavaExec>("storeCapture") {
    group = "distribution"
    description = "Renders the Play Store screenshots, or promo-video frames, from scripted play."
    mainClass.set("com.ben.laama.sortpuzzle.lwjgl3.StoreCapture")
    classpath = sourceSets.main.get().runtimeClasspath
    setWorkingDir(assetsDir)
    val mode = providers.gradleProperty("captureMode").getOrElse("screens")
    val device = providers.gradleProperty("captureDevice").getOrElse("phone")
    val size = providers.gradleProperty("captureSize").getOrElse("1080x1920")
    val out = if (mode == "video") {
        layout.buildDirectory.dir("capture/frames").get().asFile
    } else {
        rootProject.file("../../play-store/sortpuzzle/screenshots/$device")
    }
    args(mode, out.absolutePath, layout.buildDirectory.dir("capture/prefs").get().asFile.absolutePath, size)
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

/**
 * Self-contained runnable jar:  ./gradlew :lwjgl3:dist  ->  lwjgl3/build/libs/sortpuzzle-desktop.jar
 */
tasks.register<Jar>("dist") {
    group = "distribution"
    description = "Builds a single runnable jar containing the game and all of its dependencies."
    archiveBaseName.set("sortpuzzle-desktop")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = application.mainClass.get() }

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
}
