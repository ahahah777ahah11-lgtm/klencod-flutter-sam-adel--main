package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.MessageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {
    @Query("SELECT * FROM message_logs ORDER BY created_at DESC")
    fun getAllLogsFlow(): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE customer_id = :customerId ORDER BY created_at DESC")
    fun getLogsForCustomerFlow(customerId: Long): Flow<List<MessageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MessageLogEntity): Long

    @Query("DELETE FROM message_logs")
    suspend fun deleteAll()

    @Query("SELECT * FROM message_logs")
    suspend fun getAllLogsList(): List<MessageLogEntity>
}
