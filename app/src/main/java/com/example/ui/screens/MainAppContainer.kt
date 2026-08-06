package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel

enum class NavigationRoute {
    DASHBOARD,
    BOOKINGS,
    CUSTOMERS,
    DRIVERS,
    PAYMENTS,
    EXPENSES,
    BROADCAST,
    REPORTS,
    AI_COPILOT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: LogisticsViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val notifications by viewModel.inAppNotifications.collectAsState()
    var lastSeenNotification by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            val latest = notifications.first()
            if (latest != lastSeenNotification) {
                lastSeenNotification = latest
                val cleanMessage = if (latest.startsWith("[") && latest.contains("] ")) {
                    latest.substringAfter("] ")
                } else {
                    latest
                }
                snackbarHostState.showSnackbar(
                    message = cleanMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    var currentScreen by remember { mutableStateOf(NavigationRoute.DASHBOARD) }
    var activeBookingIdForDocuments by remember { mutableStateOf<Int?>(null) }

    // Owner override pin gate for Staff
    var showOwnerPinGate by remember { mutableStateOf(false) }
    var targetScreenAfterPinGate by remember { mutableStateOf<NavigationRoute?>(null) }
    var enteredOwnerPin by remember { mutableStateOf("") }

    val userRole = currentUser?.role ?: "Staff"

    fun navigateTo(route: NavigationRoute) {
        val isRestricted = route == NavigationRoute.PAYMENTS || 
                            route == NavigationRoute.EXPENSES || 
                            route == NavigationRoute.REPORTS

        if (isRestricted && userRole == "Staff") {
            // Trigger Owner PIN override check
            targetScreenAfterPinGate = route
            enteredOwnerPin = ""
            showOwnerPinGate = true
        } else {
            currentScreen = route
            activeBookingIdForDocuments = null
        }
    }

    // Adaptive Sizing determination (Responsive Design)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Adaptive Side Navigation Rail for tablets
            if (isTablet) {
                NavigationRail(
                    containerColor = BlueDark,
                    contentColor = Color.White,
                    header = {
                        Box(
                            modifier = Modifier
                                .padding(16.dp)
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SUB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.DASHBOARD && activeBookingIdForDocuments == null,
                        onClick = { navigateTo(NavigationRoute.DASHBOARD) },
                        icon = { Icon(Icons.Default.Dashboard, "Dashboard", tint = Color.White) },
                        label = { Text("Dashboard", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.BOOKINGS || activeBookingIdForDocuments != null,
                        onClick = { navigateTo(NavigationRoute.BOOKINGS) },
                        icon = { Icon(Icons.Default.Inventory, "Bookings", tint = Color.White) },
                        label = { Text("Bookings", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.CUSTOMERS,
                        onClick = { navigateTo(NavigationRoute.CUSTOMERS) },
                        icon = { Icon(Icons.Default.Groups, "Customers", tint = Color.White) },
                        label = { Text("Customers", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.DRIVERS,
                        onClick = { navigateTo(NavigationRoute.DRIVERS) },
                        icon = { Icon(Icons.Default.DirectionsCar, "Drivers", tint = Color.White) },
                        label = { Text("Drivers", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.PAYMENTS,
                        onClick = { navigateTo(NavigationRoute.PAYMENTS) },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, "Payments", tint = Color.White) },
                        label = { Text("Payments", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.EXPENSES,
                        onClick = { navigateTo(NavigationRoute.EXPENSES) },
                        icon = { Icon(Icons.Default.MoneyOff, "Expenses", tint = Color.White) },
                        label = { Text("Expenses", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.BROADCAST,
                        onClick = { navigateTo(NavigationRoute.BROADCAST) },
                        icon = { Icon(Icons.Default.RecordVoiceOver, "Broadcast", tint = Color.White) },
                        label = { Text("Broadcast", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.AI_COPILOT,
                        onClick = { navigateTo(NavigationRoute.AI_COPILOT) },
                        icon = { Icon(Icons.Default.SupportAgent, "AI Copilot", tint = Color.White) },
                        label = { Text("AI Copilot", color = Color.White, fontSize = 11.sp) }
                    )

                    NavigationRailItem(
                        selected = currentScreen == NavigationRoute.REPORTS,
                        onClick = { navigateTo(NavigationRoute.REPORTS) },
                        icon = { Icon(Icons.Default.Assessment, "Reports", tint = Color.White) },
                        label = { Text("Reports", color = Color.White, fontSize = 11.sp) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                        Icon(Icons.Default.Logout, "Logout", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Primary Content Scaffolding
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (activeBookingIdForDocuments == null) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = when (currentScreen) {
                                            NavigationRoute.DASHBOARD -> "Shree UP Bihar Logistics"
                                            NavigationRoute.BOOKINGS -> "Shipment Bookings"
                                            NavigationRoute.CUSTOMERS -> "Customer Management"
                                            NavigationRoute.DRIVERS -> "Driver Directory"
                                            NavigationRoute.PAYMENTS -> "Payment Accounts"
                                            NavigationRoute.EXPENSES -> "Expense Ledgers"
                                            NavigationRoute.BROADCAST -> "WhatsApp Broadcasts"
                                            NavigationRoute.REPORTS -> "Financial Audit Reports"
                                            NavigationRoute.AI_COPILOT -> "AI Logistics Copilot"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Role: ${currentUser?.name ?: "Operator"}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            },
                            actions = {
                                val isDark by viewModel.isDarkMode.collectAsState()
                                IconButton(
                                    onClick = { viewModel.toggleTheme() },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme",
                                        tint = Color.White
                                    )
                                }
                                if (!isTablet) {
                                    IconButton(onClick = { viewModel.logout(); onLogout() }) {
                                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
                        )
                    }
                },
                bottomBar = {
                    // Bottom Navigation Bar for standard mobile screens
                    if (!isTablet && activeBookingIdForDocuments == null) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = BluePrimary,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.DASHBOARD,
                                onClick = { navigateTo(NavigationRoute.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                                label = { Text("Home", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.BOOKINGS,
                                onClick = { navigateTo(NavigationRoute.BOOKINGS) },
                                icon = { Icon(Icons.Default.Inventory, "Bookings") },
                                label = { Text("Bookings", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.CUSTOMERS,
                                onClick = { navigateTo(NavigationRoute.CUSTOMERS) },
                                icon = { Icon(Icons.Default.Groups, "Customers") },
                                label = { Text("Clients", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.PAYMENTS,
                                onClick = { navigateTo(NavigationRoute.PAYMENTS) },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, "Payments") },
                                label = { Text("Payments", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.BROADCAST,
                                onClick = { navigateTo(NavigationRoute.BROADCAST) },
                                icon = { Icon(Icons.Default.RecordVoiceOver, "Broadcast") },
                                label = { Text("Broadcast", fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == NavigationRoute.AI_COPILOT,
                                onClick = { navigateTo(NavigationRoute.AI_COPILOT) },
                                icon = { Icon(Icons.Default.SupportAgent, "AI Copilot") },
                                label = { Text("AI Copilot", fontSize = 10.sp) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (activeBookingIdForDocuments != null) {
                        DocumentScreen(
                            viewModel = viewModel,
                            bookingId = activeBookingIdForDocuments!!,
                            onBack = { activeBookingIdForDocuments = null }
                        )
                    } else {
                        // Core Route Rendering Engine
                        when (currentScreen) {
                            NavigationRoute.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToBookings = { navigateTo(NavigationRoute.BOOKINGS) },
                                onNavigateToPayments = { navigateTo(NavigationRoute.PAYMENTS) },
                                onNavigateToExpenses = { navigateTo(NavigationRoute.EXPENSES) }
                            )
                            NavigationRoute.BOOKINGS -> BookingScreen(
                                viewModel = viewModel,
                                onNavigateToDocuments = { activeBookingIdForDocuments = it }
                            )
                            NavigationRoute.CUSTOMERS -> CustomerScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.DRIVERS -> DriverScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.PAYMENTS -> PaymentScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.EXPENSES -> ExpenseScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.BROADCAST -> BroadcastScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.REPORTS -> ReportScreen(
                                viewModel = viewModel
                            )
                            NavigationRoute.AI_COPILOT -> AiCopilotScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    // Owner PIN override Gate Dialog for Staff
    if (showOwnerPinGate) {
        AlertDialog(
            onDismissRequest = { showOwnerPinGate = false },
            title = { Text("⚠️ Restricted: Owner PIN Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This operation is locked for Staff accounts. Please enter the Owner passcode to access.", fontSize = 13.sp)
                    OutlinedTextField(
                        value = enteredOwnerPin,
                        onValueChange = { enteredOwnerPin = it },
                        placeholder = { Text("Enter 4-digit Owner PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredOwnerPin == "1234") {
                            showOwnerPinGate = false
                            currentScreen = targetScreenAfterPinGate ?: NavigationRoute.DASHBOARD
                            activeBookingIdForDocuments = null
                            Toast.makeText(context, "Access Granted.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Incorrect Owner PIN. Access Denied.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOwnerPinGate = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
