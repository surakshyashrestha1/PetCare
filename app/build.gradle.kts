import java.util.Properties

plugins {
    alias(libs.plugins.android.application)

    // Firebase
    id("com.google.gms.google-services")
}

// =============================================================
// LOCAL.PROPERTIES
// Reads MAPTILER_API_KEY from local.properties
// =============================================================

val localProperties = Properties().apply {

    val localPropertiesFile =
        rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { inputStream ->
            load(inputStream)
        }
    }
}

val mapTilerApiKey =
    localProperties.getProperty("MAPTILER_API_KEY") ?: ""

// =============================================================
// ANDROID
// =============================================================

android {

    namespace = "com.example.petcare"

    compileSdk {
        version = release(37)
    }

    defaultConfig {

        applicationId = "com.example.petcare"

        // MapTiler SDK requires minimum API 26
        minSdk = 26

        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        // =====================================================
        // MAPTILER API KEY
        // Available in Kotlin as:
        // BuildConfig.MAPTILER_API_KEY
        // =====================================================

        buildConfigField(
            "String",
            "MAPTILER_API_KEY",
            "\"${mapTilerApiKey.replace("\"", "\\\"")}\""
        )
    }

    // =========================================================
    // BUILDCONFIG
    // =========================================================

    buildFeatures {
        buildConfig = true
    }

    // =========================================================
    // BUILD TYPES
    // =========================================================

    buildTypes {

        release {

            optimization {
                enable = false
            }
        }
    }

    // =========================================================
    // JAVA
    // =========================================================

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}

// =============================================================
// DEPENDENCIES
// =============================================================

dependencies {

    // =========================================================
    // MAPTILER
    // =========================================================

    implementation(
        "com.maptiler:maptiler-sdk-kotlin:2.0.0"
    )

    // =========================================================
    // FIREBASE BOM
    // =========================================================

    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.19.0"
        )
    )

    // =========================================================
    // FIREBASE AUTH
    // =========================================================

    implementation(
        "com.google.firebase:firebase-auth"
    )

    // =========================================================
    // FIRESTORE
    // =========================================================

    implementation(
        "com.google.firebase:firebase-firestore"
    )

    // =========================================================
    // FIREBASE STORAGE
    // =========================================================

    implementation(
        "com.google.firebase:firebase-storage"
    )

    // =========================================================
    // ANDROID
    // =========================================================

    implementation(
        libs.androidx.activity.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.androidx.constraintlayout
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.material
    )

    // =========================================================
    // TESTING
    // =========================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )
}