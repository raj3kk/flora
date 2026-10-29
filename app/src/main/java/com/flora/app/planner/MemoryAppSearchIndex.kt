package com.flora.app.planner

import android.content.Context
import androidx.appsearch.app.AppSearchSchema
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * B5-04 AppSearch index — unified memory ka on-device full-text
 * index (AndroidX AppSearch, LocalStorage — app ke apne storage
 * me, koi server / sharing nahi).
 *
 * B5-12 SAFETY: index me sirf SNIPPET jata hai (max 120 chars,
 * UnifiedSearchLogic.snippet se), poora sensitive text kabhi
 * nahi. Sensitive entry (key-like / forbidden assignment) index
 * hoti hi nahi — refresh() unhe pehle hi drop kar deta hai.
 * Index ka source (chat / project / task log) device-encrypted
 * stores me rehta hai; index khud app-private storage me hai.
 *
 * Best-effort design: AppSearch session/schema/query me kuch
 * bhi fail ho to exception kabhi bahar nahi aati — caller
 * (UnifiedSearch) tab seedha store-scan par girta hai. Index
 * refresh idempotent hai (doc id source+ts+hash se banti hai,
 * dobara put karna overwrite hai, duplicate nahi).
 *
 * Session per-operation banaya jata hai (cache nahi — stale /
 * closed session se bachne ke liye); process-lifetime handle
 * hone se koi leak ka daava nahi, par volume chhota hai
 * (search waqt hi chalta hai).
 */
class MemoryAppSearchIndex(private val context: Context) {

    data class Hit(
        val source: String,
        val snippet: String,
        val ts: Long,
    )

    companion object {
        private const val DATABASE = "flora_memory"
        private const val NAMESPACE = "flora"
        private const val SCHEMA_TYPE = "FloraMemory"
        private const val MAX_DOCS_PER_SOURCE = 200
    }

    private fun openSession(): AppSearchSession? {
        return try {
            val searchContext = LocalStorage.SearchContext.Builder(context, DATABASE).build()
            LocalStorage.createSearchSessionAsync(searchContext).get()
        } catch (e: Exception) {
            null
        }
    }

    private fun schema(): AppSearchSchema {
        return AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(
                AppSearchSchema.StringPropertyConfig.Builder("body")
                    .setCardinality(AppSearchSchema.PropertyConfig.Cardinality.OPTIONAL)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.IndexingType.INDEXING_TYPE_PREFIXES)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TokenizerType.TOKENIZER_TYPE_PLAIN)
                    .build()
            )
            .addProperty(
                AppSearchSchema.StringPropertyConfig.Builder("source")
                    .setCardinality(AppSearchSchema.PropertyConfig.Cardinality.OPTIONAL)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.IndexingType.INDEXING_TYPE_EXACT_TERMS)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TokenizerType.TOKENIZER_TYPE_PLAIN)
                    .build()
            )
            .addProperty(
                AppSearchSchema.LongPropertyConfig.Builder("ts")
                    .setCardinality(AppSearchSchema.PropertyConfig.Cardinality.OPTIONAL)
                    .build()
            )
            .build()
    }

    /**
     * Stores se index refresh karo. Entries (source, text, ts)
     * ki list milti hai; sensitive entries drop, baaki snippet
     * ban kar put hote hain.
     */
    suspend fun refresh(entries: List<Triple<String, String, Long>>) {
        withContext(Dispatchers.IO) {
            val session = openSession() ?: return@withContext
            try {
                val request = SetSchemaRequest.Builder()
                    .addSchemas(schema())
                    .setForceOverride(true)
                    .build()
                session.setSchemaAsync(request).get()
                val docs = mutableListOf<GenericDocument>()
                for ((source, text, ts) in entries) {
                    if (UnifiedSearchLogic.isSensitive(text)) continue
                    val snippet = UnifiedSearchLogic.snippet(text)
                    if (snippet.isBlank()) continue
                    val id = source + ":" + ts + ":" + snippet.hashCode()
                    val doc = GenericDocument.Builder(NAMESPACE, id, SCHEMA_TYPE)
                        .addPropertyString("body", snippet)
                        .addPropertyString("source", source)
                        .addPropertyLong("ts", ts)
                        .build()
                    docs.add(doc)
                    if (docs.size >= MAX_DOCS_PER_SOURCE * 3) break
                }
                if (docs.isNotEmpty()) {
                    val putRequest = PutDocumentsRequest.Builder()
                        .addGenericDocuments(*docs.toTypedArray())
                        .build()
                    session.putAsync(putRequest).get()
                }
            } catch (e: Exception) {
            }
        }
    }

    /** AppSearch query — fail ho to khali list (caller store-scan par girega). */
    suspend fun query(queryText: String): List<Hit> {
        if (queryText.isBlank()) return emptyList()
        return withContext(Dispatchers.IO) {
            val session = openSession() ?: return@withContext emptyList()
            try {
                val spec = SearchSpec.Builder()
                    .addFilterSchemas(SCHEMA_TYPE)
                    .setRankingStrategy(SearchSpec.RANKING_STRATEGY_RELEVANCE_SCORE)
                    .setResultCountPerPage(50)
                    .build()
                val results = session.searchAsync(queryText, spec)
                val page = results.nextPageAsync.get()
                val out = mutableListOf<Hit>()
                for (result in page) {
                    try {
                        val doc = result.genericDocument
                        val snippet = doc.getPropertyString("body") ?: ""
                        val source = doc.getPropertyString("source") ?: ""
                        val ts = try {
                            doc.getPropertyLong("ts")
                        } catch (e: Exception) {
                            0L
                        }
                        if (snippet.isNotBlank() && source.isNotBlank()) {
                            out.add(Hit(source, snippet, ts))
                        }
                    } catch (e: Exception) {
                    }
                }
                out
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
