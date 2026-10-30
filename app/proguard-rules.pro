# Phase 2 Item 2 — Release R8 keep rules (Flora, AGP 8.5.2 / R8 full mode).
#
# R8 reachability se seedha refer hua code apne aap bachta hai; yahan
# sirf REFLECTION se chhune wale entry points rakhe gaye hain. Koi
# blanket "-keep class com.flora.app.**" nahi hai — wahi dex shrink
# ka fayda marta. Runtime R8 verification phone par baaki hai
# (Phase 3); koi bhi naya reflection entry point yahan add hoga.
#
# Dynamic resource lookup (getIdentifier) aur Class.forName app code
# me kahin nahi hai (audit 2026-10-30) — isliye shrinkResources
# ke liye alag keep ki zaroorat nahi; R-referenced resources apne
# aap bachte hain, manifest-referenced bhi.

-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# JNI: native methods ke naam native taraf se aate hain.
-keepclasseswithmembernames class * {
    native <methods>;
}

# kotlinx-serialization (canonical rules): @Serializable classes ke
# serializer() companion se reflection se milte hain.
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclassmembers class ** {
    *** Companion;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer {
    *** INSTANCE;
}

# Room (planner/PlanRoom.kt): @Database impl + @Entity + @Dao.
# KSP-generated code inhe seedha refer karta hai, par Room ke
# reflection path ke liye explicit keep suraksha hai.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.flora.app.planner.PlanEntity { *; }
-keep class com.flora.app.planner.PlanRoom { *; }

# WorkManager: PlanResumeWorker class-name se instantiate hota hai
# (PlannerService: OneTimeWorkRequestBuilder<PlanResumeWorker>).
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class androidx.work.** { *; }

# App Startup: FloraToolsInitializer manifest metadata se milta hai.
-keep class * implements androidx.startup.Initializer { *; }

# MLKit / TFLite / ORT wrappers: Java taraf seedha refer hoti hai,
# par inke andar ki reflection + JNI boundary ke liye conservative keep.
# (.so size par iska koi asar nahi — wo abiFilters se ghat ta hai.)
-keep class com.google.mlkit.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class ai.onnxruntime.** { *; }

# Manifest entry points (Activity/Service/Receiver/Provider) R8
# apne aap rakhta hai; ye rules sirf double-suraksha hain.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
