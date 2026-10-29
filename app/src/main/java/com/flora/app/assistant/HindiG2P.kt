// HindiG2P.kt — Devanagari text to Kokoro-82M phoneme tokens.
// Source of truth: FLORA_HINDI_G2P_SPEC.md v0.2 (clean-room; no GPL data or code).
// Pure JVM logic. The only import is java.text.Normalizer; no Android import.
// Pipeline per word: NFC normalize, akshara parse, nasal resolve, epsilon-list,
// right-to-left schwa deletion with the section 5 final-cluster check, vocab tokens.
// Style row is always the UNWRAPPED token count; wrap only adds the 0 sentinels.

package com.flora.app.assistant

import java.text.Normalizer

// Unwrapped Kokoro vocab indices plus input chars dropped along the way.
data class TokenizeResult(val tokens: List<Int>, val dropped: List<Char>)

// One phoneme segment. Vowel segments carry the schwa flag used by deletion;
// consonant segments are whole units (affricates and aspirates stay together).
private data class Seg(val phoneme: String, val isVowel: Boolean, val isSchwa: Boolean)

// One akshara: a halant-joined consonant cluster plus its vowel, if any.
// A null vowel means the akshara ends in a halant and has no inherent schwa.
private data class Akshara(
    val consonants: MutableList<String>,
    var vowel: String?,
    var vowelIsSchwa: Boolean,
    var nasal: Boolean = false,
    var visarga: Boolean = false,
    var lastBase: Char? = null
)

object HindiG2P {

    // Vocab, spec section 1.7 verbatim: "$" + 16 punctuation + 52 letters + 109 IPA.
    private const val PUNCTUATION = ";:,.!?¡¿—…\"«»“” "
    private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val LETTERS_IPA = "ɑɐɒæɓʙβɔɕçɗɖðʤəɘɚɛɜɝɞɟʄɡɠɢʛɦɧħɥʜɨɪʝɭɬɫɮʟɱɯɰŋɳɲɴøɵɸθœɶʘɹɺɾɻʀʁɽʂʃʈʧʉʊʋⱱʌɣɤʍχʎʏʑʐʒʔʡʕʢǀǁǂǃˈˌːˑʼʴʰʱʲʷˠˤ˞↓↑→↗↘'\u0329'ᵻ"

    private val SYMBOLS: List<Char> = buildList {
        add('$')
        for (c in PUNCTUATION) add(c)
        for (c in LETTERS) add(c)
        for (c in LETTERS_IPA) add(c)
    }

    // The apostrophe occurs twice in the list; the last occurrence wins, so
    // lookup gives 176. Total list size is 178 with 177 unique keys.
    private val VOCAB: Map<Char, Int> = buildMap {
        for ((index, c) in SYMBOLS.withIndex()) put(c, index)
    }

    private const val HALANT = '\u094D'
    private const val NUKTA_MARK = '\u093C'
    private const val ANUSVARA = '\u0902'
    private const val CHANDRABINDU = '\u0901'
    private const val VISARGA = '\u0903'

    // Vowels, spec section 1.2. A matra gives the same IPA as its independent vowel.
    // Note: the vocalic-R row is the letter r plus the vowel, exactly as tabled.
    private val VOWELS: Map<Char, String> = mapOf(
        'अ' to "ə", 'आ' to "aː", 'इ' to "ɪ", 'ई' to "iː",
        'उ' to "ʊ", 'ऊ' to "uː", 'ऋ' to "rɪ",
        'ए' to "eː", 'ऐ' to "ɛː", 'ओ' to "oː", 'औ' to "ɔː",
        'ऑ' to "ɔ", 'ऍ' to "ɛ"
    )

    private val MATRAS: Map<Char, String> = mapOf(
        'ा' to "aː", 'ि' to "ɪ", 'ी' to "iː",
        'ु' to "ʊ", 'ू' to "uː", 'ृ' to "rɪ",
        'े' to "eː", 'ै' to "ɛː", 'ो' to "oː", 'ौ' to "ɔː",
        'ॉ' to "ɔ", 'ॅ' to "ɛ"
    )

