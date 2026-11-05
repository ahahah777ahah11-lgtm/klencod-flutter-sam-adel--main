package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TemplateEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferEntity
import com.example.data.repository.DebtRepository
import com.example.domain.model.CustomerStatement
import com.example.domain.model.CustomerWithBalance
import com.example.domain.model.FinancialSummary
import com.example.domain.model.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebtViewModel(application: Application) : AndroidViewModel(application) {

    val repository: DebtRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DebtRepository(db)
    }

    // UI Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _customerFilter = MutableStateFlow("ALL") // ALL, INDEBTED, PAID, ZERO, ARCHIVED
    val customerFilter = _customerFilter.asStateFlow()

    private val _customerSort = MutableStateFlow("BALANCE_DESC") // BALANCE_DESC, NAME, DATE_DESC
    val customerSort = _customerSort.asStateFlow()

    private val _dashboardPeriod = MutableStateFlow("ALL") // ALL, TODAY, WEEK, MONTH
    val dashboardPeriod = _dashboardPeriod.asStateFlow()

    // Status Message for user feedback
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback = _userFeedback.asStateFlow()

    // Financial Summary
    val financialSummary: StateFlow<FinancialSummary> = repository.financialSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // All Customers with reactive balances
    val allCustomersWithBalances: StateFlow<List<CustomerWithBalance>> = repository.allCustomersWithBalancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Customers
    val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(
        repository.allCustomersWithBalancesFlow,
        _searchQuery,
        _customerFilter,
        _customerSort
    ) { list, query, filter, sort ->
        var filtered = list.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.customer.name.contains(query, ignoreCase = true) ||
                    item.customer.phone.contains(query)

            val matchesFilter = when (filter) {
                "ALL" -> item.customer.status == "ACTIVE"
                "INDEBTED" -> item.customer.status == "ACTIVE" && item.balance > 0.01
                "PAID" -> item.customer.status == "ACTIVE" && item.totalPayment > 0 && Math.abs(item.balance) <= 0.01
                "ZERO" -> item.customer.status == "ACTIVE" && Math.abs(item.balance) <= 0.01
                "ARCHIVED" -> item.customer.status == "ARCHIVED"
                else -> item.customer.status == "ACTIVE"
            }

            matchesQuery && matchesFilter
        }

        filtered = when (sort) {
            "NAME" -> filtered.sortedBy { it.customer.name }
            "BALANCE_DESC" -> filtered.sortedByDescending { it.balance }
            "DATE_DESC" -> filtered.sortedByDescending { it.customer.lastTransactionAt }
            else -> filtered.sortedByDescending { it.balance }
        }

        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent Transactions
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Incoming Transfers
    val allTransfers: StateFlow<List<TransferEntity>> = repository.allTransfersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unmatchedTransfers: StateFlow<List<TransferEntity>> = repository.unmatchedTransfersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SMS Templates
    val templates: StateFlow<List<TemplateEntity>> = repository.allTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    val settingsMap = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        viewModelScope.launch {
            repository.allSettingsFlow.collect { list ->
                settingsMap.value = list.associate { it.key to it.value }
            }
        }
    }

    fun getSetting(key: String, default: String): String {
        return settingsMap.value[key] ?: default
    }

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.setSetting(key, value)
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setCustomerFilter(f: String) {
        _customerFilter.value = f
    }

    fun setCustomerSort(s: String) {
        _customerSort.value = s
    }

    fun setDashboardPeriod(p: String) {
        _dashboardPeriod.value = p
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    // --- Customer Actions ---
    fun addCustomer(
        name: String,
        phone: String,
        alternatePhone: String,
        address: String,
        company: String,
        notes: String,
        category: String,
        dueDate: Long?,
        initialDebt: Double = 0.0,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                alternatePhone = alternatePhone.trim(),
                address = address.trim(),
                company = company.trim(),
                notes = notes.trim(),
                category = category,
                dueDate = dueDate
            )
            val result = repository.addCustomer(customer)
            result.fold(
                onSuccess = { customerId ->
                    if (initialDebt > 0.0) {
                        repository.addDebt(
                            customerId = customerId,
                            amount = initialDebt,
                            date = System.currentTimeMillis(),
                            description = "رصيد افتتاحي عند إضافة العميل",
                            referenceNumber = "INIT-$customerId",
                            notes = ""
                        )
                    }
                    _userFeedback.value = "تمت إضافة العميل بنجاح"
                    onSuccess(customerId)
                },
                onFailure = { err ->
                    _userFeedback.value = "خطأ: ${err.message}"
                }
            )
        }
    }

    fun updateCustomer(
        id: Long,
        name: String,
        phone: String,
        alternatePhone: String,
        address: String,
        company: String,
        notes: String,
        category: String,
        dueDate: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                alternatePhone = alternatePhone.trim(),
                address = address.trim(),
                company = company.trim(),
                notes = notes.trim(),
                category = category,
                dueDate = dueDate
            )
            val result = repository.updateCustomer(customer)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم تحديث بيانات العميل بنجاح"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = "خطأ: ${err.message}"
                }
            )
        }
    }

    fun archiveCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.archiveCustomer(customerId)
            _userFeedback.value = "تم نقل العميل إلى الأرشيف"
        }
    }

    fun restoreCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.restoreCustomer(customerId)
            _userFeedback.value = "تم استعادة العميل من الأرشيف"
        }
    }

    // --- Financial Operations ---
    fun addDebt(
        customerId: Long,
        amount: Double,
        date: Long,
        description: String,
        referenceNumber: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.addDebt(customerId, amount, date, description, referenceNumber, notes)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم تسجيل الدين بنجاح"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = err.message ?: "فشل تسجيل الدين"
                }
            )
        }
    }

    fun addPayment(
        customerId: Long,
        amount: Double,
        date: Long,
        paymentMethod: String,
        referenceNumber: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.addPayment(customerId, amount, date, paymentMethod, referenceNumber, notes)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم تسجيل الدفعة بنجاح"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = err.message ?: "فشل تسجيل الدفعة"
                }
            )
        }
    }

    fun reverseTransaction(transactionId: Long, reason: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.reverseTransaction(transactionId, reason)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم إلغاء الحركة المالية وتوثيق العملية في سجل التدقيق"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = err.message ?: "فشل إلغاء الحركة"
                }
            )
        }
    }

    // --- Incoming Transfers ---
    fun addIncomingTransfer(
        company: String,
        senderName: String,
        senderPhone: String,
        amount: Double,
        date: Long,
        refNumber: String,
        notes: String,
        matchedCustomerId: Long?,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val transfer = TransferEntity(
                company = company.trim(),
                senderName = senderName.trim(),
                senderPhone = senderPhone.trim(),
                amount = amount,
                transferDate = date,
                referenceNumber = refNumber.trim(),
                notes = notes.trim(),
                matchedCustomerId = matchedCustomerId,
                status = if (matchedCustomerId != null) "MATCHED" else "UNMATCHED"
            )
            val result = repository.addTransfer(transfer)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم حفظ بيانات الحوالة الواردة بنجاح"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = err.message ?: "فشل إضافة الحوالة"
                }
            )
        }
    }

    fun convertTransferToPayment(transferId: Long, customerId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.convertTransferToPayment(transferId, customerId)
            result.fold(
                onSuccess = {
                    _userFeedback.value = "تم ترحيل الحوالة وتسجيل دفعة للعميل بنجاح"
                    onSuccess()
                },
                onFailure = { err ->
                    _userFeedback.value = err.message ?: "فشل ترحيل الحوالة"
                }
            )
        }
    }

    fun deleteTransfer(transferId: Long) {
        viewModelScope.launch {
            repository.deleteTransfer(transferId)
            _userFeedback.value = "تم حذف الحوالة"
        }
    }

    // --- SMS Templates ---
    fun saveTemplate(name: String, body: String, isDefault: Boolean, id: Long = 0L) {
        viewModelScope.launch {
            repository.saveTemplate(
                TemplateEntity(
                    id = id,
                    name = name.trim(),
                    body = body.trim(),
                    isDefault = isDefault
                )
            )
            _userFeedback.value = "تم حفظ قالب الرسالة"
        }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _userFeedback.value = "تم حذف القالب"
        }
    }

    fun logSmsAction(customerId: Long, customerName: String, phone: String, message: String, status: String) {
        viewModelScope.launch {
            repository.logMessage(
                customerId = customerId,
                customerName = customerName,
                phone = phone,
                renderedMessage = message,
                status = status
            )
        }
    }

    // --- Import & Backup ---
    fun restoreBackup(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.restoreBackupJson(jsonString)
            res.fold(
                onSuccess = { msg ->
                    _userFeedback.value = msg
                    onComplete(true, msg)
                },
                onFailure = { err ->
                    val msg = err.message ?: "فشل الاستعادة"
                    _userFeedback.value = msg
                    onComplete(false, msg)
                }
            )
        }
    }

    fun importCsv(csvString: String, onComplete: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val res = repository.importFromCsv(csvString)
            _userFeedback.value = res.message
            onComplete(res)
        }
    }
}
