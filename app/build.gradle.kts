plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma de release: se toma de variables de entorno (GitHub Actions) si existen.
val keystorePath: String? = System.getenv("DOWNLY_KEYSTORE_PATH")
val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath!!).exists()

android {
    namespace = "com.downly.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.downly.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("DOWNLY_VERSION_CODE") ?: "1").toInt()
        versionName = "1.0.${System.getenv("DOWNLY_VERSION_CODE") ?: "0"}"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Clave fija en el repo: así cada APK nuevo se instala encima del anterior sin desinstalar.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("DOWNLY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("DOWNLY_KEY_ALIAS")
                keyPassword = System.getenv("DOWNLY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKey) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    // Un APK por arquitectura (más ligero) + uno universal.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    packaging {
        // Requerido por yt-dlp/python embebido: las libs nativas deben extraerse.
        jniLibs { useLegacyPackaging = true }
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val ytdl = "0.18.1"

dependencies {
    // Motor de descarga (yt-dlp + ffmpeg para unir audio/video y convertir a MP3)
    implementation("io.github.junkfood02.youtubedl-android:library:$ytdl")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$ytdl")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("io.coil-kt:coil-compose:2.7.0")

    // Anuncios (AdMob). IDs en ads/AdConfig.kt
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}
