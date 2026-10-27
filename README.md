# Maya v5.2.44 — Rebuild Skeleton (jadx result aane par bhara jayega)

Ye skeleton sirf structure hai — abhi isme koi decompiled code copy nahi kiya gaya hai.
Jadx/apktool ka result aane ke baad har module ko behaviour samajhke dobara likha jayega.

## Modules (manifest + components se)
- app: MainActivity, MayaApplication
- ui: BrowserActivity, FaceLockActivity, WatchmanActivity, UnlockActivity,
  ScreenCapturePermissionActivity
- assistant: MayaAssistActivity, MayaVoiceInteractionService,
  MayaVoiceInteractionSessionService, MayaRecognitionService
- automation: MayaAccessibilityService, MayaNotificationListener
- core: MayaForegroundService, BootReceiver, PhoneStateReceiver, DrivingOffReceiver,
  MicCaptureReceiver, ReminderReceiver
- data: Room + DataStore (entities jadx se confirm karni hain — abhi guess mat likhna)
- termux: TermuxPermissionActivity, TermuxResultReceiver
- wallpaper: MayaWallpaperService
- social/marketing: SocialAlarmReceiver, MarketingAlarmReceiver

## Tech (recovered stack se)
- Kotlin + Jetpack Compose + Material3, min SDK 26, target SDK 34
- Room, DataStore, WorkManager, CameraX, MLKit barcode
- Wakeword TFLite (hello_maya, hey_maya, wake_up_maya), onnxruntime, tensorflowlite
- Native: hunterstt, pockettts, turndetector (JNI wrappers dobara banane honge)

## Rules (rebuild ke liye)
1. Koi purani API key dobara use nahi hogi — nayi keys banao, purani revoke karo.
2. Obfuscated jadx output ko seedha copy-paste mat karo — behaviour samajhke dobara likho.
3. Har module: verify → test → audit, tabhi agla module.
4. Backup: original APK + decompiled output + ye rebuilt source, teeno alag folder me.
