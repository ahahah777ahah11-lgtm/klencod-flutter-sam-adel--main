package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM financial_transactions WHERE type IN ('DEBT', 'OPENING_BALANCE') ORDER BY transaction_date DESC")
    fun getAllDebtsFlow(): Flow<List<Debt>>

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId AND type IN ('DEBT', 'OPENING_BALANCE') ORDER BY transaction_date DESC")
    fun getDebtsForCustomerFlow(customerId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM financial_transactions WHERE customer_id = :customerId AND type IN ('DEBT', 'OPENING_BALANCE')")
    suspend fun getDebtsForCustomerList(customerId: Long): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Query("DELETE FROM financial_transactions WHERE id = :id")
    suspend fun deleteDebtById(id: Long)
}
