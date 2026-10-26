plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("javaToolchainVersion").get().toInt()))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // 17, not 21: this module is also consumed by the Android build and D8 is the
    // lowest common denominator. Language level stays modern, toolchain stays 21.
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-this-escape"))
}

dependencies {
    // `api` so :lwjgl3 and :android see the libGDX classes transitively.
    api(libs.gdx)
    // Text is rasterised from the Poppins TTFs at the device's density (see SkinFactory).
    api(libs.gdx.freetype)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "2g"
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
