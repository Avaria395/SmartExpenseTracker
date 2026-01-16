package com.example.smart_expense_tracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_expense_tracker.database.entity.AccountEntity
import com.example.smart_expense_tracker.database.entity.CategoryEntity
import com.example.smart_expense_tracker.repository.TodayStats
import com.example.smart_expense_tracker.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToAssets: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToDate: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    initialShowAddDialog: Boolean = false
) {
    var showAddTransactionSheet by remember { mutableStateOf(initialShowAddDialog) }
    var showBudgetEditDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    val books by viewModel.books.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val todayStats by viewModel.todayStats.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TodayInfoCard(todayStats = todayStats, onDateSelected = onNavigateToDate) }
            item {
                MonthlySummaryCard(
                    monthlyExpense = monthlyExpense,
                    monthlyIncome = monthlyIncome,
                    onEditBudget = { showBudgetEditDialog = true },
                    viewModel = viewModel,
                )
            }
            item {
                QuickActionButtons(
                    onAddTransaction = { showAddTransactionSheet = true },
                    onStatisticsClick = onNavigateToStatistics,
                    onAiClick = onNavigateToAi,
                    onAssetsClick = onNavigateToAssets,
                )
            }
            item {
                Text(
                    text = "最近交易",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (isLoading) {
                item { CenteredCircularProgress() }
            } else if (transactions.isEmpty()) {
                item { EmptyTransactionsCard() }
            } else {
                items(transactions) { transaction ->
                    val cat = categories.find { it.id == transaction.categoryId }
                    TransactionItemCard(
                        transaction = transaction,
                        category = cat,
                        onDelete = { viewModel.deleteTransaction(transaction) },
                    )
                }
            }
        }
    }

    error?.let {
        LaunchedEffect(it) {
            viewModel.clearError()
        }
    }

    if (showAddTransactionSheet) {
        var selectedType by remember { mutableStateOf(0) } // 0: 支出, 1: 收入
        AddTransactionDialog(
            categories = categories,
            accounts = accounts,
            selectedType = selectedType,
            onTypeChange = { selectedType = it },
            onDismiss = { showAddTransactionSheet = false },
            onSave = { cat, acc, amt, rmk, type, date ->
                val amountValue = round(amt.toDouble() * 100).toLong()
                val defaultBookId = books.firstOrNull()?.id ?: 1
                viewModel.addTransaction(defaultBookId, cat.id, acc.id, amountValue, type, rmk, date)
                showAddTransactionSheet = false
            },
        )
    }

    if (showBudgetEditDialog) {
        BudgetEditDialog(
            onDismiss = { showBudgetEditDialog = false },
            onSave = { budgetAmount ->
                coroutineScope.launch {
                    viewModel.addBudget(budgetAmount)
                    snackbarHostState.showSnackbar("预算已保存")
                }
                showBudgetEditDialog = false
            },
        )
    }
}

