package com.example.smart_expense_tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // ktlint-disable no-wildcard-imports
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smart_expense_tracker.model.AccountItem
import com.example.smart_expense_tracker.model.AccountType
import com.example.smart_expense_tracker.viewmodel.AssetsViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AssetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AssetsViewModel = viewModel(),
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountItem?>(null) }

    val accounts by viewModel.accounts.collectAsState()
    val totalAssets by viewModel.totalAssets.collectAsState()
    val totalLiabilities by viewModel.totalLiabilities.collectAsState()
    val netAssets by viewModel.netAssets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产管理") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { showAddAccountDialog = true }) { Icon(Icons.Default.Add, "添加账户") } },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        if (error != null) {
            // Error handling
        } else if (isLoading) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { AssetOverviewCard(totalAssets, totalLiabilities, netAssets) }
                item {
                    Text("账户列表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (accounts.isEmpty()) {
                    item { EmptyStateCard { showAddAccountDialog = true } }
                } else {
                    items(accounts) { account ->
                        AccountItemCard(account, { viewModel.deleteAccountById(account.id.toInt()) }) { accountToEdit = account }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = {
                val balanceInCents = (it.balance * 100).toLong()
                viewModel.addAccount(it.name, balanceInCents, it.category)
                showAddAccountDialog = false
            },
        )
    }

    accountToEdit?.let {
        EditAccountDialog(it, { accountToEdit = null }) { _, balanceDouble ->
            val balanceInCents = (balanceDouble * 100).toLong()
            viewModel.updateAccountBalance(it.id, balanceInCents)
            accountToEdit = null
        }
    }
}

data class AddAccountData(val name: String, val balance: Double, val category: String)

@Composable
private fun AssetOverviewCard(totalAssets: Double, totalLiabilities: Double, netAssets: Double) {
    val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("资产概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                AssetSummaryItem("总资产", nf.format(totalAssets), MaterialTheme.colorScheme.primary, Icons.Default.AccountBalanceWallet)
                AssetSummaryItem("总负债", nf.format(totalLiabilities), MaterialTheme.colorScheme.error, Icons.Default.CreditCard)
                val netAssetsColor = if (netAssets >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                AssetSummaryItem("净资产", nf.format(netAssets), netAssetsColor, Icons.Default.TrendingUp)
            }
        }
    }
}

@Composable
private fun AssetSummaryItem(label: String, value: String, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EmptyStateCard(onAddAccountClick: () -> Unit) {
    Card(modifier = Modifier.clickable { onAddAccountClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
            Text("还没有账户", style = MaterialTheme.typography.titleMedium)
            Text("点击添加您的第一个账户", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAddAccountClick) { Text("添加账户") }
        }
    }
}

@Composable
private fun AccountItemCard(account: AccountItem, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(getAccountIconColor(account)), contentAlignment = Alignment.Center) {
                Icon(getAccountIcon(account.category), null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.bodyLarge)
                Text(account.type.displayName, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val color = if (account.type == AccountType.ASSET) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(NumberFormat.getCurrencyInstance(Locale.CHINA).format(account.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                IconButton(onClick = { onEdit() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "编辑", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { onDelete() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

data class AccountTypeChoice(val name: String, val icon: ImageVector)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddAccountDialog(onDismiss: () -> Unit, onSave: (AddAccountData) -> Unit) {
    val accountTypes = listOf(
        AccountTypeChoice("储蓄卡", Icons.Default.CreditCard),
        AccountTypeChoice("微信", Icons.Default.Message),
        AccountTypeChoice("支付宝", Icons.Default.Payment),
        AccountTypeChoice("现金", Icons.Default.AttachMoney),
        AccountTypeChoice("信用卡", Icons.Default.CreditScore),
        AccountTypeChoice("贷款", Icons.Default.RealEstateAgent),
        AccountTypeChoice("其他", Icons.Default.Wallet),
    )
    var selectedType by remember { mutableStateOf(accountTypes.first()) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("添加新账户", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(20.dp))
                Text("选择类型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accountTypes.forEach { type ->
                        AccountTypeChip(item = type, isSelected = selectedType.name == type.name, onClick = { selectedType = type })
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("账户名称") }, placeholder = { Text("例如: 工资卡, 零钱") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("当前余额") }, placeholder = { Text("请输入金额") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.AttachMoney, null) })
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDismiss() }, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(onClick = {
                        val balance = amount.toDoubleOrNull() ?: 0.0
                        onSave(AddAccountData(name.ifEmpty { selectedType.name }, balance, selectedType.name))
                    }, modifier = Modifier.weight(1f), enabled = name.isNotEmpty() && amount.isNotEmpty()) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun AccountTypeChip(item: AccountTypeChoice, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(item.name, fontSize = 12.sp) },
        selected = isSelected,
        leadingIcon = { Icon(item.icon, item.name, modifier = Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
private fun EditAccountDialog(account: AccountItem, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(account.name) }
    var balance by remember { mutableStateOf(String.format(Locale.getDefault(), "%.2f", account.amount)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("编辑账户", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("账户名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = balance, onValueChange = { input ->
                    var s = input.filter { it.isDigit() || it == '.' }
                    if (s.count { it == '.' } > 1) {
                        val first = s.indexOf('.')
                        s = s.substring(0, first + 1) + s.substring(first + 1).replace(".", "")
                    }
                    if (s.contains('.')) {
                        val parts = s.split('.')
                        if ((parts.getOrNull(1)?.length ?: 0) > 2) {
                            s = parts[0] + "." + (parts.getOrNull(1)?.substring(0, 2) ?: "")
                        }
                    }
                    balance = s
                }, label = { Text("余额 (元)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDismiss() }, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(onClick = {
                        val bal = balance.toDoubleOrNull() ?: 0.0
                        onSave(name, bal)
                    }, modifier = Modifier.weight(1f), enabled = name.isNotBlank() && balance.isNotBlank() && (balance.toDoubleOrNull() != null)) { Text("保存") }
                }
            }
        }
    }
}

// 辅助函数
@Composable
private fun getAccountIconColor(account: AccountItem): Color {
    return if (account.color != 0) Color(account.color) else when (account.type) {
        AccountType.ASSET -> when (account.category) {
            "储蓄卡" -> Color(0xFF2196F3)
            "微信" -> Color(0xFF4CAF50)
            "支付宝" -> Color(0xFF2196F3)
            "现金" -> Color(0xFFFF9800)
            else -> Color(0xFF9C27B0)
        }
        AccountType.LIABILITY -> Color(0xFFF44336)
    }
}

@Composable
private fun getAccountIcon(category: String): ImageVector {
    return when (category) {
        "储蓄卡" -> Icons.Default.CreditCard
        "微信" -> Icons.Default.Message
        "支付宝" -> Icons.Default.Payment
        "现金" -> Icons.Default.AttachMoney
        "信用卡" -> Icons.Default.CreditScore
        "贷款" -> Icons.Default.RealEstateAgent
        else -> Icons.Default.Wallet
    }
}
