package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كيان العميل (Customer Entity) لجدول قاعدة بيانات Room
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String = "",
    @ColumnInfo(name = "alternate_phone")
    val alternatePhone: String = "",
    val address: String = "",
    val company: String = "",
    val notes: String = "",
    val category: String = "عام",
    val source: String = "يدوي",
    val status: String = "ACTIVE",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_transaction_at")
    val lastTransactionAt: Long = 0L,
    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null
)

// اسم بديل متوافق
typealias CustomerEntity = Customer