    // Consonants, spec section 1.3. Aspirated forms are base plus the marker.
    // Dental stops are plain letters; the dental diacritic is never emitted.
    // Affricates are always two units; the single-codepoint forms are never emitted.
    private val CONSONANTS: Map<Char, String> = mapOf(
        'क' to "k", 'ख' to "kʰ", 'ग' to "ɡ", 'घ' to "ɡʱ", 'ङ' to "ŋ",
        'च' to "tʃ", 'छ' to "tʃʰ", 'ज' to "dʒ", 'झ' to "dʒʱ", 'ञ' to "ɲ",
        'ट' to "ʈ", 'ठ' to "ʈʰ", 'ड' to "ɖ", 'ढ' to "ɖʱ", 'ण' to "ɳ",
        'त' to "t", 'थ' to "tʰ", 'द' to "d", 'ध' to "dʱ", 'न' to "n",
        'प' to "p", 'फ' to "pʰ", 'ब' to "b", 'भ' to "bʱ", 'म' to "m",
        'य' to "j", 'र' to "ɾ", 'ल' to "l", 'व' to "ʋ",
        'श' to "ʃ", 'ष' to "ʂ", 'स' to "s", 'ह' to "ɦ"
    )

    // Nukta consonants, spec section 1.4. Keyed by base consonant; the nukta
    // mark itself arrives as the combining mark and replaces the base phoneme.
    // A nukta on an unlisted base leaves the base phoneme unchanged.
    private val NUKTA: Map<Char, String> = mapOf(
        'क' to "q", 'ख' to "x", 'ग' to "ɣ", 'ज' to "z",
        'फ' to "f", 'ड' to "ɽ", 'ढ' to "ɽʱ"
    )

    // Lexical epsilon list, spec section 3.2. Matched at word start only;
    // this is a list, not a general rule. The first schwa becomes epsilon.
    private val EPSILON_PREFIXES = listOf("कह", "रह", "बह", "पहन", "शहर")

    // Final-cluster blockers, spec section 5.3: second member glottal or
    // voiced aspirate blocks word-final schwa deletion.
    private val VOICED_ASPIRATES = setOf("ɡʱ", "dʒʱ", "ɖʱ", "dʱ", "bʱ")

    // Sentence ends for chunking, spec section 6. Double danda closes the
    // same danda family as the single danda listed there.
    private val SENTENCE_TERMINATORS = setOf('।', '॥', '.', '!', '?')

    private const val MAX_TOKENS_PER_CHUNK = 509

    // Parse one Devanagari word into aksharas, spec sections 1.1 and 1.5.
    // A halant joins the next consonant onto the current akshara and removes
    // its vowel; a later matra replaces the pending inherent schwa. Two
    // identical halant-joined consonants collapse to consonant plus length,
    // except in a word starting with the listed morpheme below, where the
    // spec table output keeps both consonants (open item O8, not resolved
    // here by any new rule). Unknown Devanagari chars are dropped and logged.
    private fun parseWord(word: String, dropped: MutableList<Char>, noGeminate: Boolean): List<Akshara> {
        val aksharas = ArrayList<Akshara>()
        var current: Akshara? = null
        var pendingHalant = false
        for (ch in word) {
            when {
                ch == HALANT -> {
                    val cur = current
                    if (cur == null) {
                        dropped.add(ch)
                    } else {
                        cur.vowel = null
                        cur.vowelIsSchwa = false
                        pendingHalant = true
                    }
                }
                ch == NUKTA_MARK -> {
                    val cur = current
                    val base = cur?.lastBase
                    if (cur != null && base != null && cur.consonants.isNotEmpty()) {
                        val nuktaPhoneme = NUKTA[base]
                        if (nuktaPhoneme != null) {
                            cur.consonants[cur.consonants.lastIndex] = nuktaPhoneme
                        }
                    }
                }
                ch == ANUSVARA || ch == CHANDRABINDU -> {
                    val cur = current
                    if (cur == null) dropped.add(ch) else cur.nasal = true
                }
                ch == VISARGA -> {
                    val cur = current
                    if (cur == null) dropped.add(ch) else cur.visarga = true
                }
                ch == '\u200C' || ch == '\u200D' -> {
                    // Zero-width joiners carry no sound and are not drops.
                }
                CONSONANTS.containsKey(ch) -> {
                    val phoneme = CONSONANTS.getValue(ch)
                    val cur = current
                    if (pendingHalant && cur != null) {
                        if (!noGeminate && cur.consonants.isNotEmpty() && cur.consonants.last() == phoneme) {
                            cur.consonants[cur.consonants.lastIndex] = phoneme + "ː"
                        } else {
                            cur.consonants.add(phoneme)
                        }
                        cur.vowel = "ə"
                        cur.vowelIsSchwa = true
                        cur.lastBase = ch
                        pendingHalant = false
                    } else {
                        val fresh = Akshara(mutableListOf(phoneme), "ə", true)
                        fresh.lastBase = ch
                        aksharas.add(fresh)
                        current = fresh
                        pendingHalant = false
                    }
                }
                MATRAS.containsKey(ch) -> {
                    val cur = current
                    if (cur == null) {
                        dropped.add(ch)
                    } else {
                        cur.vowel = MATRAS.getValue(ch)
                        cur.vowelIsSchwa = false
                        pendingHalant = false
                    }
                }
                VOWELS.containsKey(ch) -> {
                    val fresh = Akshara(mutableListOf(), VOWELS.getValue(ch), false)
                    aksharas.add(fresh)
                    current = fresh
                    pendingHalant = false
                }
                else -> dropped.add(ch)
            }
        }
        return aksharas
    }