@Composable
private fun CenteredCircularProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BudgetEditDialog(
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var budgetAmount by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "编辑本月预算",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("预算金额 (元)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss, 
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) { Text("取消") }
                    Button(
                        onClick = {
                            val amount = budgetAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) onSave((amount * 100).toLong())
                        },
                        modifier = Modifier.weight(1f),
                        enabled = budgetAmount.toDoubleOrNull()?.let { it > 0 } ?: false,
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun TodayInfoCard(todayStats: TodayStats?, onDateSelected: (Long) -> Unit) {
    val dateFormat = SimpleDateFormat("M月d日 (E)", Locale.CHINA)
    val todayDate = "今天 ${dateFormat.format(Calendar.getInstance().time)}"
    val expense = todayStats?.expense ?: 0L
    val income = todayStats?.income ?: 0L
    val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = todayStats?.date ?: todayDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "支出 ${nf.format(expense / 100.0)} | 收入 ${nf.format(income / 100.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "选择日期",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }

    if (showDatePicker) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                onDateSelected(cal.timeInMillis)
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}

@Composable
private fun MonthlySummaryCard(
    monthlyExpense: Long,
    monthlyIncome: Long,
    onEditBudget: () -> Unit,
    viewModel: HomeViewModel,
) {
    val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)
    val balance = monthlyIncome - monthlyExpense
    val balanceColor = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val remainingBudget by viewModel.remainingBudget.collectAsState()
    val budgetUsage by viewModel.budgetUsage.collectAsState()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("本月汇总", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem("支出", nf.format(monthlyExpense / 100.0), MaterialTheme.colorScheme.error)
                SummaryItem("收入", nf.format(monthlyIncome / 100.0), MaterialTheme.colorScheme.primary)
                SummaryItem("结余", nf.format(balance / 100.0), balanceColor)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically,
                ) {
                    Text("本月总预算", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (monthlyBudget > 0L) nf.format(monthlyBudget / 100.0) else "—",
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold,
                        )
                        Button(
                            onClick = onEditBudget,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.height(32.dp),
                        ) { Text("编辑", style = MaterialTheme.typography.bodySmall) }
                    }
                }
                LinearProgressIndicator(progress = { budgetUsage.coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                Text(
                    if (monthlyBudget > 0L) {
                        val percent = (budgetUsage * 100).toInt().coerceIn(0, 100)
                        "已用 $percent% | 剩余 ${nf.format(remainingBudget / 100.0)}"
                    } else {
                        "暂无预算数据"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun QuickActionButtons(
    onAddTransaction: () -> Unit,
    onStatisticsClick: () -> Unit,
    onAiClick: () -> Unit,
    onAssetsClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickActionButton(Icons.Default.Analytics, "统计", onStatisticsClick, Modifier.weight(1f))
            QuickActionButton(Icons.Default.SmartToy, "AI分析", onAiClick, Modifier.weight(1f))
            QuickActionButton(Icons.Default.AccountBalanceWallet, "资产", onAssetsClick, Modifier.weight(1f))
        }
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "添加交易",
            onClick = onAddTransaction,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    border: BorderStroke? = null
) {
    Card(
        modifier = modifier.clickable { onClick() }, 
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor, maxLines = 1)
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: com.example.smart_expense_tracker.database.entity.TransactionEntity,
    category: CategoryEntity?,
    onDelete: () -> Unit,
) {
    val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)
    val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
    val (icon, color) = getCategoryVisuals(category?.name ?: "")
    val amountText = nf.format(transaction.amount / 100.0)
    val amountColor = if (transaction.type == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(category?.name ?: "未知", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (!transaction.remark.isNullOrEmpty()) {
                    Text(transaction.remark, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Text(dateFormat.format(Date(transaction.recordTime)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = (if (transaction.type == 0) "-" else "+") + amountText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
            Text("暂无交易记录", style = MaterialTheme.typography.titleMedium)
            Text("点击上方的“添加交易”按钮添加您的第一笔交易", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

private val categoryVisuals = mapOf(
    "餐饮" to (Icons.Default.Restaurant to Color(0xFFFF7043)),
    "购物" to (Icons.Default.ShoppingCart to Color(0xFF42A5F5)),
    "交通" to (Icons.Default.DirectionsCar to Color(0xFF66BB6A)),
    "娱乐" to (Icons.Default.LocalMovies to Color(0xFFAB47BC)),
    "医疗" to (Icons.Default.LocalHospital to Color(0xFFFFA726)),
    "教育" to (Icons.Default.School to Color(0xFF26C6DA)),
    "其他" to (Icons.Default.Category to Color(0xFF78909C)),
)

private fun getCategoryVisuals(categoryName: String): Pair<ImageVector, Color> {
    return categoryVisuals.entries.find { (key, _) -> categoryName.contains(key) }?.value
        ?: (Icons.Default.Category to Color(0xFF78909C))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    selectedType: Int,
    onTypeChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity, AccountEntity, String, String, Int, Long) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    
    val filteredCategories = categories.filter { it.type == selectedType }.distinctBy { it.name }
    
    var selectedCat by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedAcc by remember { mutableStateOf<AccountEntity?>(null) }
    
    // 当列表加载后，自动选择第一个
    LaunchedEffect(filteredCategories) {
        if (selectedCat == null || selectedCat !in filteredCategories) {
            selectedCat = filteredCategories.firstOrNull()
        }
    }
    LaunchedEffect(accounts) {
        if (selectedAcc == null && accounts.isNotEmpty()) {
            selectedAcc = accounts.firstOrNull()
        }
    }

    var transactionDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "记录一笔新交易", 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Type Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(24.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (selectedType == 0) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                                .clickable { onTypeChange(0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("支出", color = if (selectedType == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (selectedType == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onTypeChange(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("收入", color = if (selectedType == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                        label = { Text("金额 (元)", fontWeight = FontWeight.Medium) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                        )
                    )

                    OutlinedTextField(
                        value = remark, 
                        onValueChange = { remark = it }, 
                        label = { Text("备注 (可选)") }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Categories
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("选择分类", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp), 
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            filteredCategories.forEach { cat ->
                                val (icon, iconColor) = getCategoryVisuals(cat.name ?: "")
                                val isSelected = selectedCat?.id == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCat = cat },
                                    label = { Text(cat.name ?: "", fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp), tint = if(isSelected) MaterialTheme.colorScheme.primary else iconColor) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        borderWidth = 1.5.dp,
                                        selectedBorderWidth = 2.5.dp
                                    )
                                )
                            }
                        }
                    }

                    // Accounts
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("选择账户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            accounts.forEach { acc ->
                                val isSelected = selectedAcc?.id == acc.id
                                FilterChip(
                                    selected = isSelected, 
                                    onClick = { selectedAcc = acc }, 
                                    label = { Text(acc.name, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        borderWidth = 1.5.dp,
                                        selectedBorderWidth = 2.5.dp
                                    )
                                )
                            }
                        }
                    }

                    // Date Picker
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("交易日期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val format = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
                                Text(format.format(Date(transactionDate)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (showDatePicker) {
                        val cal = Calendar.getInstance().apply { timeInMillis = transactionDate }
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d)
                            transactionDate = cal.timeInMillis
                            showDatePicker = false
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDismiss() }, 
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                        ) { 
                            Text("取消", fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                        Button(
                            onClick = {
                                if (selectedCat != null && selectedAcc != null && amount.isNotBlank()) {
                                    onSave(selectedCat!!, selectedAcc!!, amount, remark, selectedType, transactionDate)
                                }
                            },
                            enabled = selectedCat != null && selectedAcc != null && amount.isNotBlank(),
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) { 
                            Text("完成记账", fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                    }
                }
            }
        }
    }
}
