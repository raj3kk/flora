package com.flora.app.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
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
    }

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
}
