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
import com.example.smart_expense_tracker.viewmodel.YearlyStatisticsViewModel
import com.example.smart_expense_tracker.viewmodel.YearlyStatisticsViewModelFactory
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyStatisticsScreen(
    year: Int,
    onNavigateBack: () -> Unit,
    onNavigateToYearly: (Int) -> Unit,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: YearlyStatisticsViewModel = viewModel(factory = YearlyStatisticsViewModelFactory(application, year))

    val yearlyStats by viewModel.yearlyStats.collectAsState()
    val categoryData by viewModel.categoryData.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$year 年") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = {
                    Row {
                        IconButton(onClick = { onNavigateToYearly(year - 1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上一年")
                        }
                        IconButton(
                            onClick = { onNavigateToYearly(year + 1) },
                            enabled = year < currentYear
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下一年")
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
                item { ExpenseOverviewCard(yearlyStats) }
                item { CategoryPieChartCard(categoryData) }
                item { ExpenseTrendCard(2, trendData, dataIsInCents = true) } // Monthly/Yearly screens data is in cents
                item { CategoryDetailsCard(categoryData) }
            }
        }
    }
}