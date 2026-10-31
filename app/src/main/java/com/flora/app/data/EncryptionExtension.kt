package com.flora.app.data

/**
 * P3-16 — Encryption extension (G18).
 *
 * Abhi sirf memories KeyStore-encrypted hain (MemoryCrypto).
 * Ye file baaki stores ki migration ka spec + state machine deti hai:
 *
 * - Scope: journal, docs, routines, jobs — user-flow me koi
 *   change nahi hoga (user ko kuch alag nahi karna padega).
 * - Encryption wahi MemoryCrypto (AndroidKeyStore AES/GCM) hai —
 *   nayi crypto invent nahi ki gayi; key kabhi file/server/backup
 *   me nahi jati.
 * - Fail-closed: encrypt fail par plaintext fallback KABHI nahi —
 *   wo entry plain hi rehti hai aur migration FAILED mark hota
 *   hai (aadha/corrupt data kabhi nahi likha jata). Data lose
 *   hone se encryption ka jhootha daava zyada khatarnak hai.
 * - Migration order: pehle backup, phir ek-ek store, har store
 *   ke baad verify, fail par turant roko + purana data barkaraar.
 */
object EncryptionExtension {

    enum class Store { MEMORIES, JOURNAL, DOCS, ROUTINES, JOBS }

    /** MEMORIES pehle se encrypted hai; baaki 4 is migration me aate hain. */
    val ALREADY_ENCRYPTED: Set<Store> = setOf(Store.MEMORIES)

    val MIGRATION_SCOPE: List<Store> = listOf(
        Store.JOURNAL, Store.DOCS, Store.ROUTINES, Store.JOBS
    )

    enum class MigrationState {
        NOT_STARTED, BACKED_UP, MIGRATING, DONE, FAILED_KEPT_ORIGINAL
    }

    enum class MigrationEvent { BACKUP_DONE, STORE_DONE, ALL_DONE, FAIL }

    /** Pure state machine — fail par original data hi rehta hai. */
    fun next(state: MigrationState, event: MigrationEvent): MigrationState {
        return when (event) {
            MigrationEvent.FAIL -> MigrationState.FAILED_KEPT_ORIGINAL
            MigrationEvent.BACKUP_DONE ->
                if (state == MigrationState.NOT_STARTED) MigrationState.BACKED_UP else state
            MigrationEvent.STORE_DONE ->
                if (state == MigrationState.BACKED_UP || state == MigrationState.MIGRATING) {
                    MigrationState.MIGRATING
                } else state
            MigrationEvent.ALL_DONE ->
                if (state == MigrationState.MIGRATING) MigrationState.DONE else state
        }
    }

    /** Migration ke kadam (user-flow change nahi). */
    val MIGRATION_STEPS: List<String> = listOf(
        "1. Migration se pehle poora backup lo (backup fail par migration shuru hi nahi)",
        "2. Ek store ek baar: plain padho → KeyStore se encrypt karo → verify karo → tabhi likho",
        "3. Encrypt fail par us store ko chhodo (plain barkaraar) + migration FAILED mark karo",
        "4. Saare stores verify hone par hi DONE — aadha DONE kabhi nahi",
        "5. User ko kuch nahi karna — app kholne par sab waisa hi milega, bas encrypted"
    )

    /**
     * App-only encode — MemoryCrypto ke through (AndroidKeyStore).
     * Fail par null (plaintext fallback kabhi nahi).
     */
    fun encodeForMigration(json: String): String? {
        return try {
            MemoryCrypto.encodeStored(json)
        } catch (e: Exception) {
            null
        }
    }
}
