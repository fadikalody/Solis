package com.solis.tracker.feature.expenses.backup

import com.solis.tracker.core.backup.BackupContributor
import com.solis.tracker.core.backup.CsvFile
import com.solis.tracker.core.backup.CsvWriter
import com.solis.tracker.core.backup.ImportReport
import com.solis.tracker.core.backup.ImportStrategy
import com.solis.tracker.feature.expenses.domain.ExpensesRepository
import com.solis.tracker.feature.expenses.domain.model.Account
import com.solis.tracker.feature.expenses.domain.model.AccountType
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject

class ExpensesBackupContributor @Inject constructor(
    private val repository: ExpensesRepository
) : BackupContributor {
    override val key: String = "expenses"
    override val schemaVersion: Int = 1

    override suspend fun export(): JsonElement {
        val categories = repository.getAllCategories().first()
        val accounts = repository.getAllAccounts().first()
        val transactions = repository.getAllTransactions().first()
        
        return buildJsonObject {
            put("categories", buildJsonArray {
                categories.forEach { cat ->
                    add(buildJsonObject {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("colorKey", cat.colorKey)
                        put("icon", cat.icon)
                        put("kind", cat.kind.name)
                        put("isDefault", cat.isDefault)
                    })
                }
            })
            put("accounts", buildJsonArray {
                accounts.forEach { acc ->
                    add(buildJsonObject {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("type", acc.type.name)
                        put("openingBalanceMinor", acc.openingBalanceMinor)
                    })
                }
            })
            put("transactions", buildJsonArray {
                transactions.forEach { tx ->
                    add(buildJsonObject {
                        put("id", tx.id)
                        put("kind", tx.kind.name)
                        put("amountMinor", tx.amountMinor)
                        put("currencyCode", tx.currencyCode)
                        put("categoryId", tx.category.id)
                        tx.account?.let { put("accountId", it.id) }
                        put("note", tx.note)
                        put("occurredAt", tx.occurredAt.toEpochMilli())
                        put("createdAt", tx.createdAt.toEpochMilli())
                        put("updatedAt", tx.updatedAt.toEpochMilli())
                    })
                }
            })
        }
    }

    override suspend fun import(data: JsonElement, strategy: ImportStrategy): ImportReport {
        var added = 0
        var updated = 0
        
        val obj = data.jsonObject
        
        // Categories
        val catsArray = obj["categories"]?.jsonArray
        if (catsArray != null) {
            val existingCats = repository.getAllCategories().first().associateBy { it.id }
            for (elem in catsArray) {
                val item = elem.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                if (existingCats[id] == null || strategy == ImportStrategy.REPLACE) {
                    val cat = ExpenseCategory(
                        id = id,
                        name = item["name"]?.jsonPrimitive?.content ?: "",
                        colorKey = item["colorKey"]?.jsonPrimitive?.content ?: "",
                        icon = item["icon"]?.jsonPrimitive?.content ?: "",
                        kind = runCatching { TransactionKind.valueOf(item["kind"]?.jsonPrimitive?.content ?: "EXPENSE") }.getOrDefault(TransactionKind.EXPENSE),
                        isDefault = item["isDefault"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    )
                    repository.saveCategory(cat)
                    if (existingCats[id] == null) added++ else updated++
                }
            }
        }
        
        // Accounts
        val accountsArray = obj["accounts"]?.jsonArray
        if (accountsArray != null) {
            val existingAccounts = repository.getAllAccounts().first().associateBy { it.id }
            for (elem in accountsArray) {
                val item = elem.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                if (existingAccounts[id] == null || strategy == ImportStrategy.REPLACE) {
                    val acc = Account(
                        id = id,
                        name = item["name"]?.jsonPrimitive?.content ?: "",
                        type = runCatching { AccountType.valueOf(item["type"]?.jsonPrimitive?.content ?: "CASH") }.getOrDefault(AccountType.CASH),
                        openingBalanceMinor = item["openingBalanceMinor"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    )
                    repository.saveAccount(acc)
                    if (existingAccounts[id] == null) added++ else updated++
                }
            }
        }
        
        // Transactions
        val txsArray = obj["transactions"]?.jsonArray
        if (txsArray != null) {
            val existingTxs = repository.getAllTransactions().first().associateBy { it.id }
            val currentCats = repository.getAllCategories().first().associateBy { it.id }
            val currentAccs = repository.getAllAccounts().first().associateBy { it.id }
            
            for (elem in txsArray) {
                val item = elem.jsonObject
                val id = item["id"]?.jsonPrimitive?.content ?: continue
                
                val existing = existingTxs[id]
                val incomingUpdated = item["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                
                if (existing == null || strategy == ImportStrategy.REPLACE || (strategy == ImportStrategy.MERGE && incomingUpdated > existing.updatedAt.toEpochMilli())) {
                    val catId = item["categoryId"]?.jsonPrimitive?.content ?: continue
                    val cat = currentCats[catId] ?: ExpenseCategory(catId, "Restored", "", "", TransactionKind.EXPENSE, false)
                    val accId = item["accountId"]?.jsonPrimitive?.content
                    val acc = accId?.let { currentAccs[it] }
                    
                    val tx = Transaction(
                        id = id,
                        kind = runCatching { TransactionKind.valueOf(item["kind"]?.jsonPrimitive?.content ?: "EXPENSE") }.getOrDefault(TransactionKind.EXPENSE),
                        amountMinor = item["amountMinor"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                        currencyCode = item["currencyCode"]?.jsonPrimitive?.content ?: "USD",
                        category = cat,
                        account = acc,
                        note = item["note"]?.jsonPrimitive?.content ?: "",
                        occurredAt = Instant.ofEpochMilli(item["occurredAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: Instant.now().toEpochMilli()),
                        createdAt = Instant.ofEpochMilli(item["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: Instant.now().toEpochMilli()),
                        updatedAt = Instant.ofEpochMilli(incomingUpdated)
                    )
                    repository.saveTransaction(tx)
                    if (existing == null) added++ else updated++
                }
            }
        }
        
        return ImportReport(added = added, updated = updated)
    }

    override suspend fun exportCsv(): List<CsvFile> {
        val txs = repository.getAllTransactions().first()
        val rows = txs.map { tx ->
            listOf(
                tx.id,
                tx.kind.name,
                (tx.amountMinor / 100.0).toString(),
                tx.currencyCode,
                tx.category.name,
                tx.account?.name ?: "",
                tx.note,
                tx.occurredAt.toString()
            )
        }
        val content = CsvWriter.generateCsv(
            headers = listOf("ID", "Kind", "Amount", "Currency", "Category", "Account", "Note", "Date"),
            rows = rows
        )
        return listOf(CsvFile("expenses.csv", content))
    }
}
