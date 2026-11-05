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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TemplateEntity
import com.example.domain.model.CustomerWithBalance
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.FinancialTurquoise
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsScreen(
    viewModel: DebtViewModel,
    preselectedCustomerId: Long? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val customers by viewModel.allCustomersWithBalances.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val logs by viewModel.repository.messageLogsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val storeName = viewModel.getSetting("store_name", "متجري التجاري")
    val contactPhone = viewModel.getSetting("contact_phone", "")
    val currency = viewModel.getSetting("currency", "ر.ي")

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: تجهيز رسالة, 1: القوالب, 2: سجل الإرسال

    // Prepared Message State
    val indebtedCustomersWithPhone = remember(customers) {
        customers.filter { it.customer.status == "ACTIVE" && it.customer.phone.isNotBlank() }
    }
    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: indebtedCustomersWithPhone.firstOrNull()?.customer?.id ?: 0L)
    }
    var selectedTemplateId by remember {
        mutableStateOf(templates.find { it.isDefault }?.id ?: templates.firstOrNull()?.id ?: 0L)
    }
    var customMessageText by remember { mutableStateOf("") }
    var isEditingText by remember { mutableStateOf(false) }

    // Dialogs
    var templateToEdit by remember { mutableStateOf<TemplateEntity?>(null) }
    var showAddTemplateDialog by remember { mutableStateOf(false) }

    val selectedCustomer = customers.find { it.customer.id == selectedCustomerId }
    val currentTemplate = templates.find { it.id == selectedTemplateId } ?: templates.firstOrNull()

    // Rendered message calculation
    val renderedText = remember(selectedCustomer, currentTemplate, storeName, contactPhone, currency, customMessageText, isEditingText) {
        if (isEditingText && customMessageText.isNotBlank()) {
            customMessageText
        } else if (selectedCustomer != null && currentTemplate != null) {
            viewModel.repository.renderMessage(
                templateBody = currentTemplate.body,
                customerName = selectedCustomer.customer.name,
                balance = selectedCustomer.balance,
                totalDebt = selectedCustomer.totalDebt,
                totalPayment = selectedCustomer.totalPayment,
                storeName = storeName,
                contactPhone = contactPhone,
                currency = currency
            )
        } else {
            ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة رسائل SMS والتذكيرات", fontWeight = FontWeight.Bold) },
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
                .testTag("sms_screen")
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("تجهيز رسالة") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("قوالب الرسائل (${templates.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("سجل الرسائل (${logs.size})") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Send / Prepare Message Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = "اختيار العميل المستلم",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            var customerMenuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = customerMenuExpanded,
                                onExpandedChange = { customerMenuExpanded = !customerMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCustomer?.let { "${it.customer.name} - ${it.customer.phone} (المتبقي: ${it.balance} $currency)" } ?: "اختر العميل",
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
                                    indebtedCustomersWithPhone.forEach { c ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(c.customer.name, fontWeight = FontWeight.Bold)
                                                    Text("${c.customer.phone} | الرصيد: ${c.balance} $currency", style = MaterialTheme.typography.bodySmall)
                                                }
                                            },
                                            onClick = {
                                                selectedCustomerId = c.customer.id
                                                isEditingText = false
                                                customerMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Template Selector
                        item {
                            Text(
                                text = "اختيار قالب الرسالة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            var templateMenuExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = templateMenuExpanded,
                                onExpandedChange = { templateMenuExpanded = !templateMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = currentTemplate?.name ?: "اختر قالباً",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("القالب") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = templateMenuExpanded,
                                    onDismissRequest = { templateMenuExpanded = false }
                                ) {
                                    templates.forEach { tm ->
                                        DropdownMenuItem(
                                            text = { Text(tm.name + if (tm.isDefault) " (الافتراضي)" else "") },
                                            onClick = {
                                                selectedTemplateId = tm.id
                                                isEditingText = false
                                                templateMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Message Preview Card
                        item {
                            Text(
                                text = "معاينة الرسالة قبل الفتح في تطبيق SMS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    if (isEditingText) {
                                        OutlinedTextField(
                                            value = customMessageText,
                                            onValueChange = { customMessageText = it },
                                            label = { Text("تعديل نص الرسالة يدوياً") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 4
                                        )
                                    } else {
                                        Text(
                                            text = renderedText.ifBlank { "يرجى اختيار عميل وقالب لمعاينة الرسالة." },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "عدد الأحرف: ${renderedText.length}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                        Row {
                                            TextButton(
                                                onClick = {
                                                    if (!isEditingText) {
                                                        customMessageText = renderedText
                                                    }
                                                    isEditingText = !isEditingText
                                                }
                                            ) {
                                                Text(if (isEditingText) "إلغاء التعديل اليدوي" else "تعديل النص")
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(renderedText))
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "نسخ النص", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Button: Open SMS App
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    if (selectedCustomer == null || selectedCustomer.customer.phone.isBlank()) return@Button
                                    val phone = selectedCustomer.customer.phone
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:$phone")
                                        putExtra("sms_body", renderedText)
                                    }
                                    try {
                                        context.startActivity(intent)
                                        viewModel.logSmsAction(
                                            customerId = selectedCustomer.customer.id,
                                            customerName = selectedCustomer.customer.name,
                                            phone = phone,
                                            message = renderedText,
                                            status = "OPENED_SMS_APP"
                                        )
                                    } catch (e: Exception) {
                                        viewModel.logSmsAction(
                                            customerId = selectedCustomer.customer.id,
                                            customerName = selectedCustomer.customer.name,
                                            phone = phone,
                                            message = renderedText,
                                            status = "FAILED"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("open_sms_app_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                enabled = selectedCustomer != null && selectedCustomer.customer.phone.isNotBlank() && renderedText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح في تطبيق الرسائل (SMS)")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ملاحظة أمان: التطبيق يفتح تطبيق الرسائل الأصلي بجهازك ولا يرسل أي رسائل في الخلفية بدون موافقتك وتأكيدك المباشر.",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Templates Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قوالب الرسائل الجاهزة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showAddTemplateDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قالب جديد")
                                }
                            }
                        }

                        items(templates, key = { it.id }) { tm ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = tm.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (tm.isDefault) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = StatusPaidGreen.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "القالب الافتراضي",
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StatusPaidGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Row {
                                            if (!tm.isDefault) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.saveTemplate(tm.name, tm.body, true, tm.id)
                                                    }
                                                ) {
                                                    Icon(Icons.Default.StarBorder, contentDescription = "تعيين كافتراضي")
                                                }
                                            }
                                            IconButton(
                                                onClick = { templateToEdit = tm }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "تعديل", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteTemplate(tm.id) }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp), tint = StatusDebtRed)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = tm.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Logs Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "سجل الرسائل التي تم تجهيزها أو فتحها",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (logs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("لا توجد رسائل مسجلة بالسجل حتى الآن.", color = TextSecondary)
                                }
                            }
                        } else {
                            items(logs, key = { it.id }) { log ->
                                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${log.customerName} (${log.phone})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (log.status == "OPENED_SMS_APP") StatusPaidGreen.copy(alpha = 0.15f) else StatusDebtRed.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (log.status == "OPENED_SMS_APP") "فُتح تطبيق الرسائل" else "فشل الفتح",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (log.status == "OPENED_SMS_APP") StatusPaidGreen else StatusDebtRed
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = log.renderedMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateFormat.format(Date(log.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Template Dialog
    if (showAddTemplateDialog || templateToEdit != null) {
        val editing = templateToEdit
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var body by remember { mutableStateOf(editing?.body ?: "مرحبًا {اسم_العميل}، رصيدكم المستحق هو {الرصيد}. شكراً لتعاملكم.") }
        var isDefault by remember { mutableStateOf(editing?.isDefault ?: false) }

        AlertDialog(
            onDismissRequest = {
                showAddTemplateDialog = false
                templateToEdit = null
            },
            title = { Text(if (editing == null) "إضافة قالب رسالة جديد" else "تعديل قالب الرسالة") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم القالب") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("نص القالب") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )

                    Text(
                        text = "المتغيرات المدعومة: {اسم_العميل}، {الرصيد}، {إجمالي_الديون}، {إجمالي_المدفوعات}، {التاريخ}، {اسم_المتجر}، {رقم_التواصل}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && body.isNotBlank()) {
                            viewModel.saveTemplate(name, body, isDefault, editing?.id ?: 0L)
                            showAddTemplateDialog = false
                            templateToEdit = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTemplateDialog = false
                        templateToEdit = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
