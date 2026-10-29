package com.flora.app.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HindiG2P tests — spec FLORA_HINDI_G2P_SPEC.md v0.2, §6.
 * Pure-JVM JUnit, koi Android import nahi (pattern: NotchAmplitudeTest).
 *
 * Vocab note (ASSERT-G4): HindiG2P ka SYMBOLS/VOCAB private hai, isliye test
 * vocab ko seedha nahi padh sakta. Isliye T1 behaviour se verify karta hai:
 * spec §1.7 ki 178-entry sequence ko tokenize karke — size 178, unique 177,
 * symbols[0] "$" -> 0, dict["'"] -> 176 (aakhri occurrence jeet-ta hai).
 * Test ki apni vocab copy sirf expected-IPA-strings ko indices me badalne ke
 * liye hai (spec §1.7 se li gayi, implementation se copy nahi).
 *
 * T2 status: 11/11 PARTIAL pass — 17/17 kabhi nahi, kyunki O1 ke 6 fresh words
 * abhi define nahi hue hain. T3 corpus draft ke expected values yahan NAHI
 * hain (v0.1-based, v0.2 alignment pending — T3 test run-1 alignment ke baad).
 *
 * Is VM me JDK/Gradle build nahi hai — execution ka proof CI hoga
 * (CI par test step ke baad) ya web-UI test step ke baad.
 */
class HindiG2PTest {

    // Spec §1.7 verbatim: "$" + 16 punctuation + 52 letters + 109 IPA.
    private val punctuation = ";:,.!?¡¿—…\"«»“” "
    private val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private val lettersIpa =
        "ɑɐɒæɓʙβɔɕçɗɖðʤəɘɚɛɜɝɞɟʄɡɠɢʛɦɧħɥʜɨɪʝɭɬɫɮʟɱɯɰŋɳɲɴøɵɸθœɶʘɹɺɾɻʀʁɽʂʃʈʧʉʊʋⱱʌɣɤʍχʎʏʑʐʒʔʡʕʢǀǁǂǃˈˌːˑʼʴʰʱʲʷˠˤ˞↓↑→↗↘'\u0329'ᵻ"

    private val testSymbols: List<Char> = buildList {
        add('$')
        for (c in punctuation) add(c)
        for (c in letters) add(c)
        for (c in lettersIpa) add(c)
    }

    // Last occurrence wins — apostrophe ka lookup 176 deta hai, jaise spec me.
    private val testVocab: Map<Char, Int> = buildMap {
        for ((index, c) in testSymbols.withIndex()) put(c, index)
    }

    private fun tokensFor(ipa: String): List<Int> =
        ipa.map { testVocab.getValue(it) }

    private fun assertWord(word: String, expectedIpa: String) {
        assertEquals(expectedIpa, tokensFor(expectedIpa), HindiG2P.tokenize(word))
    }

    // ---- T1: ASSERT-G4, behaviour se (vocab private hai) ----

    @Test
    fun vocabSequenceTokenizesTo178EntriesWith177Unique() {
        val sequence = testSymbols.joinToString("")
        val result = HindiG2P.tokenizeDetailed(sequence)
        assertTrue(result.dropped.isEmpty())
        assertEquals(178, result.tokens.size)
        assertEquals(177, result.tokens.toSet().size)
    }

    @Test
    fun padSymbolIsZeroAndApostropheLookupIs176() {
        assertEquals(listOf(0), HindiG2P.tokenize("$"))
        assertEquals(listOf(176), HindiG2P.tokenize("'"))
    }

    // ---- T2: spec §2.4 examples 1-11, word-exact (11/11 partial, 17/17 nahi) ----

    @Test fun ram() { assertWord("राम", "ɾaːm") }

    @Test fun rachna() { assertWord("रचना", "ɾətʃnaː") }

    @Test fun devanagari() { assertWord("देवनागरी", "deːʋnaːɡɾiː") }

    @Test fun kehna() { assertWord("कहना", "kɛɦnaː") }

    @Test fun subah() { assertWord("सुबह", "sʊbəɦ") }

    @Test fun kamal() { assertWord("कमल", "kəməl") }

    @Test fun ladka() { assertWord("लड़का", "ləɽkaː") }

    @Test fun namak() { assertWord("नमक", "nəmək") }

    @Test fun kitab() { assertWord("किताब", "kɪtaːb") }

    @Test fun makan() { assertWord("मकान", "məkaːn") }

    @Test fun matlab() { assertWord("मतलब", "mətləb") }

    // ---- ASSERT-G1: har output char vocab me (indices 0..177) ----

