import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Count commits on the default branch so versionName = "1.0.<commits>".
// Uses providers.exec so the value is configuration-cache-safe.
// Falls back to 0 when git is unavailable (e.g. shallow CI checkout);
// versionCode is coerced to >= 1 below so Play never sees 0.
val commitCount: Int = providers.exec {
    commandLine("git", "rev-list", "--count", "origin/main")
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 0 }
    .orElse(0).get()


// AdMob ids. Debug builds use Google's sample ids, which only ever serve test
// ads: requesting live ads from a device you develop on is invalid traffic, and
// invalid traffic is what gets an AdMob account suspended.
val admobAppIdTest = "ca-app-pub-3940256099942544~3347511713"
val admobBannerTest = "ca-app-pub-3940256099942544/6300978111"
val admobAppIdLive = "ca-app-pub-6038890007283978~3972333313"
val admobBannerLive = "ca-app-pub-6038890007283978/8297491052"

// Release signing is read from keystore.properties (git-ignored) when present,
// so CI/dev builds without it still succeed (producing an unsigned release).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.gpsanywhere.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gpsanywhere.app"
        minSdk = 26
        targetSdk = 36
        versionCode = commitCount.coerceAtLeast(1)
        versionName = "1.0.$commitCount"

        manifestPlaceholders["admobAppId"] = admobAppIdTest
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"" + admobBannerTest + "\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            manifestPlaceholders["admobAppId"] = admobAppIdLive
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"" + admobBannerLive + "\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    bundle {
        // The app changes locale at runtime, so every installed bundle must
        // include all packaged language resources.
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // OSMDroid
    implementation(libs.osmdroid)

    // Gson
    implementation(libs.gson)

    // AdMob
    implementation(libs.play.services.ads)

    // Preferences
    implementation(libs.androidx.preference)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
