plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    // B4: Room + WorkManager compilers ke liye KSP — Kotlin 1.9.24
    // ke saath AGP 8.5.2-compatible pin (1.9.24-1.0.20).
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}
