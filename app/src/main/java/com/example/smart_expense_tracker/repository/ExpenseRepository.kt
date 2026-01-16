// kotlin
package com.example.smart_expense_tracker.repository

import android.content.Context
import com.example.smart_expense_tracker.database.AppDatabase
import com.example.smart_expense_tracker.database.entity.*
import com.example.smart_expense_tracker.model.AccountItem
import com.example.smart_expense_tracker.model.AccountType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ExpenseRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val bookDao = database.bookDao()
    private val categoryDao = database.categoryDao()
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()

    companion object {
        @Volatile
        private var INSTANCE: ExpenseRepository? = null

        fun getInstance(context: Context): ExpenseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExpenseRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 账本操作
    suspend fun getAllBooks(): List<BookEntity> = withContext(Dispatchers.IO) {
        bookDao.getAllBooks()
    }

    suspend fun getDefaultBook(): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getDefaultBook()
    }

    suspend fun insertBook(book: BookEntity): Long = withContext(Dispatchers.IO) {
        bookDao.insert(book)
    }

    suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.update(book)
    }

    // 分类操作
    suspend fun getAllCategories(): List<CategoryEntity> = withContext(Dispatchers.IO) {
        categoryDao.getAllCategories()
    }

    suspend fun getCategoriesByType(type: Int): List<CategoryEntity> = withContext(Dispatchers.IO) {
        categoryDao.getCategoriesByType(type)
    }

    suspend fun getCategoryById(id: Int): CategoryEntity? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insert(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.delete(category)
    }

    // 账户操作
    suspend fun getAllAccounts(): List<AccountEntity> = withContext(Dispatchers.IO) {
        accountDao.getAllAccounts()
    }

    suspend fun getAccountById(id: Int): AccountEntity? = withContext(Dispatchers.IO) {
        accountDao.getAccountById(id)
    }

    suspend fun insertAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
        accountDao.insert(account)
    }

    suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.update(account)
    }

    suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        accountDao.delete(account)
    }

    // 按增量更新余额（调用 DAO 的 updateBalance，传入变动值）
    suspend fun changeAccountBalance(accountId: Int, changeAmount: Long) = withContext(Dispatchers.IO) {
        accountDao.updateBalance(accountId, changeAmount)
    }

    // 交易操作
    suspend fun getAllTransactions(): List<TransactionEntity> = withContext(Dispatchers.IO) {
        transactionDao.getAllTransactions().first()
    }

    suspend fun getTransactionsByBook(bookId: Int): List<TransactionEntity> = withContext(Dispatchers.IO) {
        transactionDao.getTransactionsByBook(bookId)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        val transactionId = transactionDao.insert(transaction)

        // 更新账户余额
        if (transaction.accountId != null && transaction.type != 2) { // 非转账
            val changeAmount = if (transaction.type == 0) -transaction.amount else transaction.amount
            changeAccountBalance(transaction.accountId, changeAmount)
        }

        // 如果是支出，更新相应预算的 spentAmount（按交易发生时间的年月）
        if (transaction.type == 0 && transaction.categoryId != null) {
            try {
                val category = categoryDao.getCategoryById(transaction.categoryId!!)
                if (category != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.recordTime }
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1

                    // 更新分类预算
                    val b = budgetDao.getBudgetByCategoryAndMonth(category.name, year, month)
                    if (b != null) {
                        b.spentAmount = b.spentAmount + transaction.amount
                        b.updateTime = System.currentTimeMillis()
                        budgetDao.update(b)
                    }

                    // 更新总预算（如果存在）
                    val total = budgetDao.getBudgetByCategoryAndMonth("总预算", year, month)
                    if (total != null) {
                        total.spentAmount = total.spentAmount + transaction.amount
                        total.updateTime = System.currentTimeMillis()
                        budgetDao.update(total)
                    }
                }
            } catch (_: Exception) {
                // 忽略预算更新错误，调用方可通过日志/错误流处理
            }
        }

        transactionId
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        // 1. 回滚账户余额
        if (transaction.accountId != null && transaction.type != 2) {
            // 如果是支出(0)，删除后应加回金额；如果是收入(1)，删除后应减去金额
            val changeAmount = if (transaction.type == 0) transaction.amount else -transaction.amount
            changeAccountBalance(transaction.accountId, changeAmount)
        }

        // 2. 如果是支出，调整预算的 spentAmount（按交易发生时间的年月）
        if (transaction.type == 0 && transaction.categoryId != null) {
            try {
                val category = categoryDao.getCategoryById(transaction.categoryId!!)
                if (category != null) {
                    val cal = Calendar.getInstance().apply { timeInMillis = transaction.recordTime }
                    val year = cal.get(Calendar.YEAR)
                    val month = cal.get(Calendar.MONTH) + 1

                    // 调整分类预算
                    val b = budgetDao.getBudgetByCategoryAndMonth(category.name, year, month)
                    if (b != null) {
                        b.spentAmount = (b.spentAmount - transaction.amount).coerceAtLeast(0L)
                        b.updateTime = System.currentTimeMillis()
                        budgetDao.update(b)
                    }

                    // 调整总预算
                    val total = budgetDao.getBudgetByCategoryAndMonth("总预算", year, month)
                    if (total != null) {
                        total.spentAmount = (total.spentAmount - transaction.amount).coerceAtLeast(0L)
                        total.updateTime = System.currentTimeMillis()
                        budgetDao.update(total)
                    }
                }
            } catch (_: Exception) {
                // 忽略错误
            }
        }

        // 3. 执行物理删除
        transactionDao.delete(transaction)
    }

    // 获取指定时间范围的交易统计
    suspend fun getTotalExpense(startTime: Long, endTime: Long): Long = withContext(Dispatchers.IO) {
        transactionDao.getTotalExpense(startTime, endTime) ?: 0L
    }

    suspend fun getTotalIncome(startTime: Long, endTime: Long): Long = withContext(Dispatchers.IO) {
        transactionDao.getTotalIncome(startTime, endTime) ?: 0L
    }

    suspend fun getTransactionsByPeriod(
        startTime: Long,
        endTime: Long,
        bookId: Int? = null
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        if (bookId != null) {
            transactionDao.getTransactionsByPeriodAndBook(startTime, endTime, bookId)
        } else {
            transactionDao.getTransactionsByPeriod(startTime, endTime)
        }
    }

    suspend fun getCategoryStatistics(
        startTime: Long,
        endTime: Long,
        bookId: Int? = null
    ): Map<Int, Long> = withContext(Dispatchers.IO) {
        val transactions = getTransactionsByPeriod(startTime, endTime, bookId)
        transactions.filter { it.type == 0 } // 只统计支出
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
    }

    suspend fun getTransactionsByDate(
        date: String,
        bookId: Int? = null
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startOfDay = dateFormat.parse(date)?.time ?: 0L
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

        getTransactionsByPeriod(startOfDay, endOfDay, bookId)
    }


