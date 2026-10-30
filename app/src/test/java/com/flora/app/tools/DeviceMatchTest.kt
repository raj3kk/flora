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
    fun noHijackOfOtherRoutes() {
        assertNull(DeviceActions.match("whatsapp kholo"))
        assertNull(DeviceActions.match("aaj ka mausam kaisa hai"))
        assertNull(DeviceActions.match("plan banao"))
    }
}
