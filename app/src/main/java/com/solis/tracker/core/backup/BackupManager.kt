package com.solis.tracker.core.backup

import com.solis.tracker.core.common.AppDispatchers
import com.solis.tracker.core.common.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    contributorsSet: Set<@JvmSuppressWildcards BackupContributor>,
    @Dispatcher(AppDispatchers.Io) private val ioDispatcher: CoroutineDispatcher
) {
    private val contributors = contributorsSet.toList()
    private val jsonFormat = Json { prettyPrint = true }
    
    suspend fun createExportPayload(): String = withContext(ioDispatcher) {
        val moduleExports = contributors.map { contributor ->
            contributor to contributor.export()
        }
        
        val root = buildJsonObject {
            put("format", "personal-tracker-backup")
            put("formatVersion", 1)
            put("appVersionName", "1.0.0")
            put("createdAt", Instant.now().toEpochMilli())
            
            putJsonObject("modules") {
                moduleExports.forEach { (contributor, data) ->
                    putJsonObject(contributor.key) {
                        put("schemaVersion", contributor.schemaVersion)
                        put("data", data)
                    }
                }
            }
        }
        jsonFormat.encodeToString(JsonElement.serializer(), root)
    }

    suspend fun writePlainJson(outputStream: OutputStream) = withContext(ioDispatcher) {
        outputStream.use { out ->
            out.write(createExportPayload().toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun writeEncrypted(outputStream: OutputStream, password: CharArray) = withContext(ioDispatcher) {
        val jsonPayload = createExportPayload().toByteArray(Charsets.UTF_8)
        val encrypted = BackupCrypto.encrypt(jsonPayload, password)
        outputStream.use { out ->
            out.write(encrypted)
        }
        password.fill('0')
    }

    suspend fun exportCsvs(): List<CsvFile> = withContext(ioDispatcher) {
        contributors.flatMap { it.exportCsv() }
    }
    
    suspend fun parseDryRun(inputStream: InputStream, password: CharArray? = null): Map<String, Int> = withContext(ioDispatcher) {
        val bytes = inputStream.use { it.readBytes() }
        
        val payloadStr = if (password != null) {
            val decrypted = BackupCrypto.decrypt(bytes, password)
            password.fill('0')
            String(decrypted, Charsets.UTF_8)
        } else {
            String(bytes, Charsets.UTF_8)
        }

        val element = jsonFormat.parseToJsonElement(payloadStr)
        val root = element.jsonObject
        
        val format = root["format"]?.jsonPrimitive?.content
        val version = root["formatVersion"]?.jsonPrimitive?.intOrNull
        
        if (format != "personal-tracker-backup") {
            throw Exception("Invalid backup format identifier")
        }
        if (version != null && version > 1) {
            throw Exception("Backup file format version $version is newer than what this app supports")
        }

        val counts = mutableMapOf<String, Int>()
        val modules = root["modules"]?.jsonObject ?: return@withContext counts
        
        // This is a naive dry run that just counts JSON array elements.
        modules.forEach { (key, value) ->
            val data = value.jsonObject["data"]
            if (data is JsonObject) {
                var items = 0
                data.values.forEach {
                    if (it is kotlinx.serialization.json.JsonArray) {
                        items += it.size
                    }
                }
                counts[key] = items
            }
        }
        counts
    }
    
    suspend fun executeImport(inputStream: InputStream, password: CharArray?, strategy: ImportStrategy): ImportReport = withContext(ioDispatcher) {
        val bytes = inputStream.use { it.readBytes() }
        
        val payloadStr = if (password != null) {
            val decrypted = BackupCrypto.decrypt(bytes, password)
            password.fill('0')
            String(decrypted, Charsets.UTF_8)
        } else {
            String(bytes, Charsets.UTF_8)
        }

        val element = jsonFormat.parseToJsonElement(payloadStr)
        val root = element.jsonObject
        
        val format = root["format"]?.jsonPrimitive?.content
        val version = root["formatVersion"]?.jsonPrimitive?.intOrNull
        
        if (format != "personal-tracker-backup") {
            throw Exception("Invalid backup format identifier")
        }
        if (version != null && version > 1) {
            throw Exception("Backup file format version $version is newer than what this app supports")
        }

        val modules = root["modules"]?.jsonObject ?: return@withContext ImportReport(error = "No modules found")
        
        var totalReport = ImportReport()
        val exceptions = mutableListOf<Throwable>()
        
        contributors.forEach { contributor ->
            val moduleData = modules[contributor.key]?.jsonObject
            if (moduleData != null) {
                try {
                    val data = moduleData["data"] ?: buildJsonObject {}
                    val report = contributor.import(data, strategy)
                    totalReport += report
                } catch (e: Exception) {
                    exceptions.add(e)
                }
            }
        }
        
        if (exceptions.isNotEmpty()) {
            throw exceptions.first() // Trigger rollback
        }
        
        totalReport
    }
}
