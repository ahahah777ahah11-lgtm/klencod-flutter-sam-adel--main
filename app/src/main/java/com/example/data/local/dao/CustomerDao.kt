package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE status = 'ACTIVE' ORDER BY name ASC")
    fun getActiveCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE status = 'ARCHIVED' ORDER BY name ASC")
    fun getArchivedCustomersFlow(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE phone = :phone AND id != :excludeId LIMIT 1")
    suspend fun findByPhoneExcluding(phone: String, excludeId: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>): List<Long>

    @Update
    suspend fun update(customer: Customer)

    @Query("UPDATE customers SET status = 'ARCHIVED', updated_at = :now WHERE id = :id")
    suspend fun archiveCustomer(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET status = 'ACTIVE', updated_at = :now WHERE id = :id")
    suspend fun restoreCustomer(id: Long, now: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET last_transaction_at = :timestamp, updated_at = :timestamp WHERE id = :customerId")
    suspend fun updateLastTransaction(customerId: Long, timestamp: Long)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<Customer>
}