// 替换 ExpenseRepository 中的 initializeDefaultData 与 addAccount

    suspend fun initializeDefaultData() = withContext(Dispatchers.IO) {
        // 初始化默认账本（使用无参构造并 apply 赋值）
        val defaultBook = BookEntity().apply {
            id = 0
            name = "默认账本"
        }
        if (bookDao.getAllBooks().isEmpty()) {
            bookDao.insert(defaultBook)
        }

        // 初始化默认分类（使用无参构造并 apply 赋值）
        val categories = listOf(
            CategoryEntity().apply { id = 0; name = "餐饮"; type = 0; iconRes = "restaurant" },
            CategoryEntity().apply { id = 0; name = "购物"; type = 0; iconRes = "shopping" },
            CategoryEntity().apply { id = 0; name = "交通"; type = 0; iconRes = "transport" },
            CategoryEntity().apply { id = 0; name = "娱乐"; type = 0; iconRes = "entertainment" },
            CategoryEntity().apply { id = 0; name = "医疗"; type = 0; iconRes = "medical" },
            CategoryEntity().apply { id = 0; name = "教育"; type = 0; iconRes = "education" },
            CategoryEntity().apply { id = 0; name = "其他"; type = 0; iconRes = "other" },
            CategoryEntity().apply { id = 0; name = "工资"; type = 1; iconRes = "salary" },
            CategoryEntity().apply { id = 0; name = "投资"; type = 1; iconRes = "investment" },
            CategoryEntity().apply { id = 0; name = "其他收入"; type = 1; iconRes = "other_income" }
        )

        // Only insert categories if none exist to avoid duplicates
        if (categoryDao.getAllCategories().isEmpty()) {
            categories.forEach { category ->
                categoryDao.insert(category)
            }
        }

        // 初始化默认账户（使用无参构造并 apply 赋值）
        val accounts = listOf(
            AccountEntity().apply { id = 0; name = "现金"; balance = 0L },
            AccountEntity().apply { id = 0; name = "银行卡"; balance = 0L },
            AccountEntity().apply { id = 0; name = "微信"; balance = 0L },
            AccountEntity().apply { id = 0; name = "支付宝"; balance = 0L }
        )

        // Only insert default accounts when the accounts table is empty to avoid re-creating them every startup
        if (accountDao.getAllAccounts().isEmpty()) {
            accounts.forEach { account ->
                accountDao.insert(account)
            }
        }
    }

    // 获取今日统计
    suspend fun getTodayStats(): TodayStats = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

        val expense = getTotalExpense(startOfDay, endOfDay)
        val income = getTotalIncome(startOfDay, endOfDay)

        TodayStats(
            date = SimpleDateFormat("M月d日 (E)", Locale.CHINA).format(calendar.time),
            expense = expense,
            income = income,
            balance = income - expense
        )
    }

    // 获取月度统计
    suspend fun getMonthlyStats(year: Int, month: Int): MonthlyStats = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfMonth = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        val expense = getTotalExpense(startOfMonth, endOfMonth)
        val income = getTotalIncome(startOfMonth, endOfMonth)
        val balance = income - expense

        MonthlyStats(
            year = year,
            month = month,
            expense = expense,
            income = income,
            balance = balance,
            categoryStats = getCategoryStatistics(startOfMonth, endOfMonth)
        )
    }

    // 资产管理相关方法
    suspend fun getAssetOverview(): AssetOverview = withContext(Dispatchers.IO) {
        val allAccounts = accountDao.getAllAccounts()

        val totalAssets = allAccounts.filter { it.balance >= 0 }.sumOf { it.balance }
        val totalLiabilities = allAccounts.filter { it.balance < 0 }.sumOf { it.balance }
        val netAssets = totalAssets + totalLiabilities

        AssetOverview(
            totalAssets = totalAssets,
            totalLiabilities = Math.abs(totalLiabilities),
            netAssets = netAssets,
            accounts = allAccounts.map { it.toAccountItem() }
        )
    }

    // 按新余额设置账户余额（计算差值并通过 DAO 更新）
    suspend fun setAccountBalance(accountId: Int, newBalance: Long) = withContext(Dispatchers.IO) {
        val current = getAccountById(accountId)?.balance ?: 0L
        accountDao.updateBalance(accountId, newBalance - current)
    }

    suspend fun deleteAccountById(accountId: Int) = withContext(Dispatchers.IO) {
        val account = accountDao.getAccountById(accountId)
        if (account != null) {
            accountDao.delete(account)
        }
    }

    // 预算相关操作
    suspend fun getAllBudgets() = withContext(Dispatchers.IO) {
        budgetDao.getAllBudgets()
    }

    suspend fun getBudgetsByMonth(year: Int, month: Int) = withContext(Dispatchers.IO) {
        budgetDao.getBudgetsByMonth(year, month)
    }

    suspend fun insertBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.insert(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.update(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        budgetDao.delete(budget)
    }

    suspend fun getBudgetByCategoryAndMonth(category: String, year: Int, month: Int) = withContext(Dispatchers.IO) {
        budgetDao.getBudgetByCategoryAndMonth(category, year, month)
    }

    suspend fun getTotalBudgetByMonth(year: Int, month: Int) = withContext(Dispatchers.IO) {
        budgetDao.getTotalBudgetByMonth(year, month)
    }

    suspend fun getRemainingBudgetByMonth(year: Int, month: Int) = withContext(Dispatchers.IO) {
        budgetDao.getRemainingBudgetByMonth(year, month)
    }

    // 新增：为“总预算”按月设置一个唯一条目（先删除同类项，再插入），避免出现重复累加
    suspend fun setTotalBudgetForMonth(year: Int, month: Int, totalBudgetAmount: Long, spentAmount: Long = 0L) = withContext(Dispatchers.IO) {
        // 删除同类的条目，确保本月只有一个“总预算”条目
        budgetDao.deleteByCategoryAndMonth("总预算", year, month)
        val b = BudgetEntity().apply {
            id = 0
            this.category = "总预算"
            budgetAmount = totalBudgetAmount
            this.spentAmount = spentAmount
            this.year = year
            this.month = month
            note = "本月总预算"
            createTime = System.currentTimeMillis()
            updateTime = System.currentTimeMillis()
        }
        budgetDao.insert(b)
    }

    // 更新预算支出金额
    suspend fun updateBudgetSpentAmount(category: String, year: Int, month: Int, spentAmount: Long) = withContext(Dispatchers.IO) {
        val budget = budgetDao.getBudgetByCategoryAndMonth(category, year, month)
        if (budget != null) {
            budget.spentAmount = spentAmount
            budget.updateTime = System.currentTimeMillis()
            budgetDao.update(budget)
        }
    }

    // 初始化默认分类（如果不存在）
    suspend fun initializeDefaultCategories() = withContext(Dispatchers.IO) {
        // categoryDao.getAllCategories() returns a List<CategoryEntity>, so use it directly
        val existingCategories = categoryDao.getAllCategories()

        if (existingCategories.isEmpty()) {
            val categories = listOf(
                // 支出分类
                CategoryEntity().apply { name = "餐饮"; type = 0; iconRes = "restaurant" },
                CategoryEntity().apply { name = "购物"; type = 0; iconRes = "shopping" },
                CategoryEntity().apply { name = "交通"; type = 0; iconRes = "transport" },
                CategoryEntity().apply { name = "娱乐"; type = 0; iconRes = "entertainment" },
                CategoryEntity().apply { name = "医疗"; type = 0; iconRes = "medical" },
                CategoryEntity().apply { name = "教育"; type = 0; iconRes = "education" },
                CategoryEntity().apply { name = "住房"; type = 0; iconRes = "housing" },
                CategoryEntity().apply { name = "保险"; type = 0; iconRes = "insurance" },
                CategoryEntity().apply { name = "通讯"; type = 0; iconRes = "communication" },
                CategoryEntity().apply { name = "旅游"; type = 0; iconRes = "travel" },
                CategoryEntity().apply { name = "其他"; type = 0; iconRes = "other" },

                // 收入分类
                CategoryEntity().apply { name = "工资"; type = 1; iconRes = "salary" },
                CategoryEntity().apply { name = "奖金"; type = 1; iconRes = "bonus" },
                CategoryEntity().apply { name = "投资"; type = 1; iconRes = "investment" },
                CategoryEntity().apply { name = "兼职"; type = 1; iconRes = "part_time" },
                CategoryEntity().apply { name = "其他收入"; type = 1; iconRes = "other_income" }
             )

             categories.forEach { category ->
                 categoryDao.insert(category)
             }
         }
     }
 }

 // 统计数据类
