package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentVerifyTest {

    @Test
    fun duplicateAndReplayAreRejected() {
        val ledger = UtrLedger()
        assertEquals(PaymentVerify.SubmitResult.FIRST, ledger.submit("123456789012"))
        assertEquals(PaymentVerify.SubmitResult.DUPLICATE, ledger.submit("123456789012"))
        // Spaces/dashes ke saath wahi UTR = replay, phir bhi duplicate.
        assertEquals(PaymentVerify.SubmitResult.DUPLICATE, ledger.submit("1234 5678 9012"))
        // Fixer L1: invalid format ab DUPLICATE nahi — alag INVALID.
        assertEquals(PaymentVerify.SubmitResult.INVALID, ledger.submit("12345"))
    }

    @Test
    fun invalidDoesNotPoisonLedger() {
        val ledger = UtrLedger()
        assertEquals(PaymentVerify.SubmitResult.INVALID, ledger.submit("abc"))
        assertEquals(PaymentVerify.SubmitResult.FIRST, ledger.submit("123456789012"))
    }

    @Test
    fun concurrentApprovalOnlyOnce() {
        val ledger = UtrLedger()
        assertTrue(ledger.tryApprove("123456789012"))
        assertFalse(ledger.tryApprove("123456789012"))
        ledger.finishApprove("123456789012")
        assertTrue(ledger.tryApprove("123456789012"))
    }

    @Test
    fun receiptShowsLast4Only() {
        val receipt = PaymentVerify.receiptFor("123456789012", "pro", "pending-admin-match")
        assertEquals("9012", receipt.utrLast4)
        assertFalse(receipt.toString().contains("123456789012"))
        assertTrue(PaymentVerify.isValidUtrFormat("123456789012"))
        assertFalse(PaymentVerify.isValidUtrFormat("12345"))
    }
}
