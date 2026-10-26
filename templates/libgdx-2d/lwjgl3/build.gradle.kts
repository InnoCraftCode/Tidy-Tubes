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
    mainClass.set("com.example.gametemplate.lwjgl3.Lwjgl3Launcher")
}

/** Desktop and Android read the same asset folder, so an asset dropped in once works in both. */
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
}

tasks.named<JavaExec>("run") {
    setWorkingDir(assetsDir)
    isIgnoreExitValue = true
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}
