package com.example.smart_expense_tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smart_expense_tracker.database.entity.CategoryEntity
import com.example.smart_expense_tracker.repository.MonthlyStats

@Composable
fun ExpenseOverviewCard(monthlyStats: MonthlyStats?) {
    val totalExpense = monthlyStats?.expense ?: 0
    val totalIncome = monthlyStats?.income ?: 0
    val balance = monthlyStats?.balance ?: 0

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("消费概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                ExpenseSummaryItem("支出", "¥${totalExpense / 100.0}", MaterialTheme.colorScheme.error, Icons.Default.TrendingDown)
                ExpenseSummaryItem("收入", "¥${totalIncome / 100.0}", MaterialTheme.colorScheme.primary, Icons.Default.TrendingUp)
                val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ExpenseSummaryItem("结余", "¥${balance / 100.0}", balanceColor, Icons.Default.AccountBalance)
            }
        }
    }
}

@Composable
fun ExpenseSummaryItem(label: String, value: String, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CategoryPieChartCard(categoryData: Map<CategoryEntity, Long>) {
    fun nameToColor(name: String): Color {
        val hash = name.hashCode()
        return Color((0xFF shl 24) or (hash and 0xFFFFFF))
    }
    val pieChartData = categoryData.map {
        PieChartData(it.key.name ?: "", it.value.toDouble() / 100.0, nameToColor(it.key.name ?: ""))
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("分类占比", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (pieChartData.isEmpty()) {
                Text("暂无数据", Modifier.align(Alignment.CenterHorizontally))
            } else {
                PieChartWithLegend(pieChartData, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun CategoryDetailsCard(categoryData: Map<CategoryEntity, Long>) {
    val total = categoryData.values.sum().takeIf { it > 0 } ?: 1L
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("分类详情", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            categoryData.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                CategoryDetailItem(category.name ?: "", amount / 100.0, amount.toFloat() / total.toFloat())
            }
        }
    }
}

@Composable
fun CategoryDetailItem(category: String, amount: Double, percentage: Float) {
    val color = when (category) {
        "餐饮" -> Color(0xFFFF7043)
        "购物" -> Color(0xFF42A5F5)
        "交通" -> Color(0xFF66BB6A)
        else -> Color(0xFFAB47BC)
    }
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(16.dp).background(color, CircleShape))
            Text(category, style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("¥${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ExpenseTrendCard(selectedPeriod: Int, trendData: List<Float>, dataIsInCents: Boolean) {
    val title = when (selectedPeriod) {
        0 -> "本周消费趋势"
        1 -> "本月消费趋势"
        else -> "本年消费趋势"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (trendData.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxAmount = trendData.maxOrNull()?.takeIf { it > 0f } ?: 1f
                    trendData.forEachIndexed { index, amount ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (amount > 0f) {
                                val displayAmount = if (dataIsInCents) amount / 100f else amount
                                Text(
                                    text = "¥${String.format("%.2f", displayAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            val barHeight = (amount / maxAmount * 120).dp
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = when (selectedPeriod) {
                                    0 -> "第 ${index + 1} 天"
                                    1 -> "${index + 1}"
                                    else -> "${index + 1} 月"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
