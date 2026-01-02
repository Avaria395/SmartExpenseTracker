package com.example.smart_expense_tracker.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.smart_expense_tracker.database.dao.AccountDao;
import com.example.smart_expense_tracker.database.dao.AiReportDao;
import com.example.smart_expense_tracker.database.dao.BookDao;
import com.example.smart_expense_tracker.database.dao.BudgetDao;
import com.example.smart_expense_tracker.database.dao.CategoryDao;
import com.example.smart_expense_tracker.database.dao.TransactionDao;
import com.example.smart_expense_tracker.database.entity.AccountEntity;
import com.example.smart_expense_tracker.database.entity.AiReportEntity;
import com.example.smart_expense_tracker.database.entity.BudgetEntity;
import com.example.smart_expense_tracker.database.entity.BookEntity;
import com.example.smart_expense_tracker.database.entity.CategoryEntity;
import com.example.smart_expense_tracker.database.entity.TransactionEntity;

@Database(entities = {
        BookEntity.class,
        CategoryEntity.class,
        AccountEntity.class,
        TransactionEntity.class,
        AiReportEntity.class,
        BudgetEntity.class
}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract BookDao bookDao();
    public abstract CategoryDao categoryDao();
    public abstract AccountDao accountDao();
    public abstract TransactionDao transactionDao();
    public abstract AiReportDao aiReportDao();
    public abstract BudgetDao budgetDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "smart_expense.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
