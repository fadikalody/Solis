package com.solis.tracker.feature.expenses.domain.model

import java.time.Instant

enum class TransactionKind {
    EXPENSE, INCOME
}

enum class AccountType {
    CASH, BANK, CARD, WALLET
}

data class ExpenseCategory(
    val id: String,
    val name: String,
    val colorKey: String,
    val icon: String,
    val kind: TransactionKind,
    val isDefault: Boolean
)

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingBalanceMinor: Long
)

data class Transaction(
    val id: String,
    val kind: TransactionKind,
    val amountMinor: Long,
    val currencyCode: String,
    val category: ExpenseCategory,
    val account: Account?,
    val note: String,
    val occurredAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Budget(
    val id: String,
    val categoryId: String?, // Null means overall budget
    val month: String, // Format YYYY-MM
    val limitMinor: Long
)

data class CategorySummary(
    val category: ExpenseCategory,
    val totalAmountMinor: Long
)
