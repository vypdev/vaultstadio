/**
 * VaultStadio Full-Text Search Plugin
 *
 * Provides full-text search capabilities for file content using Apache Lucene.
 * Supports indexing of text files, PDFs, Office documents, and more via Apache Tika.
 */

package com.vaultstadio.plugins.search

import com.vaultstadio.core.domain.event.EventHandlerResult
import com.vaultstadio.core.domain.event.FileEvent
import com.vaultstadio.core.domain.model.StorageItem
import com.vaultstadio.plugins.api.AbstractPlugin
import com.vaultstadio.plugins.api.PluginMetadata
import com.vaultstadio.plugins.api.PluginPermission
import com.vaultstadio.plugins.config.FieldType
import com.vaultstadio.plugins.config.pluginConfiguration
import com.vaultstadio.plugins.context.PluginContext
import com.vaultstadio.plugins.hooks.ContentAnalysisHook
import com.vaultstadio.plugins.hooks.ContentAnalysisResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.store.FSDirectory
import org.apache.tika.Tika
import java.io.InputStream
import java.nio.file.Path
import org.apache.tika.metadata.Metadata as TikaMetadata

private val logger = KotlinLogging.logger {}

/**
 * Full-text search plugin using Apache Lucene.
 *
 * Features:
 * - Automatic indexing of uploaded files
 * - Content extraction from PDFs, Office docs, etc. via Tika
 * - Full-text search with relevance scoring
 * - Snippet/highlight generation for search results
 */
class FullTextSearchPlugin : AbstractPlugin(), ContentAnalysisHook {

    override val metadata = PluginMetadata(
        id = "com.vaultstadio.plugins.fulltext-search",
        name = "Full-Text Search",
        version = "1.0.0",
        description = "Provides full-text search capabilities for file content",
        author = "VaultStadio",
        website = "https://vaultstadio.io",
        permissions = setOf(
            PluginPermission.READ_FILES,
            PluginPermission.WRITE_METADATA,
            PluginPermission.BACKGROUND_TASKS,
        ),
        supportedMimeTypes = setOf(
            // Text
            "text/plain",
            "text/html",
            "text/xml",
            "text/css",
            "text/javascript",
            "text/markdown",
            "text/csv",
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            // Code
            "application/json",
            "application/xml",
            "application/javascript",
            "application/x-yaml",
        ),
    )

    private val configSchema = pluginConfiguration {
        group("indexing", "Indexing Settings") {
            field("autoIndex", "Auto-index on Upload", FieldType.BOOLEAN) {
                description = "Automatically index files when uploaded"
                defaultValue = true
            }
            field("maxFileSize", "Max File Size (MB)", FieldType.NUMBER) {
                description = "Maximum file size to index (in MB)"
                defaultValue = 50
                validation = "value >= 1 && value <= 500"
            }
            field("indexTextContent", "Index Text Content", FieldType.BOOLEAN) {
                description = "Extract and index text content from documents"
                defaultValue = true
            }
        }
        group("search", "Search Settings") {
            field("maxResults", "Max Search Results", FieldType.NUMBER) {
                description = "Maximum number of search results to return"
                defaultValue = 100
                validation = "value >= 10 && value <= 1000"
            }
            field("snippetLength", "Snippet Length", FieldType.NUMBER) {
                description = "Length of text snippets in search results"
                defaultValue = 200
                validation = "value >= 50 && value <= 500"
            }
        }
    }

    override fun getConfigurationSchema() = configSchema

    private lateinit var pluginContext: PluginContext
    private lateinit var indexPath: Path
    private var indexWriter: IndexWriter? = null
    private val indexMutex = Mutex()

    private val tika = Tika()
    private val analyzer = StandardAnalyzer()

    // Configuration
    private var autoIndex = true
    private var maxFileSizeMb = 50
    private var indexTextContent = true
    private var maxResults = 100
    private var snippetLength = 200

