plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    // AGP release packaging needs a dotted namespace; Kotlin package remains zxingcpp.
    namespace = "zxingcpp.lib"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        minSdk = 26

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += listOf(
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DANDROID_ARM_NEON=ON",
                    "-DZXING_WRITERS=OFF",
                )
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        consumerProguardFiles(
            "../third_party/zxing-cpp/wrappers/android/zxingcpp/consumer-rules.pro",
        )
    }

    externalNativeBuild {
        cmake {
            path = file("../third_party/zxing-cpp/wrappers/android/zxingcpp/src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Vendored Kotlin API (package zxingcpp, class BarcodeReader) from the submodule.
    sourceSets["main"].java.srcDirs(
        "../third_party/zxing-cpp/wrappers/android/zxingcpp/src/main/java",
    )
}

dependencies {
    // BarcodeReader.read(ImageProxy) needs CameraX ImageProxy.
    implementation(libs.androidx.camera.core)
}