    // Nasal rule, spec section 1.6: homorganic nasal before a stop or
    // affricate, dropped before a fricative or sonorant, and the velar nasal
    // before a vowel or at word end. A null next consonant covers both the
    // vowel-initial and the word-final cases.
    private fun nextFirstConsonant(aksharas: List<Akshara>, index: Int): String? {
        if (index + 1 >= aksharas.size) return null
        val first = aksharas[index + 1].consonants.firstOrNull() ?: return null
        return first.removeSuffix("ː")
    }

    private fun resolveNasal(nextFirst: String?): String? {
        if (nextFirst == null) return "ŋ"
        return when (nextFirst) {
            "k", "kʰ", "ɡ", "ɡʱ", "ŋ", "q" -> "ŋ"
            "tʃ", "tʃʰ", "dʒ", "dʒʱ", "ɲ" -> "ɲ"
            "ʈ", "ʈʰ", "ɖ", "ɖʱ", "ɳ", "ɽ", "ɽʱ" -> "ɳ"
            "t", "tʰ", "d", "dʱ", "n" -> "n"
            "p", "pʰ", "b", "bʱ", "m" -> "m"
            else -> null
        }
    }

    // Build the segment stream, apply the epsilon list, then schwa deletion.
    private fun buildSegments(aksharas: List<Akshara>, applyEpsilon: Boolean): List<Seg> {
        val segs = ArrayList<Seg>()
        for (i in aksharas.indices) {
            val ak = aksharas[i]
            for (cons in ak.consonants) segs.add(Seg(cons, false, false))
            val vowel = ak.vowel
            if (vowel != null) segs.add(Seg(vowel, true, ak.vowelIsSchwa))
            if (ak.visarga) segs.add(Seg("ɦ", false, false))
            if (ak.nasal) {
                val nasalPhoneme = resolveNasal(nextFirstConsonant(aksharas, i))
                if (nasalPhoneme != null) segs.add(Seg(nasalPhoneme, false, false))
            }
        }
        if (applyEpsilon) {
            val firstVowel = segs.indexOfFirst { it.isVowel }
            if (firstVowel >= 0 && segs[firstVowel].isSchwa) {
                segs[firstVowel] = Seg("ɛ", true, false)
            }
        }
        return applySchwaDeletion(segs)
    }

    // Schwa deletion, spec section 2, applied right to left. A medial schwa
    // deletes only when the immediately preceding pair is vowel plus
    // consonant and the immediately following pair is consonant plus vowel;
    // the first-syllable schwa never deletes. A word-final schwa deletes by
    // default and is blocked only when the resulting final cluster has two
    // or more consonants whose last member is glottal or voiced aspirate.
    // Deletions happen to the right first, so earlier indices stay valid.
    private fun applySchwaDeletion(input: List<Seg>): List<Seg> {
        val segs = input.toMutableList()
        val firstVowelIdx = segs.indexOfFirst { it.isVowel }
        val schwaIndices = segs.indices.filter { segs[it].isVowel && segs[it].isSchwa }.sortedDescending()
        for (i in schwaIndices) {
            if (i >= segs.size || !segs[i].isVowel || !segs[i].isSchwa) continue
            if (i == firstVowelIdx) continue
            if (i == segs.lastIndex) {
                var j = i - 1
                val run = ArrayList<String>()
                while (j >= 0 && !segs[j].isVowel) {
                    run.add(0, segs[j].phoneme)
                    j--
                }
                if (run.size < 2) {
                    segs.removeAt(i)
                } else {
                    val lastBase = run.last().removeSuffix("ː")
                    if (lastBase != "ɦ" && lastBase !in VOICED_ASPIRATES) {
                        segs.removeAt(i)
                    }
                }
            } else {
                val prevOk = i >= 2 && !segs[i - 1].isVowel && segs[i - 2].isVowel
                val nextOk = i + 2 < segs.size && !segs[i + 1].isVowel && segs[i + 2].isVowel
                if (prevOk && nextOk) segs.removeAt(i)
            }
        }
        return segs
    }

