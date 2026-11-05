package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * كيان الديون والعمليات المالية (Debt Entity) لجدول قاعدة بيانات Room
 */
@Entity(
    tableName = "financial_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["customer_id"]),
        Index(value = ["transaction_date"]),
        Index(value = ["type"])
    ]
)
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "customer_id")
    val customerId: Long,
    val type: String = "DEBT", // DEBT, PAYMENT, OPENING_BALANCE, ADJUSTMENT, REVERSAL
    val amount: Double,
    @ColumnInfo(name = "transaction_date")
    val transactionDate: Long = System.currentTimeMillis(),
    val description: String = "",
    @ColumnInfo(name = "reference_number")
    val referenceNumber: String = "",
    val notes: String = "",
    @ColumnInfo(name = "payment_method")
    val paymentMethod: String = "نقداً",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED",
    @ColumnInfo(name = "reversed_transaction_id")
    val reversedTransactionId: Long? = null
)

// اسم بديل متوافق
typealias TransactionEntity = Debt
