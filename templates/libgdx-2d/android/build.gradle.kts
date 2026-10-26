import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val gdxVersion = libs.versions.gdx.get()
val natives: Configuration by configurations.creating

fun loadProps(name: String) = Properties().apply {
    val file = rootProject.file(name)
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * Release signing: `keystore.properties` at the project root (see keystore.properties.example).
 * Without it the release build still succeeds, unsigned. Git-ignored — never commit a keystore.
 */
val keystoreProperties = loadProps("keystore.properties")
val hasSigningConfig = keystoreProperties.getProperty("storeFile") != null

/**
 * Ad configuration. Debug builds ALWAYS use Google's demo IDs. Release builds read production IDs
 * from `admob.properties` (see admob.properties.example); when it is missing, release ships with
 * ads disabled rather than with demo IDs. `-Pads.enabled=false` disables ads in any build.
 * https://developers.google.com/admob/android/test-ads
 */
val admob = loadProps("admob.properties")
val adsEnabled = (findProperty("ads.enabled") ?: "true").toString().toBoolean()
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val releaseAdsConfigured = admob.getProperty("admob.appId", "").isNotBlank()

fun quoted(s: String) = "\"" + s.replace("\"", "") + "\""

android {
    namespace = "com.example.gametemplate"
    compileSdk = (findProperty("android.compileSdk") ?: "35").toString().toInt()

    defaultConfig {
        applicationId = "com.example.gametemplate"
        minSdk = (findProperty("android.minSdk") ?: "24").toString().toInt()
        targetSdk = (findProperty("android.targetSdk") ?: "35").toString().toInt()
        versionCode = (findProperty("app.versionCode") ?: "1").toString().toInt()
        versionName = rootProject.version.toString()
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("libs")
            assets.srcDirs("src/main/assets")
        }
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        named("debug") {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
            buildConfigField("String", "ADMOB_APP_ID", quoted(testAppId))
            buildConfigField("String", "AD_UNIT_REWARDED", quoted(testRewardedId))
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", quoted(testInterstitialId))
            // Comma-separated hashed device IDs from logcat; only ever applied in debug.
            buildConfigField("String", "TEST_DEVICE_IDS", quoted(admob.getProperty("admob.testDeviceIds", "")))
        }
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            // UMP still reads the app ID from the manifest (Next-Gen takes it in code), so the entry
            // must exist even when ads are disabled; the demo ID is harmless as nothing is requested.
            manifestPlaceholders["admobAppId"] = admob.getProperty("admob.appId", testAppId)
            buildConfigField("boolean", "ADS_ENABLED", (adsEnabled && releaseAdsConfigured).toString())
            buildConfigField("String", "ADMOB_APP_ID", quoted(admob.getProperty("admob.appId", "")))
            buildConfigField("String", "AD_UNIT_REWARDED", quoted(admob.getProperty("admob.rewardedId", "")))
            buildConfigField("String", "AD_UNIT_INTERSTITIAL", quoted(admob.getProperty("admob.interstitialId", "")))
            buildConfigField("String", "TEST_DEVICE_IDS", quoted(""))
        }
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/robovm/ios/robovm.xml",
            "META-INF/DEPENDENCY",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.android)
    implementation(libs.gma.nextgen)
    implementation(libs.ump)

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

/** Unpacks libGDX's per-ABI native libraries into `libs/<abi>/`. */
tasks.register("copyAndroidNatives") {
    group = "build"
    description = "Extracts the libGDX native libraries for every supported ABI."
    val outputDir = layout.projectDirectory.dir("libs")
    val nativeJars = natives
    outputs.dir(outputDir)
    doLast {
        nativeJars.files.forEach { jar ->
            val abi = jar.nameWithoutExtension.substringAfterLast("natives-")
            copy {
                from(zipTree(jar)) { include("*.so") }
                into(outputDir.dir(abi))
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyAndroidNatives")
}

tasks.named<Delete>("clean") {
    delete(layout.projectDirectory.dir("libs"))
}

/** `gradlew :android:printAdConfig` — quick check before a release build. */
tasks.register("printAdConfig") {
    group = "help"
    description = "Prints which ad configuration each build type will use (no secrets involved)."
    doLast {
        println("ads.enabled=$adsEnabled  release admob.properties present=$releaseAdsConfigured")
    }
}
