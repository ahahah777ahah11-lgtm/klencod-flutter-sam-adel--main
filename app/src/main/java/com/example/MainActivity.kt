package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.AddDebtDialog
import com.example.ui.components.AddPaymentDialog
import com.example.ui.screens.BackupImportScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.SmartDebtTheme
import com.example.ui.viewmodel.CustomerViewModel
import com.example.ui.viewmodel.DebtViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Dashboard : Screen("dashboard", "الرئيسية", Icons.Default.Home)
    data object Customers : Screen("customers", "العملاء", Icons.Default.People)
    data object Transfers : Screen("transfers", "الحوالات", Icons.Default.Receipt)
    data object Sms : Screen("sms", "رسائل SMS", Icons.Default.Message)
    data object Settings : Screen("settings", "الإعدادات", Icons.Default.Settings)
    data class CustomerDetail(val customerId: Long) : Screen("customer_detail", "تفاصيل العميل")
    data class Reports(val customerId: Long? = null) : Screen("reports", "التقارير")
    data object BackupImport : Screen("backup_import", "النسخ والاستيراد")
}

class MainActivity : ComponentActivity() {

    private val viewModel: DebtViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsMap by viewModel.settingsMap.collectAsState()
            val themeStyle = settingsMap["theme_style"] ?: "blue_teal"
            val darkModeSetting = settingsMap["dark_mode"] ?: "system"

