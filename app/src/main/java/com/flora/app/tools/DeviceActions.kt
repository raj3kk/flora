package com.flora.app.tools

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.PowerManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.flora.app.assistant.BatteryStateReader
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * NoAccess functions (user order 2026-10-30) — bina AccessibilityService
 * ke device functions. Spec: docs/FLORA_NOACCESS_FUNCTIONS_PLAN.md.
 *
 * Batch A scope: read-only info (time/battery/device/storage/RAM/
 * network/Wi-Fi state) + intent actions (alarm/timer/dial/settings).
 * Torch (Batch B) aur volume/ringer (Batch C) yahaan nahi hain — wo
 * apne batch me isi file me judenge (match + execute dono).
 *
 * Rules: sirf user ke chat/voice utterance se chalta hai (koi background
 * trigger nahi); har result SdResult me — fail par imaandaar wajah,
 * kabhi jhootha "ho gaya" nahi. Battery read yahaan NAHI hai —
 * BatteryStateReader (assistant/BatteryState.kt) hi ekmatra reader hai.
 */
object DeviceActions {

    sealed class DeviceRequest {
        object TimeInfo : DeviceRequest()
        object DateInfo : DeviceRequest()
        object BatteryInfo : DeviceRequest()
        object DeviceInfo : DeviceRequest()
        object StorageInfo : DeviceRequest()
        object RamInfo : DeviceRequest()
        object NetworkInfo : DeviceRequest()
        object WifiState : DeviceRequest()
        data class Alarm(val hour: Int?, val minute: Int?) : DeviceRequest()
        data class Timer(val seconds: Int?) : DeviceRequest()
        data class Dial(val number: String?) : DeviceRequest()
        object WifiSettings : DeviceRequest()
        object SoundSettings : DeviceRequest()
        object TorchOn : DeviceRequest()
        object TorchOff : DeviceRequest()
        object TorchStatus : DeviceRequest()
        data class VolumeRead(val stream: Int) : DeviceRequest()
        data class VolumeSet(val percent: Int, val stream: Int) : DeviceRequest()
        object VolumeUp : DeviceRequest()
        object VolumeDown : DeviceRequest()
        object RingerRead : DeviceRequest()
        object RingerNormal : DeviceRequest()
        object RingerVibrate : DeviceRequest()
        object Silent : DeviceRequest()
        object ThermalInfo : DeviceRequest()
        object ThermalStop : DeviceRequest()
    }

    /** Torch ka aakhri Flora-action (process-local). CameraManager me
     * torch state ka getter nahi hai — isliye status ka jawab sirf isi
     * se diya jayega; bahar se badla ho to "pata nahi" hi jawab hoga. */
    private var lastTorchState: Boolean? = null

