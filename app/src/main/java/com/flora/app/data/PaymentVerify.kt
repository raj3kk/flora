package com.flora.app.data

/**
 * P3-06 — Payment verification hardening, FLORA ROUTE ke mutabiq.
 *
 * Flora route UPI + admin UTR-match hai. Razorpay NOT configured
 * hai — isliye webhook/gateway route is file me NAHI hai; wo sirf
 * tab aayega jab gateway aaye (alag decision, invent nahi kiya).
 *
 * Ye file app-side hardening deti hai (admin-match ka faisla
 * server/admin ka hi rehta hai, ye use replace nahi karta):
 *
 * - UTR normalize: sirf digits; valid format = exactly 12 digits.
 * - Duplicate: jo UTR pehle submit ho chuka hai, wo dobara
 *   FIRST nahi banega — DUPLICATE milega.
 * - Replay: ek hi UTR ko alag-alag daave ke saath dobara pesh
 *   karna bhi duplicate hi hai (UTR apne aap me ek hi payment
 *   ka saboot hai; dobara use = replay).
 * - Concurrent approval: ek UTR par ek waqt me ek hi approval
 *   flow chalega — doosra [tryApprove] false payega.
 * - Receipt: UTR sirf last-4 ke saath dikhta hai, poora kabhi nahi.
 * - Payment auto-confirm kabhi nahi (exclusion rule).
 */
object PaymentVerify {

    enum class SubmitResult { FIRST, DUPLICATE }

    data class Receipt(
        val utrLast4: String,
        val plan: String,
        val status: String
    )

    fun normalizeUtr(raw: String): String = raw.filter { it.isDigit() }

    fun isValidUtrFormat(raw: String): Boolean {
        val digits = normalizeUtr(raw)
        return digits.length == 12
    }

    fun last4(raw: String): String {
        val digits = normalizeUtr(raw)
        return if (digits.length >= 4) digits.takeLast(4) else "****"
    }

    fun receiptFor(raw: String, plan: String, status: String): Receipt {
        return Receipt(utrLast4 = last4(raw), plan = plan, status = status)
    }
}

/** Ek device-session ka UTR ledger (server ledger iska replacement nahi). */
class UtrLedger {
    private val seen = mutableSetOf<String>()
    private val approving = mutableSetOf<String>()

    @Synchronized
    fun submit(raw: String): PaymentVerify.SubmitResult {
        if (!PaymentVerify.isValidUtrFormat(raw)) return PaymentVerify.SubmitResult.DUPLICATE
        val utr = PaymentVerify.normalizeUtr(raw)
        if (!seen.add(utr)) return PaymentVerify.SubmitResult.DUPLICATE
        return PaymentVerify.SubmitResult.FIRST
    }

    /** Ek UTR par ek waqt me ek hi approval — doosra false payega. */
    @Synchronized
    fun tryApprove(raw: String): Boolean {
        val utr = PaymentVerify.normalizeUtr(raw)
        if (utr in approving) return false
        approving.add(utr)
        return true
    }

    @Synchronized
    fun finishApprove(raw: String) {
        approving.remove(PaymentVerify.normalizeUtr(raw))
    }
}
