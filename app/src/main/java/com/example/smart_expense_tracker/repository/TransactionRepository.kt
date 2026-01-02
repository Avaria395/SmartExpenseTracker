package com.example.smart_expense_tracker.repository

import android.app.Application
import com.example.smart_expense_tracker.database.AppDatabase
import com.example.smart_expense_tracker.database.dao.CategoryDao
import com.example.smart_expense_tracker.database.dao.TransactionDao
import com.example.smart_expense_tracker.database.entity.CategoryEntity
import com.example.smart_expense_tracker.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TransactionRepository(application: Application) {

    private val transactionDao: TransactionDao = AppDatabase.getDatabase(application).transactionDao()
    private val categoryDao: CategoryDao = AppDatabase.getDatabase(application).categoryDao()

    fun getTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    fun getMonthlyStats(startTime: Long, endTime: Long): Flow<MonthlyStats> {
        return transactionDao.getTransactionsBetween(startTime, endTime).map { transactions ->
            var income = 0L
            var expense = 0L
            transactions.forEach { transaction ->
                when (transaction.type) {
                    0 -> expense += transaction.amount
                    1 -> income += transaction.amount
                }
            }
            val categoryStats = transactions.filter { it.type == 0 }
                .groupBy { it.categoryId }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }

            val calendar = Calendar.getInstance().apply { timeInMillis = startTime }

            MonthlyStats(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                expense = expense,
                income = income,
                balance = income - expense,
                categoryStats = categoryStats
            )
        }
    }

    fun getCategoryExpensesForPeriod(startTime: Long, endTime: Long): Flow<Map<CategoryEntity, Long>> {
        return transactionDao.getTransactionsBetween(startTime, endTime).map { transactions ->
            val categories = categoryDao.getAllCategories()
            val categoryMap = categories.associateBy { it.id }
            transactions.filter { it.type == 0 }
                .groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .mapKeys { entry ->
                    categoryMap[entry.key] ?: CategoryEntity().apply { id = entry.key ?: 0; name = "未知分类" }
                }
        }
    }

    fun getDailyExpenseTrend(startTime: Long, endTime: Long): Flow<List<Float>> {
        return transactionDao.getTransactionsBetween(startTime, endTime).map { transactions ->
            val expenseTransactions = transactions.filter { it.type == 0 }

            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val trend = FloatArray(daysInMonth) { 0f }

            val tempCal = Calendar.getInstance()
            for (tx in expenseTransactions) {
                tempCal.timeInMillis = tx.recordTime
                val dayOfMonth = tempCal.get(Calendar.DAY_OF_MONTH)
                if (dayOfMonth >= 1 && dayOfMonth <= daysInMonth) {
                    trend[dayOfMonth - 1] += tx.amount.toFloat()
                }
            }

            trend.toList()
        }
    }

    fun getMonthlyExpenseTrend(startTime: Long, endTime: Long): Flow<List<Float>> {
        return transactionDao.getTransactionsBetween(startTime, endTime).map { transactions ->
            val expenseTransactions = transactions.filter { it.type == 0 }
            val trend = FloatArray(12) { 0f }

            val tempCal = Calendar.getInstance()
            for (tx in expenseTransactions) {
                tempCal.timeInMillis = tx.recordTime
                val month = tempCal.get(Calendar.MONTH) // 0-11
                if (month >= 0 && month < 12) {
                    trend[month] += tx.amount.toFloat()
                }
            }

            trend.toList()
        }
    }
}