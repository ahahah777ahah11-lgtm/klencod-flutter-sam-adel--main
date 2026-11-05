package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incoming_transfers",
    indices = [
        Index(value = ["company", "reference_number"], unique = true)
    ]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val company: String,
    @ColumnInfo(name = "sender_name")
    val senderName: String = "",
    @ColumnInfo(name = "sender_phone")
    val senderPhone: String = "",
    val amount: Double,
    @ColumnInfo(name = "transfer_date")
    val transferDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String,
    @ColumnInfo(name = "matched_customer_id")
    val matchedCustomerId: Long? = null,
    val notes: String = "",
    val status: String = "UNMATCHED", // UNMATCHED, MATCHED, CONVERTED_TO_PAYMENT
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
