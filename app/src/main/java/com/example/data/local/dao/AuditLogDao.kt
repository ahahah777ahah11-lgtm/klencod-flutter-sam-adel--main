package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 200")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs")
    suspend fun getAllAuditLogsList(): List<AuditLogEntity>

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAll()
}
