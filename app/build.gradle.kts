plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")

    // ✅ FIX: add version
    id("com.google.gms.google-services") version "4.4.0"
}
android {
    namespace = "com.smartcity.citizen"

    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartcity.citizen"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // ⚠️ WARNING: Do NOT expose real API keys in production
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"AIzaSyARoQFY6hWcnERZP8oTq4Zcj-ems2CfxjU\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // 🔥 Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))

    // 🔹 Compose Core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")

    // 🔹 Material
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    // 🔹 Activity
    implementation("androidx.activity:activity-compose:1.8.2")

    // 🔹 Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 🔹 Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // 🔹 DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 🔹 Firebase (BoM)
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // 🔹 ML Kit
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // 🔹 Camera
    implementation("androidx.camera:camera-core:1.3.1")

    // 🔹 Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.4.0")
    // 🔹 Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 🔹 Coroutines (Play Services)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // 🔹 Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 🔹 Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // 🔹 Pager
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.remote.creation.core)

    // 🔹 Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}