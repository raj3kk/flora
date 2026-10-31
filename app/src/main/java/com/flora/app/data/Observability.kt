package com.flora.app.data

/**
 * P3-17 — Observability: trace_id + metrics + alerts + playbooks.
 *
 * SCOPE NOTE (overlap rule): server-side functions ka code change
 * Part 2 team ka scope hai. Ye file SIRF app-side contract +
 * client hooks deti hai:
 *
 * - trace_id har action par banta hai (app → function → tool tak
 *   ek hi id chalegi — server is header ko aage badhayega, wo
 *   kaam Part 2 ka hai; yahan server code nahi chheda gaya).
 * - Metrics app-side counters hain (server metrics Part 2 ke hain).
 * - Alerts thresholds design numbers hain (alerting setup server
 *   side Part 2/ops ka hai — yahan sirf threshold + matlab darj hai).
 * - Playbooks 8 hain (plan P3-17 list) — kadam darj hain, koi
 *   auto-action nahi (koi silent third-party action nahi).
 *
 * Trace record me safe error code + status + timestamp hota hai —
 * chat text, secret, poora UTR kabhi nahi.
 */
object Observability {

    /** Server ko bheja jaane wala header naam (contract). */
    const val TRACE_HEADER = "x-flora-trace-id"

    data class TraceRecord(
        val traceId: String,
        val action: String,
        val tool: String?,
        val status: String,
        val errorCode: String?,
        val timestampMs: Long
    )

    /** Format: flora-<millis>-<6 random alphanumerics>. */
    fun newTraceId(nowMs: Long, randomSuffix: String): String {
        val clean = randomSuffix.filter { it.isLetterOrDigit() }.take(6)
        return "flora-$nowMs-$clean"
    }

    fun isValidTraceId(id: String): Boolean {
        return Regex("^flora-\\d+-[A-Za-z0-9]{6}$").matches(id)
    }

    /** App-side metric counters (server counters iske alag hain). */
    class MetricCounters {
        private val counts = mutableMapOf<String, Long>()

        @Synchronized
        fun increment(metric: String) {
            counts[metric] = (counts[metric] ?: 0L) + 1L
        }

        @Synchronized
        fun snapshot(): Map<String, Long> = counts.toMap()
    }

    /** Alert thresholds — design numbers, alerting setup Part 2/ops ka hai. */
    val ALERT_THRESHOLDS: Map<String, String> = mapOf(
        "http-5xx" to "5xx rate badhe to alert (server health ke saath)",
        "http-429" to "429 spike = rate-limit/quota dabav, alert",
        "provider-fail" to "koi provider lane lagatar fail ho to alert",
        "queue-backlog" to "queue backlog badhe to alert (koi silent queue nahi)",
        "fcm-fail" to "token-sync fail badhe to alert (delivery panel-side evidence)",
        "admin-activity" to "har admin action ka trace + audit note",
        "quota-anomaly" to "usage me achaanak uchhaal to alert",
        "crash-spike" to "crash count spike to alert + Catcher report check"
    )

    /** 8 playbooks (plan list) — sirf kadam, koi auto-action nahi. */
    val PLAYBOOKS: Map<String, List<String>> = mapOf(
        "provider-outage" to listOf(
            "Kaunsa provider fail hai, trace_id se pehchano",
            "Fallback lane par jao (system lane barkaraar rakho)",
            "User ko saaf message do, jhootha success kabhi nahi"
        ),
        "leaked-key" to listOf(
            "Key revoke karo, nayi key secure jagah par rakho (P3-13 steps)",
            "Purani key wale clients update karo, naya build do"
        ),
        "payment-dispute" to listOf(
            "UTR last-4 se payment dhoondho (poora UTR report me kabhi nahi)",
            "Admin-match record check karo, auto-confirm kabhi nahi"
        ),
        "bad-release" to listOf(
            "Rollback path chalao (P3-07, user-tap par)",
            "Kharab version ka Catcher/crash evidence jama karo"
        ),
        "migration-fail" to listOf(
            "Migration roko — original data barkaraar hai (P3-16 fail-closed)",
            "Kaunsa store fail hua, trace_id se pehchano, dobara backup ke baad try karo"
        ),
        "fcm-outage" to listOf(
            "Token-sync fail counts dekho (on-device delivery capture nahi hota)",
            "Panel-side evidence (ZP-F13) se confirm karo"
        ),
        "account-compromise" to listOf(
            "Session revoke karo, licence re-link karwao",
            "Admin activity traces check karo"
        ),
        "restore" to listOf(
            "Restore fail par original data hi rehta hai (P3-08 guard)",
            "Backup se dobara restore karo, verify ke baad hi adopt karo"
        )
    )
}
