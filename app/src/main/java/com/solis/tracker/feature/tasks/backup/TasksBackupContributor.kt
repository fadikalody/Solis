package com.solis.tracker.feature.tasks.backup

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.core.backup.CsvFile
import com.solis.tracker.core.backup.CsvWriter
import com.solis.tracker.core.backup.ImportReport
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.feature.tasks.domain.TasksRepository
import com.solis.tracker.feature.tasks.domain.model.Task
import com.solis.tracker.feature.tasks.domain.model.TaskCategory
import com.solis.tracker.feature.tasks.domain.model.TaskPriority
import com.solis.tracker.feature.tasks.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class TasksBackupContributor @Inject constructor(
    private val repository: TasksRepository
) : BackupContributor {
    override val key: String = "tasks"
    override val schemaVersion: Int = 1

    override suspend fun export(): JsonElement {
        val tasks = repository.getAllTasks().first()
        val categories = repository.getAllCategories().first()
        
        return buildJsonObject {
            put("categories", buildJsonArray {
                categories.forEach { cat ->
                    add(buildJsonObject {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("colorKey", cat.colorKey)
                        put("icon", cat.icon)
                    })
                }
            })
            put("tasks", buildJsonArray {
                tasks.forEach { task ->
                    add(buildJsonObject {
                        put("id", task.id)
                        put("title", task.title)
                        put("notes", task.notes)
                        task.dueDate?.let { put("dueDate", it.toEpochDay()) }
                        task.dueTime?.let { put("dueTime", it) }
                        put("priority", task.priority.name)
                        put("status", task.status.name)
                        task.completedAt?.let { put("completedAt", it.toEpochMilli()) }
                        task.repeatRule?.let { put("repeatRule", it) }
                        task.reminderAt?.let { put("reminderAt", it.toEpochMilli()) }
                        task.category?.let { put("categoryId", it.id) }
                        put("createdAt", task.createdAt.toEpochMilli())
                        put("updatedAt", task.updatedAt.toEpochMilli())
                        
                        // We are not importing subtasks right now to keep it simple, but let's just serialize them
                        put("subtasks", buildJsonArray {
                            task.subtasks.forEach { sub ->
                                add(buildJsonObject {
                                    put("id", sub.id)
                                    put("taskId", sub.taskId)
                                    put("title", sub.title)
                                    put("done", sub.done)
                                    put("position", sub.position)
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    override suspend fun import(data: JsonElement, strategy: ImportStrategy): ImportReport {
        var added = 0
        var updated = 0
        
        val obj = data.jsonObject
        
        // 1. Categories
        val catsArray = obj["categories"]?.jsonArray
        if (catsArray != null) {
            for (catElem in catsArray) {
                val catObj = catElem.jsonObject
                val id = catObj["id"]?.jsonPrimitive?.content ?: continue
                val name = catObj["name"]?.jsonPrimitive?.content ?: ""
                val colorKey = catObj["colorKey"]?.jsonPrimitive?.content ?: ""
                val icon = catObj["icon"]?.jsonPrimitive?.content ?: ""
                
                val existing = repository.getCategoryById(id)
                if (existing == null || strategy == ImportStrategy.REPLACE) {
                    repository.saveCategory(TaskCategory(id, name, colorKey, icon))
                    if (existing == null) added++ else updated++
                }
            }
        }
        
        // 2. Tasks
        val tasksArray = obj["tasks"]?.jsonArray
        if (tasksArray != null) {
            for (taskElem in tasksArray) {
                val tObj = taskElem.jsonObject
                val id = tObj["id"]?.jsonPrimitive?.content ?: continue
                
                val existing = repository.getTaskById(id)
                val incomingUpdated = tObj["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                
                if (existing == null || strategy == ImportStrategy.REPLACE || (strategy == ImportStrategy.MERGE && incomingUpdated > existing.updatedAt.toEpochMilli())) {
                    val categoryId = tObj["categoryId"]?.jsonPrimitive?.content
                    val category = categoryId?.let { repository.getCategoryById(it) }
                    
                    val task = Task(
                        id = id,
                        title = tObj["title"]?.jsonPrimitive?.content ?: "",
                        notes = tObj["notes"]?.jsonPrimitive?.content ?: "",
                        dueDate = tObj["dueDate"]?.jsonPrimitive?.content?.toLongOrNull()?.let { LocalDate.ofEpochDay(it) },
                        dueTime = tObj["dueTime"]?.jsonPrimitive?.content,
                        priority = runCatching { TaskPriority.valueOf(tObj["priority"]?.jsonPrimitive?.content ?: "MEDIUM") }.getOrDefault(TaskPriority.MEDIUM),
                        status = runCatching { TaskStatus.valueOf(tObj["status"]?.jsonPrimitive?.content ?: "OPEN") }.getOrDefault(TaskStatus.OPEN),
                        completedAt = tObj["completedAt"]?.jsonPrimitive?.content?.toLongOrNull()?.let { Instant.ofEpochMilli(it) },
                        repeatRule = tObj["repeatRule"]?.jsonPrimitive?.content,
                        reminderAt = tObj["reminderAt"]?.jsonPrimitive?.content?.toLongOrNull()?.let { Instant.ofEpochMilli(it) },
                        category = category,
                        subtasks = emptyList(), // Ignored for simplicity in this demo phase
                        createdAt = Instant.ofEpochMilli(tObj["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: Instant.now().toEpochMilli()),
                        updatedAt = Instant.ofEpochMilli(incomingUpdated)
                    )
                    repository.saveTask(task)
                    if (existing == null) added++ else updated++
                }
            }
        }
        
        return ImportReport(added = added, updated = updated)
    }

    override suspend fun exportCsv(): List<CsvFile> {
        val tasks = repository.getAllTasks().first()
        val rows = tasks.map { task ->
            listOf(
                task.id,
                task.title,
                task.notes,
                task.dueDate?.toString() ?: "",
                task.status.name,
                task.priority.name,
                task.category?.name ?: ""
            )
        }
        
        val content = CsvWriter.generateCsv(
            headers = listOf("ID", "Title", "Notes", "DueDate", "Status", "Priority", "Category"),
            rows = rows
        )
        return listOf(CsvFile("tasks.csv", content))
    }
}
