package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3-04 / P3-07 / P3-08 / P3-15 / P3-16 guards ke tests.
 * Har guard ka ek hi usool hai: fail par data/faisla surakshit rahe.
 */
class Part3GuardsTest {

    @Test
    fun deleteRefusedWithoutConfirmation() {
        assertFalse(DataDeleteExport.guardDelete(false).ok)
        assertTrue(DataDeleteExport.guardDelete(true).ok)
        assertTrue(DataDeleteExport.CHAT_MEMORY_EXEMPT)
        assertEquals(30, DataDeleteExport.RETENTION_DAYS)
    }

    @Test
    fun rollbackNeedsChannelAndPrevious() {
        assertFalse(UpdateRollback.channelReady("", ""))
        assertFalse(UpdateRollback.channelReady("https://x/y.apk", ""))
        assertTrue(UpdateRollback.channelReady("https://x/y.apk", "abc123"))
        assertFalse(UpdateRollback.isRollbackAvailable(null, 100L))
        val prev = UpdateRollback.PreviousStable(99L, "v1.0", "deadbeef12345678")
        assertTrue(UpdateRollback.isRollbackAvailable(prev, 100L))
        assertFalse(UpdateRollback.isRollbackAvailable(prev, 99L))
        assertEquals("12345678", UpdateRollback.shaShort(prev.sha256))
    }

    @Test
    fun restoreFailKeepsOriginal() {
        val guard = BackupRestoreGuard()
        assertFalse(guard.beginRestore(false))
        assertTrue(guard.originalKept())
        val guard2 = BackupRestoreGuard()
        guard2.markBackupTaken()
        assertTrue(guard2.beginRestore(true))
        assertEquals(
            BackupRestoreGuard.State.FAILED_KEPT_ORIGINAL,
            guard2.verifyRestored(false)
        )
        assertTrue(guard2.originalKept())
    }

    @Test
    fun releaseGateNeedsEveryBlockingItem() {
        val all = ReleaseChecklist.ITEMS.associate { it.id to true }
        assertTrue(ReleaseChecklist.gatePass(all))
        val oneMissing = all.toMutableMap().apply { put("rel-08", false) }
        assertFalse(ReleaseChecklist.gatePass(oneMissing))
        assertEquals(listOf("rel-08"), ReleaseChecklist.missing(oneMissing))
    }

    @Test
    fun encryptionMigrationFailKeepsOriginal() {
        assertEquals(
            EncryptionExtension.MigrationState.BACKED_UP,
            EncryptionExtension.next(
                EncryptionExtension.MigrationState.NOT_STARTED,
                EncryptionExtension.MigrationEvent.BACKUP_DONE
            )
        )
        assertEquals(
            EncryptionExtension.MigrationState.FAILED_KEPT_ORIGINAL,
            EncryptionExtension.next(
                EncryptionExtension.MigrationState.MIGRATING,
                EncryptionExtension.MigrationEvent.FAIL
            )
        )
        assertTrue(EncryptionExtension.MIGRATION_SCOPE.size == 4)
        assertTrue(EncryptionExtension.ALREADY_ENCRYPTED.contains(EncryptionExtension.Store.MEMORIES))
    }

    @Test
    fun offlinePolicyNeverSilentlyQueuesLlmWork() {
        assertEquals(
            OfflinePolicy.WorkClass.OFFLINE_OK,
            OfflinePolicy.classify("basic-talk")
        )
        assertEquals(
            OfflinePolicy.WorkClass.NEEDS_NET,
            OfflinePolicy.classify("brain-answer")
        )
        assertTrue(OfflinePolicy.offlineBlockedMessage("brain-answer").contains("queue me nahi"))
    }
}
