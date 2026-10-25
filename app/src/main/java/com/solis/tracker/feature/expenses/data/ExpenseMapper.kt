package com.solis.tracker.feature.expenses.data

import com.solis.tracker.core.database.dao.TransactionWithDetails
import com.solis.tracker.core.database.entity.AccountEntity
import com.solis.tracker.core.database.entity.BudgetEntity
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.database.entity.TransactionEntity
import com.solis.tracker.feature.expenses.domain.model.Account
import com.solis.tracker.feature.expenses.domain.model.AccountType
import com.solis.tracker.feature.expenses.domain.model.Budget
import com.solis.tracker.feature.expenses.domain.model.ExpenseCategory
import com.solis.tracker.feature.expenses.domain.model.Transaction
import com.solis.tracker.feature.expenses.domain.model.TransactionKind

fun ExpenseCategoryEntity.toDomain() = ExpenseCategory(
    id = id,
    name = name,
    colorKey = colorKey,
    icon = icon,
    kind = runCatching { TransactionKind.valueOf(kind) }.getOrElse { TransactionKind.EXPENSE },
    isDefault = isDefault
)

fun ExpenseCategory.toEntity() = ExpenseCategoryEntity(
    id = id,
    name = name,
    colorKey = colorKey,
    icon = icon,
    kind = kind.name,
    isDefault = isDefault
)

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = runCatching { AccountType.valueOf(type) }.getOrElse { AccountType.CASH },
    openingBalanceMinor = openingBalanceMinor
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    openingBalanceMinor = openingBalanceMinor
)

fun TransactionWithDetails.toDomain() = Transaction(
    id = transaction.id,
    kind = runCatching { TransactionKind.valueOf(transaction.kind) }.getOrElse { TransactionKind.EXPENSE },
    amountMinor = transaction.amountMinor,
    currencyCode = transaction.currencyCode,
    category = category.toDomain(),
    account = account?.toDomain(),
    note = transaction.note,
    occurredAt = transaction.occurredAt,
    createdAt = transaction.createdAt,
    updatedAt = transaction.updatedAt
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    kind = kind.name,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    categoryId = category.id,
    accountId = account?.id,
    note = note,
    occurredAt = occurredAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    month = month,
    limitMinor = limitMinor
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    month = month,
    limitMinor = limitMinor
)
