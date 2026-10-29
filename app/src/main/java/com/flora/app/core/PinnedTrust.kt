package com.flora.app.core

import java.net.URL
import java.net.URLConnection
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * G17 — Flora ke apne domains ki certificate pinning (Phase 8).
 *
 * Pin = leaf certificate ke public key (SPKI) ka SHA-256, base64.
 * Live certs se liya gaya (2026-10-29, openssl s_client):
 *  - keomyweghrmhqglaxmyv.supabase.co leaf: B9zUXavotYYIs217h2OZnCqCTp2vB/2pNC1zDijcIAo=
 *    (leaf expiry 22 Jan 2027; issuer Google WE1)
 *  - flora-henna-alpha.vercel.app leaf: xRUwi70J41GFIbtkDY38VXIAoWtbsGGG+LWfenCZh7k=
 *    (leaf expiry 27 Nov 2026; issuer Google WR1)
 *  - Dono chains ka shared Google intermediate: 47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=
 *
 * Check: pehle system ka default TrustManager chain validate karta
 * hai (expiry, CA trust, hostname verifier alag se chalta hai — use
 * chheda nahi gaya), phir chain ke KISI cert ka SPKI pin set me
 * hona chahiye (leaf ya intermediate). Leaf rotate ho to
 * intermediate pin kaam karta rahega; intermediate badle to pin
 * refresh ke saath app update chahiye (rotation plan docs me:
 * docs/FLORA_G17_PINNING.md). Pin fail = CertificateException =
 * call fail — kabhi silent unpinned fallback NAHI.
 *
 * Scope: sirf Flora ke apne domains (Supabase + Vercel). Doosre
 * hosts (Google, Deepgram, search providers) par ye helper plain
 * connection deta hai — unka default system trust hi chalta hai.
 * OkHttp CertificatePinner isliye NAHI use hua: Flora-domain
 * traffic HttpURLConnection par chalta hai (OkHttp sirf
 * DeepgramStreamClient me hai, jo Flora domain nahi hai) —
 * CertificatePinner un paths ko cover hi nahi karta.
 */
object PinnedTrust {

    private val PINS: Map<String, Set<String>> = mapOf(
        "keomyweghrmhqglaxmyv.supabase.co" to setOf(
            "B9zUXavotYYIs217h2OZnCqCTp2vB/2pNC1zDijcIAo=",
            "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
        ),
        "flora-henna-alpha.vercel.app" to setOf(
            "xRUwi70J41GFIbtkDY38VXIAoWtbsGGG+LWfenCZh7k=",
            "47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU="
        )
    )

    private val factoryCache = mutableMapOf<Set<String>, SSLSocketFactory>()

    fun openConnection(url: URL): URLConnection {
        val conn = url.openConnection()
        if (conn is HttpsURLConnection) {
            val pins = PINS[url.host]
            if (pins != null) {
                conn.sslSocketFactory = pinnedFactory(pins)
            }
        }
        return conn
    }

    private fun pinnedFactory(pins: Set<String>): SSLSocketFactory {
        synchronized(factoryCache) {
            factoryCache[pins]?.let { return it }
        }
        val tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        tmf.init(null as KeyStore?)
        val defaultTm = tmf.trustManagers
            .filterIsInstance<X509TrustManager>().first()
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>, authType: String
            ) {
                defaultTm.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>, authType: String
            ) {
                // Step 1: system chain validation (CA trust + expiry).
                defaultTm.checkServerTrusted(chain, authType)
                // Step 2: SPKI pin — leaf ya intermediate me se koi
                // ek pin set me hona chahiye, warna MITM maan kar fail.
                val ok = chain.any { cert ->
                    val spki = MessageDigest.getInstance("SHA-256")
                        .digest(cert.publicKey.encoded)
                    android.util.Base64.encodeToString(
                        spki, android.util.Base64.NO_WRAP
                    ) in pins
                }
                if (!ok) {
                    throw CertificateException(
                        "Flora pin check fail: " +
                            (chain.firstOrNull()?.subjectX500Principal?.name ?: "?")
                    )
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                defaultTm.acceptedIssuers
        }
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(tm), null)
        val factory = ctx.socketFactory
        synchronized(factoryCache) {
            factoryCache[pins] = factory
        }
        return factory
    }
}
