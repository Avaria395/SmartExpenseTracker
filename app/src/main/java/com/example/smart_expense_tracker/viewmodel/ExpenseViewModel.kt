package com.example.smart_expense_tracker.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smart_expense_tracker.SummaryWidgetProvider
import com.example.smart_expense_tracker.database.entity.AccountEntity
import com.example.smart_expense_tracker.database.entity.BookEntity
import com.example.smart_expense_tracker.database.entity.CategoryEntity
import com.example.smart_expense_tracker.database.entity.TransactionEntity
import com.example.smart_expense_tracker.model.AccountItem
import com.example.smart_expense_tracker.repository.ExpenseRepository
import com.example.smart_expense_tracker.repository.TodayStats
import com.example.smart_expense_tracker.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository.getInstance(application)

    private val _todayStats = MutableStateFlow<TodayStats?>(null)
    val todayStats: StateFlow<TodayStats?> = _todayStats.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(0L)
    val monthlyBudget: StateFlow<Long> = _monthlyBudget.asStateFlow()

    private val _remainingBudget = MutableStateFlow(0L)
    val remainingBudget: StateFlow<Long> = _remainingBudget.asStateFlow()

    private val _budgetUsage = MutableStateFlow(0f)
    val budgetUsage: StateFlow<Float> = _budgetUsage.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountEntity>>(emptyList())
    val accounts: StateFlow<List<AccountEntity>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.initializeDefaultData()
                repository.initializeDefaultCategories()
                loadTodayStats()
                loadRecentTransactions()
                loadBudgetData()
                loadCategories()
                loadAccounts()
                loadBooks()
            } catch (e: Exception) {
                _error.value = "加载数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadTodayStats() {
        _todayStats.value = repository.getTodayStats()
    }

    private suspend fun loadRecentTransactions() {
        val list = repository.getAllTransactions()
        _transactions.value = list.take(10)
    }

    private suspend fun loadAccounts() {
        _accounts.value = repository.getAllAccounts()
    }

    private suspend fun loadCategories() {
        _categories.value = repository.getAllCategories()
    }

    private suspend fun loadBooks() {
        _books.value = repository.getAllBooks()
    }

    fun addTransaction(
        bookId: Int, categoryId: Int, accountId: Int, amount: Long, type: Int, remark: String, transactionDate: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val transaction = TransactionEntity().apply {
                    this.bookId = bookId
                    this.categoryId = categoryId
                    this.accountId = accountId
                    this.amount = amount
                    this.type = type
                    this.recordTime = transactionDate ?: System.currentTimeMillis()
                    this.remark = remark
                }
                repository.insertTransaction(transaction)
                loadData()
                notifyWidgetsDataChanged()
            } catch (e: Exception) {
                _error.value = "添加交易失败: ${e.message}"
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                loadData()
                notifyWidgetsDataChanged()
            } catch (e: Exception) {
                _error.value = "删除交易失败: ${e.message}"
            }
        }
    }

    private suspend fun loadBudgetData() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        _monthlyBudget.value = repository.getTotalBudgetByMonth(year, month)
    }

    fun addBudget(budgetAmount: Long) {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                val existing = repository.getBudgetByCategoryAndMonth("总预算", year, month)
                val existingSpent = existing?.spentAmount ?: 0L
                repository.setTotalBudgetForMonth(year, month, budgetAmount, existingSpent)
                loadBudgetData()
                notifyWidgetsDataChanged()
            } catch (e: Exception) {
                _error.value = "添加预算失败: ${e.message}"
            }
        }
    }

    private fun notifyWidgetsDataChanged() {
        val intent = Intent(getApplication(), SummaryWidgetProvider::class.java).apply {
            action = Constants.ACTION_DATA_UPDATED
        }
        getApplication<Application>().sendBroadcast(intent)
    }

    fun clearError() {
        _error.value = null
    }
}

class AssetsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository.getInstance(application)

    private val _accounts = MutableStateFlow<List<AccountItem>>(emptyList())
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()

    private val _totalAssets = MutableStateFlow(0.0)
    val totalAssets: StateFlow<Double> = _totalAssets.asStateFlow()

    private val _totalLiabilities = MutableStateFlow(0.0)
    val totalLiabilities: StateFlow<Double> = _totalLiabilities.asStateFlow()

    private val _netAssets = MutableStateFlow(0.0)
    val netAssets: StateFlow<Double> = _netAssets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshAssetOverview()
    }

    fun refreshAssetOverview() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val overview = repository.getAssetOverview()
                _accounts.value = overview.accounts
                _totalAssets.value = overview.totalAssets / 100.0
                _totalLiabilities.value = overview.totalLiabilities / 100.0
                _netAssets.value = overview.netAssets / 100.0
            } catch (e: Exception) {
                _error.value = "加载资产数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addAccount(name: String, balance: Long, category: String) {
        viewModelScope.launch {
            try {
                val account = AccountEntity().apply {
                    this.name = name
                    this.balance = balance
                    this.color = getDefaultColorForCategory(category)
                }
                repository.insertAccount(account)
                refreshAssetOverview()
            } catch (e: Exception) {
                _error.value = "添加账户失败: ${e.message}"
            }
        }
    }

    private fun getDefaultColorForCategory(category: String): Int {
        return when (category) {
            "现金" -> 0xFFFF9800.toInt()
            "储蓄卡" -> 0xFF2196F3.toInt()
            "微信" -> 0xFF4CAF50.toInt()
            "支付宝" -> 0xFF2196F3.toInt()
            "信用卡" -> 0xFFF44336.toInt()
            "贷款" -> 0xFFF44336.toInt()
            else -> 0xFF9C27B0.toInt()
        }
    }

    fun deleteAccountById(accountId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteAccountById(accountId)
                refreshAssetOverview()
            } catch (e: Exception) {
                _error.value = "删除账户失败: ${e.message}"
            }
        }
    }

    fun updateAccountBalance(accountId: Long, newBalance: Long) {
        viewModelScope.launch {
            try {
                repository.setAccountBalance(accountId.toInt(), newBalance)
                refreshAssetOverview()
            } catch (e: Exception) {
                _error.value = "更新账户余额失败: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
