package com.example.smart_expense_tracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smart_expense_tracker.ui.screens.AssetsScreen
import com.example.smart_expense_tracker.ui.screens.DateTransactionScreen
import com.example.smart_expense_tracker.ui.screens.HomeScreen
import com.example.smart_expense_tracker.ui.screens.StatisticsScreen
import com.example.smart_expense_tracker.ui.screens.AiScreen
import com.example.smart_expense_tracker.ui.screens.MonthlyStatisticsScreen
import com.example.smart_expense_tracker.ui.screens.YearlyStatisticsScreen
import com.example.smart_expense_tracker.ui.theme.SmartExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    
    private var showAddTransactionState = mutableStateOf(false)
    private var showStatisticsState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)
        
        setContent {
            SmartExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val showAdd by remember { showAddTransactionState }
                    val showStats by remember { showStatisticsState }
                    SmartExpenseApp(
                        showAddTransaction = showAdd,
                        showStatistics = showStats,
                        onAddConsumed = { showAddTransactionState.value = false },
                        onStatsConsumed = { showStatisticsState.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "com.example.smart_expense_tracker.ACTION_QUICK_ADD" -> {
                showAddTransactionState.value = true
            }
            "com.example.smart_expense_tracker.ACTION_VIEW_STATISTICS" -> {
                showStatisticsState.value = true
            }
        }
    }
}

@Composable
fun SmartExpenseApp(
    navController: NavHostController = rememberNavController(),
    showAddTransaction: Boolean = false,
    showStatistics: Boolean = false,
    onAddConsumed: () -> Unit = {},
    onStatsConsumed: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    onNavigateToAssets = { navController.navigate("assets") },
                    onNavigateToStatistics = { navController.navigate("statistics") },
                    onNavigateToAi = { navController.navigate("ai") },
                    onNavigateToDate = { date -> navController.navigate("date_transaction/$date") },
                    initialShowAddDialog = showAddTransaction
                )
                
                if (showAddTransaction) {
                    LaunchedEffect(Unit) {
                        onAddConsumed()
                    }
                }
                
                if (showStatistics) {
                    LaunchedEffect(Unit) {
                        navController.navigate("statistics")
                        onStatsConsumed()
                    }
                }
            }
            composable("assets") {
                AssetsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("statistics") {
                StatisticsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMonthly = { year, month -> navController.navigate("monthly_statistics/$year/$month") },
                    onNavigateToYearly = { year -> navController.navigate("yearly_statistics/$year") }
                )
            }
            composable("ai") {
                AiScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                "date_transaction/{date}",
                arguments = listOf(navArgument("date") { type = NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: 0L
                DateTransactionScreen(
                    date = date,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                "monthly_statistics/{year}/{month}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 0
                val month = backStackEntry.arguments?.getInt("month") ?: 0
                MonthlyStatisticsScreen(
                    year = year,
                    month = month,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMonthly = { newYear, newMonth ->
                        navController.navigate("monthly_statistics/$newYear/$newMonth") {
                            popUpTo("monthly_statistics/{year}/{month}") { inclusive = true }
                        }
                    }
                )
            }
            composable(
                "yearly_statistics/{year}",
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val year = backStackEntry.arguments?.getInt("year") ?: 0
                YearlyStatisticsScreen(
                    year = year,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToYearly = { newYear ->
                        navController.navigate("yearly_statistics/$newYear") {
                            popUpTo("yearly_statistics/{year}") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
