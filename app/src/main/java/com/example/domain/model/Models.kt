package com.example.domain.model

import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val totalDebt: Double,
    val totalPayment: Double,
    val balance: Double,
    val transactionCount: Int = 0,
    val isOverdue: Boolean = false
)

data class FinancialSummary(
    val totalDebts: Double = 0.0,
    val totalPayments: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalCustomersCount: Int = 0,
    val indebtedCustomersCount: Int = 0,
    val overdueCustomersCount: Int = 0,
    val zeroBalanceCustomersCount: Int = 0,
    val creditBalanceCustomersCount: Int = 0,
    val unmatchedTransfersCount: Int = 0
)

data class StatementItem(
    val date: Long,
    val type: String,
    val typeArabic: String,
    val description: String,
    val reference: String,
    val debit: Double, // مدين (دين)
    val credit: Double, // دائن (دفعة)
    val runningBalance: Double
)

data class CustomerStatement(
    val customer: CustomerEntity,
    val startDate: Long,
    val endDate: Long,
    val openingBalance: Double,
    val totalPeriodDebt: Double,
    val totalPeriodPayment: Double,
    val closingBalance: Double,
    val items: List<StatementItem>
)

data class ImportResult(
    val success: Boolean,
    val importedCustomers: Int,
    val importedTransactions: Int,
    val skippedCount: Int,
    val errors: List<String>,
    val message: String
)