    /**
     * Pure phrase matching (koi Android API nahi — unit test me chalti
     * hai). Order binding hai: intent actions (dial/settings/alarm/timer)
     * info phrases se PEHLE, warna "alarm lagao subah 7 baje" time me
     * gir jayega. Akele shabd ("torch", "volume", "battery") match
     * NAHI karte — ambiguous hai, brain par girne do.
     */
    fun match(lower: String): DeviceRequest? {
        val t = lower.trim()
        if (t.isEmpty()) return null
        // Intent actions pehle.
        if (t.contains("dial karo") || t.contains("dialer kholo") || t.contains("dial kholo")) {
            val digits = t.filter { it.isDigit() }
            return DeviceRequest.Dial(if (digits.length in 6..15) digits else null)
        }
        if (t.contains("wifi settings kholo") || t.contains("wifi setting kholo")) {
            return DeviceRequest.WifiSettings
        }
        if (t.contains("sound settings kholo") || t.contains("sound setting kholo")) {
            return DeviceRequest.SoundSettings
        }
        if (t.contains("alarm lagao") || t.contains("alarm laga")) {
            return DeviceRequest.Alarm(parseAlarmHour(t), parseAlarmMinute(t))
        }
        if (t.contains("timer lagao") || t.contains("timer laga")) {
            return DeviceRequest.Timer(parseTimerSeconds(t))
        }
        // Torch (Batch B) — status phrases on/off se PEHLE ("torch on hai"
        // me "torch on" bhi hai).
        if (t.contains("torch on hai") || t.contains("torch jal raha") ||
            t.contains("torch status")
        ) {
            return DeviceRequest.TorchStatus
        }
        if (t.contains("torch on") || t.contains("torch jalao") ||
            t.contains("flashlight on")
        ) {
            return DeviceRequest.TorchOn
        }
        if (t.contains("torch off") || t.contains("torch band") ||
            t.contains("flashlight off")
        ) {
            return DeviceRequest.TorchOff
        }
        // Volume + ringer (Batch C) — up/down/set/read order binding hai:
        // "volume badhao" me digits nahi hote, isliye up/down SET se pehle.
        if (t.contains("silent karo") || t.contains("silent kar do") ||
            t.contains("phone silent")
        ) {
            return DeviceRequest.Silent
        }
        if (t.contains("vibrate karo") || t.contains("vibration karo")) {
            return DeviceRequest.RingerVibrate
        }
        if (t.contains("ringer normal") || t.contains("normal mode karo")) {
            return DeviceRequest.RingerNormal
        }
        if (t.contains("ringer mode") || t.contains("ringer kya") ||
            t.contains("sound mode kya")
        ) {
            return DeviceRequest.RingerRead
        }
        if (t.contains("volume badhao") || t.contains("volume up") ||
            t.contains("awaz badhao")
        ) {
            return DeviceRequest.VolumeUp
        }
        if (t.contains("volume kam karo") || t.contains("volume ghatao") ||
            t.contains("volume down") || t.contains("awaz kam karo")
        ) {
            return DeviceRequest.VolumeDown
        }
        if (t.contains("volume")) {
            val digits = Regex("""(\d{1,3})""").find(t)?.groupValues?.get(1)?.toIntOrNull()
            if (digits != null) {
                return DeviceRequest.VolumeSet(digits.coerceIn(0, 100), volumeStream(t))
            }
        }
        // Read sirf specific patterns par — akela "volume" shabd match
        // NAHI karta (Batch A ka bare-word rule; Batch C ka generic
        // "volume" branch is rule ko tod raha tha, isliye hataya gaya).
        if (t.contains("volume kitna") || t.contains("volume kya hai") ||
            t.contains("media volume") || t.contains("alarm volume") ||
            t.contains("ring volume")
        ) {
            return DeviceRequest.VolumeRead(volumeStream(t))
        }
        // Thermal (Batch D) — stop phrase info se PEHLE ("thermal
        // throttle band karo" me "thermal" bhi hai).
        if (t.contains("thermal throttle band karo") || t.contains("throttle band karo")) {
            return DeviceRequest.ThermalStop
        }
        if (t.contains("phone garam") || t.contains("phone heat") ||
            t.contains("thermal") || t.contains("heat kitna") ||
            t.contains("phone ka temperature")
        ) {
            return DeviceRequest.ThermalInfo
        }
        // Info phrases.
        if (t.contains("time batao") || t.contains("time kya hai") ||
            t.contains("kitne baje") || t.contains("samay batao") ||
            t.contains("what is the time") || t.contains("tell me the time")
        ) {
            return DeviceRequest.TimeInfo
        }
        if (t.contains("date batao") || t.contains("aaj kaun sa din") ||
            t.contains("aaj date") || t.contains("aaj ka din")
        ) {
            return DeviceRequest.DateInfo
        }
        if (t.contains("battery kitni") || t.contains("battery percentage") ||
            t.contains("battery kitna") || t.contains("battery level") ||
            t.contains("battery status")
        ) {
            return DeviceRequest.BatteryInfo
        }
        if (t.contains("phone ka model") || t.contains("device info") ||
            t.contains("android version")
        ) {
            return DeviceRequest.DeviceInfo
        }
        if (t.contains("storage kitni") || t.contains("kitni jagah") ||
            t.contains("storage khaali")
        ) {
            return DeviceRequest.StorageInfo
        }
        if (t.contains("ram kitni") || t.contains("memory kitni")) {
            return DeviceRequest.RamInfo
        }
        if (t.contains("network kaun sa") || t.contains("internet kaun sa") ||
            t.contains("network type")
        ) {
            return DeviceRequest.NetworkInfo
        }
        if (t.contains("wifi on hai") || t.contains("wifi off hai") ||
            t.contains("wifi chalu hai") || t.contains("wifi band hai")
        ) {
            return DeviceRequest.WifiState
        }
        return null
    }