    override suspend fun onInitialize(context: PluginContext) {
        pluginContext = context
        loadConfiguration()

        // Set up index directory
        indexPath = context.dataDirectory.resolve("lucene-index")
        initializeIndex()

        // Subscribe to file events
        context.eventBus.subscribe<FileEvent.Uploaded>(metadata.id) { event ->
            if (autoIndex) {
                handleFileUploaded(event)
            } else {
                EventHandlerResult.Success
            }
        }

        context.eventBus.subscribe<FileEvent.Deleted>(metadata.id) { event ->
            handleFileDeleted(event)
        }

        logger.info { "Full-Text Search Plugin initialized with index at $indexPath" }
    }

    private fun loadConfiguration() {
        autoIndex = pluginContext.config.getBoolean("autoIndex") ?: true
        maxFileSizeMb = pluginContext.config.getInt("maxFileSize") ?: 50
        indexTextContent = pluginContext.config.getBoolean("indexTextContent") ?: true
        maxResults = pluginContext.config.getInt("maxResults") ?: 100
        snippetLength = pluginContext.config.getInt("snippetLength") ?: 200
    }

    private suspend fun initializeIndex() = withContext(Dispatchers.IO) {
        val indexDir = indexPath.toFile()
        if (!indexDir.exists()) {
            indexDir.mkdirs()
        }

        val directory = FSDirectory.open(indexPath)
        val config = IndexWriterConfig(analyzer)
        config.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
        indexWriter = IndexWriter(directory, config)
    }

