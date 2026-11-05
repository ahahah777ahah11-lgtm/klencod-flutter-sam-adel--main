package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.CustomerWithBalance
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusPaidGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نموذج إضافة عميل جديد سريع يطلب الاسم ورقم الهاتف
 * ويرتبط بـ ViewModel لإضافة العميل وتحديث الواجهة مباشرة
 */
@Composable
fun NewCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إضافة عميل جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("اسم العميل *") },
                    placeholder = { Text("أدخل الاسم الكامل للعميل") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_customer_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    placeholder = { Text("أدخل رقم الهاتف للتواصل و SMS") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_customer_phone_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (name.isBlank()) {
                        errorText = "يرجى كتابة اسم العميل"
                        return@Button
                    }
                    isSubmitting = true
                    onConfirm(name.trim(), phone.trim())
                },
                modifier = Modifier.testTag("confirm_add_customer_button"),
                enabled = !isSubmitting
            ) {
                Text("إضافة")
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
fun AddCustomerDialog(
    initialCustomer: CustomerWithBalance? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, altPhone: String, address: String, company: String, notes: String, category: String, dueDate: Long?, initialDebt: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.customer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.customer?.phone ?: "") }
    var altPhone by remember { mutableStateOf(initialCustomer?.customer?.alternatePhone ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.customer?.address ?: "") }
    var company by remember { mutableStateOf(initialCustomer?.customer?.company ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.customer?.notes ?: "") }
    var category by remember { mutableStateOf(initialCustomer?.customer?.category ?: "عام") }
    var initialDebtStr by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf("عام", "تجاري", "فردي", "VIP", "مورد")
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCustomer == null) "إضافة عميل جديد" else "تعديل بيانات العميل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("الاسم الكامل *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف (للتواصل و SMS)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = altPhone,
                    onValueChange = { altPhone = it },
                    label = { Text("رقم هاتف بديل (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تصنيف العميل") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("اسم المحل أو النشاط (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (initialCustomer == null) {
                    OutlinedTextField(
                        value = initialDebtStr,
                        onValueChange = { initialDebtStr = it },
                        label = { Text("رصيد افتتاحي / دين سابق (اختياري)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("initial_debt_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.Note, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    if (name.isBlank()) {
                        errorText = "يرجى كتابة اسم العميل"
                        return@Button
                    }
                    val initialDebt = initialDebtStr.toDoubleOrNull() ?: 0.0
                    if (initialDebt < 0.0) {
                        errorText = "الرصيد الافتتاحي لا يمكن أن يكون سالباً"
                        return@Button
                    }
                    isSubmitting = true
                    onSave(name, phone, altPhone, address, company, notes, category, null, initialDebt)
                },
                modifier = Modifier.testTag("save_customer_button"),
                enabled = !isSubmitting
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddDebtDialog(
    customers: List<CustomerWithBalance>,
    preselectedCustomerId: Long? = null,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (customerId: Long, amount: Double, date: Long, description: String, refNumber: String, notes: String) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: customers.firstOrNull()?.customer?.id ?: 0L)
    }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var refNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل دين جديد (+)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StatusDebtRed
            )
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

                // If preselected, show customer name, else picker
                val selectedCustomer = customers.find { it.customer.id == selectedCustomerId }
                if (preselectedCustomerId != null && selectedCustomer != null) {
                    OutlinedTextField(
                        value = "${selectedCustomer.customer.name} (الرصيد: ${selectedCustomer.balance} $currency)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العميل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("مبلغ الدين ($currency) *") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("بيان الدين / سبب العملية") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    placeholder = { Text("مثال: بضاعة آجلة، فاتورة رقم...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("رقم الفاتورة أو المرجع (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                        return@Button
                    }
                    if (selectedCustomerId == 0L) {
                        errorText = "يرجى اختيار العميل"
                        return@Button
                    }
                    isSubmitting = true
                    onSave(selectedCustomerId, amount, System.currentTimeMillis(), description, refNumber, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusDebtRed),
                modifier = Modifier.testTag("confirm_debt_button"),
                enabled = !isSubmitting
            ) {
                Text("حفظ الدين")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun AddPaymentDialog(
    customers: List<CustomerWithBalance>,
    preselectedCustomerId: Long? = null,
    currency: String,
    allowCreditBalance: Boolean,
    onDismiss: () -> Unit,
    onSave: (customerId: Long, amount: Double, date: Long, method: String, refNumber: String, notes: String) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: customers.firstOrNull()?.customer?.id ?: 0L)
    }
    var amountStr by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("نقداً") }
    var refNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val methods = listOf("نقداً", "حوالة مالية", "إيداع بنكي", "شيك", "أخرى")
    val selectedCustomer = customers.find { it.customer.id == selectedCustomerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل دفعة / سداد (-)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StatusPaidGreen
            )
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

                if (selectedCustomer != null) {
                    Text(
                        text = "العميل: ${selectedCustomer.customer.name} | الرصيد المستحق: ${String.format(Locale.US, "%.2f", selectedCustomer.balance)} $currency",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorText = null
                    },
                    label = { Text("مبلغ الدفعة ($currency) *") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    singleLine = true
                )

                // Payment method quick selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    methods.take(3).forEach { m ->
                        OutlinedButton(
                            onClick = { method = m },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (method == m) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(m, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("رقم الإيصال أو الحوالة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        errorText = "يرجى إدخال مبلغ صحيح أكبر من الصفر"
                        return@Button
                    }
                    if (selectedCustomer != null && !allowCreditBalance && amount > selectedCustomer.balance + 0.01) {
                        errorText = "مبلغ الدفعة ($amount) يتجاوز الرصيد المستحق (${selectedCustomer.balance} $currency). يرجى مراجعة المبلغ أو تفعيل الرصيد الدائن من الإعدادات."
                        return@Button
                    }
                    isSubmitting = true
                    onSave(selectedCustomerId, amount, System.currentTimeMillis(), method, refNumber, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusPaidGreen),
                modifier = Modifier.testTag("confirm_payment_button"),
                enabled = !isSubmitting
            ) {
                Text("حفظ الدفعة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun ReverseTransactionDialog(
    transactionId: Long,
    amount: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (reason: String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إلغاء وعكس الحركة المالية",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StatusDebtRed
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "سيتم توثيق إلغاء الحركة المالية رقم #$transactionId بمبلغ $amount $currency وإنشاء حركة عكسية في سجل التدقيق المالي لحفظ سلامة الحسابات دون حذف السجل التاريخي.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = {
                        reason = it
                        errorText = null
                    },
                    label = { Text("سبب الإلغاء أو التصحيح *") },
                    placeholder = { Text("مثال: خطأ في إدخال المبلغ، تكرار بالخطأ...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank()) {
                        errorText = "يرجى ذكر سبب الإلغاء لتوثيقه في سجل التدقيق"
                        return@Button
                    }
                    onConfirm(reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusDebtRed)
            ) {
                Text("تأكيد الإلغاء الموثق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تراجع")
            }
        }
    )
}
