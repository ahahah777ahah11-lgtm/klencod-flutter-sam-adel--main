package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_logs")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    @ColumnInfo(name = "customer_name")
    val customerName: String,
    val phone: String,
    @ColumnInfo(name = "rendered_message")
    val renderedMessage: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val status: String, // PREPARED, OPENED_SMS_APP, FAILED
    @ColumnInfo(name = "status_note")
    val statusNote: String = ""
)
