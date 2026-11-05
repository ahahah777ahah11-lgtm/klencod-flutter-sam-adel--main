package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM financial_transactions ORDER BY transaction_date DESC, id DESC")
    fun getAllTransactionsFlow(): Flow<List<Debt>>

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId ORDER BY transaction_date DESC, id DESC")
    fun getTransactionsForCustomerFlow(customerId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId ORDER BY transaction_date ASC, id ASC")
    suspend fun getTransactionsForCustomerList(customerId: Long): List<Debt>

    @Query("SELECT * FROM financial_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Debt?

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId AND transaction_date < :startTime")
    suspend fun getTransactionsBefore(customerId: Long, startTime: Long): List<Debt>

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId AND transaction_date BETWEEN :startTime AND :endTime ORDER BY transaction_date ASC")
    suspend fun getTransactionsBetween(customerId: Long, startTime: Long, endTime: Long): List<Debt>

    @Query("SELECT * FROM financial_transactions WHERE transaction_date >= :sinceTimestamp ORDER BY transaction_date DESC")
    fun getRecentTransactionsFlow(sinceTimestamp: Long): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: Debt): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Debt>): List<Long>

    @Update
    suspend fun update(transaction: Debt)

    @Query("UPDATE financial_transactions SET status = 'REVERSED', updated_at = :now WHERE id = :id")
    suspend fun markAsReversed(id: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM financial_transactions")
    suspend fun getAllTransactionsList(): List<Debt>

    @Query("DELETE FROM financial_transactions")
    suspend fun deleteAll()
}
