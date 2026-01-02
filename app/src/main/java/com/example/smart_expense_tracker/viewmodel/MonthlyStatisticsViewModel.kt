package com.example.smart_expense_tracker.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smart_expense_tracker.database.entity.CategoryEntity
import com.example.smart_expense_tracker.repository.MonthlyStats
import com.example.smart_expense_tracker.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MonthlyStatisticsViewModel(application: Application, private val year: Int, private val month: Int) : ViewModel() {

    private val repository: TransactionRepository = TransactionRepository(application)

    private val _monthlyStats = MutableStateFlow<MonthlyStats?>(null)
    val monthlyStats: StateFlow<MonthlyStats?> = _monthlyStats.asStateFlow()

    private val _categoryData = MutableStateFlow<Map<CategoryEntity, Long>>(emptyMap())
    val categoryData: StateFlow<Map<CategoryEntity, Long>> = _categoryData.asStateFlow()

    private val _trendData = MutableStateFlow<List<Float>>(emptyList())
    val trendData: StateFlow<List<Float>> = _trendData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadMonthlyData()
    }

    private fun loadMonthlyData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (monthlyStatsResult, categoryDataResult, trendDataResult) = withContext(Dispatchers.IO) {
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month - 1, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTime = calendar.timeInMillis

                    val stats = repository.getMonthlyStats(startTime, endTime).first()
                    val categories = repository.getCategoryExpensesForPeriod(startTime, endTime).first()
                    val trend = repository.getDailyExpenseTrend(startTime, endTime).first()
                    Triple(stats, categories, trend)
                }

                _monthlyStats.value = monthlyStatsResult
                _categoryData.value = categoryDataResult
                _trendData.value = trendDataResult
            } catch (e: Exception) {
                _error.value = "Failed to load monthly data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class MonthlyStatisticsViewModelFactory(private val application: Application, private val year: Int, private val month: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonthlyStatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MonthlyStatisticsViewModel(application, year, month) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}