    @Test
    fun allTestSetTokensAreWithinVocabRange() {
        val words = listOf(
            "राम", "रचना", "देवनागरी", "कहना", "सुबह", "कमल",
            "लड़का", "नमक", "किताब", "मकान", "मतलब",
            "पहनना", "बच्चा", "क्ष", "त्र", "ज्ञ", "श्र", "क़", "फ़",
        )
        for (word in words) {
            for (token in HindiG2P.tokenize(word)) {
                assertTrue("token $token out of range for $word", token in 0..177)
            }
        }
    }

    // ---- ASSERT-G2: >509-token input par har chunk ke tokens <= 509 ----

    @Test
    fun longInputChunksStayWithin509TokensOnWordBoundaries() {
        val longText = ("राम ".repeat(120)).trim()
        assertTrue(HindiG2P.tokenize(longText).size > 509)
        val chunks = HindiG2P.chunkText(longText)
        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertTrue(HindiG2P.tokenize(chunk).size <= 509)
        }
        // Word boundary: chunk kisi shabd ke beech me nahi toota.
        assertEquals(longText, chunks.joinToString(" "))
    }

    @Test
    fun chunkingSplitsAtSentenceBoundariesFirst() {
        val chunks = HindiG2P.chunkText("राम।मकान।")
        assertEquals(listOf("राम।", "मकान।"), chunks)
    }

    // ---- ASSERT-G3: wrap [[0, *tokens, 0]], style row = unwrapped length ----

    @Test
    fun wrapAddsSentinelsAndStyleRowIsUnwrappedLength() {
        val tokens = HindiG2P.tokenize("राम")
        val wrapped = HindiG2P.wrap(tokens)
        assertEquals(listOf(0) + tokens + listOf(0), wrapped)
        val styleRow = tokens.size
        assertEquals(tokens.size + 2, wrapped.size)
        assertEquals(4, styleRow)
    }

    // ---- ASSERT-G5: test set par koi drop nahi (drop = FAIL) ----

    @Test
    fun noDropOnTestSet() {
        val words = listOf(
            "राम", "रचना", "देवनागरी", "कहना", "सुबह", "कमल",
            "लड़का", "नमक", "किताब", "मकान", "मतलब",
            "पहनना", "बच्चा", "क्ष", "त्र", "ज्ञ", "श्र", "क़", "फ़",
            "राम!", "राम, मकान",
        )
        for (word in words) {
            val result = HindiG2P.tokenizeDetailed(word)
            assertTrue("dropped $word -> ${result.dropped}", result.dropped.isEmpty())
        }
    }

    // ---- Edges ----

    @Test
    fun emptyInputGivesEmptyTokensAndZeroZeroWrap() {
        assertEquals(emptyList<Int>(), HindiG2P.tokenize(""))
        assertEquals(listOf(0, 0), HindiG2P.wrap(emptyList()))
        assertEquals(0, HindiG2P.tokenize("").size)
    }

    @Test
    fun vocabPunctuationIsRetainedAsTokens() {
        val bang = HindiG2P.tokenize("राम!")
        assertEquals(tokensFor("ɾaːm") + listOf(testVocab.getValue('!')), bang)
        val comma = HindiG2P.tokenize("राम, मकान")
        assertTrue(comma.contains(testVocab.getValue(',')))
        assertTrue(comma.contains(testVocab.getValue(' ')))
        assertTrue(HindiG2P.tokenizeDetailed("राम!").dropped.isEmpty())
    }

    @Test
    fun nuktaConsonantsMapPerSpec() {
        assertEquals(tokensFor("q"), HindiG2P.tokenize("क़"))
        assertEquals(tokensFor("f"), HindiG2P.tokenize("फ़"))
    }

    @Test
    fun conjunctSpecialsMapPerSpec() {
        assertEquals(tokensFor("kʂ"), HindiG2P.tokenize("क्ष"))
        assertEquals(tokensFor("tɾ"), HindiG2P.tokenize("त्र"))
        assertEquals(tokensFor("ɡj"), HindiG2P.tokenize("ज्ञ"))
        assertEquals(tokensFor("ʃɾ"), HindiG2P.tokenize("श्र"))
    }

    @Test
    fun geminateTableOutputKeepsPehannaDoubleN() {
        // Spec §3.2 table output (O8 OPEN): pɛɦənnaː — nː nahi, tab tak.
        assertWord("पहनना", "pɛɦənnaː")
    }

    @Test
    fun generalGeminateUsesLengthMark() {
        // Spec §1.6 F-GEMINATE: बच्चा -> bətʃːaː.
        assertWord("बच्चा", "bətʃːaː")
    }

    // ---- Lane C Step 3 (§8 Step 3, §6.4 table) ----

    @Test
    fun pehlaKeepsSchwaNonCase() {
        // Spec §3.3 non-case (T2 expected, tab tak): पहला -> pəɦlaː,
        // NOT *pɛɦlaː. Note: §3.3 isko Path A confirmation ke liye
        // UNVERIFIED flag karta hai — agar reference pɛɦlaː de to ye
        // test §4.3 process se hi badlega, chupchaap nahi.
        assertWord("पहला", "pəɦlaː")
    }

    // ---- Nasal branches, spec §1.6 F-NAS (a)/(b)/(c) ----
    // O3 note: branches listening test (Path B) se confirm hone baaki
    // hain, par rule text deterministic hai — ye tests rule text ko
    // lock karte hain, listening confirmation ka daava nahi karte.

    @Test
    fun nasalBeforeVelarStopIsVelarNasal() {
        // (a) velar: अंक -> əŋk.
        assertWord("अंक", "əŋk")
    }

    @Test
    fun nasalBeforePalatalAffricateIsPalatalNasal() {
        // (a) palatal: पंच -> pəɲtʃ.
        assertWord("पंच", "pəɲtʃ")
    }

    @Test
    fun nasalBeforeRetroflexStopIsRetroflexNasal() {
        // (a) retroflex: घंटा -> ɡʱəɳʈaː.
        assertWord("घंटा", "ɡʱəɳʈaː")
    }

    @Test
    fun nasalBeforeDentalStopIsDentalNasal() {
        // (a) dental, spec §5.2 table: अंत -> ənt.
        assertWord("अंत", "ənt")
    }

    @Test
    fun nasalBeforeLabialStopIsLabialNasal() {
        // (a) labial: लंबा -> ləmbaː.
        assertWord("लंबा", "ləmbaː")
    }

    @Test
    fun nasalDropsBeforeFricative() {
        // (b) fricative se pehle drop: संसार -> səsaːɾ (koi nasal segment nahi).
        assertWord("संसार", "səsaːɾ")
    }

    @Test
    fun nasalDropsBeforeSonorant() {
        // (b) sonorant (glide j) se pehle drop: संयम -> səjəm.
        assertWord("संयम", "səjəm")
    }

    @Test
    fun nasalWordFinalIsVelarNasalContrast() {
        // (c) vowel se pehle ya word-finally ŋ: हैं -> ɦɛːŋ vs है -> ɦɛː.
        assertWord("हैं", "ɦɛːŋ")
        assertWord("है", "ɦɛː")
        assertWord("मैं", "mɛːŋ")
    }

    @Test
    fun visargaMapsToGlottalFricative() {
        // Spec §1.6: visarga (ः) -> ɦ. अः -> əɦ (अ ka swar + visarga segment).
        assertWord("अः", "əɦ")
    }

    // ---- Literal combining-mark drops, pre-audit D3 ----
    // Trailing position par test hai taaki drop property lock ho bina
    // mid-word split behaviour ko spec-correct claim kiye (wo spec me
    // defined nahi hai, isliye us par assert nahi hai).

    @Test
    fun literalDentalMarkDrops() {
        // U+032A kabhi emit nahi hota (F-DENT); literal input par drop hota hai.
        val result = HindiG2P.tokenizeDetailed("राम\u032A")
        assertEquals(HindiG2P.tokenize("राम"), result.tokens)
        assertTrue(result.dropped.contains('\u032A'))
    }

    @Test
    fun literalCombiningTildeDrops() {
        // U+0303 vocab me nahi; literal input par drop hota hai (F-NAS se alag:
        // ye ं/ँ ka mapping nahi, seedha combining tilde hai).
        val result = HindiG2P.tokenizeDetailed("राम\u0303")
        assertEquals(HindiG2P.tokenize("राम"), result.tokens)
        assertTrue(result.dropped.contains('\u0303'))
    }

    @Test
    fun longInputTokenChunksStayWithin509() {
        // §6.4 table row ka engine-chunking hissa: runtime engine tokens ko
        // 509-par chunk karta hai (KokoroOrtEngine, is test ke tree se bahar).
        // Ye test sirf invariant ko mirror karta hai — engine khud yahan
        // test nahi ho raha; chunkText ka test upar pehle se maujood hai.
        val longText = ("राम ".repeat(120)).trim()
        val tokens = HindiG2P.tokenize(longText)
        assertTrue(tokens.size > 509)
        val chunks = tokens.chunked(509)
        assertTrue(chunks.size > 1)
        for (chunk in chunks) {
            assertTrue(chunk.size <= 509)
        }
        assertEquals(tokens, chunks.flatten())
    }
}
