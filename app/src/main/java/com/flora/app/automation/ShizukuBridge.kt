package com.flora.app.automation

import android.content.pm.PackageManager
import android.os.Parcel
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper

/**
 * Shizuku bridge (v2.22, G19 "impossible → possible").
 *
 * Android kisi app ko khud apni system-level permissions nahi dene deta.
 * Shizuku ek user-chalayi service hai (Shizuku app + wireless debugging,
 * root NAHI) jiske through permission grant ho sakta hai — sirf tab jab
 * user ne Shizuku khud chalaya ho aur Flora ko Shizuku permission di ho.
 * Reboot par Shizuku band ho jata hai; user kabhi bhi rok sakta hai.
 *
 * Raasta (Shizuku API 13.1.5 ke VERIFIED public methods se — artifact
 * khud padh kar nikala gaya, kyunki Shizuku.newProcess is version me
 * private hai): SystemServiceHelper se package service ka binder lo,
 * grantRuntimePermission ka transaction code + interface-token wala
 * Parcel lo, args likho, transact karo. Ye wahi kaam hai jo
 * `adb shell pm grant` karta hai — sirf Shizuku ke through.
 *
 * Har call try/catch me hai: Shizuku app na ho, service band ho, ya
 * permission na mile — to false/status, kabhi crash nahi.
 */
object ShizukuBridge {

    /** Shizuku service zinda hai ya nahi. */
    fun isReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    /** Shizuku ne Flora ko apni permission di hai ya nahi. */
    fun hasPermission(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }

    /**
     * Ek runtime permission grant karo (pm grant ke barabar).
     * Sirf tab jab isReady + hasPermission. Sirf normal runtime
     * permissions (Notification/Camera/Mic) ke liye use hota hai.
     * @return true jab grant transact bina exception ke poora ho.
     */
    fun grantPermission(pkg: String, perm: String): Boolean {
        if (!hasPermission()) return false
        return try {
            val binder = SystemServiceHelper.getSystemService("package") ?: return false
            val code = SystemServiceHelper.getTransactionCode(
                "android.content.pm.IPackageManager", "grantRuntimePermission"
            ) ?: return false
            val data = SystemServiceHelper.obtainParcel(
                "package", "android.content.pm.IPackageManager", "grantRuntimePermission"
            )
            val reply = Parcel.obtain()
            try {
                data.writeString(pkg)
                data.writeString(perm)
                data.writeInt(android.os.Process.myUid() / 100000)
                binder.transact(code, data, reply, 0)
                reply.readException()
                true
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (e: Exception) {
            false
        }
    }

    /** UI + voice ke liye saaf status text (koi technical jargon nahi). */
    fun statusText(): String = try {
        when {
            !Shizuku.pingBinder() ->
                "Shizuku: band hai — Shizuku app se chalao, phir yahan wapas aao."
            !hasPermission() ->
                "Shizuku: chal raha hai, par Flora ko permission nahi mili — 'Shizuku permission do' dabao."
            else ->
                "Shizuku: taiyaar hai — ab permission ek tap me mil sakti hai."
        }
    } catch (e: Exception) {
        "Shizuku: pata nahi chala — Shizuku app installed hai ya nahi, check karo."
    }

    /** Shizuku ki apni permission dialog kholo (user ka tap zaroori). */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            // Shizuku app nahi hai ya service band — status text batayega.
        }
    }
}
