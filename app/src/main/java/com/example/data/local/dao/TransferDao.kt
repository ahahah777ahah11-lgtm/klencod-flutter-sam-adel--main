package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM incoming_transfers ORDER BY transfer_date DESC")
    fun getAllTransfersFlow(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM incoming_transfers WHERE status = 'UNMATCHED' ORDER BY transfer_date DESC")
    fun getUnmatchedTransfersFlow(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM incoming_transfers WHERE company = :company AND reference_number = :refNumber LIMIT 1")
    suspend fun findByCompanyAndRef(company: String, refNumber: String): TransferEntity?

    @Query("SELECT * FROM incoming_transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transfer: TransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transfers: List<TransferEntity>): List<Long>

    @Update
    suspend fun update(transfer: TransferEntity)

    @Query("UPDATE incoming_transfers SET status = 'CONVERTED_TO_PAYMENT', matched_customer_id = :customerId WHERE id = :transferId")
    suspend fun markConverted(transferId: Long, customerId: Long)

    @Query("SELECT * FROM incoming_transfers")
    suspend fun getAllTransfersList(): List<TransferEntity>

    @Query("DELETE FROM incoming_transfers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM incoming_transfers")
    suspend fun deleteAll()
}
