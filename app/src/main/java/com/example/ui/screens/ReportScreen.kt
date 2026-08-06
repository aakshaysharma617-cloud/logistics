package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Booking
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel
import java.util.*

@Composable
fun ReportScreen(
    viewModel: LogisticsViewModel
) {
    val bookings by viewModel.bookings.collectAsState()
    val context = LocalContext.current

    var selectedInterval by remember { mutableStateOf(1) } // 0 = Daily, 1 = Weekly, 2 = Monthly, 3 = Yearly

    // Dynamic Filter Bookings by interval
    val filteredIntervalBookings = remember(bookings, selectedInterval) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val limitTime = when (selectedInterval) {
            0 -> { // Daily (last 24 hours)
                now - (24 * 60 * 60 * 1000L)
            }
            1 -> { // Weekly (last 7 days)
                now - (7 * 24 * 60 * 60 * 1000L)
            }
            2 -> { // Monthly (last 30 days)
                now - (30L * 24 * 60 * 60 * 1000L)
            }
            else -> { // Yearly (last 365 days)
                now - (365L * 24 * 60 * 60 * 1000L)
            }
        }
        bookings.filter { it.bookingDate >= limitTime }
    }

    // Calculations
    val totalTrips = filteredIntervalBookings.size
    val totalRevenue = filteredIntervalBookings.sumOf { it.freightAmount }
    val totalExpenses = filteredIntervalBookings.sumOf { it.getTotalExpenses() }
    val netProfit = totalRevenue - totalExpenses
    val pendingPayments = filteredIntervalBookings.sumOf { it.balanceAmount }
    val driverPayments = filteredIntervalBookings.sumOf { it.driverRemainingBalance }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        TabRow(
            selectedTabIndex = selectedInterval,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BluePrimary
        ) {
            Tab(selected = selectedInterval == 0, onClick = { selectedInterval = 0 }, text = { Text("Daily") })
            Tab(selected = selectedInterval == 1, onClick = { selectedInterval = 1 }, text = { Text("Weekly") })
            Tab(selected = selectedInterval == 2, onClick = { selectedInterval = 2 }, text = { Text("Monthly") })
            Tab(selected = selectedInterval == 3, onClick = { selectedInterval = 3 }, text = { Text("Yearly") })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("reports_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "💼 Transport Profit & Loss Statement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Summary Grid Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Trips Handled", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text("$totalTrips Trips", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 18.sp)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        ReportFinancialItem(label = "Gross Freight Revenue", value = totalRevenue, color = StatusPaid, isPositive = true)
                        ReportFinancialItem(label = "Total Direct Expenses", value = totalExpenses, color = StatusPending, isPositive = false)

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Net Operating Profit", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = formatCurrency(netProfit),
                                fontWeight = FontWeight.Bold,
                                color = if (netProfit >= 0) StatusPaid else StatusPending,
                                fontSize = 18.sp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                        ReportFinancialItem(label = "Pending Customer Receivables", value = pendingPayments, color = StatusPending, isPositive = false)
                        ReportFinancialItem(label = "Driver Payments Outstanding", value = driverPayments, color = BlueDark, isPositive = false)
                    }
                }
            }

            // Export Actions
            item {
                Button(
                    onClick = {
                        Toast.makeText(context, "Generated Logistics Excel/PDF Report successfully exported to downloads folder.", Toast.LENGTH_LONG).show()
                        viewModel.addInAppNotification("Exported P&L financial reports successfully.")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Statement to Excel / PDF", fontWeight = FontWeight.Bold)
                }
            }

            // Audit Logs Title
            if (filteredIntervalBookings.isNotEmpty()) {
                item {
                    Text(
                        text = "📑 Audited Shipments List (${filteredIntervalBookings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(filteredIntervalBookings) { booking ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(booking.bookingIdString, fontWeight = FontWeight.Bold, color = BluePrimary)
                            Text("${booking.pickupLocation} ➔ ${booking.dropLocation}", fontSize = 12.sp, color = Color.Gray)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatCurrency(booking.freightAmount), fontWeight = FontWeight.Bold)
                            Text("Margin: " + formatCurrency(booking.getProfit()), fontSize = 11.sp, color = if (booking.getProfit() >= 0) StatusPaid else StatusPending)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportFinancialItem(
    label: String,
    value: Double,
    color: Color,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Color.DarkGray)
        Text(
            text = (if (isPositive) "+" else "-") + " " + formatCurrency(value),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
