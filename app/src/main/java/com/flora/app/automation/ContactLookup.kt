package com.flora.app.automation

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Contacts lookup (v2.13): naam se number dhoondhna — "call Maa" ya
 * "sms Rahul hello" ke liye. Sirf tab chalta hai jab user ne
 * READ_CONTACTS permission di ho. Koi contact data kabhi save,
 * log, backup ya server par nahi jata — number sirf usi waqt ke
 * confirmation dialog me use hota hai, phir bhool jaya jata hai.
 */
object ContactLookup {

    suspend fun findNumber(context: Context, name: String): String? =
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext null
            }
            val clean = name.trim()
            if (clean.isEmpty()) return@withContext null
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
                val selection =
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?"
                val args = arrayOf("%$clean%")
                context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                    var firstNumber: String? = null
                    val nameIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    val numberIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    while (cursor.moveToNext()) {
                        val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                        val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                        if (number.isBlank()) continue
                        if (firstNumber == null) firstNumber = number
                        // Exact naam match ko pehli pasand do.
                        if (displayName.equals(clean, ignoreCase = true)) return@withContext number
                    }
                    firstNumber
                }
            } catch (e: Exception) {
                null
            }
        }
}