    private fun wordToTokens(word: String, dropped: MutableList<Char>): List<Int> {
        if (word.isEmpty()) return emptyList()
        val applyEpsilon = EPSILON_PREFIXES.any { word.startsWith(it) }
        val noGeminate = word.startsWith("पहन")
        val aksharas = parseWord(word, dropped, noGeminate)
        val segments = buildSegments(aksharas, applyEpsilon)
        val out = ArrayList<Int>()
        for (seg in segments) {
            for (ch in seg.phoneme) {
                val index = VOCAB[ch]
                if (index != null) out.add(index) else dropped.add(ch)
            }
        }
        return out
    }

    private fun isDevanagari(ch: Char): Boolean = ch in '\u0900'..'\u097F'

    // Full text to unwrapped tokens plus dropped chars. Devanagari runs are
    // words for schwa purposes; every other char is either a vocab token
    // (vocab punctuation, including space, and Latin letters are kept) or a
    // drop. Empty input yields empty tokens, so wrap gives the 0, 0 pair.
    fun tokenizeDetailed(text: String): TokenizeResult {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val tokens = ArrayList<Int>()
        val dropped = ArrayList<Char>()
        val word = StringBuilder()
        fun flushWord() {
            if (word.isNotEmpty()) {
                tokens.addAll(wordToTokens(word.toString(), dropped))
                word.clear()
            }
        }
        for (ch in normalized) {
            if (ch == '\u200C' || ch == '\u200D') {
                continue
            } else if (isDevanagari(ch)) {
                word.append(ch)
            } else {
                flushWord()
                val index = VOCAB[ch]
                if (index != null) tokens.add(index) else dropped.add(ch)
            }
        }
        flushWord()
        return TokenizeResult(tokens, dropped)
    }

    // Unwrapped Kokoro vocab indices. Unknown chars are silently dropped,
    // matching the runtime tokenize behaviour.
    fun tokenize(text: String): List<Int> = tokenizeDetailed(text).tokens

    private fun splitSentences(text: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            current.append(ch)
            if (ch in SENTENCE_TERMINATORS) {
                while (i + 1 < text.length && text[i + 1] in SENTENCE_TERMINATORS) {
                    i++
                    current.append(text[i])
                }
                out.add(current.toString())
                current.clear()
            }
            i++
        }
        if (current.isNotEmpty()) out.add(current.toString())
        return out
    }

    // Sentence boundaries first, then word boundaries, so every chunk holds
    // at most 509 tokens. A single over-long word is split greedily by
    // characters only as a last resort, to keep the token bound hard.
    fun chunkText(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val chunks = ArrayList<String>()
        for (sentence in splitSentences(text)) {
            if (tokenize(sentence).size <= MAX_TOKENS_PER_CHUNK) {
                chunks.add(sentence)
                continue
            }
            var current = StringBuilder()
            fun flushCurrent() {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString())
                    current = StringBuilder()
                }
            }
            for (part in sentence.split(' ')) {
                if (part.isEmpty()) continue
                val candidate = if (current.isEmpty()) part else current.toString() + " " + part
                if (tokenize(candidate).size <= MAX_TOKENS_PER_CHUNK) {
                    current = StringBuilder(candidate)
                } else {
                    flushCurrent()
                    if (tokenize(part).size <= MAX_TOKENS_PER_CHUNK) {
                        current = StringBuilder(part)
                    } else {
                        var piece = StringBuilder()
                        for (ch in part) {
                            val trial = piece.toString() + ch
                            if (tokenize(trial).size <= MAX_TOKENS_PER_CHUNK) {
                                piece.append(ch)
                            } else {
                                chunks.add(piece.toString())
                                piece = StringBuilder(ch.toString())
                            }
                        }
                        current = piece
                    }
                }
            }
            flushCurrent()
        }
        return chunks
    }

    // Add the start and end sentinels. The style row index is the UNWRAPPED
    // token count, computed by the caller before calling this function.
    fun wrap(tokens: List<Int>): List<Int> {
        val out = ArrayList<Int>(tokens.size + 2)
        out.add(0)
        out.addAll(tokens)
        out.add(0)
        return out
    }

}