    /** "7 baje" + subah/shaam se hour (24h). Parse na ho to null. */
    private fun parseAlarmHour(t: String): Int? {
        val m = Regex("""(\d{1,2})\s*baje""").find(t) ?: return null
        var hour = m.groupValues[1].toIntOrNull() ?: return null
        if (hour !in 1..12) return null
        val pm = t.contains("raat") || t.contains("shaam") || t.contains("pm") ||
            t.contains("evening") || t.contains("night")
        if (pm && hour < 12) hour += 12
        if (t.contains("paune") && hour > 1) hour -= 1
        return hour
    }

    /** "saade" = :30, "paune" = :45, warna :00. */
    private fun parseAlarmMinute(t: String): Int? {
        if (!t.contains("baje")) return null
        return when {
            t.contains("saade") -> 30
            t.contains("paune") -> 45
            else -> 0
        }
    }

    /** "5 minute" / "30 second" / "1 ghanta" se seconds. Parse na ho to null. */
    private fun parseTimerSeconds(t: String): Int? {
        val m = Regex("""(\d+)\s*(minute|min|second|sec|ghanta|hour)""").find(t)
            ?: return null
        val n = m.groupValues[1].toIntOrNull() ?: return null
        return when (m.groupValues[2]) {
            "minute", "min" -> n * 60
            "second", "sec" -> n
            "ghanta", "hour" -> n * 3600
            else -> null
        }
    }

