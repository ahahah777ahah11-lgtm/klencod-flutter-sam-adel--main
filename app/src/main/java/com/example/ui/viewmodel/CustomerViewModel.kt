package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.DebtRepository
import com.example.domain.model.CustomerWithBalance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CustomerViewModel: مسؤول عن إدارة جلب وحفظ وعرض العملاء
 * يعتمد على Coroutines لتنفيذ عمليات قاعدة البيانات (Room) في الخلفية (Dispatchers.IO)
 * لضمان أداء عالي وسلاسة الواجهة، مع بث التحديثات ديناميكياً عبر StateFlow
 */
class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val customerDao = database.customerDao()
    private val debtDao = database.debtDao()
    val repository = DebtRepository(database)

    // حالة البحث
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // حالة التحميل
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // رسائل التنبيه والنتائج للمستخدم
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // قائمة العملاء المحدثة ديناميكياً بشكل تفاعلي من قاعدة بيانات Room
    val customers: StateFlow<List<CustomerWithBalance>> = combine(
        repository.allCustomersWithBalancesFlow,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list.filter { it.customer.status == "ACTIVE" }
        } else {
            list.filter {
                it.customer.status == "ACTIVE" && (
                    it.customer.name.contains(query, ignoreCase = true) ||
                    it.customer.phone.contains(query)
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /**
     * حفظ عميل جديد في قاعدة بيانات Room في الخلفية عبر Coroutines (Dispatchers.IO)
     * مع التحقق من عدم تكرار الهاتف وتسجيل الرصيد الافتتاحي كدين إن وجد
     */
    fun saveCustomer(
        name: String,
        phone: String,
        initialDebt: Double = 0.0,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (name.isBlank()) {
            _userMessage.value = "يرجى كتابة اسم العميل"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCustomerId = withContext(Dispatchers.IO) {
                    val cleanPhone = phone.trim()
                    if (cleanPhone.isNotEmpty()) {
                        val existing = customerDao.findByPhone(cleanPhone)
                        if (existing != null) {
                            throw IllegalArgumentException("رقم الهاتف ($cleanPhone) مسجل مسبقاً للعميل: ${existing.name}")
                        }
                    }

                    val customer = CustomerEntity(
                        name = name.trim(),
                        phone = cleanPhone,
                        source = "يدوي",
                        createdAt = System.currentTimeMillis()
                    )
                    val id = customerDao.insert(customer)

                    if (initialDebt > 0.0) {
                        debtDao.insertDebt(
                            TransactionEntity(
                                customerId = id,
                                type = "DEBT",
                                amount = initialDebt,
                                transactionDate = System.currentTimeMillis(),
                                description = "دين افتتاحي عند إضافة العميل",
                                referenceNumber = "INIT-$id",
                                status = "COMPLETED"
                            )
                        )
                        customerDao.updateLastTransaction(id, System.currentTimeMillis())
                    }
                    id
                }

                _userMessage.value = "تمت إضافة العميل بنجاح"
                onSuccess(newCustomerId)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "حدث خطأ أثناء حفظ بيانات العميل"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * أرشفة العميل بأمان في الخلفية عبر Coroutines
     */
    fun archiveCustomer(customerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            customerDao.archiveCustomer(customerId)
            _userMessage.value = "تم نقل العميل إلى الأرشيف بنجاح"
        }
    }
}
