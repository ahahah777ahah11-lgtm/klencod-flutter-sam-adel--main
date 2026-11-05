package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CustomerStatement
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.FinancialTurquoise
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DebtViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: DebtViewModel,
    preselectedCustomerId: Long? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val customers by viewModel.allCustomersWithBalances.collectAsState()
    val summary by viewModel.financialSummary.collectAsState()
    val currency = viewModel.getSetting("currency", "ر.ي")
    val storeName = viewModel.getSetting("store_name", "متجري التجاري")
    val contactPhone = viewModel.getSetting("contact_phone", "")
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(if (preselectedCustomerId != null) 0 else 1) }

    // Statement tab states
    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: customers.firstOrNull()?.customer?.id ?: 0L)
    }
    var statementPeriod by remember { mutableStateOf("ALL") } // ALL, MONTH, YEAR
    var statement by remember { mutableStateOf<CustomerStatement?>(null) }
    var isLoadingStatement by remember { mutableStateOf(false) }

    // Generate statement when customer or period changes
    LaunchedEffect(selectedCustomerId, statementPeriod) {
        if (selectedCustomerId > 0L) {
            isLoadingStatement = true
            val now = System.currentTimeMillis()
            val startDate = when (statementPeriod) {
                "MONTH" -> now - 30L * 24 * 60 * 60 * 1000
                "YEAR" -> now - 365L * 24 * 60 * 60 * 1000
                else -> 0L
            }
            try {
                statement = viewModel.repository.generateCustomerStatement(selectedCustomerId, startDate, now)
            } catch (e: Exception) {
                statement = null
            }
            isLoadingStatement = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير وكشوف الحسابات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("reports_screen")
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("كشف حساب عميل") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("التقارير العامة") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Statement Screen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Customer Picker
                        item {
                            val selectedCust = customers.find { it.customer.id == selectedCustomerId }
                            var customerMenuExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = customerMenuExpanded,
                                onExpandedChange = { customerMenuExpanded = !customerMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCust?.customer?.name ?: "اختر العميل لتوليد الكشف",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("العميل") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = customerMenuExpanded,
                                    onDismissRequest = { customerMenuExpanded = false }
                                ) {
                                    customers.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c.customer.name) },
                                            onClick = {
                                                selectedCustomerId = c.customer.id
                                                customerMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Period Filter Chips
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = statementPeriod == "ALL",
                                    onClick = { statementPeriod = "ALL" },
                                    label = { Text("كامل المدة") }
                                )
                                FilterChip(
                                    selected = statementPeriod == "MONTH",
                                    onClick = { statementPeriod = "MONTH" },
                                    label = { Text("آخر 30 يوماً") }
                                )
                                FilterChip(
                                    selected = statementPeriod == "YEAR",
                                    onClick = { statementPeriod = "YEAR" },
                                    label = { Text("هذا العام") }
                                )
                            }
                        }

                        if (statement != null) {
                            val st = statement!!
                            // Statement Summary Card
                            item {
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "كشف حساب: ${st.customer.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("الرصيد الافتتاحي:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("${String.format(Locale.US, "%,.2f", st.openingBalance)} $currency", fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ديون الفترة:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("${String.format(Locale.US, "%,.2f", st.totalPeriodDebt)} $currency", color = StatusDebtRed, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("مدفوعات الفترة:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text("${String.format(Locale.US, "%,.2f", st.totalPeriodPayment)} $currency", color = StatusPaidGreen, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("الرصيد الختامي المستحق:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${String.format(Locale.US, "%,.2f", st.closingBalance)} $currency",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (st.closingBalance > 0) StatusDebtRed else StatusPaidGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val text = formatStatementText(st, storeName, contactPhone, currency)
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, "كشف حساب ${st.customer.name}")
                                                        putExtra(Intent.EXTRA_TEXT, text)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب"))
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("مشاركة الكشف")
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val text = formatStatementText(st, storeName, contactPhone, currency)
                                                    clipboardManager.setText(AnnotatedString(text))
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("نسخ النص")
                                            }
                                        }
                                    }
                                }
                            }

                            // Statement Table Header
                            item {
                                Text(
                                    text = "تفاصيل العمليات الموثقة (${st.items.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (st.items.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("لا توجد حركات مسجلة خلال هذه الفترة.", color = TextSecondary)
                                    }
                                }
                            } else {
                                items(st.items) { item ->
                                    val df = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(item.description, fontWeight = FontWeight.Bold)
                                                Text(df.format(Date(item.date)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (item.debit > 0) {
                                                    Text("مدين (دين): +${item.debit}", color = StatusDebtRed, style = MaterialTheme.typography.bodySmall)
                                                }
                                                if (item.credit > 0) {
                                                    Text("دائن (سداد): -${item.credit}", color = StatusPaidGreen, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Text("الرصيد: ${String.format(Locale.US, "%.2f", item.runningBalance)} $currency", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // General Reports Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // General Summary Cards
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("الملخص المالي الشامل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("إجمالي الديون:", color = TextSecondary)
                                        Text("${String.format(Locale.US, "%,.2f", summary.totalDebts)} $currency", color = StatusDebtRed, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("إجمالي المدفوعات المسددة:", color = TextSecondary)
                                        Text("${String.format(Locale.US, "%,.2f", summary.totalPayments)} $currency", color = StatusPaidGreen, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("صافي الأرصدة المستحقة:", color = TextSecondary)
                                        Text("${String.format(Locale.US, "%,.2f", summary.netBalance)} $currency", color = FinancialDarkBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Top Debtors Section
                        item {
                            Text("أكثر العملاء مديونية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        val topDebtors = customers.filter { it.customer.status == "ACTIVE" && it.balance > 0.01 }.sortedByDescending { it.balance }.take(5)
                        if (topDebtors.isEmpty()) {
                            item {
                                Text("لا يوجد عملاء مدينون حالياً.", color = TextSecondary)
                            }
                        } else {
                            items(topDebtors) { c ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(c.customer.name, fontWeight = FontWeight.Bold)
                                            if (c.customer.phone.isNotBlank()) {
                                                Text(c.customer.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                        }
                                        Text(
                                            "${String.format(Locale.US, "%,.2f", c.balance)} $currency",
                                            fontWeight = FontWeight.Bold,
                                            color = StatusDebtRed
                                        )
                                    }
                                }
                            }
                        }

                        // Export CSV Button
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val csv = viewModel.repository.exportCustomersCsv()
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_SUBJECT, "تصدير أرصدة العملاء CSV")
                                            putExtra(Intent.EXTRA_TEXT, csv)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير CSV"))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير تقرير العملاء والأرصدة كملف CSV")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatStatementText(
    statement: CustomerStatement,
    storeName: String,
    contactPhone: String,
    currency: String
): String {
    val df = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("=============================\n")
    sb.append("      $storeName      \n")
    if (contactPhone.isNotBlank()) sb.append("هاتف التواصل: $contactPhone\n")
    sb.append("=============================\n")
    sb.append("كشف حساب العميل: ${statement.customer.name}\n")
    if (statement.customer.phone.isNotBlank()) sb.append("رقم الهاتف: ${statement.customer.phone}\n")
    sb.append("تاريخ الإصدار: ${df.format(Date())}\n")
    sb.append("-----------------------------\n")
    sb.append("الرصيد السابق/الافتتاحي: ${String.format(Locale.US, "%.2f", statement.openingBalance)} $currency\n")
    sb.append("إجمالي ديون الفترة: ${String.format(Locale.US, "%.2f", statement.totalPeriodDebt)} $currency\n")
    sb.append("إجمالي مدفوعات الفترة: ${String.format(Locale.US, "%.2f", statement.totalPeriodPayment)} $currency\n")
    sb.append("-----------------------------\n")
    sb.append("الرصيد المتبقي المستحق: ${String.format(Locale.US, "%.2f", statement.closingBalance)} $currency\n")
    sb.append("=============================\n\n")
    sb.append("تفاصيل العمليات:\n")

    statement.items.forEachIndexed { i, item ->
        val dateStr = df.format(Date(item.date))
        val typeDesc = if (item.debit > 0) "+دين: ${item.debit}" else "-سداد: ${item.credit}"
        sb.append("${i + 1}. $dateStr | ${item.description} | $typeDesc | الرصيد: ${String.format(Locale.US, "%.2f", item.runningBalance)}\n")
    }

    sb.append("\nشكرًا لتعاملكم معنا.")
    return sb.toString()
}
