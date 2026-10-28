package com.flora.app.automation

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Shizuku bridge (v2.22, G19 "impossible → possible").
 *
 * Android kisi app ko khud apni system-level permissions nahi dene deta.
 * Shizuku ek user-chalayi service hai (Shizuku app + wireless debugging,
 * root NAHI) jiske through `pm grant` chal sakta hai — sirf tab jab user
 * ne Shizuku khud chalaya ho aur Flora ko Shizuku permission di ho.
 * Reboot par Shizuku band ho jata hai; user kabhi bhi rok sakta hai.
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
     * Ek permission `pm grant` se do. Sirf tab jab isReady + hasPermission.
     * @return true jab pm grant exit code 0 de.
     */
    fun grantPermission(pkg: String, perm: String): Boolean {
        if (!hasPermission()) return false
        return try {
            val process = Shizuku.newProcess(
                arrayOf("pm", "grant", pkg, perm), null, null
            )
            process.waitFor()
            process.exitValue() == 0
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
