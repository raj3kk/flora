package com.flora.app.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NoAccess Batch A — DeviceActions.match() pure-logic tests.
 * match() me koi Android API nahi hai, isliye plain JUnit kaafi hai
 * (Robolectric nahi chahiye). CI par ye tests chalte hain ya nahi,
 * usse azaad — match logic ka galat-hijack proof yahi hai.
 */
class DeviceMatchTest {

    @Test
    fun timeMatches() {
        assertTrue(DeviceActions.match("time batao") is DeviceActions.DeviceRequest.TimeInfo)
        assertTrue(DeviceActions.match("kitne baje") is DeviceActions.DeviceRequest.TimeInfo)
    }

    @Test
    fun dateMatches() {
        assertTrue(DeviceActions.match("aaj kaun sa din") is DeviceActions.DeviceRequest.DateInfo)
    }

    @Test
    fun batteryMatches() {
        assertTrue(DeviceActions.match("battery kitni hai") is DeviceActions.DeviceRequest.BatteryInfo)
    }

    @Test
    fun alarmBeforeTime() {
        val r = DeviceActions.match("alarm lagao subah 7 baje")
        assertTrue(r is DeviceActions.DeviceRequest.Alarm)
        assertEquals(7, (r as DeviceActions.DeviceRequest.Alarm).hour)
        assertEquals(0, r.minute)
    }

    @Test
    fun timerMinutes() {
        val r = DeviceActions.match("timer lagao 5 minute")
        assertTrue(r is DeviceActions.DeviceRequest.Timer)
        assertEquals(300, (r as DeviceActions.DeviceRequest.Timer).seconds)
    }

    @Test
    fun dialDigits() {
        val r = DeviceActions.match("dial karo 9876543210")
        assertTrue(r is DeviceActions.DeviceRequest.Dial)
        assertEquals("9876543210", (r as DeviceActions.DeviceRequest.Dial).number)
        assertTrue(DeviceActions.match("dialer kholo") is DeviceActions.DeviceRequest.Dial)
    }

    @Test
    fun bareWordsDoNotMatch() {
        assertNull(DeviceActions.match("torch"))
        assertNull(DeviceActions.match("volume"))
        assertNull(DeviceActions.match("battery"))
        assertNull(DeviceActions.match("network"))
    }

    @Test
    fun torchMatches() {
        assertTrue(DeviceActions.match("torch on karo") is DeviceActions.DeviceRequest.TorchOn)
        assertTrue(DeviceActions.match("torch jalao") is DeviceActions.DeviceRequest.TorchOn)
        assertTrue(DeviceActions.match("torch band karo") is DeviceActions.DeviceRequest.TorchOff)
        assertTrue(DeviceActions.match("flashlight off") is DeviceActions.DeviceRequest.TorchOff)
    }

    @Test
    fun torchStatusBeforeOnOff() {
        assertTrue(DeviceActions.match("torch on hai") is DeviceActions.DeviceRequest.TorchStatus)
        assertTrue(DeviceActions.match("torch status") is DeviceActions.DeviceRequest.TorchStatus)
    }

    @Test
    fun volumeMatches() {
        assertTrue(DeviceActions.match("volume badhao") is DeviceActions.DeviceRequest.VolumeUp)
        assertTrue(DeviceActions.match("volume kam karo") is DeviceActions.DeviceRequest.VolumeDown)
        val set = DeviceActions.match("volume 50 karo")
        assertTrue(set is DeviceActions.DeviceRequest.VolumeSet)
        assertEquals(50, (set as DeviceActions.DeviceRequest.VolumeSet).percent)
        val alarmSet = DeviceActions.match("alarm volume 30")
        assertTrue(alarmSet is DeviceActions.DeviceRequest.VolumeSet)
        assertEquals(30, (alarmSet as DeviceActions.DeviceRequest.VolumeSet).percent)
        val ringRead = DeviceActions.match("ring volume kitna")
        assertTrue(ringRead is DeviceActions.DeviceRequest.VolumeRead)
        assertTrue(DeviceActions.match("volume kitna") is DeviceActions.DeviceRequest.VolumeRead)
        val clamped = DeviceActions.match("volume 150")
        assertEquals(100, (clamped as DeviceActions.DeviceRequest.VolumeSet).percent)
    }

    @Test
    fun ringerAndSilentMatch() {
        assertTrue(DeviceActions.match("silent karo") is DeviceActions.DeviceRequest.Silent)
        assertTrue(DeviceActions.match("vibrate karo") is DeviceActions.DeviceRequest.RingerVibrate)
        assertTrue(DeviceActions.match("ringer normal") is DeviceActions.DeviceRequest.RingerNormal)
        assertTrue(DeviceActions.match("ringer mode kya hai") is DeviceActions.DeviceRequest.RingerRead)
    }

    @Test
    fun noHijackOfOtherRoutes() {
        assertNull(DeviceActions.match("whatsapp kholo"))
        assertNull(DeviceActions.match("aaj ka mausam kaisa hai"))
        assertNull(DeviceActions.match("plan banao"))
    }
}
