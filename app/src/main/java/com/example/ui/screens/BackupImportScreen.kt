package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.domain.model.ImportResult
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusWarningOrange
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DebtViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupImportScreen(
    viewModel: DebtViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: النسخ الاحتياطي JSON, 1: استيراد CSV

    // JSON Backup states
    var isExporting by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restorePreviewInfo by remember { mutableStateOf<String?>(null) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }

    // CSV states
    var csvInput by remember { mutableStateOf("") }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("النسخ الاحتياطي واستيراد البيانات", fontWeight = FontWeight.Bold) },
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
                .testTag("backup_import_screen")
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("النسخ والاستعادة (JSON)") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("استيراد وتصدير (CSV)") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // JSON Backup & Restore Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Create Backup Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("إنشاء نسخة احتياطية كاملة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "يتم حفظ ملف JSON يحتوي على كافة بيانات العملاء، والعمليات المالية، والحوالات، والقوالب، والإعدادات، وسجلات التدقيق.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val json = viewModel.repository.exportBackupJson()
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/json"
                                                        putExtra(Intent.EXTRA_SUBJECT, "SmartDebtManager-backup.json")
                                                        putExtra(Intent.EXTRA_TEXT, json)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة / حفظ النسخة الاحتياطية"))
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مشاركة وحفظ النسخة")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val json = viewModel.repository.exportBackupJson()
                                                    clipboardManager.setText(AnnotatedString(json))
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("نسخ JSON")
                                        }
                                    }
                                }
                            }
                        }

                        // Restore Backup Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Restore, contentDescription = null, tint = StatusWarningOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("استعادة نسخة احتياطية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "الصق محتوى ملف النسخة الاحتياطية JSON في الحقل أدناه لفحصه ومعاينته قبل استعادة البيانات.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = restoreJsonInput,
                                        onValueChange = {
                                            restoreJsonInput = it
                                            restorePreviewInfo = null
                                        },
                                        label = { Text("محتوى ملف النسخة JSON") },
                                        placeholder = { Text("الصق نص ملف SmartDebtManager-backup.json هنا...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4,
                                        maxLines = 8
                                    )

                                    if (restorePreviewInfo != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = restorePreviewInfo!!,
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val obj = JSONObject(restoreJsonInput.trim())
                                                    val custs = obj.optJSONArray("customers")?.length() ?: 0
                                                    val txs = obj.optJSONArray("transactions")?.length() ?: 0
                                                    val transfers = obj.optJSONArray("transfers")?.length() ?: 0
                                                    restorePreviewInfo = "معاينة الملف السليم: يحتوي على $custs عميل، و $txs حركة مالية، و $transfers حوالة واردة."
                                                    showConfirmRestoreDialog = true
                                                } catch (e: Exception) {
                                                    restorePreviewInfo = "خطأ: الملف غير صالح أو لا يطابق بنية JSON للتطبيق."
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarningOrange),
                                            enabled = restoreJsonInput.isNotBlank()
                                        ) {
                                            Text("فحص واستعادة البيانات")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // CSV Import & Export Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("استيراد عملاء وأرصدة من CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "قواعد الاستيراد الآمن:\n• صيغة الأعمدة: الاسم, الهاتف, الرصيد\n• إذا تضمن الملف رصيداً فقط، يسجل كرصيد افتتاحي موثق بتاريخ الاستيراد دون اختلاق حركات وهمية.\n• منع تكرار العميل بنفس رقم الهاتف.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = csvInput,
                                        onValueChange = {
                                            csvInput = it
                                            importResult = null
                                        },
                                        label = { Text("بيانات CSV") },
                                        placeholder = {
                                            Text("مثال:\nمحمد أحمد, 770123456, 15000\nعلي حسن, 771987654, 5200")
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4,
                                        maxLines = 8
                                    )

                                    if (importResult != null) {
                                        val res = importResult!!
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (res.success) StatusPaidGreen.copy(alpha = 0.15f) else StatusDebtRed.copy(alpha = 0.15f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(res.message, fontWeight = FontWeight.Bold, color = if (res.success) StatusPaidGreen else StatusDebtRed)
                                                if (res.errors.isNotEmpty()) {
                                                    Text("أخطاء تم تجاوزها (${res.errors.size}):", style = MaterialTheme.typography.labelSmall, color = StatusDebtRed)
                                                    res.errors.take(3).forEach { err ->
                                                        Text("• $err", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (csvInput.isNotBlank()) {
                                                viewModel.importCsv(csvInput) { res ->
                                                    importResult = res
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = csvInput.isNotBlank()
                                    ) {
                                        Text("تنفيذ الاستيراد الآمن")
                                    }
                                }
                            }
                        }

                        // Export All Transactions CSV
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("تصدير الحركات المالية CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("تصدير السجل الكامل لكافة الديون والمدفوعات والتسويات بصيغة CSV المتوافقة مع Excel.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                val csv = viewModel.repository.exportTransactionsCsv()
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(Intent.EXTRA_SUBJECT, "financial_transactions.csv")
                                                    putExtra(Intent.EXTRA_TEXT, csv)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة سجل العمليات"))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تصدير الحركات المالية CSV")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Restore Dialog
    if (showConfirmRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            title = { Text("تأكيد استعادة النسخة الاحتياطية") },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في استعادة هذه النسخة؟\n${restorePreviewInfo ?: ""}\nسيتم دمج وتحديث البيانات بأمان في قاعدة البيانات المحلية."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestoreDialog = false
                        viewModel.restoreBackup(restoreJsonInput.trim()) { success, msg ->
                            restorePreviewInfo = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarningOrange)
                ) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
