package com.example.smart_expense_tracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.smart_expense_tracker.R
import com.example.smart_expense_tracker.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class BudgetDisplayWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        scope.launch {
            try {
                val repository = ExpenseRepository.getInstance(context.applicationContext)
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1

                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val endOfMonth = cal.timeInMillis

                val totalBudget = repository.getTotalBudgetByMonth(year, month)
                val totalExpense = repository.getTotalExpense(startOfMonth, endOfMonth)
                val remainingBudget = if (totalBudget > 0) totalBudget - totalExpense else 0L
                val progress = if (totalBudget > 0) (totalExpense * 100 / totalBudget).toInt().coerceIn(0, 100) else 0

                val nf = NumberFormat.getCurrencyInstance(Locale.CHINA)
                nf.minimumFractionDigits = 2
                nf.maximumFractionDigits = 2

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_budget_display)

                    // 设置文本数据
                    views.setTextViewText(R.id.tv_spent_amount, nf.format(totalExpense / 100.0))
                    views.setTextViewText(R.id.tv_total_budget, nf.format(totalBudget / 100.0))
                    views.setTextViewText(R.id.tv_remaining_budget, nf.format(remainingBudget / 100.0))
                    views.setTextViewText(R.id.tv_progress_percent, "$progress%")
                    
                    // 【关键修复】使用官方最稳定的 setProgressBar 方法，不直接操作 View 权重
                    // 即使在 4x2 尺寸下，ProgressBar 的显示也是最稳定的
                    views.setProgressBar(R.id.pb_budget, 100, progress, false)

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
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BudgetDisplayWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, BudgetDisplayWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
