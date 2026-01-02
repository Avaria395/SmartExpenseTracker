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

class YearlyStatisticsViewModel(application: Application, private val year: Int) : ViewModel() {

    private val repository: TransactionRepository = TransactionRepository(application)

    private val _yearlyStats = MutableStateFlow<MonthlyStats?>(null)
    val yearlyStats: StateFlow<MonthlyStats?> = _yearlyStats.asStateFlow()

    private val _categoryData = MutableStateFlow<Map<CategoryEntity, Long>>(emptyMap())
    val categoryData: StateFlow<Map<CategoryEntity, Long>> = _categoryData.asStateFlow()

    private val _trendData = MutableStateFlow<List<Float>>(emptyList())
    val trendData: StateFlow<List<Float>> = _trendData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadYearlyData()
    }

    private fun loadYearlyData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (yearlyStatsResult, categoryDataResult, trendDataResult) = withContext(Dispatchers.IO) {
                    val calendar = Calendar.getInstance()
                    calendar.set(year, 0, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startTime = calendar.timeInMillis

                    calendar.set(year, 11, 31, 23, 59, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    val endTime = calendar.timeInMillis

                    val stats = repository.getMonthlyStats(startTime, endTime).first()
                    val categories = repository.getCategoryExpensesForPeriod(startTime, endTime).first()
                    val trend = repository.getMonthlyExpenseTrend(startTime, endTime).first()
                    Triple(stats, categories, trend)
                }

                _yearlyStats.value = yearlyStatsResult
                _categoryData.value = categoryDataResult
                _trendData.value = trendDataResult
            } catch (e: Exception) {
                _error.value = "Failed to load yearly data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class YearlyStatisticsViewModelFactory(private val application: Application, private val year: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YearlyStatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YearlyStatisticsViewModel(application, year) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}