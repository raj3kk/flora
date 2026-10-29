package com.flora.app.models

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipOutputStream

/**
 * KokoroOrtEngine.parseVoices() — synthetic-NPZ parse test (Lane C Step 4).
 *
 * Pure-JVM JUnit, koi Android import nahi, koi asli model/voice file nahi:
 * test ke andar hi ek minimal synthetic NPZ (ZIP with .npy entries) banaya
 * jata hai — sirf header magic + float32 LE payload, koi asli voice data
 * nahi. ORT session creation / OrtEnvironment / native lib load is test
 * me NAHI hai; sawaal sirf ye hai ki pure-JVM parse path CI par chalta
 * hai ya class load hote hi native/Android stub par atak jata hai.
 *
 * Design (KokoroOrtEngine.kt ke verified base se):
 *  - entry naam "hf_alpha.npy" (ya ".../hf_alpha.npy" suffix),
 *  - 128-byte .npy header (magic 0x93 "NUMPY" + padding),
 *  - payload EXACT 510 x 256 float32 LE = 522,240 bytes.
 */
class KokoroOrtEngineParseTest {

    private val rows = KokoroOrtEngine.VOICE_ROWS // 510
    private val cols = KokoroOrtEngine.STYLE_DIM // 256
    private val headerBytes = KokoroOrtEngine.NPY_HEADER_BYTES // 128

    /** Markers ke saath poora voice grid; baaki sab 0.0f. */
    private fun markerFloats(): FloatArray {
        val floats = FloatArray(rows * cols)
        floats[0] = 1.5f
        floats[1] = -2.25f
        floats[cols - 1] = 3.75f // pehli row ka aakhri col
        floats[cols] = -0.5f // doosri row ka pehla col
        floats[floats.lastIndex] = 42.0f // aakhri value
        return floats
    }

    /** Ek .npy entry ka raw bytes: 128-byte header + float32 LE payload. */
    private fun syntheticNpy(
        floats: FloatArray,
        magicOk: Boolean = true,
        extraTrailingBytes: Int = 0,
    ): ByteArray {
        val header = ByteArray(headerBytes) { 0x20 } // spaces padding
        val magic = byteArrayOf(
            0x93.toByte(),
            'N'.code.toByte(),
            'U'.code.toByte(),
            'M'.code.toByte(),
            'P'.code.toByte(),
            'Y'.code.toByte(),
        )
        if (magicOk) {
            magic.copyInto(header)
        } else {
            // Galat magic: pehla byte badal do, baaki header wahi.
            header[0] = 0x00
            magic.copyInto(header, destinationOffset = 1)
        }
        val payload = ByteBuffer.allocate(floats.size * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .also { buf -> for (f in floats) buf.putFloat(f) }
            .array()
        return header + payload + ByteArray(extraTrailingBytes)
    }

    /** Di gayi entries ke saath ek temp NPZ (ZIP) file banao. */
    private fun writeNpz(entries: List<Pair<String, ByteArray>>): File {
        val file = File.createTempFile("synthetic-voices", ".npz")
        file.deleteOnExit()
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun assertParsesToMarkers(file: File) {
        val parsed = KokoroOrtEngine.parseVoices(file)
        assertEquals(rows * cols, parsed.size)
        assertEquals(1.5f, parsed[0], 0.0f)
        assertEquals(-2.25f, parsed[1], 0.0f)
        assertEquals(3.75f, parsed[cols - 1], 0.0f)
        assertEquals(-0.5f, parsed[cols], 0.0f)
        assertEquals(0.0f, parsed[100], 0.0f)
        assertEquals(42.0f, parsed[parsed.lastIndex], 0.0f)
    }

    private fun assertParseThrows(file: File) {
        try {
            KokoroOrtEngine.parseVoices(file)
        } catch (e: IllegalStateException) {
            return // expected guard
        }
        fail("parseVoices ko IllegalStateException dena chahiye tha")
    }

    @Test
    fun validSyntheticNpzParsesToVoiceRows() {
        // Decoy entry pehle: parse ko use skip karke sahi entry tak
        // pahunchna chahiye.
        val npz = writeNpz(
            listOf(
                "README.txt" to "not a voice entry".toByteArray(),
                "hf_alpha.npy" to syntheticNpy(markerFloats()),
            )
        )
        assertParsesToMarkers(npz)
    }

    @Test
    fun nestedPathEntryParsesViaSuffixMatch() {
        val npz = writeNpz(
            listOf(
                "voices/hf_alpha.npy" to syntheticNpy(markerFloats())
            )
        )
        assertParsesToMarkers(npz)
    }

    @Test
    fun trailingBytesAfterPayloadAreIgnored() {
        val npz = writeNpz(
            listOf(
                "hf_alpha.npy" to syntheticNpy(
                    markerFloats(),
                    extraTrailingBytes = 16,
                )
            )
        )
        assertParsesToMarkers(npz)
    }

    @Test
    fun badMagicThrows() {
        val npz = writeNpz(
            listOf(
                "hf_alpha.npy" to syntheticNpy(markerFloats(), magicOk = false)
            )
        )
        assertParseThrows(npz)
    }

    @Test
    fun shortPayloadThrows() {
        // Sirf 100 floats = 400-byte payload, 522,240 se bahut kam.
        val npz = writeNpz(
            listOf(
                "hf_alpha.npy" to syntheticNpy(FloatArray(100) { 1.0f })
            )
        )
        assertParseThrows(npz)
    }

    @Test
    fun missingEntryThrows() {
        val npz = writeNpz(
            listOf("other.npy" to syntheticNpy(markerFloats()))
        )
        assertParseThrows(npz)
    }
}
