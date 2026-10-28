package com.flora.app.automation

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.flora.app.data.FloraPreferences

/**
 * ContactResolver (v2.22, mega build chunk 2) — naam se kaun:
 * Maya se behtar tareeqa. Pehle wale ko chupke se chunne ke
 * bajay Flora poochhti hai, aur jawab yaad rakhti hai.
 *
 * Order: seekha hua mapping → exact match → ek match →
 * kai matches (poochho) → koi nahi (poochho).
 * Seekha hua mapping sirf phone par rehta hai (DataStore),
 * kabhi server/backup par nahi jata.
 */
object ContactResolver {

    sealed class Result {
        data class Resolved(val name: String, val number: String) : Result()
        data class Ambiguous(val candidates: List<ContactLookup.ContactMatch>) : Result()
        object NotFound : Result()
        object NoPermission : Result()
    }

    suspend fun resolve(context: Context, prefs: FloraPreferences, name: String): Result {
        val clean = name.trim()
        if (clean.isEmpty()) return Result.NotFound
        val learned = try { prefs.learnedContactNumber(clean) } catch (e: Exception) { "" }
        if (learned.isNotEmpty()) {
            return Result.Resolved(clean, learned)
        }
        if (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.NoPermission
        }
        val all = ContactLookup.findAll(context, clean)
        if (all.isEmpty()) return Result.NotFound
        val exact = all.filter { it.name.equals(clean, ignoreCase = true) }
        if (exact.size == 1) {
            return Result.Resolved(exact[0].name, exact[0].number)
        }
        if (all.size == 1) {
            return Result.Resolved(all[0].name, all[0].number)
        }
        // Exact kai hon ya partial kai hon — dono me poochhna hi sahi hai.
        return Result.Ambiguous(all)
    }
}
