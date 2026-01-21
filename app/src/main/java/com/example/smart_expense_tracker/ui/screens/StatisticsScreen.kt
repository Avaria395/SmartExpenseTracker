package com.example.smart_expense_tracker.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_expense_tracker.repository.MonthlyStats
import com.example.smart_expense_tracker.ui.components.CategoryDetailsCard
import com.example.smart_expense_tracker.ui.components.CategoryPieChartCard
import com.example.smart_expense_tracker.ui.components.ExpenseOverviewCard
import com.example.smart_expense_tracker.ui.components.ExpenseTrendCard
import com.example.smart_expense_tracker.viewmodel.StatisticsViewModel
import com.example.smart_expense_tracker.viewmodel.StatisticsViewModelFactory
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMonthly: (Int, Int) -> Unit,
    onNavigateToYearly: (Int) -> Unit,
    context: Context = LocalContext.current,
) {
    val application = context.applicationContext as Application
    val viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModelFactory(application))

    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val categoryData by viewModel.categoryData.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(selectedPeriod) {
        viewModel.setPeriod(selectedPeriod)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("消费统计") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") } },
                actions = { IconButton(onClick = { viewModel.setPeriod(selectedPeriod) }) { Icon(Icons.Default.Refresh, contentDescription = "刷新") } },
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
                item { TimePeriodSelector(selectedPeriod, onPeriodSelected = { viewModel.setPeriod(it) }, onNavigateToMonthly = onNavigateToMonthly, onNavigateToYearly = onNavigateToYearly) }
                item { PeriodTitle(selectedPeriod, monthlyStats) }
                item { ExpenseOverviewCard(monthlyStats) }
                item { CategoryPieChartCard(categoryData) }
                // 将分类详情放到分类占比下方
                item { CategoryDetailsCard(categoryData) }
                item { ExpenseTrendCard(selectedPeriod, trendData, dataIsInCents = false) } // Main screen data is in yuan
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: Int, 
    onPeriodSelected: (Int) -> Unit,
    onNavigateToMonthly: (Int, Int) -> Unit,
    onNavigateToYearly: (Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH) + 1

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.spacedBy(8.dp)) {
            TimePeriodButton("本周", selectedPeriod == 0, { onPeriodSelected(0) }, Modifier.weight(1f))
            TimePeriodButton("本月", selectedPeriod == 1, {
                if (selectedPeriod == 1) {
                    onNavigateToMonthly(currentYear, currentMonth)
                } else {
                    onPeriodSelected(1)
                }
            }, Modifier.weight(1f))
            TimePeriodButton("本年", selectedPeriod == 2, {
                if (selectedPeriod == 2) {
                    onNavigateToYearly(currentYear)
                } else {
                    onPeriodSelected(2)
                }
            }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TimePeriodButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick, modifier = modifier,
        colors = ButtonDefaults.buttonColors(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
        shape = CircleShape,
    ) {
        Text(text, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PeriodTitle(selectedPeriod: Int, monthlyStats: MonthlyStats?) {
    val title = when (selectedPeriod) {
        0 -> if (monthlyStats != null) "${monthlyStats.year}年第${monthlyStats.month}周" else "本周"
        2 -> if (monthlyStats != null) "${monthlyStats.year}年" else "本年"
        else -> if (monthlyStats != null) "${monthlyStats.year}年${monthlyStats.month}月" else "本月"
    }
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}
