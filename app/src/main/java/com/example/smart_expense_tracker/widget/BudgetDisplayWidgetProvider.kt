package com.example.smart_expense_tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.smart_expense_tracker.MainActivity
import com.example.smart_expense_tracker.R
import com.example.smart_expense_tracker.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class BudgetDisplayWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val ACTION_ROOT_CLICK = "com.example.smart_expense_tracker.ACTION_WIDGET_ROOT_CLICK"
        private const val ACTION_WIDGET_SWITCH = "com.example.smart_expense_tracker.ACTION_WIDGET_SWITCH"
        private const val PREFS_NAME = "WidgetPrefs"
        private const val PREF_VIEW_MODE = "view_mode_"
        private const val PREF_LAST_CLICK_TIME = "last_click_"

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BudgetDisplayWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BudgetDisplayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                val repository = ExpenseRepository.getInstance(context.applicationContext)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1

                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                val endOfMonth = cal.timeInMillis

                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = todayCal.timeInMillis
                todayCal.set(Calendar.HOUR_OF_DAY, 23); todayCal.set(Calendar.MINUTE, 59); todayCal.set(Calendar.SECOND, 59); todayCal.set(Calendar.MILLISECOND, 999)
                val endOfToday = todayCal.timeInMillis

                val totalBudget = repository.getTotalBudgetByMonth(year, month)
                val totalExpense = repository.getTotalExpense(startOfMonth, endOfMonth)
                val remainingBudget = if (totalBudget > 0) totalBudget - totalExpense else 0L
                val progress = if (totalBudget > 0) (totalExpense * 100 / totalBudget).toInt().coerceIn(0, 100) else 0

                val todayExpense = repository.getTotalExpense(startOfToday, endOfToday)
                val todayIncome = repository.getTotalIncome(startOfToday, endOfToday)

                val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_budget_display)
                    val mode = prefs.getInt(PREF_VIEW_MODE + appWidgetId, 0)

                    if (mode == 0) {
                        views.setViewVisibility(R.id.layout_monthly_view, View.VISIBLE)
                        views.setViewVisibility(R.id.layout_daily_view, View.GONE)
                        views.setTextViewText(R.id.tv_spent_amount, nf.format(totalExpense / 100.0))
                        views.setTextViewText(R.id.tv_total_budget, nf.format(totalBudget / 100.0))
                        views.setTextViewText(R.id.tv_remaining_budget, nf.format(remainingBudget / 100.0))
                        views.setTextViewText(R.id.tv_progress_percent, "$progress%")
                        views.setProgressBar(R.id.pb_budget, 100, progress, false)
                    } else {
                        views.setViewVisibility(R.id.layout_monthly_view, View.GONE)
                        views.setViewVisibility(R.id.layout_daily_view, View.VISIBLE)
                        views.setTextViewText(R.id.tv_today_expense, nf.format(todayExpense / 100.0))
                        views.setTextViewText(R.id.tv_today_income, nf.format(todayIncome / 100.0))
                    }

                    // 绑定切换按钮逻辑
                    val switchIntent = Intent(context, BudgetDisplayWidgetProvider::class.java).apply {
                        action = ACTION_WIDGET_SWITCH
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val switchPendingIntent = PendingIntent.getBroadcast(
                        context, appWidgetId + 1000, switchIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_switch, switchPendingIntent)

                    // 绑定根容器点击逻辑（单击进入统计，双击也可切换）
                    val rootIntent = Intent(context, BudgetDisplayWidgetProvider::class.java).apply {
                        action = ACTION_ROOT_CLICK
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    val rootPendingIntent = PendingIntent.getBroadcast(
                        context, appWidgetId, rootIntent, 
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root_container, rootPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Log.e("BudgetWidget", "Error updating widget", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (ids != null && ids.isNotEmpty()) {
                onUpdate(context, AppWidgetManager.getInstance(context), ids)
            }
        }
        
        super.onReceive(context, intent)
        
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_WIDGET_SWITCH -> {
                toggleViewMode(context, appWidgetId)
            }
            ACTION_ROOT_CLICK -> {
                handleRootClick(context, appWidgetId)
            }
        }
    }

    private fun toggleViewMode(context: Context, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentMode = prefs.getInt(PREF_VIEW_MODE + appWidgetId, 0)
        val newMode = if (currentMode == 0) 1 else 0
        prefs.edit().putInt(PREF_VIEW_MODE + appWidgetId, newMode).apply()
        onUpdate(context, AppWidgetManager.getInstance(context), intArrayOf(appWidgetId))
    }

    private fun handleRootClick(context: Context, appWidgetId: Int) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentTime = System.currentTimeMillis()
                val lastClickTime = prefs.getLong(PREF_LAST_CLICK_TIME + appWidgetId, 0L)

                if (currentTime - lastClickTime < 500) {
                    // 双击：依然保留切换逻辑
                    toggleViewMode(context, appWidgetId)
                    prefs.edit().putLong(PREF_LAST_CLICK_TIME + appWidgetId, 0L).apply()
                } else {
                    // 单击逻辑
                    prefs.edit().putLong(PREF_LAST_CLICK_TIME + appWidgetId, currentTime).apply()
                    delay(520)
                    val latestLastClick = prefs.getLong(PREF_LAST_CLICK_TIME + appWidgetId, 0L)
                    if (latestLastClick == currentTime) {
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            action = "com.example.smart_expense_tracker.ACTION_VIEW_STATISTICS"
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(mainIntent)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
