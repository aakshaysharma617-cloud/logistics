package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Booking
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel
import com.example.ui.viewmodel.ReminderAlert
import com.example.ui.viewmodel.ReminderType
import java.text.NumberFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: LogisticsViewModel,
    onNavigateToBookings: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToExpenses: () -> Unit
) {
    val bookings by viewModel.filteredBookings.collectAsState()
    val rawBookings by viewModel.bookings.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val notifications by viewModel.inAppNotifications.collectAsState()

    // Dynamic Calculations for 8 Dashboard KPIs
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val bookingsToday = rawBookings.filter { it.bookingDate >= todayStart }

    val vehiclesToday = bookingsToday.filter { it.vehicleNumber.isNotBlank() }.size
    val totalPickups = rawBookings.filter { it.bookingStatus == "Booked" || it.bookingStatus == "Loading" }.size
    val totalDeliveries = rawBookings.filter { it.bookingStatus == "Delivered" || it.bookingStatus == "Completed" }.size
    val todayRevenue = bookingsToday.sumOf { it.freightAmount }

    val pendingCustomerPayments = rawBookings.filter { it.paymentStatus != "Paid" }.sumOf { it.balanceAmount }
    val pendingDriverPayments = rawBookings.sumOf { it.driverRemainingBalance }

    val activeBookings = rawBookings.filter { it.bookingStatus != "Completed" }.size
    val completedBookings = rawBookings.filter { it.bookingStatus == "Completed" }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Welcome Hero Banner
        item {
            WelcomeBanner()
        }

        // Reminders & Active Alerts Panel
        if (reminders.isNotEmpty()) {
            item {
                Text(
                    text = "⚠️ Operations Alerts (${reminders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(reminders.take(3)) { alert ->
                ReminderAlertCard(alert = alert, onNavigateToBookings = onNavigateToBookings, onNavigateToPayments = onNavigateToPayments)
            }
        }

        // 💰 Fleet Profitability Ledger Card
        item {
            val totalRevenue = rawBookings.sumOf { it.freightAmount }
            val totalExpenses = rawBookings.sumOf { it.getTotalExpenses() }
            val netProfit = totalRevenue - totalExpenses

            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💰 Fleet Profitability Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gross Revenue
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Gross Revenue", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }

                        // Total Expenses
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Direct Expenses", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(totalExpenses), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusPending)
                        }

                        // Net Profit
                        val profitColor = if (netProfit >= 0) StatusPaid else StatusPending
                        val profitBg = if (netProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .background(profitBg, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text("Net Profit", fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatCurrency(netProfit), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = profitColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-calculated in real-time based on linked costs.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = onNavigateToExpenses,
                            colors = ButtonDefaults.textButtonColors(contentColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Manage Expenses")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage Expenses", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 8 KPI Grid Metrics
        item {
            Text(
                text = "📊 Operational Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        title = "Active Bookings",
                        value = activeBookings.toString(),
                        icon = Icons.Default.Timeline,
                        backgroundColor = BlueAccent,
                        iconColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Completed Trips",
                        value = completedBookings.toString(),
                        icon = Icons.Default.DoneAll,
                        backgroundColor = Color(0xFFE8F5E9),
                        iconColor = StatusPaid,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        title = "Vehicles Today",
                        value = vehiclesToday.toString(),
                        icon = Icons.Default.LocalShipping,
                        backgroundColor = Color(0xFFE3F2FD),
                        iconColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Today's Revenue",
                        value = formatCurrency(todayRevenue),
                        icon = Icons.Default.AccountBalanceWallet,
                        backgroundColor = Color(0xFFFFFDE7),
                        iconColor = StatusPartial,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        title = "Pending Payments",
                        value = formatCurrency(pendingCustomerPayments),
                        icon = Icons.Default.TrendingUp,
                        backgroundColor = Color(0xFFFFEBEE),
                        iconColor = StatusPending,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Driver Pending",
                        value = formatCurrency(pendingDriverPayments),
                        icon = Icons.Default.AssignmentLate,
                        backgroundColor = Color(0xFFECEFF1),
                        iconColor = Color.DarkGray,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        title = "Total Customers",
                        value = customers.size.toString(),
                        icon = Icons.Default.Groups,
                        backgroundColor = Color(0xFFF3E5F5),
                        iconColor = Color(0xFF8E24AA),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Drivers",
                        value = drivers.size.toString(),
                        icon = Icons.Default.DirectionsCar,
                        backgroundColor = Color(0xFFE0F7FA),
                        iconColor = Color(0xFF00ACC1),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Charts & Insights
        item {
            Text(
                text = "📈 Monthly Financial Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FinancialChartsSection(bookings = rawBookings)
        }

        // Dynamic System Notifications Logs
        item {
            Text(
                text = "🔔 Live Operational Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (notifications.isEmpty()) {
                        Text(
                            text = "No recent operations recorded.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    } else {
                        notifications.take(5).forEach { log ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BluePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BluePrimary)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BlueDark, BluePrimary)
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "Shree UP Bihar Logistics",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Welcome Back, Owner Console",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Truck Icon",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(56.dp)
                        .weight(0.5f)
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ReminderAlertCard(
    alert: ReminderAlert,
    onNavigateToBookings: () -> Unit,
    onNavigateToPayments: () -> Unit
) {
    val indicatorColor = when (alert.type) {
        ReminderType.CUSTOMER_DUE -> StatusPending
        ReminderType.DRIVER_CASH -> StatusPartial
        ReminderType.DRIVER_PENDING -> BluePrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (alert.type == ReminderType.CUSTOMER_DUE) {
                    onNavigateToPayments()
                } else {
                    onNavigateToBookings()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
                Text(
                    text = alert.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Resolve",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FinancialChartsSection(bookings: List<Booking>) {
    // Collect last 6 bookings or mock revenue/expenses for visualization
    val recentRevenue = bookings.take(5).reversed().map { it.freightAmount }
    val recentExpenses = bookings.take(5).reversed().map { it.getTotalExpenses() }
    val recentProfits = bookings.take(5).reversed().map { it.getProfit() }

    val labels = bookings.take(5).reversed().map { it.bookingIdString }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Revenue vs Direct Expenses (Last 5 Bookings)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (recentRevenue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No financial data to plot.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Interactive Canvas Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val maxVal = (recentRevenue.maxOrNull() ?: 1.0) * 1.15
                    val itemsCount = recentRevenue.size

                    val colWidth = width / (itemsCount * 2 + 1)

                    // Draw baseline
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, height - 20f),
                        end = Offset(width, height - 20f),
                        strokeWidth = 2f
                    )

                    recentRevenue.forEachIndexed { index, rev ->
                        val exp = recentExpenses.getOrElse(index) { 0.0 }

                        val revHeight = ((rev / maxVal) * (height - 40f)).toFloat()
                        val expHeight = ((exp / maxVal) * (height - 40f)).toFloat()

                        val xRev = (index * 2 + 1) * colWidth
                        val xExp = xRev + colWidth / 1.5f

                        // Draw Revenue Bar
                        drawRect(
                            color = BluePrimary,
                            topLeft = Offset(xRev, height - 20f - revHeight),
                            size = Size(colWidth / 1.8f, revHeight)
                        )

                        // Draw Expense Bar
                        drawRect(
                            color = StatusPending,
                            topLeft = Offset(xExp, height - 20f - expHeight),
                            size = Size(colWidth / 1.8f, expHeight)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(12.dp).background(BluePrimary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revenue", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(12.dp).background(StatusPending))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Direct Expenses", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}
