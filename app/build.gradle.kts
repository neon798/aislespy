plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.aislespy"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.aislespy"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.0-beta.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Conditional release signing: only when all four props/env vars are present.
    // Absent in CI → unsigned release (same as before). Never commit secrets.
    // Prefer -P gradle properties; fall back to env vars with the same names.
    val keystorePath = (findProperty("aislespy.keystore.path") as String?)
        ?: System.getenv("aislespy.keystore.path")
    val keystorePassword = (findProperty("aislespy.keystore.password") as String?)
        ?: System.getenv("aislespy.keystore.password")
    val keyAlias = (findProperty("aislespy.key.alias") as String?)
        ?: System.getenv("aislespy.key.alias")
    val keyPassword = (findProperty("aislespy.key.password") as String?)
        ?: System.getenv("aislespy.key.password")
    val releaseSigningReady = listOf(keystorePath, keystorePassword, keyAlias, keyPassword)
        .all { !it.isNullOrBlank() }

    if (releaseSigningReady) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // CameraX + FOSS barcode decode (no ML Kit / Play Services)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.zxing.cpp.android)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    // Room (history + product cache) — FOSS; no room-testing needed for pure JVM DAO fakes
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore (first-launch / onboarding flag) — FOSS
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