            val isDark = when (darkModeSetting) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            SmartDebtTheme(
                darkTheme = isDark,
                themeStyle = themeStyle
            ) {
                MainApp(
                    viewModel = viewModel,
                    customerViewModel = customerViewModel
                )
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: DebtViewModel,
    customerViewModel: CustomerViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Customers) }
    var screenHistory by remember { mutableStateOf(listOf<Screen>()) }

    fun navigateTo(screen: Screen) {
        screenHistory = screenHistory + currentScreen
        currentScreen = screen
    }

    fun navigateBack() {
        if (screenHistory.isNotEmpty()) {
            val previous = screenHistory.last()
            screenHistory = screenHistory.dropLast(1)
            currentScreen = previous
        } else {
            currentScreen = Screen.Customers
        }
    }

    // Global Dialogs
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEditId by remember { mutableStateOf<Long?>(null) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var preselectedDebtCustomerId by remember { mutableStateOf<Long?>(null) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var preselectedPaymentCustomerId by remember { mutableStateOf<Long?>(null) }

    val customersWithBalances by viewModel.allCustomersWithBalances.collectAsState()
    val currency = viewModel.getSetting("currency", "ر.ي")
    val allowCreditBalance = viewModel.getSetting("allow_credit_balance", "false").toBoolean()

    // Snackbar Host
    val snackbarHostState = remember { SnackbarHostState() }
    val userFeedback by viewModel.userFeedback.collectAsState()
    val customerFeedback by customerViewModel.userMessage.collectAsState()

    LaunchedEffect(userFeedback) {
        userFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(customerFeedback) {
        customerFeedback?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            customerViewModel.clearUserMessage()
        }
    }

    val bottomNavItems = listOf(
        Screen.Customers,
        Screen.Dashboard,
        Screen.Transfers,
        Screen.Sms,
        Screen.Settings
    )

    val isTopLevelScreen = currentScreen in bottomNavItems

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevelScreen) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentScreen.route == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentScreen != screen) {
                                    currentScreen = screen
                                }
                            },
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            modifier = Modifier.testTag("nav_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCustomers = { currentScreen = Screen.Customers },
                        onNavigateToCustomerDetail = { cId -> navigateTo(Screen.CustomerDetail(cId)) },
                        onNavigateToTransfers = { currentScreen = Screen.Transfers },
                        onNavigateToSms = { currentScreen = Screen.Sms },
                        onNavigateToReports = { navigateTo(Screen.Reports()) },
                        onNavigateToBackup = { navigateTo(Screen.BackupImport) },
                        onOpenAddCustomer = {
                            customerToEditId = null
                            showAddCustomerDialog = true
                        },
                        onOpenAddDebt = {
                            preselectedDebtCustomerId = null
                            showAddDebtDialog = true
                        },
                        onOpenAddPayment = {
                            preselectedPaymentCustomerId = null
                            showAddPaymentDialog = true
                        }
                    )
                }

                is Screen.Customers -> {
                    CustomersScreen(
                        viewModel = viewModel,
                        customerViewModel = customerViewModel,
                        onNavigateToCustomerDetail = { cId -> navigateTo(Screen.CustomerDetail(cId)) },
                        onOpenAddCustomer = {
                            customerToEditId = null
                            showAddCustomerDialog = true
                        },
                        onOpenAddDebt = { cId ->
                            preselectedDebtCustomerId = cId
                            showAddDebtDialog = true
                        },
                        onOpenAddPayment = { cId ->
                            preselectedPaymentCustomerId = cId
                            showAddPaymentDialog = true
                        },
                        onOpenSms = { cId ->
                            navigateTo(Screen.Sms)
                        }
                    )
                }

                is Screen.CustomerDetail -> {
                    CustomerDetailScreen(
                        customerId = screen.customerId,
                        viewModel = viewModel,
                        onBack = { navigateBack() },
                        onOpenEdit = {
                            customerToEditId = screen.customerId
                            showAddCustomerDialog = true
                        },
                        onOpenSms = { cId ->
                            navigateTo(Screen.Sms)
                        },
                        onOpenStatement = { cId ->
                            navigateTo(Screen.Reports(cId))
                        }
                    )
                }

                is Screen.Transfers -> {
                    TransfersScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }

                is Screen.Sms -> {
                    SmsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }

                is Screen.Reports -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        preselectedCustomerId = screen.customerId,
                        onBack = { navigateBack() }
                    )
                }

                is Screen.BackupImport -> {
                    BackupImportScreen(
                        viewModel = viewModel,
                        onBack = { navigateBack() }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = Screen.Dashboard }
                    )
                }
            }
        }
    }

    // Global Add/Edit Customer Dialog
    if (showAddCustomerDialog) {
        val initialCustomer = customerToEditId?.let { id -> customersWithBalances.find { it.customer.id == id } }
        AddCustomerDialog(
            initialCustomer = initialCustomer,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, altPhone, address, company, notes, category, dueDate, initialDebt ->
                if (initialCustomer == null) {
                    customerViewModel.saveCustomer(name, phone, initialDebt) {
                        showAddCustomerDialog = false
                    }
                } else {
                    viewModel.updateCustomer(initialCustomer.customer.id, name, phone, altPhone, address, company, notes, category, dueDate) {
                        showAddCustomerDialog = false
                    }
                }
            }
        )
    }

    // Global Add Debt Dialog
    if (showAddDebtDialog) {
        AddDebtDialog(
            customers = customersWithBalances.filter { it.customer.status == "ACTIVE" },
            preselectedCustomerId = preselectedDebtCustomerId,
            currency = currency,
            onDismiss = { showAddDebtDialog = false },
            onSave = { customerId, amount, date, description, refNumber, notes ->
                viewModel.addDebt(customerId, amount, date, description, refNumber, notes) {
                    showAddDebtDialog = false
                }
            }
        )
    }

    // Global Add Payment Dialog
    if (showAddPaymentDialog) {
        AddPaymentDialog(
            customers = customersWithBalances.filter { it.customer.status == "ACTIVE" },
            preselectedCustomerId = preselectedPaymentCustomerId,
            currency = currency,
            allowCreditBalance = allowCreditBalance,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { customerId, amount, date, method, refNumber, notes ->
                viewModel.addPayment(customerId, amount, date, method, refNumber, notes) {
                    showAddPaymentDialog = false
                }
            }
        )
    }
}
