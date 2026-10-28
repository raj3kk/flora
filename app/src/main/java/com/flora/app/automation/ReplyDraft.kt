package com.flora.app.automation

import android.content.Context
import com.flora.app.assistant.BrainClient
import com.flora.app.data.NotificationStore

/**
 * Phase 6 — Auto-reply DESIGN (ask-first, kabhi silent nahi).
 *
 * User order 04:03 IST: notification ka message + pehle ke
 * 5-6 messages padh kar situation samajhna, phir us hisab se
 * reply. Ye design silent auto-reply se BILKUL alag hai (wo
 * refused item hai): yahan Flora sirf DRAFT banati hai —
 * thread (aakhri 6, purane se naye tak) brain ko de kar —
 * aur draft user ko sunati + chat me likhti hai. Bhejna sirf
 * tab hoga jab user confirm dialog me Haan dabaya ho, aur
 * WhatsApp par bhi aakhri Send user khud dabata hai (wa.me
 * prefilled path). WhatsApp ke bahar kisi app me Flora direct
 * reply nahi bhej sakti — wahan sirf draft milega, imaandaari
 * se bata kar.
 *
 * NotificationStore me zyada se zyada 20 items hote hain (sab
 * apps milakar), isliye kisi app ke 6 se kam mile to jo mile
 * wahi context hai — 6 ka daava kabhi nahi kiya jayega.
 */
data class ReplyThread(
    val pkg: String,
    val senderTitle: String,
    val lines: List<String>,
)

sealed class DraftResult {
    data class Ready(val thread: ReplyThread, val draft: String) : DraftResult()
    object Empty : DraftResult()
}

object ReplyDraft {

    suspend fun buildDraft(context: Context, pkg: String, brain: BrainClient): DraftResult {
        val all = try {
            NotificationStore.load(context)
        } catch (e: Exception) {
            emptyList()
        }
        val threadItems = all.filter { it.pkg == pkg }.takeLast(6)
        if (threadItems.isEmpty()) return DraftResult.Empty
        val sender = threadItems.last().title.ifBlank { threadItems.last().text }
        val lines = threadItems.map { item ->
            val t = if (item.title.isNotBlank()) item.title else item.text
            "$t: ${item.text}"
        }
        val history = lines.map { BrainClient.ChatTurn(false, it) }
        val draft = try {
            brain.ask(
                "Upar wali baatchit ke hisab se ek chhota, vinamra jawab likho — " +
                    "sirf jawab, koi vyakhya ya heading nahi, user ki bhasha me.",
                "",
                history
            ).trim()
        } catch (e: Exception) {
            ""
        }
        return DraftResult.Ready(ReplyThread(pkg, sender, lines), draft)
    }
}
