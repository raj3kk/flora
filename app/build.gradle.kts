plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.flora.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.flora.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 44
        versionName = "2.22.0-rebuild"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // Compose UI 1.6.8 + Kotlin 1.9.24 pair with compiler 1.5.14.
        // (1.6.14 is not a valid compose-compiler version for this stack.)
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // B2 Builder core — sirf ye ek dep: project metadata models ke liye.
    // Room (B4), WorkManager, Hilt, Retrofit, Timber, AppSearch,
    // Logging Interceptor B2 me jaanboojh kar NAHI jode gaye.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // B3-07 Logging Interceptor — version OkHttp se EXACT match
    // (4.12.0) hona hi compatibility rule hai; 4.12.0 Kotlin
    // 1.9.24 stack par canonical hai (B2 ke 1.7.3 serialization
    // wale sabak ke mutabiq version bump nahi kiya gaya).
    // Level.HEADERS + redactHeader hi use hota hai — BODY kabhi nahi.
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.firebase:firebase-messaging:24.0.0")
    // Shizuku (v2.22, G19): system permissions bina root ke, user ki
    // Shizuku app ke through — sirf jab user ne Shizuku chalaya ho.
    implementation("dev.rikka.shizuku:api:13.1.5")
    // B4 Planner — Room (plan/task state, F02) + WorkManager
    // (durable scheduling/resume ONLY, F03/M13 — live executor
    // nahi). Dono AGP 8.5.2 / Kotlin 1.9.24 stack par pinned.
    // Deliberately NAHI: lifecycle-runtime-ktx (v1.3 CI fail),
    // Hilt (manual constructor DI hi fallback hai, F15),
    // Retrofit (B3 OkHttp clients ka duplicate hota).
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    // B5 tools consolidation — Timber (planner-step logging,
    // B5-09) + App Startup (registry init ka single point,
    // B5-09) + AppSearch (unified memory index, B5-04).
    // Sab Apache-2.0, AGP 8.5.2 / Kotlin 1.9.24 stack par pinned.
    implementation("androidx.appsearch:appsearch:1.1.0")
    implementation("androidx.appsearch:appsearch-local-storage:1.1.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.startup:startup-runtime:1.1.1")
    // B5-11 test-only + debug-only — production/release APK me kabhi nahi.
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.google.truth:truth:1.4.4")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    // B2 unit tests — sirf test scope, production APK me nahi.
    testImplementation("junit:junit:4.13.2")
    // Google sign-in (v2.22, Phase 5, G14): sirf id_token
    // lene ke liye; Web OAuth client ID dashboard par
    // banana baaki hai (FloraConfig.GOOGLE_WEB_CLIENT_ID).
    implementation("com.google.android.gms:play-services-auth:21.0.0")
}