data class TodayStats(
    val date: String,
    val expense: Long,
    val income: Long,
    val balance: Long
)

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val expense: Long,
    val income: Long,
    val balance: Long,
    val categoryStats: Map<Int, Long>
)

data class AssetOverview(
    val totalAssets: Long,
    val totalLiabilities: Long,
    val netAssets: Long,
    val accounts: List<AccountItem>
)

// 扩展函数
fun AccountEntity.toAccountItem(): AccountItem {
    return AccountItem(
        id = this.id.toLong(),
        type = if (this.balance >= 0) AccountType.ASSET else AccountType.LIABILITY,
        name = this.name,
        amount = this.balance.toDouble() / 100.0, // 转换为元
        category = getAccountCategory(this.name),
        color = if (this.color == 0) getDefaultAccountColor(this.name) else this.color
    )
}

private fun getAccountCategory(name: String): String {
    return when {
        name.contains("现金") -> "现金"
        name.contains("银行卡") || name.contains("储蓄") -> "储蓄卡"
        name.contains("微信") -> "微信"
        name.contains("支付宝") -> "支付宝"
        name.contains("信用卡") -> "信用卡"
        name.contains("贷款") -> "贷款"
        else -> "其他"
    }
}

private fun getDefaultAccountColor(name: String): Int {
    return when {
        name.contains("现金") -> 0xFFFF9800.toInt() // 橙色
        name.contains("银行卡") || name.contains("储蓄") -> 0xFF2196F3.toInt() // 蓝色
        name.contains("微信") -> 0xFF4CAF50.toInt() // 绿色
        name.contains("支付宝") -> 0xFF2196F3.toInt() // 蓝色
        name.contains("信用卡") -> 0xFFF44336.toInt() // 红色
        name.contains("贷款") -> 0xFFF44336.toInt() // 红色
        else -> 0xFF9C27B0.toInt() // 紫色
    }
}
