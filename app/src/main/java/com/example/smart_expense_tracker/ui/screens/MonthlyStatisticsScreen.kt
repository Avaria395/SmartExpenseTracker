package com.example.smart_expense_tracker.ui.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_expense_tracker.ui.components.CategoryDetailsCard
import com.example.smart_expense_tracker.ui.components.CategoryPieChartCard
import com.example.smart_expense_tracker.ui.components.ExpenseOverviewCard
import com.example.smart_expense_tracker.ui.components.ExpenseTrendCard
import com.example.smart_expense_tracker.viewmodel.MonthlyStatisticsViewModel
import com.example.smart_expense_tracker.viewmodel.MonthlyStatisticsViewModelFactory
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyStatisticsScreen(
    year: Int,
    month: Int,
    onNavigateBack: () -> Unit,
    onNavigateToMonthly: (Int, Int) -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: MonthlyStatisticsViewModel = viewModel(factory = MonthlyStatisticsViewModelFactory(application, year, month))

    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val categoryData by viewModel.categoryData.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$year 年 $month 月") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = {
                    Row {
                        IconButton(onClick = {
                            val newMonth = if (month == 1) 12 else month - 1
                            val newYear = if (month == 1) year - 1 else year
                            onNavigateToMonthly(newYear, newMonth)
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上个月")
                        }
                        IconButton(
                            onClick = {
                                val newMonth = if (month == 12) 1 else month + 1
                                val newYear = if (month == 12) year + 1 else year
                                onNavigateToMonthly(newYear, newMonth)
                            },
                            enabled = year < currentYear || (year == currentYear && month < currentMonth)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下个月")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (error != null) {
            // Error state UI
        } else if (isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ExpenseOverviewCard(monthlyStats) }
                item { CategoryPieChartCard(categoryData) }
                item { ExpenseTrendCard(1, trendData, dataIsInCents = true) } // Monthly/Yearly screens data is in cents
                item { CategoryDetailsCard(categoryData) }
            }
        }
    }
}