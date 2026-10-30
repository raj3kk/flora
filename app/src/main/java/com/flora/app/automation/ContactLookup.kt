package com.flora.app.automation

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
 *
 * v2.22 (mega build chunk 2): findAll — saare matches (exact
 * pehle), taaki Flora do ek-naam walon me se poochh sake,
 * pehle wale ko chupke se chunne ke bajay.
 */
object ContactLookup {

    data class ContactMatch(val name: String, val number: String)

    suspend fun findAll(context: Context, name: String): List<ContactMatch> =
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext emptyList()
            }
            val clean = name.trim()
            if (clean.isEmpty()) return@withContext emptyList()
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                )
                val selection =
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?"
                val args = arrayOf("%$clean%")
                val found = mutableListOf<ContactMatch>()
                val seen = mutableSetOf<String>()
                context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                    val numberIndex = cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    while (cursor.moveToNext()) {
                        val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                        val number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""
                        if (number.isBlank() || displayName.isBlank()) continue
                        if (!seen.add(number)) continue
                        found.add(ContactMatch(displayName, number))
                    }
                }
                // Exact naam wale pehle, phir baaki (pehle mile order me).
                val exact = found.filter { it.name.equals(clean, ignoreCase = true) }
                val rest = found.filter { !it.name.equals(clean, ignoreCase = true) }
                exact + rest
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Number → naam (call announce v1.2): PhoneLookup se reverse
     * lookup. Sirf tab chalta hai jab READ_CONTACTS di ho. Naam sirf
     * usi waqt ke announce me use hoga — kabhi save/log/backup/
     * server par nahi jayega. Na mile to null (caller "unknown").
     */
    suspend fun lookupNameByNumber(context: Context, number: String): String? =
        withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext null
            }
            val clean = number.trim()
            if (clean.isEmpty()) return@withContext null
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(clean),
                )
                val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(
                            ContactsContract.PhoneLookup.DISPLAY_NAME
                        )
                        val name = if (idx >= 0) cursor.getString(idx) ?: "" else ""
                        if (name.isNotBlank()) return@withContext name
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

}
