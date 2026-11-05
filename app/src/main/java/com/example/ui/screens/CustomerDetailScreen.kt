package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.AddPaymentDialog
import com.example.ui.components.ReverseTransactionDialog
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusDebtRedContainer
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
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: DebtViewModel,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenSms: (Long) -> Unit,
    onOpenStatement: (Long) -> Unit
) {
    BackHandler { onBack() }

    val allCustomersWithBalances by viewModel.allCustomersWithBalances.collectAsState()
    val item = allCustomersWithBalances.find { it.customer.id == customerId }
    val transactions by viewModel.repository.getTransactionsForCustomerFlow(customerId).collectAsState(initial = emptyList())
    val currency = viewModel.getSetting("currency", "ر.ي")
    val allowCreditBalance = viewModel.getSetting("allow_credit_balance", "false").toBoolean()
    val context = LocalContext.current

    var showDebtDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var transactionToReverse by remember { mutableStateOf<TransactionEntity?>(null) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("جاري تحميل بيانات العميل...")
        }
        return
    }

    val customer = item.customer

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("كشف حساب العميل") },
                            onClick = {
                                menuExpanded = false
                                onOpenStatement(customerId)
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("إرسال رسالة SMS") },
                            onClick = {
                                menuExpanded = false
                                onOpenSms(customerId)
                            },
                            leadingIcon = { Icon(Icons.Default.Message, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (customer.status == "ACTIVE") "أرشفة العميل" else "استعادة من الأرشيف") },
                            onClick = {
                                menuExpanded = false
                                showArchiveConfirm = true
                            },
                            leadingIcon = {
                                Icon(
                                    if (customer.status == "ACTIVE") Icons.Default.Archive else Icons.Default.Unarchive,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("customer_detail_screen"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header & Financial Balance Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (customer.company.isNotBlank()) {
                                    Text(
                                        text = customer.company,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            if (customer.phone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Financial Balance Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "الرصيد المتبقي المستحق",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", item.balance)} $currency",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "إجمالي الديون: ${String.format(Locale.US, "%,.2f", item.totalDebt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "إجمالي المدفوعات: ${String.format(Locale.US, "%,.2f", item.totalPayment)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        if (customer.address.isNotBlank() || customer.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (customer.address.isNotBlank()) {
                                Text(
                                    text = "العنوان: ${customer.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            if (customer.notes.isNotBlank()) {
                                Text(
                                    text = "ملاحظات: ${customer.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showDebtDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusDebtRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دين")
                    }

                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل دفعة")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenStatement(customerId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("كشف الحساب")
                    }

                    OutlinedButton(
                        onClick = { onOpenSms(customerId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تجهيز SMS")
                    }
                }
            }

            // Transaction History Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل العمليات المالية (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد عمليات مسجلة لهذا العميل حتى الآن.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    val isDebt = tx.type in listOf("DEBT", "OPENING_BALANCE")
                    val isReversal = tx.type == "REVERSAL" || tx.status == "REVERSED"
                    val accentColor = when {
                        isReversal -> Color.Gray
                        isDebt -> StatusDebtRed
                        else -> StatusPaidGreen
                    }
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDebt) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.description.ifBlank { if (isDebt) "دين" else "دفعة مسددة" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = dateFormat.format(Date(tx.transactionDate)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (isDebt) "+" else "-"}${String.format(Locale.US, "%,.2f", tx.amount)} $currency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }

                            if (tx.referenceNumber.isNotBlank() || tx.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                if (tx.referenceNumber.isNotBlank()) {
                                    Text(
                                        text = "رقم المرجع/الإيصال: ${tx.referenceNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                if (tx.notes.isNotBlank()) {
                                    Text(
                                        text = "ملاحظات: ${tx.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Reversal info or button
                            if (tx.status == "REVERSED") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        text = "تم إلغاء هذه الحركة وعكسها بسجل التدقيق",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }
                            } else if (tx.type != "REVERSAL") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { transactionToReverse = tx }
                                    ) {
                                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إلغاء موثق", style = MaterialTheme.typography.labelSmall, color = StatusDebtRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Debt Dialog
    if (showDebtDialog) {
        AddDebtDialog(
            customers = listOf(item),
            preselectedCustomerId = customerId,
            currency = currency,
            onDismiss = { showDebtDialog = false },
            onSave = { cId, amount, date, desc, ref, notes ->
                viewModel.addDebt(cId, amount, date, desc, ref, notes) {
                    showDebtDialog = false
                }
            }
        )
    }

    // Payment Dialog
    if (showPaymentDialog) {
        AddPaymentDialog(
            customers = listOf(item),
            preselectedCustomerId = customerId,
            currency = currency,
            allowCreditBalance = allowCreditBalance,
            onDismiss = { showPaymentDialog = false },
            onSave = { cId, amount, date, method, ref, notes ->
                viewModel.addPayment(cId, amount, date, method, ref, notes) {
                    showPaymentDialog = false
                }
            }
        )
    }

    // Reverse Transaction Dialog
    if (transactionToReverse != null) {
        ReverseTransactionDialog(
            transactionId = transactionToReverse!!.id,
            amount = transactionToReverse!!.amount,
            currency = currency,
            onDismiss = { transactionToReverse = null },
            onConfirm = { reason ->
                val txId = transactionToReverse!!.id
                transactionToReverse = null
                viewModel.reverseTransaction(txId, reason)
            }
        )
    }

    // Archive Confirmation Dialog
    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(if (customer.status == "ACTIVE") "أرشفة العميل" else "استعادة العميل") },
            text = {
                Text(
                    if (customer.status == "ACTIVE")
                        "هل تريد نقل العميل (${customer.name}) إلى الأرشيف؟ ستبقى جميع عملياته المالية وحساباته محفوظة ولن يتم حذف أي سجل."
                    else
                        "هل تريد استعادة العميل (${customer.name}) إلى قائمة العملاء النشطين؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customer.status == "ACTIVE") {
                            viewModel.archiveCustomer(customerId)
                        } else {
                            viewModel.restoreCustomer(customerId)
                        }
                        showArchiveConfirm = false
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
