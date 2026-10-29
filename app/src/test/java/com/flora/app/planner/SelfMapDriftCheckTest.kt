package com.flora.app.planner

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lane A — B5-02 "Self-map drift check" ka host-side runner.
 *
 * SelfMapGenerator.generate(initialMap()) chalata hai (org.json ke
 * real implementation ke liye Robolectric chahiye — plain JUnit me
 * wahi calls "Stub!" error dete hain) aur shipped asset
 * app/src/main/assets/flora_self_map.json se SEMANTIC SET COMPARE
 * karta hai. Raw string compare NAHI: asset ki key order
 * alphabetical hai, generate() ki order alag hai.
 *
 * Har section ka key:
 * - screens: name (value: title + route)
 * - features: name (value: detail)
 * - services: name (value: detail)
 * - dataFlows: from + "->" + to (value: detail)
 * - permissions: name (value: detail)
 * - dependencies: name (value: detail)
 *
 * Koi count HARDCODE nahi hai — sirf har section ka non-empty hona
 * (khaali-vs-khaali vacuous pass rokne ke liye) aur set equality
 * check hoti hai. Drift mile to test FAIL hota hai, fail message
 * me added/removed/changed entries ke saath.
 *
 * Asset path: unit test ka working dir module dir (app/) hota hai,
 * isliye pehla candidate src/main/assets/... hai; root se chalane
 * par doosra candidate app/src/main/assets/... kaam aata hai.
 * Dono na milen to fail message me tried paths + user.dir likha
 * hota hai — chupchaap skip kabhi nahi hota.
 */
@RunWith(RobolectricTestRunner::class)
class SelfMapDriftCheckTest {

    private fun readShippedAsset(): String {
        val candidates = listOf(
            "src/main/assets/flora_self_map.json",
            "app/src/main/assets/flora_self_map.json",
        )
        for (path in candidates) {
            val file = File(path)
            if (file.isFile) {
                return file.readText()
            }
        }
        fail(
            "Shipped self-map asset nahi mila. Tried: $candidates, " +
                "user.dir=${System.getProperty("user.dir")}"
        )
        // fail() hamesha throw karta hai; compiler ke liye unreachable return.
        return ""
    }

    private fun sectionEntries(root: JSONObject, section: String): List<Pair<String, String>> {
        val arr: JSONArray = root.optJSONArray(section) ?: JSONArray()
        val out = ArrayList<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            when (section) {
                "screens" -> out.add(
                    obj.optString("name") to
                        "title=${obj.optString("title")};route=${obj.optString("route")}"
                )
                "dataFlows" -> out.add(
                    "${obj.optString("from")}->${obj.optString("to")}" to
                        "detail=${obj.optString("detail")}"
                )
                else -> out.add(obj.optString("name") to "detail=${obj.optString("detail")}")
            }
        }
        return out
    }

    private fun duplicateKeys(entries: List<Pair<String, String>>): List<String> {
        val seen = HashSet<String>()
        val dups = ArrayList<String>()
        for (entry in entries) {
            if (!seen.add(entry.first)) {
                dups.add(entry.first)
            }
        }
        return dups
    }

    @Test
    fun generatedMapMatchesShippedAsset() {
        val generated = JSONObject(SelfMapGenerator.generate(SelfMapGenerator.initialMap()))
        val shipped = JSONObject(readShippedAsset())
        val sections = listOf(
            "screens", "features", "services",
            "dataFlows", "permissions", "dependencies",
        )
        val drift = ArrayList<String>()
        for (section in sections) {
            val genEntries = sectionEntries(generated, section)
            val shipEntries = sectionEntries(shipped, section)
            // Vacuous pass rokna: dono taraf section khaali ho to ye
            // compare kuch prove nahi karta, isliye non-empty zaroori hai.
            assertTrue(
                "Section '$section' generated me khaali hai — drift check vacuous ho jayega",
                genEntries.isNotEmpty(),
            )
            assertTrue(
                "Section '$section' shipped asset me khaali hai — drift check vacuous ho jayega",
                shipEntries.isNotEmpty(),
            )
            val genMap = genEntries.associate { it.first to it.second }
            val shipMap = shipEntries.associate { it.first to it.second }
            for (key in shipMap.keys - genMap.keys) {
                drift.add("$section: ADDED in asset (initialMap me nahi): $key = ${shipMap[key]}")
            }
            for (key in genMap.keys - shipMap.keys) {
                drift.add("$section: REMOVED from asset (initialMap me hai): $key = ${genMap[key]}")
            }
            for (key in genMap.keys.intersect(shipMap.keys)) {
                if (genMap[key] != shipMap[key]) {
                    drift.add(
                        "$section: CHANGED $key: generated=${genMap[key]} vs asset=${shipMap[key]}"
                    )
                }
            }
        }
        if (drift.isNotEmpty()) {
            fail(
                "Self-map DRIFT mila (generated vs shipped asset):\n" +
                    drift.joinToString("\n")
            )
        }
    }

    @Test
    fun noDuplicateKeysInGeneratedOrShippedMap() {
        val generated = JSONObject(SelfMapGenerator.generate(SelfMapGenerator.initialMap()))
        val shipped = JSONObject(readShippedAsset())
        val sections = listOf(
            "screens", "features", "services",
            "dataFlows", "permissions", "dependencies",
        )
        val dups = ArrayList<String>()
        for (section in sections) {
            for (key in duplicateKeys(sectionEntries(generated, section))) {
                dups.add("$section generated duplicate key: $key")
            }
            for (key in duplicateKeys(sectionEntries(shipped, section))) {
                dups.add("$section asset duplicate key: $key")
            }
        }
        if (dups.isNotEmpty()) {
            fail("Self-map me duplicate keys (set compare inhe chhupa deta):\n" + dups.joinToString("\n"))
        }
    }
}