    fun execute(context: Context, req: DeviceRequest): SdResult<String> {
        return try {
            when (req) {
                is DeviceRequest.TimeInfo -> SdResult.Ok(timeReply(full = true))
                is DeviceRequest.DateInfo -> SdResult.Ok(timeReply(full = false))
                is DeviceRequest.BatteryInfo -> batteryReply(context)
                is DeviceRequest.DeviceInfo -> SdResult.Ok(
                    "Ye phone ${Build.MANUFACTURER} ${Build.MODEL} hai, " +
                        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) " +
                        "par chal raha hai.",
                )
                is DeviceRequest.StorageInfo -> storageReply(context)
                is DeviceRequest.RamInfo -> ramReply(context)
                is DeviceRequest.NetworkInfo -> networkReply(context)
                is DeviceRequest.WifiState -> wifiStateReply(context)
                is DeviceRequest.Alarm -> alarmReply(context, req)
                is DeviceRequest.Timer -> timerReply(context, req)
                is DeviceRequest.Dial -> dialReply(context, req)
                is DeviceRequest.TorchOn -> setTorch(context, true)
                is DeviceRequest.TorchOff -> setTorch(context, false)
                is DeviceRequest.TorchStatus -> torchStatus()
                is DeviceRequest.VolumeRead -> volumeRead(context, req.stream)
                is DeviceRequest.VolumeSet -> volumeSet(context, req.stream, req.percent)
                is DeviceRequest.VolumeUp -> volumeAdjust(context, AudioManager.ADJUST_RAISE)
                is DeviceRequest.VolumeDown -> volumeAdjust(context, AudioManager.ADJUST_LOWER)
                is DeviceRequest.RingerRead -> ringerRead(context)
                is DeviceRequest.RingerNormal -> ringerSet(context, AudioManager.RINGER_MODE_NORMAL)
                is DeviceRequest.RingerVibrate -> ringerSet(context, AudioManager.RINGER_MODE_VIBRATE)
                is DeviceRequest.Silent -> silentAction(context)
                is DeviceRequest.ThermalInfo -> thermalInfo(context)
                is DeviceRequest.ThermalStop -> thermalStop(context)
                is DeviceRequest.WifiSettings -> intentReply(
                    context,
                    Intent(Settings.ACTION_WIFI_SETTINGS),
                    "Wi-Fi settings khol raha hun.",
                )
                is DeviceRequest.SoundSettings -> intentReply(
                    context,
                    Intent(Settings.ACTION_SOUND_SETTINGS),
                    "Sound settings khol raha hun.",
                )
            }
        } catch (e: Exception) {
            SdResult.Failure("Device action me dikkat aayi — dobara koshish karo.")
        }
    }

    private fun timeReply(full: Boolean): String {
        val now = LocalDateTime.now()
        val dayEn = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val dayHi = when (now.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Somvaar"
            java.time.DayOfWeek.TUESDAY -> "Mangalvaar"
            java.time.DayOfWeek.WEDNESDAY -> "Budhvaar"
            java.time.DayOfWeek.THURSDAY -> "Guruvaar"
            java.time.DayOfWeek.FRIDAY -> "Shukravaar"
            java.time.DayOfWeek.SATURDAY -> "Shanivaar"
            java.time.DayOfWeek.SUNDAY -> "Ravivaar"
        }
        val monthEn = now.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val dateStr = "${now.dayOfMonth} $monthEn ${now.year}"
        if (!full) return "Aaj $dayEn ($dayHi), $dateStr hai."
        val h = now.hour
        val hour12 = if (h % 12 == 0) 12 else h % 12
        val amPm = if (h < 12) "AM" else "PM"
        val qualifier = when {
            h < 12 -> "subah"
            h < 17 -> "dopahar"
            h < 21 -> "shaam"
            else -> "raat"
        }
        val min = now.minute.toString().padStart(2, '0')
        return "Abhi $hour12:$min $amPm ho raha hai ($qualifier ka waqt) — " +
            "aaj $dayEn ($dayHi), $dateStr hai."
    }

    private fun batteryReply(context: Context): SdResult<String> {
        val state = BatteryStateReader.read(context)
        if (state.percent < 0) {
            return SdResult.Failure("Battery state padh nahi paya — dobara koshish karo.")
        }
        val chargingText = if (state.charging) "charger laga hua hai" else "charger nahi laga hai"
        return SdResult.Ok("Battery ${state.percent}% hai, $chargingText.")
    }

    private fun gb(bytes: Long): String {
        return "%.1f".format(Locale.ENGLISH, bytes / 1073741824.0)
    }

    private fun storageReply(context: Context): SdResult<String> {
        val dir = context.filesDir
            ?: return SdResult.Failure("Storage padh nahi paya — dobara koshish karo.")
        val stat = StatFs(dir.path)
        return SdResult.Ok(
            "Storage me ${gb(stat.availableBytes)} GB khaali hai, " +
                "kul ${gb(stat.totalBytes)} GB me se.",
        )
    }

    private fun ramReply(context: Context): SdResult<String> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return SdResult.Failure("RAM state padh nahi paya — dobara koshish karo.")
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return SdResult.Ok(
            "RAM me ${gb(mi.availMem)} khaali hai, kul ${gb(mi.totalMem)} me se.",
        )
    }

    private fun networkReply(context: Context): SdResult<String> {
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return SdResult.Failure("Network state padh nahi paya — dobara koshish karo.")
        val net = cm.activeNetwork
            ?: return SdResult.Ok("Koi network connected nahi hai.")
        val caps = cm.getNetworkCapabilities(net)
            ?: return SdResult.Ok("Network connected hai, par type saaf nahi padh paya.")
        return SdResult.Ok(
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi se connected hai."
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data se connected hai."
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cable (Ethernet) se connected hai."
                else -> "Kisi network se connected hai (type pata nahi chala)."
            },
        )
    }

    private fun wifiStateReply(context: Context): SdResult<String> {
        val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return SdResult.Failure("Wi-Fi state padh nahi paya — dobara koshish karo.")
        return SdResult.Ok(if (wm.isWifiEnabled) "Wi-Fi ON hai." else "Wi-Fi OFF hai.")
    }

    private fun intentReply(context: Context, intent: Intent, okText: String): SdResult<String> {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            SdResult.Ok(okText)
        } catch (e: Exception) {
            SdResult.Failure("Wo screen khol nahi paya — dobara koshish karo.")
        }
    }

    private fun alarmReply(context: Context, req: DeviceRequest.Alarm): SdResult<String> {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        if (req.hour != null) {
            intent.putExtra(AlarmClock.EXTRA_HOUR, req.hour)
            intent.putExtra(AlarmClock.EXTRA_MINUTES, req.minute ?: 0)
        }
        val text = if (req.hour != null) {
            "Alarm lagane ke liye clock app khol raha hun — wahan time confirm karke set kar dena."
        } else {
            "Clock app khol raha hun — alarm ka time wahan khud bhar dena."
        }
        return intentReply(context, intent, text)
    }

    private fun timerReply(context: Context, req: DeviceRequest.Timer): SdResult<String> {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
        if (req.seconds != null) {
            intent.putExtra(AlarmClock.EXTRA_LENGTH, req.seconds)
        }
        val text = if (req.seconds != null) {
            "Timer lagane ke liye clock app khol raha hun — wahan confirm kar dena."
        } else {
            "Clock app khol raha hun — timer ka time wahan khud bhar dena."
        }
        return intentReply(context, intent, text)
    }

    private fun dialReply(context: Context, req: DeviceRequest.Dial): SdResult<String> {
        val intent = Intent(Intent.ACTION_DIAL)
        if (req.number != null) {
            intent.data = Uri.parse("tel:${req.number}")
        }
        val text = if (req.number != null) {
            "Dialer khol raha hun — call button tum dabana, Flora khud call nahi lagayegi."
        } else {
            "Dialer khol raha hun — number khud daal dena, call tumhare haath se lagegi."
        }
        return intentReply(context, intent, text)
    }

    /** Batch B — torch. Fail-closed: permission / flash / camera-access
     * har kami par imaandaar message, kabhi jhootha "jal gaya" nahi. */
    private fun setTorch(context: Context, on: Boolean): SdResult<String> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SdResult.Failure(
                "Torch ke liye Camera permission chahiye — Permissions screen par " +
                    "Camera ON karo, phir dobara bolo.",
            )
        }
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return SdResult.Failure("Camera service nahi mila — dobara koshish karo.")
        val cameraId = try {
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                facing == CameraCharacteristics.LENS_FACING_BACK && hasFlash
            }
        } catch (e: Exception) {
            null
        } ?: return SdResult.Failure("Is phone me flash/torch nahi hai.")
        return try {
            cm.setTorchMode(cameraId, on)
            lastTorchState = on
            SdResult.Ok(if (on) "Torch ON kar diya hai." else "Torch OFF kar diya hai.")
        } catch (e: SecurityException) {
            SdResult.Failure(
                "Torch ke liye Camera permission chahiye — Permissions screen par " +
                    "Camera ON karo, phir dobara bolo.",
            )
        } catch (e: Exception) {
            SdResult.Failure(
                "Torch chal nahi paya — camera kisi aur app me khula ho sakta hai, " +
                    "us app ko band karke dobara koshish karo.",
            )
        }
    }

    private fun torchStatus(): SdResult<String> {
        val state = lastTorchState
            ?: return SdResult.Ok(
                "Torch ki haalat Flora ko pata nahi hai — is baar maine torch " +
                    "chalaya hi nahi hai. 'Torch on karo' bolo to jala dungi.",
            )
        return SdResult.Ok(
            "Torch ${if (state) "ON" else "OFF"} hai " +
                "(aakhri baar Flora ne hi ${if (state) "jalaya" else "band kiya"} tha).",
        )
    }

    /** Batch C — volume/ringer. MODIFY_AUDIO_SETTINGS normal permission
     * Batch A me manifest me jud chuka hai; DND sirf grant-gated hai. */
    private fun audioManager(context: Context): AudioManager? {
        return context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private fun volumeStream(t: String): Int {
        return when {
            t.contains("alarm") -> AudioManager.STREAM_ALARM
            t.contains("ring") -> AudioManager.STREAM_RING
            else -> AudioManager.STREAM_MUSIC
        }
    }

    private fun streamName(stream: Int): String {
        return when (stream) {
            AudioManager.STREAM_ALARM -> "Alarm"
            AudioManager.STREAM_RING -> "Ring"
            else -> "Media"
        }
    }

    private fun volumePercent(am: AudioManager, stream: Int): Int {
        val max = am.getStreamMaxVolume(stream)
        if (max <= 0) return -1
        return am.getStreamVolume(stream) * 100 / max
    }

    private fun volumeRead(context: Context, stream: Int): SdResult<String> {
        val am = audioManager(context)
            ?: return SdResult.Failure("Volume padh nahi paya — dobara koshish karo.")
        val pct = volumePercent(am, stream)
        if (pct < 0) return SdResult.Failure("Volume padh nahi paya — dobara koshish karo.")
        return SdResult.Ok("${streamName(stream)} volume $pct% hai.")
    }

    private fun volumeSet(context: Context, stream: Int, percent: Int): SdResult<String> {
        val am = audioManager(context)
            ?: return SdResult.Failure("Volume set nahi ho paya — dobara koshish karo.")
        return try {
            val max = am.getStreamMaxVolume(stream)
            if (max <= 0) return SdResult.Failure("Volume set nahi ho paya — dobara koshish karo.")
            am.setStreamVolume(stream, percent * max / 100, AudioManager.FLAG_SHOW_UI)
            val pct = volumePercent(am, stream)
            SdResult.Ok("${streamName(stream)} volume $pct% kar diya hai.")
        } catch (e: SecurityException) {
            SdResult.Failure("Volume set nahi ho paya — system ne rok diya.")
        } catch (e: Exception) {
            SdResult.Failure("Volume set nahi ho paya — dobara koshish karo.")
        }
    }

    private fun volumeAdjust(context: Context, direction: Int): SdResult<String> {
        val am = audioManager(context)
            ?: return SdResult.Failure("Volume badal nahi paya — dobara koshish karo.")
        return try {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val pct = volumePercent(am, AudioManager.STREAM_MUSIC)
            SdResult.Ok("Media volume $pct% ho gaya hai.")
        } catch (e: Exception) {
            SdResult.Failure("Volume badal nahi paya — dobara koshish karo.")
        }
    }

    private fun ringerModeName(mode: Int): String {
        return when (mode) {
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            else -> "Normal"
        }
    }

    private fun ringerRead(context: Context): SdResult<String> {
        val am = audioManager(context)
            ?: return SdResult.Failure("Ringer mode padh nahi paya — dobara koshish karo.")
        return SdResult.Ok("Ringer mode: ${ringerModeName(am.ringerMode)} hai.")
    }

    private fun ringerSet(context: Context, mode: Int): SdResult<String> {
        val am = audioManager(context)
            ?: return SdResult.Failure("Ringer mode badal nahi paya — dobara koshish karo.")
        return try {
            am.setRingerMode(mode)
            SdResult.Ok("Ringer mode: ${ringerModeName(am.ringerMode)} kar diya hai.")
        } catch (e: SecurityException) {
            SdResult.Failure("Ringer mode badal nahi paya — system ne rok diya.")
        } catch (e: Exception) {
            SdResult.Failure("Ringer mode badal nahi paya — dobara koshish karo.")
        }
    }

    /** Silent grant-gated: policy access mile to silent, warna bypass
     * kabhi nahi — settings page kholo (page khulna hi is step ka Ok
     * hai), user khud allow karega, phir dobara bole. */
    private fun silentAction(context: Context): SdResult<String> {
        audioManager(context)
            ?: return SdResult.Failure("Silent nahi kar paya — dobara koshish karo.")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            return ringerSet(context, AudioManager.RINGER_MODE_SILENT)
        }
        return intentReply(
            context,
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
            "Silent ke liye Do Not Disturb access chahiye — settings page khol " +
                "raha hun, wahan Flora ko allow karo, phir dobara 'silent karo' bolo.",
        )
    }

    /** Batch D — thermal detect + manage. Status enum OS ka hai (API
     * 29+); battery temp sensor ka hai (tenths °C) — exact SoC temp
     * ka daava kabhi nahi. MODERATE tak sirf salah; SEVERE+ par
     * visible cooling mode (notification + Auto Log + stop). */
    private fun thermalInfo(context: Context): SdResult<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SdResult.Ok(
                "Is Android version par thermal status nahi milta (thermal API " +
                    "Android 10+ me hai). Battery % alag se poochh sakte ho.",
            )
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return SdResult.Failure("Thermal status padh nahi paya — dobara koshish karo.")
        val status = try {
            pm.currentThermalStatus
        } catch (e: Exception) {
            -1
        }
        val state = BatteryStateReader.read(context)
        val tempText = state.tempTenths?.let { t ->
            val whole = t / 10
            val frac = kotlin.math.abs(t % 10)
            " Battery sensor ke mutabiq $whole.$frac°C hai."
        } ?: ""
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE ->
                SdResult.Ok("Phone ka thermal status normal hai.$tempText")
            PowerManager.THERMAL_STATUS_LIGHT ->
                SdResult.Ok(
                    "Phone halka garam hai.$tempText Bhaari kaam thoda kam rakhna behtar rahega.",
                )
            PowerManager.THERMAL_STATUS_MODERATE ->
                SdResult.Ok(
                    "Phone garam hai.$tempText Suggestion: bhaari apps band karo, charging " +
                        "par game mat khelo, cover hatayo to thanda hone me madad milegi. " +
                        "Cooling mode abhi ON nahi hua hai.",
                )
            PowerManager.THERMAL_STATUS_SEVERE,
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                val word = when (status) {
                    PowerManager.THERMAL_STATUS_SEVERE -> "bahut garam"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "critical garam"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency par"
                    else -> "shutdown ke kareeb"
                }
                if (ThermalThrottle.isThrottled()) {
                    SdResult.Ok(
                        "Phone $word hai.$tempText Cooling mode pehle se ON hai — viewing " +
                            "pause hai, brightness kam hai. Thanda hote hi apne aap resume ho jayega.",
                    )
                } else {
                    ThermalThrottle.engage(context, word)
                    SdResult.Ok(
                        "Phone $word hai.$tempText Cooling mode ON kar raha hun: live viewing " +
                            "pause ho gayi hai, apni window ki brightness kam ho gayi hai. Thanda " +
                            "hote hi apne aap resume ho jayega; band karne ke liye " +
                            "'thermal throttle band karo' bolo.",
                    )
                }
            }
            else -> SdResult.Failure("Thermal status padh nahi paya — dobara koshish karo.")
        }
    }

    private fun thermalStop(context: Context): SdResult<String> {
        if (!ThermalThrottle.isThrottled()) {
            return SdResult.Ok("Cooling mode ON hi nahi hai — kuch band karne ko nahi hai.")
        }
        ThermalThrottle.disengage(context, "user-stop")
        return SdResult.Ok(
            "Cooling mode band kar diya hai — viewing resume ho gayi hai, " +
                "brightness wapas normal hai.",
        )
    }
}
