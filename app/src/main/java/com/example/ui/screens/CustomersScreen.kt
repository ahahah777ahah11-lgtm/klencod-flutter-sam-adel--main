package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.domain.model.CustomerWithBalance
import com.example.ui.theme.FinancialDarkBlue
import com.example.ui.theme.FinancialTurquoise
import com.example.ui.theme.StatusDebtRed
import com.example.ui.theme.StatusDebtRedContainer
import com.example.ui.theme.StatusPaidGreen
import com.example.ui.theme.StatusPaidGreenContainer
import com.example.ui.theme.StatusWarningOrange
import com.example.ui.theme.StatusWarningOrangeContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.NewCustomerDialog
import com.example.ui.viewmodel.CustomerViewModel

@Composable
fun CustomersScreen(
    viewModel: DebtViewModel,
    customerViewModel: CustomerViewModel? = null,
    onNavigateToCustomerDetail: (Long) -> Unit,
    onOpenAddCustomer: (() -> Unit)? = null,
    onOpenAddDebt: (Long) -> Unit,
    onOpenAddPayment: (Long) -> Unit,
    onOpenSms: (Long) -> Unit
) {
    val customers by (customerViewModel?.customers ?: viewModel.filteredCustomers).collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.customerFilter.collectAsState()
    val currentSort by viewModel.customerSort.collectAsState()
    val currency = viewModel.getSetting("currency", "ر.ي")
    val context = LocalContext.current

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showDirectAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (onOpenAddCustomer != null) {
                        onOpenAddCustomer()
                    } else {
                        showDirectAddDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة عميل")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("customers_screen")
        ) {
            // Search & Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("بحث بالاسم أو الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_customer_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "ترتيب")
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("حسب الرصيد (الأعلى أولاً)") },
                            onClick = {
                                viewModel.setCustomerSort("BALANCE_DESC")
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حسب الاسم (أبجدياً)") },
                            onClick = {
                                viewModel.setCustomerSort("NAME")
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حسب آخر معاملة") },
                            onClick = {
                                viewModel.setCustomerSort("DATE_DESC")
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "جميع العملاء",
                    "INDEBTED" to "المدينون فقط",
                    "PAID" to "المسددون",
                    "ZERO" to "بلا رصيد",
                    "ARCHIVED" to "الأرشيف"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = currentFilter == key,
                        onClick = { viewModel.setCustomerFilter(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Cards List
            if (customers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "لم يتم العثور على عملاء يطابقون البحث" else "لا يوجد عملاء في هذه القائمة",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customers, key = { it.customer.id }) { customerWithBal ->
                        CustomerCard(
                            item = customerWithBal,
                            currency = currency,
                            onClick = { onNavigateToCustomerDetail(customerWithBal.customer.id) },
                            onQuickDebt = { onOpenAddDebt(customerWithBal.customer.id) },
                            onQuickPayment = { onOpenAddPayment(customerWithBal.customer.id) },
                            onQuickSms = { onOpenSms(customerWithBal.customer.id) },
                            onCall = { phone ->
                                if (phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDirectAddDialog) {
        NewCustomerDialog(
            onDismiss = { showDirectAddDialog = false },
            onConfirm = { name, phone ->
                if (customerViewModel != null) {
                    customerViewModel.saveCustomer(name, phone) {
                        showDirectAddDialog = false
                    }
                } else {
                    viewModel.addCustomer(name, phone, "", "", "", "", "عام", null) {
                        showDirectAddDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    currency: String,
    onClick: () -> Unit,
    onQuickDebt: () -> Unit,
    onQuickPayment: () -> Unit,
    onQuickSms: () -> Unit,
    onCall: (String) -> Unit
) {
    val isIndebted = item.balance > 0.01
    val isCredit = item.balance < -0.01
    val isSettled = Math.abs(item.balance) <= 0.01

    val balanceColor = when {
        isIndebted -> StatusDebtRed
        isCredit -> FinancialDarkBlue
        else -> StatusPaidGreen
    }

    val balanceBg = when {
        isIndebted -> StatusDebtRedContainer
        isCredit -> FinancialTurquoise.copy(alpha = 0.15f)
        else -> StatusPaidGreenContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_card_${item.customer.id}"),
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
                // Name & Tags
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.customer.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (item.customer.category.isNotBlank() && item.customer.category != "عام") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = item.customer.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (item.customer.phone.isNotBlank()) {
                        Text(
                            text = item.customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Balance Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = balanceBg
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = when {
                                isIndebted -> "المستحق عليه"
                                isCredit -> "رصيد دائن له"
                                else -> "مسدد بالكامل"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = balanceColor
                        )
                        Text(
                            text = "${String.format(Locale.US, "%,.2f", Math.abs(item.balance))} $currency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = balanceColor
                        )
                    }
                }
            }

            // Overdue warning banner if due date passed
            if (item.isOverdue) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(StatusWarningOrangeContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusWarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تجاوز تاريخ الاستحقاق المحدد",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusWarningOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Add Debt
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onQuickDebt() },
                    shape = RoundedCornerShape(8.dp),
                    color = StatusDebtRedContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = StatusDebtRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ دين", style = MaterialTheme.typography.labelSmall, color = StatusDebtRed, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Add Payment
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onQuickPayment() },
                    shape = RoundedCornerShape(8.dp),
                    color = StatusPaidGreenContainer
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ دفعة", style = MaterialTheme.typography.labelSmall, color = StatusPaidGreen, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick SMS
                if (item.customer.phone.isNotBlank() && isIndebted) {
                    IconButton(
                        onClick = onQuickSms,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = "SMS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Quick Call
                if (item.customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = { onCall(item.customer.phone) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "اتصال",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
