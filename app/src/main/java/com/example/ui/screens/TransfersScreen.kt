package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TransferEntity
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.FinancialTurquoise
import com.example.ui.theme.StatusAlertYellow
import com.example.ui.theme.StatusAlertYellowContainer
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPaidGreenContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransfersScreen(
    viewModel: DebtViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allTransfers by viewModel.allTransfers.collectAsState()
    val unmatchedTransfers by viewModel.unmatchedTransfers.collectAsState()
    val customers by viewModel.allCustomersWithBalances.collectAsState()
    val customerMap = remember(customers) { customers.associate { it.customer.id to it.customer.name } }
    val currency = viewModel.getSetting("currency", "ر.ي")

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: غير المطابقة, 1: جميع الحوالات
    var showAddDialog by remember { mutableStateOf(false) }
    var transferToConvert by remember { mutableStateOf<TransferEntity?>(null) }

    val displayedList = if (selectedTabIndex == 0) unmatchedTransfers else allTransfers

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الحوالات المالية الواردة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة حوالة")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("transfers_screen")
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("غير مطابقة (${unmatchedTransfers.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("جميع الحوالات (${allTransfers.size})") }
                )
            }

            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTabIndex == 0) "لا توجد حوالات معلقة أو غير مطابقة." else "لا توجد حوالات مسجلة بعد.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedList, key = { it.id }) { transfer ->
                        TransferCard(
                            transfer = transfer,
                            matchedCustomerName = transfer.matchedCustomerId?.let { customerMap[it] },
                            currency = currency,
                            onConvertClick = { transferToConvert = transfer },
                            onDeleteClick = { viewModel.deleteTransfer(transfer.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Transfer Dialog
    if (showAddDialog) {
        AddTransferDialog(
            customers = customers.map { it.customer },
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { company, sender, phone, amount, date, ref, notes, matchedCustomerId ->
                viewModel.addIncomingTransfer(company, sender, phone, amount, date, ref, notes, matchedCustomerId) {
                    showAddDialog = false
                }
            }
        )
    }

    // Convert Transfer to Payment Dialog
    if (transferToConvert != null) {
        ConvertTransferDialog(
            transfer = transferToConvert!!,
            customers = customers,
            currency = currency,
            onDismiss = { transferToConvert = null },
            onConfirm = { customerId ->
                val trId = transferToConvert!!.id
                transferToConvert = null
                viewModel.convertTransferToPayment(trId, customerId)
            }
        )
    }
}

@Composable
fun TransferCard(
    transfer: TransferEntity,
    matchedCustomerName: String?,
    currency: String,
    onConvertClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isConverted = transfer.status == "CONVERTED_TO_PAYMENT"
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = transfer.company,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مرجع: ${transfer.referenceNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%,.2f", transfer.amount)} $currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConverted) StatusPaidGreen else FinancialDarkBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (transfer.senderName.isNotBlank() || transfer.senderPhone.isNotBlank()) {
                Text(
                    text = "المرسل: ${transfer.senderName} ${if (transfer.senderPhone.isNotBlank()) "(${transfer.senderPhone})" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Text(
                text = "التاريخ: ${dateFormat.format(Date(transfer.transferDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (transfer.notes.isNotBlank()) {
                Text(
                    text = "ملاحظات: ${transfer.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConverted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusPaidGreenContainer
                    ) {
                        Text(
                            text = "تم الترحيل كدفعة للعميل: ${matchedCustomerName ?: "معرف #${transfer.matchedCustomerId}"}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusPaidGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusAlertYellowContainer
                    ) {
                        Text(
                            text = "حوالة معلقة غير مرحلة",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF825800),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConvertClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ترحيل لحساب عميل")
                    }
                }

                if (!isConverted) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferDialog(
    customers: List<com.example.data.local.entity.CustomerEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (company: String, sender: String, phone: String, amount: Double, date: Long, ref: String, notes: String, matchedCustomerId: Long?) -> Unit
) {
    val companies = listOf("النجم", "الامتياز إكسبرس", "داديه", "الأكوع موني", "إتش بي", "الكريمي", "أخرى")
    var company by remember { mutableStateOf(companies.first()) }
    var customCompany by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var refNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var companyMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل حوالة واردة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error)
                }

                // Company Picker
                ExposedDropdownMenuBox(
                    expanded = companyMenuExpanded,
                    onExpandedChange = { companyMenuExpanded = !companyMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("شركة التحويل") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = companyMenuExpanded,
                        onDismissRequest = { companyMenuExpanded = false }
                    ) {
                        companies.forEach { comp ->
                            DropdownMenuItem(
                                text = { Text(comp) },
                                onClick = {
                                    company = comp
                                    companyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (company == "أخرى") {
                    OutlinedTextField(
                        value = customCompany,
                        onValueChange = { customCompany = it },
                        label = { Text("اسم شركة التحويل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = {
                        refNumber = it
                        errorText = null
                    },
                    label = { Text("رقم الحوالة / المرجع *") },
                    placeholder = { Text("مثال: ت/8000011 أو 1234567") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("مبلغ الحوالة ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("اسم المرسل") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senderPhone,
                    onValueChange = { senderPhone = it },
                    label = { Text("هاتف المرسل (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val finalCompany = if (company == "أخرى") customCompany.trim() else company
                    if (finalCompany.isBlank()) {
                        errorText = "يرجى تحديد شركة التحويل"
                        return@Button
                    }
                    if (refNumber.isBlank()) {
                        errorText = "يرجى إدخال رقم الحوالة أو المرجع"
                        return@Button
                    }
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "يرجى إدخال مبلغ صحيح للحوالة"
                        return@Button
                    }
                    isSubmitting = true
                    onSave(finalCompany, senderName, senderPhone, amount, System.currentTimeMillis(), refNumber, notes, selectedCustomerId)
                },
                enabled = !isSubmitting
            ) {
                Text("حفظ الحوالة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertTransferDialog(
    transfer: TransferEntity,
    customers: List<com.example.domain.model.CustomerWithBalance>,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (customerId: Long) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(customers.firstOrNull()?.customer?.id ?: 0L)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = customers.find { it.customer.id == selectedCustomerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ترحيل الحوالة لحساب عميل") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "سيتم تسجيل دفعة مالية بمبلغ ${transfer.amount} $currency برقم مرجع (${transfer.referenceNumber}) لخصمها من رصيد العميل المحدد.",
                    style = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = !menuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.let { "${it.customer.name} (الرصيد: ${it.balance} $currency)" } ?: "اختر العميل",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("ترحيل للعميل *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.customer.name} (رصيد: ${c.balance})") },
                                onClick = {
                                    selectedCustomerId = c.customer.id
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCustomerId > 0L) {
                        onConfirm(selectedCustomerId)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen)
            ) {
                Text("تأكيد الترحيل المالي")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