    private suspend fun handleFileUploaded(event: FileEvent.Uploaded): EventHandlerResult {
        val item = event.item

        // Check if this is a supported file type
        if (!metadata.supportedMimeTypes.contains(item.mimeType)) {
            return EventHandlerResult.Success
        }

        // Check file size
        if (item.size > maxFileSizeMb * 1024 * 1024) {
            logger.info { "Skipping indexing of ${item.name}: file too large (${item.size} bytes)" }
            return EventHandlerResult.Success
        }

        return try {
            val streamResult = pluginContext.storage.retrieve(
                item.storageKey ?: return EventHandlerResult.Success,
            )

            streamResult.fold(
                { error ->
                    logger.error { "Failed to retrieve file for indexing: ${error.message}" }
                    EventHandlerResult.Error(error)
                },
                { stream ->
                    stream.use { indexFile(item, it) }
                    EventHandlerResult.Success
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to index file ${item.id}" }
            EventHandlerResult.Error(e)
        }
    }

    private suspend fun indexFile(item: StorageItem, stream: InputStream) = withContext(Dispatchers.IO) {
        try {
            // Extract text content using Tika
            val tikaMetadata = TikaMetadata()
            val content = if (indexTextContent) {
                tika.parseToString(stream, tikaMetadata)
            } else {
                ""
            }

            indexMutex.withLock {
                val writer = indexWriter ?: return@withContext

                // Create Lucene document
                val doc = Document().apply {
                    add(StringField("id", item.id, Field.Store.YES))
                    add(StringField("ownerId", item.ownerId, Field.Store.YES))
                    add(TextField("name", item.name, Field.Store.YES))
                    add(TextField("path", item.path, Field.Store.YES))
                    item.mimeType?.let { add(StringField("mimeType", it, Field.Store.YES)) }

                    if (content.isNotBlank()) {
                        add(TextField("content", content, Field.Store.YES))
                    }

                    // Add Tika-extracted metadata
                    tikaMetadata.names().forEach { name ->
                        tikaMetadata.get(name)?.let { value ->
                            add(TextField("meta_$name", value, Field.Store.NO))
                        }
                    }
                }

                // Update or add document
                writer.updateDocument(Term("id", item.id), doc)
                writer.commit()
            }

            // Save word count as metadata
            if (content.isNotBlank()) {
                val wordCount = content.split(Regex("\\s+")).size
                pluginContext.metadata.setValue(item.id, "wordCount", wordCount.toString())
                pluginContext.metadata.setValue(item.id, "indexed", "true")
                pluginContext.metadata.setValue(
                    item.id,
                    "indexedAt",
                    Clock.System.now().toString(),
                )
            }

            logger.info { "Indexed file: ${item.name}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to index file: ${item.name}" }
            throw e
        }
    }

    private suspend fun handleFileDeleted(event: FileEvent.Deleted): EventHandlerResult {
        return try {
            removeFromIndex(event.item.id)
            EventHandlerResult.Success
        } catch (e: Exception) {
            logger.error(e) { "Failed to remove file from index: ${event.item.id}" }
            EventHandlerResult.Error(e)
        }
    }

    private suspend fun removeFromIndex(itemId: String) = withContext(Dispatchers.IO) {
        indexMutex.withLock {
            val writer = indexWriter ?: return@withContext
            writer.deleteDocuments(Term("id", itemId))
            writer.commit()
        }
        logger.info { "Removed from index: $itemId" }
    }

    /**
     * Search for files matching the query.
     *
     * @param query Search query string
     * @param ownerId Owner ID to filter results (optional)
     * @param limit Maximum results to return
     * @return List of search results
     */
    suspend fun search(
        query: String,
        ownerId: String? = null,
        limit: Int = maxResults,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val directory = FSDirectory.open(indexPath)
            val reader = DirectoryReader.open(directory)
            val searcher = IndexSearcher(reader)

            // Build query
            val queryParser = QueryParser("content", analyzer)
            val luceneQuery = if (ownerId != null) {
                // Add owner filter
                val contentQuery = queryParser.parse(query)
                org.apache.lucene.search.BooleanQuery.Builder()
                    .add(contentQuery, org.apache.lucene.search.BooleanClause.Occur.MUST)
                    .add(
                        org.apache.lucene.search.TermQuery(Term("ownerId", ownerId)),
                        org.apache.lucene.search.BooleanClause.Occur.MUST,
                    )
                    .build()
            } else {
                queryParser.parse(query)
            }

            // Execute search
            val topDocs = searcher.search(luceneQuery, limit)

            val results = topDocs.scoreDocs.map { scoreDoc ->
                docToSearchResult(searcher, scoreDoc, query)
            }

            reader.close()
            results
        } catch (e: Exception) {
            logger.error(e) { "Search failed for query: $query" }
            emptyList()
        }
    }

    private fun docToSearchResult(
        searcher: IndexSearcher,
        scoreDoc: ScoreDoc,
        query: String,
    ): SearchResult {
        val doc = searcher.doc(scoreDoc.doc)
        val content = doc.get("content") ?: ""

        // Generate snippet
        val snippet = generateSnippet(content, query)

        return SearchResult(
            itemId = doc.get("id"),
            name = doc.get("name"),
            path = doc.get("path"),
            mimeType = doc.get("mimeType"),
            score = scoreDoc.score,
            snippet = snippet,
        )
    }

    private fun generateSnippet(content: String, query: String): String {
        if (content.isBlank()) return ""

        // Find first occurrence of query term
        val queryTerms = query.lowercase().split(Regex("\\s+"))
        val contentLower = content.lowercase()

        var startIndex = 0
        for (term in queryTerms) {
            val idx = contentLower.indexOf(term)
            if (idx >= 0) {
                startIndex = maxOf(0, idx - snippetLength / 2)
                break
            }
        }

        // Extract snippet
        val endIndex = minOf(content.length, startIndex + snippetLength)
        var snippet = content.substring(startIndex, endIndex).trim()

        // Add ellipsis
        if (startIndex > 0) snippet = "...$snippet"
        if (endIndex < content.length) snippet = "$snippet..."

        return snippet
    }

    // ContentAnalysisHook implementation
    override suspend fun analyzeContent(
        item: StorageItem,
        stream: InputStream,
    ): ContentAnalysisResult {
        return try {
            val tikaMetadata = TikaMetadata()
            val content = tika.parseToString(stream, tikaMetadata)

            val language = tikaMetadata.get("Content-Language")

            ContentAnalysisResult(
                text = content,
                language = language,
                confidence = 1.0,
            )
        } catch (e: Exception) {
            logger.error(e) { "Content analysis failed for ${item.name}" }
            ContentAnalysisResult(text = null, confidence = 0.0)
        }
    }

    /**
     * Rebuild the entire search index.
     * Iterates through all indexable files and re-indexes them.
     */
    suspend fun rebuildIndex(): Int = withContext(Dispatchers.IO) {
        var indexedCount = 0
        var errorCount = 0

        logger.info { "Starting index rebuild..." }

        // Clear existing index
        indexMutex.withLock {
            val writer = indexWriter ?: return@withContext 0
            writer.deleteAll()
            writer.commit()
        }

        // Iterate through all supported MIME types and index matching files
        for (mimeType in metadata.supportedMimeTypes) {
            try {
                val itemsResult = pluginContext.storage.getItemsByMimeType(
                    pattern = mimeType,
                    ownerId = null, // All users
                    limit = 10000, // High limit for rebuild
                )

                itemsResult.fold(
                    { error ->
                        logger.warn { "Failed to get items for MIME type $mimeType: ${error.message}" }
                    },
                    { items ->
                        for (item in items) {
                            // Check file size
                            if (item.size > maxFileSizeMb * 1024 * 1024) {
                                logger.debug { "Skipping ${item.name}: too large" }
                                continue
                            }

                            try {
                                val streamResult = pluginContext.storage.retrieve(
                                    item.storageKey ?: continue,
                                )

                                streamResult.fold(
                                    { error ->
                                        logger.warn { "Failed to retrieve ${item.name}: ${error.message}" }
                                        errorCount++
                                    },
                                    { stream ->
                                        stream.use { indexFile(item, it) }
                                        indexedCount++

                                        // Log progress every 100 files
                                        if (indexedCount % 100 == 0) {
                                            logger.info { "Index rebuild progress: $indexedCount files indexed" }
                                        }
                                    },
                                )
                            } catch (e: Exception) {
                                logger.warn { "Failed to index ${item.name}: ${e.message}" }
                                errorCount++
                            }
                        }
                    },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error processing MIME type $mimeType during rebuild" }
            }
        }

        logger.info { "Index rebuild complete: $indexedCount files indexed, $errorCount errors" }
        indexedCount
    }

    /**
     * Get index statistics.
     */
    suspend fun getIndexStats(): IndexStats = withContext(Dispatchers.IO) {
        try {
            val directory = FSDirectory.open(indexPath)
            val reader = DirectoryReader.open(directory)
            val numDocs = reader.numDocs()
            val numDeletedDocs = reader.numDeletedDocs()
            reader.close()

            IndexStats(
                documentCount = numDocs,
                deletedCount = numDeletedDocs,
                indexPath = indexPath.toString(),
            )
        } catch (e: Exception) {
            IndexStats(documentCount = 0, deletedCount = 0, indexPath = indexPath.toString())
        }
    }

    override suspend fun getStatistics(): Map<String, Any> {
        val stats = getIndexStats()
        return mapOf(
            "documentCount" to stats.documentCount,
            "deletedCount" to stats.deletedCount,
            "indexPath" to stats.indexPath,
        )
    }

    override suspend fun onShutdown() {
        indexMutex.withLock {
            indexWriter?.close()
            indexWriter = null
        }
        logger.info { "Full-Text Search Plugin shut down" }
    }
}

/**
 * Search result data class.
 */
@Serializable
data class SearchResult(
    val itemId: String,
    val name: String,
    val path: String,
    val mimeType: String?,
    val score: Float,
    val snippet: String,
)

/**
 * Index statistics.
 */
@Serializable
data class IndexStats(
    val documentCount: Int,
    val deletedCount: Int,
    val indexPath: String,
)
