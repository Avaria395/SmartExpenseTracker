package com.example.smart_expense_tracker.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.smart_expense_tracker.database.entity.TransactionEntity;
import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface TransactionDao {
    @Insert
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("SELECT * FROM transactions ORDER BY record_time DESC")
    Flow<List<TransactionEntity>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE book_id = :bookId ORDER BY record_time DESC")
    List<TransactionEntity> getTransactionsByBook(int bookId);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 0 AND record_time BETWEEN :startTime AND :endTime")
    long getTotalExpense(long startTime, long endTime);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 1 AND record_time BETWEEN :startTime AND :endTime")
    long getTotalIncome(long startTime, long endTime);

    @Query("SELECT * FROM transactions WHERE record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC")
    List<TransactionEntity> getTransactionsByPeriod(long startTime, long endTime);

    @Query("SELECT * FROM transactions WHERE book_id = :bookId AND record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC")
    List<TransactionEntity> getTransactionsByPeriodAndBook(long startTime, long endTime, int bookId);

    @Query("SELECT * FROM transactions WHERE record_time BETWEEN :startTime AND :endTime")
    Flow<List<TransactionEntity>> getTransactionsBetween(long startTime, long endTime);
}
