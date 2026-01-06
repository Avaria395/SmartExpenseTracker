package com.example.smart_expense_tracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.smart_expense_tracker.repository.ExpenseRepository
import com.example.smart_expense_tracker.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class SummaryWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (Constants.ACTION_DATA_UPDATED == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SummaryWidgetProvider::class.java))
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        coroutineScope.launch {
            val repository = ExpenseRepository.getInstance(context)
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1

            val todayStats = repository.getTodayStats()
            val remainingBudget = repository.getRemainingBudgetByMonth(year, month) ?: 0L
            val totalBudget = repository.getTotalBudgetByMonth(year, month) ?: 0L

            val views = RemoteViews(context.packageName, R.layout.widget_summary)
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

            views.setTextViewText(R.id.tv_expense, currencyFormat.format(todayStats.expense / 100.0))
            views.setTextViewText(R.id.tv_income, currencyFormat.format(todayStats.income / 100.0))
            views.setTextViewText(R.id.tv_budget_remaining, "剩余 " + currencyFormat.format(remainingBudget / 100.0))

            val progress = if (totalBudget > 0) {
                ((totalBudget - remainingBudget) * 100 / totalBudget).toInt()
            } else {
                0
            }
            views.setProgressBar(R.id.pb_budget, 100, progress, false)

            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        job.cancel()
    }
}
