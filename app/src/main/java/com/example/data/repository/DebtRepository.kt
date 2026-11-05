package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.MessageLogEntity
import com.example.data.local.entity.SettingEntity
import com.example.data.local.entity.TemplateEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferEntity
import com.example.domain.model.CustomerStatement
import com.example.domain.model.CustomerWithBalance
import com.example.domain.model.FinancialSummary
import com.example.domain.model.ImportResult
import com.example.domain.model.StatementItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebtRepository(private val database: AppDatabase) {

    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val transferDao = database.transferDao()
    private val templateDao = database.templateDao()
    private val messageLogDao = database.messageLogDao()
    private val settingDao = database.settingDao()
    private val auditLogDao = database.auditLogDao()

    // --- Customers With Reactive Balances ---
    val allCustomersWithBalancesFlow: Flow<List<CustomerWithBalance>> = combine(
        customerDao.getAllCustomersFlow(),
        transactionDao.getAllTransactionsFlow()
    ) { customers, transactions ->
        val now = System.currentTimeMillis()
        val transByCustomer = transactions.groupBy { it.customerId }

        customers.map { customer ->
            val customerTrans = transByCustomer[customer.id] ?: emptyList()
            var debts = 0.0
            var payments = 0.0

            customerTrans.forEach { tx ->
                if (tx.status == "COMPLETED") {
                    when (tx.type) {
                        "DEBT", "OPENING_BALANCE" -> debts += tx.amount
                        "PAYMENT" -> payments += tx.amount
                        "ADJUSTMENT" -> {
                            if (tx.amount >= 0) debts += tx.amount else payments += Math.abs(tx.amount)
                        }
                        "REVERSAL" -> {
                            // Already marked or handled
                        }
                    }
                }
            }

            val balance = debts - payments
            val isOverdue = customer.dueDate != null && customer.dueDate < now && balance > 0.01

            CustomerWithBalance(
                customer = customer,
                totalDebt = debts,
                totalPayment = payments,
                balance = balance,
                transactionCount = customerTrans.size,
                isOverdue = isOverdue
            )
        }
    }

    // --- Financial Summary Flow ---
    val financialSummaryFlow: Flow<FinancialSummary> = combine(
        allCustomersWithBalancesFlow,
        transferDao.getUnmatchedTransfersFlow()
    ) { customersWithBal, unmatchedTransfers ->
        var totalDebts = 0.0
        var totalPayments = 0.0
        var indebtedCount = 0
        var overdueCount = 0
        var zeroCount = 0
        var creditCount = 0

        customersWithBal.filter { it.customer.status == "ACTIVE" }.forEach { c ->
            totalDebts += c.totalDebt
            totalPayments += c.totalPayment
            when {
                c.balance > 0.01 -> indebtedCount++
                c.balance < -0.01 -> creditCount++
                else -> zeroCount++
            }
            if (c.isOverdue) {
                overdueCount++
            }
        }

        FinancialSummary(
            totalDebts = totalDebts,
            totalPayments = totalPayments,
            netBalance = totalDebts - totalPayments,
            totalCustomersCount = customersWithBal.count { it.customer.status == "ACTIVE" },
            indebtedCustomersCount = indebtedCount,
            overdueCustomersCount = overdueCount,
            zeroBalanceCustomersCount = zeroCount,
            creditBalanceCustomersCount = creditCount,
            unmatchedTransfersCount = unmatchedTransfers.size
        )
    }

    // Customer operations
    suspend fun addCustomer(customer: CustomerEntity): Result<Long> {
        val trimmedPhone = customer.phone.trim()
        if (trimmedPhone.isNotEmpty()) {
            val existing = customerDao.findByPhone(trimmedPhone)
            if (existing != null) {
                return Result.failure(Exception("رقم الهاتف (${trimmedPhone}) مسجل مسبقاً للعميل: ${existing.name}"))
            }
        }
        val id = customerDao.insert(customer.copy(phone = trimmedPhone))
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "CUSTOMER",
                entityId = id,
                action = "CREATE",
                newValue = customer.name
            )
        )
        return Result.success(id)
    }

    suspend fun updateCustomer(customer: CustomerEntity): Result<Unit> {
        val trimmedPhone = customer.phone.trim()
        if (trimmedPhone.isNotEmpty()) {
            val existing = customerDao.findByPhoneExcluding(trimmedPhone, customer.id)
            if (existing != null) {
                return Result.failure(Exception("رقم الهاتف (${trimmedPhone}) مسجل مسبقاً لعميل آخر: ${existing.name}"))
            }
        }
        customerDao.update(customer.copy(phone = trimmedPhone, updatedAt = System.currentTimeMillis()))
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "CUSTOMER",
                entityId = customer.id,
                action = "UPDATE",
                newValue = customer.name
            )
        )
        return Result.success(Unit)
    }

    suspend fun archiveCustomer(customerId: Long) {
        customerDao.archiveCustomer(customerId)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "CUSTOMER",
                entityId = customerId,
                action = "ARCHIVE"
            )
        )
    }

    suspend fun restoreCustomer(customerId: Long) {
        customerDao.restoreCustomer(customerId)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "CUSTOMER",
                entityId = customerId,
                action = "RESTORE"
            )
        )
    }

    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?> = customerDao.getCustomerByIdFlow(id)

    // Transaction operations
    suspend fun addDebt(
        customerId: Long,
        amount: Double,
        date: Long,
        description: String,
        referenceNumber: String,
        notes: String
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ الدين يجب أن يكون أكبر من الصفر"))
        }
        val customer = customerDao.getCustomerById(customerId)
            ?: return Result.failure(IllegalArgumentException("العميل غير موجود"))

        val tx = TransactionEntity(
            customerId = customerId,
            type = "DEBT",
            amount = amount,
            transactionDate = date,
            description = description.ifBlank { "دين جديد" },
            referenceNumber = referenceNumber,
            notes = notes
        )
        val id = transactionDao.insert(tx)
        customerDao.updateLastTransaction(customerId, date)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "TRANSACTION",
                entityId = id,
                action = "ADD_DEBT",
                newValue = "$amount - ${customer.name}"
            )
        )
        return Result.success(id)
    }

    suspend fun addPayment(
        customerId: Long,
        amount: Double,
        date: Long,
        paymentMethod: String,
        referenceNumber: String,
        notes: String
    ): Result<Long> {
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("مبلغ الدفعة يجب أن يكون أكبر من الصفر"))
        }
        val customer = customerDao.getCustomerById(customerId)
            ?: return Result.failure(IllegalArgumentException("العميل غير موجود"))

        // Check customer balance
        val currentBal = calculateCustomerBalance(customerId)
        val allowCredit = settingDao.getSettingValue("allow_credit_balance")?.toBooleanStrictOrNull() ?: false

        if (!allowCredit && amount > currentBal + 0.001) {
            return Result.failure(
                IllegalArgumentException("مبلغ الدفعة ($amount) يتجاوز الرصيد المستحق ($currentBal). يمكنك تفعيل الرصيد الدائن من الإعدادات إذا أردت السماح بدفعات أكبر.")
            )
        }

        val tx = TransactionEntity(
            customerId = customerId,
            type = "PAYMENT",
            amount = amount,
            transactionDate = date,
            description = "سداد دفعة ($paymentMethod)",
            referenceNumber = referenceNumber,
            notes = notes,
            paymentMethod = paymentMethod
        )
        val id = transactionDao.insert(tx)
        customerDao.updateLastTransaction(customerId, date)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "TRANSACTION",
                entityId = id,
                action = "ADD_PAYMENT",
                newValue = "$amount - ${customer.name}"
            )
        )
        return Result.success(id)
    }

    suspend fun reverseTransaction(transactionId: Long, reason: String): Result<Long> {
        val original = transactionDao.getTransactionById(transactionId)
            ?: return Result.failure(IllegalArgumentException("الحركة المالية غير موجودة"))

        if (original.status == "REVERSED") {
            return Result.failure(IllegalStateException("تم إلغاء هذه الحركة مسبقاً"))
        }

        val now = System.currentTimeMillis()
        transactionDao.markAsReversed(transactionId, now)

        val reversalType = when (original.type) {
            "DEBT" -> "PAYMENT"
            "PAYMENT" -> "DEBT"
            else -> "ADJUSTMENT"
        }

        val reversalTx = TransactionEntity(
            customerId = original.customerId,
            type = "REVERSAL",
            amount = original.amount,
            transactionDate = now,
            description = "حركة إلغاء وعكس للحركة #${original.id}: $reason",
            referenceNumber = "REV-${original.id}",
            notes = "معكوسة للحركة رقم ${original.id}. السبب: $reason",
            status = "COMPLETED",
            reversedTransactionId = original.id
        )
        val id = transactionDao.insert(reversalTx)
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "TRANSACTION",
                entityId = id,
                action = "REVERSAL",
                oldValue = "Original: #${original.id} (${original.amount})",
                newValue = "Reversal: $reason"
            )
        )
        return Result.success(id)
    }

    suspend fun calculateCustomerBalance(customerId: Long): Double {
        val txs = transactionDao.getTransactionsForCustomerList(customerId)
        var debts = 0.0
        var payments = 0.0
        txs.forEach { tx ->
            if (tx.status == "COMPLETED") {
                when (tx.type) {
                    "DEBT", "OPENING_BALANCE" -> debts += tx.amount
                    "PAYMENT" -> payments += tx.amount
                    "ADJUSTMENT" -> {
                        if (tx.amount >= 0) debts += tx.amount else payments += Math.abs(tx.amount)
                    }
                }
            }
        }
        return debts - payments
    }

    fun getTransactionsForCustomerFlow(customerId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsForCustomerFlow(customerId)

    val allTransactionsFlow: Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow()

    // --- Incoming Transfers ---
    val allTransfersFlow: Flow<List<TransferEntity>> = transferDao.getAllTransfersFlow()
    val unmatchedTransfersFlow: Flow<List<TransferEntity>> = transferDao.getUnmatchedTransfersFlow()

    suspend fun addTransfer(transfer: TransferEntity): Result<Long> {
        val cleanRef = transfer.referenceNumber.trim()
        val cleanCompany = transfer.company.trim()
        if (cleanRef.isBlank()) {
            return Result.failure(IllegalArgumentException("رقم الحوالة أو المرجع إلزامي"))
        }
        val existing = transferDao.findByCompanyAndRef(cleanCompany, cleanRef)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("هذه الحوالة مسجلة مسبقاً بنفس الشركة ورقم المرجع ($cleanRef)"))
        }
        val id = transferDao.insert(transfer.copy(referenceNumber = cleanRef, company = cleanCompany))
        auditLogDao.insert(
            AuditLogEntity(
                entityType = "TRANSFER",
                entityId = id,
                action = "CREATE",
                newValue = "${transfer.amount} via $cleanCompany ($cleanRef)"
            )
        )
        return Result.success(id)
    }

    suspend fun convertTransferToPayment(transferId: Long, customerId: Long): Result<Long> {
        val transfer = transferDao.getTransferById(transferId)
            ?: return Result.failure(IllegalArgumentException("الحوالة غير موجودة"))

        val paymentRes = addPayment(
            customerId = customerId,
            amount = transfer.amount,
            date = transfer.transferDate,
            paymentMethod = "حوالة (${transfer.company})",
            referenceNumber = transfer.referenceNumber,
            notes = "ترحيل حوالة واردة من: ${transfer.senderName} (${transfer.senderPhone})"
        )

        return paymentRes.map { txId ->
            transferDao.markConverted(transferId, customerId)
            auditLogDao.insert(
                AuditLogEntity(
                    entityType = "TRANSFER",
                    entityId = transferId,
                    action = "CONVERT_TO_PAYMENT",
                    newValue = "Customer #$customerId, Tx #$txId"
                )
            )
            txId
        }
    }

    suspend fun deleteTransfer(transferId: Long) {
        transferDao.delete(transferId)
    }

    // --- SMS Templates & Message Rendering ---
    val allTemplatesFlow: Flow<List<TemplateEntity>> = templateDao.getAllTemplatesFlow()

    suspend fun saveTemplate(template: TemplateEntity): Long {
        return if (template.id == 0L) {
            templateDao.insert(template)
        } else {
            templateDao.update(template.copy(updatedAt = System.currentTimeMillis()))
            template.id
        }
    }

    suspend fun setDefaultTemplate(id: Long) {
        templateDao.clearDefault()
        templateDao.setDefault(id)
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.delete(id)
    }

    suspend fun getDefaultTemplate(): TemplateEntity {
        return templateDao.getDefaultTemplate()
            ?: templateDao.getAllTemplatesList().firstOrNull()
            ?: TemplateEntity(
                name = "افتراضي",
                body = "مرحبًا {اسم_العميل}، رصيدكم المستحق هو {الرصيد}. شكراً لتعاملكم معنا.",
                isDefault = true
            )
    }

    fun renderMessage(
        templateBody: String,
        customerName: String,
        balance: Double,
        totalDebt: Double,
        totalPayment: Double,
        storeName: String,
        contactPhone: String,
        currency: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        fun formatAmount(amt: Double): String = String.format(Locale.US, "%.2f", amt) + " " + currency

        return templateBody
            .replace("{اسم_العميل}", customerName)
            .replace("{الرصيد}", formatAmount(balance))
            .replace("{إجمالي_الديون}", formatAmount(totalDebt))
            .replace("{إجمالي_المدفوعات}", formatAmount(totalPayment))
            .replace("{التاريخ}", currentDate)
            .replace("{اسم_المتجر}", storeName)
            .replace("{رقم_التواصل}", contactPhone)
            .replace("{رقم_الحساب}", "ACC-${customerName.hashCode().let { Math.abs(it) % 100000 }}")
    }

    suspend fun logMessage(
        customerId: Long,
        customerName: String,
        phone: String,
        renderedMessage: String,
        status: String,
        statusNote: String = ""
    ) {
        messageLogDao.insert(
            MessageLogEntity(
                customerId = customerId,
                customerName = customerName,
                phone = phone,
                renderedMessage = renderedMessage,
                status = status,
                statusNote = statusNote
            )
        )
    }

    val messageLogsFlow: Flow<List<MessageLogEntity>> = messageLogDao.getAllLogsFlow()

    // --- Customer Statement Generation ---
    suspend fun generateCustomerStatement(
        customerId: Long,
        startDate: Long,
        endDate: Long
    ): CustomerStatement {
        val customer = customerDao.getCustomerById(customerId)
            ?: throw IllegalArgumentException("العميل غير موجود")

        val beforeTxs = transactionDao.getTransactionsBefore(customerId, startDate)
        var openingDebt = 0.0
        var openingPayment = 0.0
        beforeTxs.forEach { tx ->
            if (tx.status == "COMPLETED") {
                when (tx.type) {
                    "DEBT", "OPENING_BALANCE" -> openingDebt += tx.amount
                    "PAYMENT" -> openingPayment += tx.amount
                    "ADJUSTMENT" -> {
                        if (tx.amount >= 0) openingDebt += tx.amount else openingPayment += Math.abs(tx.amount)
                    }
                }
            }
        }
        val openingBalance = openingDebt - openingPayment

        val periodTxs = transactionDao.getTransactionsBetween(customerId, startDate, endDate)
        var runningBal = openingBalance
        var periodTotalDebt = 0.0
        var periodTotalPayment = 0.0

        val items = mutableListOf<StatementItem>()
        periodTxs.forEach { tx ->
            if (tx.status == "COMPLETED") {
                val debit = when (tx.type) {
                    "DEBT", "OPENING_BALANCE" -> tx.amount
                    "ADJUSTMENT" -> if (tx.amount >= 0) tx.amount else 0.0
                    else -> 0.0
                }
                val credit = when (tx.type) {
                    "PAYMENT" -> tx.amount
                    "ADJUSTMENT" -> if (tx.amount < 0) Math.abs(tx.amount) else 0.0
                    else -> 0.0
                }

                runningBal = runningBal + debit - credit
                periodTotalDebt += debit
                periodTotalPayment += credit

                val typeAr = when (tx.type) {
                    "DEBT" -> "دين"
                    "PAYMENT" -> "دفعة / سداد"
                    "OPENING_BALANCE" -> "رصيد افتتاحي"
                    "ADJUSTMENT" -> "تسوية رصيد"
                    "REVERSAL" -> "إلغاء موثق"
                    else -> tx.type
                }

                items.add(
                    StatementItem(
                        date = tx.transactionDate,
                        type = tx.type,
                        typeArabic = typeAr,
                        description = tx.description,
                        reference = tx.referenceNumber,
                        debit = debit,
                        credit = credit,
                        runningBalance = runningBal
                    )
                )
            }
        }

        return CustomerStatement(
            customer = customer,
            startDate = startDate,
            endDate = endDate,
            openingBalance = openingBalance,
            totalPeriodDebt = periodTotalDebt,
            totalPeriodPayment = periodTotalPayment,
            closingBalance = runningBal,
            items = items
        )
    }

    // --- App Settings ---
    val allSettingsFlow: Flow<List<SettingEntity>> = settingDao.getAllSettingsFlow()

    suspend fun getSetting(key: String, defaultValue: String): String {
        return settingDao.getSettingValue(key) ?: defaultValue
    }

    suspend fun setSetting(key: String, value: String) {
        settingDao.setSetting(SettingEntity(key, value))
    }

    // --- JSON Backup & Restore ---
    suspend fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("app", "SmartDebtManagerProSMS")
        root.put("version", "1.0")
        root.put("timestamp", System.currentTimeMillis())

        // Customers
        val customersArray = JSONArray()
        customerDao.getAllCustomersList().forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone)
            obj.put("alternate_phone", c.alternatePhone)
            obj.put("address", c.address)
            obj.put("company", c.company)
            obj.put("notes", c.notes)
            obj.put("category", c.category)
            obj.put("source", c.source)
            obj.put("status", c.status)
            obj.put("created_at", c.createdAt)
            obj.put("due_date", c.dueDate ?: -1L)
            customersArray.put(obj)
        }
        root.put("customers", customersArray)

        // Transactions
        val transArray = JSONArray()
        transactionDao.getAllTransactionsList().forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("customer_id", t.customerId)
            obj.put("type", t.type)
            obj.put("amount", t.amount)
            obj.put("transaction_date", t.transactionDate)
            obj.put("description", t.description)
            obj.put("reference_number", t.referenceNumber)
            obj.put("notes", t.notes)
            obj.put("payment_method", t.paymentMethod)
            obj.put("status", t.status)
            obj.put("created_at", t.createdAt)
            transArray.put(obj)
        }
        root.put("transactions", transArray)

        // Transfers
        val transfersArray = JSONArray()
        transferDao.getAllTransfersList().forEach { tr ->
            val obj = JSONObject()
            obj.put("id", tr.id)
            obj.put("company", tr.company)
            obj.put("sender_name", tr.senderName)
            obj.put("sender_phone", tr.senderPhone)
            obj.put("amount", tr.amount)
            obj.put("transfer_date", tr.transferDate)
            obj.put("reference_number", tr.referenceNumber)
            obj.put("matched_customer_id", tr.matchedCustomerId ?: -1L)
            obj.put("status", tr.status)
            transfersArray.put(obj)
        }
        root.put("transfers", transfersArray)

        // Templates
        val templatesArray = JSONArray()
        templateDao.getAllTemplatesList().forEach { tm ->
            val obj = JSONObject()
            obj.put("name", tm.name)
            obj.put("body", tm.body)
            obj.put("is_default", tm.isDefault)
            templatesArray.put(obj)
        }
        root.put("templates", templatesArray)

        // Settings
        val settingsArray = JSONArray()
        settingDao.getAllSettingsList().forEach { s ->
            val obj = JSONObject()
            obj.put("key", s.key)
            obj.put("value", s.value)
            settingsArray.put(obj)
        }
        root.put("settings", settingsArray)

        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<String> {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("customers") || !root.has("transactions")) {
                return Result.failure(IllegalArgumentException("الملف لا يحتوي على بيانات نسخة احتياطية صالحة للتطبيق."))
            }

            val customersArray = root.getJSONArray("customers")
            val transactionsArray = root.getJSONArray("transactions")

            // Restore customers
            val restoredCustomers = mutableListOf<CustomerEntity>()
            for (i in 0 until customersArray.length()) {
                val obj = customersArray.getJSONObject(i)
                val dueDate = if (obj.has("due_date") && obj.getLong("due_date") > 0) obj.getLong("due_date") else null
                restoredCustomers.add(
                    CustomerEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        phone = obj.optString("phone", ""),
                        alternatePhone = obj.optString("alternate_phone", ""),
                        address = obj.optString("address", ""),
                        company = obj.optString("company", ""),
                        notes = obj.optString("notes", ""),
                        category = obj.optString("category", "عام"),
                        source = obj.optString("source", "استعادة"),
                        status = obj.optString("status", "ACTIVE"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        dueDate = dueDate
                    )
                )
            }

            // Restore transactions
            val restoredTransactions = mutableListOf<TransactionEntity>()
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                restoredTransactions.add(
                    TransactionEntity(
                        id = obj.optLong("id", 0L),
                        customerId = obj.getLong("customer_id"),
                        type = obj.getString("type"),
                        amount = obj.getDouble("amount"),
                        transactionDate = obj.optLong("transaction_date", System.currentTimeMillis()),
                        description = obj.optString("description", ""),
                        referenceNumber = obj.optString("reference_number", ""),
                        notes = obj.optString("notes", ""),
                        paymentMethod = obj.optString("payment_method", "نقداً"),
                        status = obj.optString("status", "COMPLETED"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }

            customerDao.insertAll(restoredCustomers)
            transactionDao.insertAll(restoredTransactions)

            // Transfers if available
            if (root.has("transfers")) {
                val transfersArray = root.getJSONArray("transfers")
                val restoredTransfers = mutableListOf<TransferEntity>()
                for (i in 0 until transfersArray.length()) {
                    val obj = transfersArray.getJSONObject(i)
                    val matchedId = if (obj.has("matched_customer_id") && obj.getLong("matched_customer_id") > 0) {
                        obj.getLong("matched_customer_id")
                    } else null

                    restoredTransfers.add(
                        TransferEntity(
                            id = obj.optLong("id", 0L),
                            company = obj.getString("company"),
                            senderName = obj.optString("sender_name", ""),
                            senderPhone = obj.optString("sender_phone", ""),
                            amount = obj.getDouble("amount"),
                            transferDate = obj.optLong("transfer_date", System.currentTimeMillis()),
                            referenceNumber = obj.getString("reference_number"),
                            matchedCustomerId = matchedId,
                            status = obj.optString("status", "UNMATCHED")
                        )
                    )
                }
                transferDao.insertAll(restoredTransfers)
            }

            Result.success("تم استعادة ${restoredCustomers.size} عميل و ${restoredTransactions.size} حركة مالية بنجاح.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- CSV Import ---
    suspend fun importFromCsv(csvContent: String): ImportResult {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ImportResult(false, 0, 0, 0, listOf("الملف فارغ"), "الملف لا يحتوي على أسطر صالحة")
        }

        val errors = mutableListOf<String>()
        var importedCust = 0
        var importedTx = 0
        var skipped = 0

        // Parse header or guess indices
        val firstLine = lines.first()
        val headers = firstLine.split(",", ";", "\t").map { it.trim().lowercase() }
        val hasHeader = headers.any {
            it.contains("name") || it.contains("اسم") || it.contains("عميل") || it.contains("balance") || it.contains("رصيد")
        }

        val dataLines = if (hasHeader) lines.drop(1) else lines

        for ((idx, line) in dataLines.withIndex()) {
            val parts = line.split(",", ";", "\t").map { it.trim().trim('"', '\'') }
            if (parts.isEmpty() || parts.all { it.isBlank() }) {
                skipped++
                continue
            }

            val name = parts.getOrNull(0) ?: ""
            if (name.isBlank()) {
                errors.add("سطر ${idx + 1}: اسم العميل فارغ")
                skipped++
                continue
            }

            val phone = parts.getOrNull(1) ?: ""
            val balanceStr = parts.getOrNull(2) ?: "0"
            val balance = balanceStr.toDoubleOrNull() ?: 0.0

            // Check if customer exists by name or phone
            val existing = if (phone.isNotBlank()) customerDao.findByPhone(phone) else null
            val customerId = if (existing != null) {
                existing.id
            } else {
                customerDao.insert(
                    CustomerEntity(
                        name = name,
                        phone = phone,
                        source = "استيراد CSV",
                        notes = "تم الاستيراد من ملف CSV"
                    )
                )
            }
            importedCust++

            // If there is an imported balance, record it as a documented OPENING_BALANCE
            if (balance > 0.001) {
                transactionDao.insert(
                    TransactionEntity(
                        customerId = customerId,
                        type = "OPENING_BALANCE",
                        amount = balance,
                        description = "رصيد افتتاحي مستورد من ملف",
                        referenceNumber = "CSV-IMP-${idx + 1}"
                    )
                )
                importedTx++
            } else if (balance < -0.001) {
                transactionDao.insert(
                    TransactionEntity(
                        customerId = customerId,
                        type = "PAYMENT",
                        amount = Math.abs(balance),
                        description = "رصيد دائن افتتاحي مستورد",
                        referenceNumber = "CSV-IMP-${idx + 1}"
                    )
                )
                importedTx++
            }
        }

        return ImportResult(
            success = errors.size < dataLines.size,
            importedCustomers = importedCust,
            importedTransactions = importedTx,
            skippedCount = skipped,
            errors = errors,
            message = "تم استيراد $importedCust عميل و $importedTx رصيد افتتاحي بنجاح."
        )
    }

    // --- CSV Exports ---
    suspend fun exportCustomersCsv(): String {
        val customersWithBal = allCustomersWithBalancesFlow.first()
        val sb = StringBuilder()
        sb.append("معرف العميل,الاسم,الهاتف,الحالة,إجمالي الديون,إجمالي المدفوعات,الرصيد المتبقي,تاريخ الاستحقاق\n")
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        customersWithBal.forEach { c ->
            val due = c.customer.dueDate?.let { df.format(Date(it)) } ?: ""
            sb.append("${c.customer.id},\"${c.customer.name}\",\"${c.customer.phone}\",${c.customer.status},${c.totalDebt},${c.totalPayment},${c.balance},\"$due\"\n")
        }
        return sb.toString()
    }

    suspend fun exportTransactionsCsv(): String {
        val txs = transactionDao.getAllTransactionsList()
        val customers = customerDao.getAllCustomersList().associateBy { it.id }
        val sb = StringBuilder()
        sb.append("رقم الحركة,العميل,النوع,المبلغ,التاريخ,البيان,المرجع,الحالة\n")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        txs.forEach { t ->
            val custName = customers[t.customerId]?.name ?: "معرف #${t.customerId}"
            val dateStr = df.format(Date(t.transactionDate))
            sb.append("${t.id},\"$custName\",${t.type},${t.amount},\"$dateStr\",\"${t.description}\",\"${t.referenceNumber}\",${t.status}\n")
        }
        return sb.toString()
    }

    val allAuditLogsFlow: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogsFlow()
}
