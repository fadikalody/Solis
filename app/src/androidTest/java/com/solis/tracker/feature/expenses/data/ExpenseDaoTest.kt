package com.solis.tracker.feature.expenses.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solis.tracker.core.database.AppDatabase
import com.solis.tracker.core.database.dao.ExpenseDao
import com.solis.tracker.core.database.entity.ExpenseCategoryEntity
import com.solis.tracker.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        expenseDao = database.expenseDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getExpenseSumsByCategory_aggregatesCorrectly() = runBlocking {
        val cat1 = ExpenseCategoryEntity("cat1", "Food", "", "", "EXPENSE", false)
        val cat2 = ExpenseCategoryEntity("cat2", "Transport", "", "", "EXPENSE", false)
        expenseDao.insertCategory(cat1)
        expenseDao.insertCategory(cat2)

        val baseTime = Instant.parse("2026-03-08T12:00:00Z")
        
        // 3 transactions for cat1 = 1500 (15.00)
        expenseDao.insertTransaction(TransactionEntity("t1", "EXPENSE", 500, "USD", "cat1", null, "", baseTime, baseTime, baseTime))
        expenseDao.insertTransaction(TransactionEntity("t2", "EXPENSE", 1000, "USD", "cat1", null, "", baseTime, baseTime, baseTime))
        
        // 1 transaction for cat2 = 2000 (20.00)
        expenseDao.insertTransaction(TransactionEntity("t3", "EXPENSE", 2000, "USD", "cat2", null, "", baseTime, baseTime, baseTime))

        val start = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli()
        val end = Instant.parse("2026-03-31T23:59:59Z").toEpochMilli()

        val sums = expenseDao.getExpenseSumsByCategory(start, end).first()

        assertEquals(2, sums.size)
        val cat1Sum = sums.find { it.categoryId == "cat1" }
        assertEquals(1500L, cat1Sum?.totalAmount)
        val cat2Sum = sums.find { it.categoryId == "cat2" }
        assertEquals(2000L, cat2Sum?.totalAmount)
    }
    
    @Test
    fun getTotalAmountByKind_aggregatesCorrectly() = runBlocking {
        val cat1 = ExpenseCategoryEntity("cat1", "Food", "", "", "EXPENSE", false)
        expenseDao.insertCategory(cat1)

        val baseTime = Instant.parse("2026-03-08T12:00:00Z")
        
        expenseDao.insertTransaction(TransactionEntity("t1", "EXPENSE", 500, "USD", "cat1", null, "", baseTime, baseTime, baseTime))
        expenseDao.insertTransaction(TransactionEntity("t2", "EXPENSE", 1000, "USD", "cat1", null, "", baseTime, baseTime, baseTime))
        expenseDao.insertTransaction(TransactionEntity("t3", "INCOME", 20000, "USD", "cat1", null, "", baseTime, baseTime, baseTime))

        val start = Instant.parse("2026-03-01T00:00:00Z").toEpochMilli()
        val end = Instant.parse("2026-03-31T23:59:59Z").toEpochMilli()

        val totalExpense = expenseDao.getTotalAmountByKind(start, end, "EXPENSE").first()
        val totalIncome = expenseDao.getTotalAmountByKind(start, end, "INCOME").first()

        assertEquals(1500L, totalExpense)
        assertEquals(20000L, totalIncome)
    }
}
