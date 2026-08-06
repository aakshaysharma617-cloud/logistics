package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.Booking
import com.example.data.models.Customer
import com.example.data.models.Driver
import com.example.ui.theme.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.ui.viewmodel.LogisticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(
    viewModel: LogisticsViewModel,
    onNavigateToDocuments: (Int) -> Unit
) {
    val bookings by viewModel.filteredBookings.collectAsState()
    val rawBookings by viewModel.bookings.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val drivers by viewModel.drivers.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBookingForDetail by remember { mutableStateOf<Booking?>(null) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val selectedPaymentStatusFilter by viewModel.selectedPaymentStatusFilter.collectAsState()

    val statuses = listOf("Active Trips", "All", "Booked", "Loading", "In Transit", "Delivered", "Completed")
    val paymentStatuses = listOf("All Payments", "Paid", "Pending", "Partial", "Overdue")

    // KPI Counts for status summary header
    val activeTripsCount = rawBookings.count { it.bookingStatus != "Completed" }
    val bookedCount = rawBookings.count { it.bookingStatus == "Booked" }
    val loadingCount = rawBookings.count { it.bookingStatus == "Loading" }
    val inTransitCount = rawBookings.count { it.bookingStatus == "In Transit" }
    val deliveredCount = rawBookings.count { it.bookingStatus == "Delivered" }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search by customer name, origin, ID, driver...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("booking_search"),
                shape = RoundedCornerShape(12.dp)
            )

            // Scrollable Status & Payment Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trip Status Chips
                items(statuses) { status ->
                    val isSelected = selectedStatusFilter == status || (selectedStatusFilter == "All" && status == "All")
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedStatusFilter.value = status },
                        label = { Text(status, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }

                item {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }

                // Payment Status Chips
                items(paymentStatuses) { pStatus ->
                    val isSelected = selectedPaymentStatusFilter == pStatus || (selectedPaymentStatusFilter == "All Payments" && pStatus == "All Payments")
                    val chipColor = when (pStatus) {
                        "Paid" -> Color(0xFF2E7D32)
                        "Pending" -> Color(0xFFED6C02)
                        "Partial" -> Color(0xFF0288D1)
                        "Overdue" -> Color(0xFFD32F2F)
                        else -> BluePrimary
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedPaymentStatusFilter.value = pStatus },
                        label = { Text(pStatus, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Trips Overview Summary Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = "Active Logistics",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active Logistics Trips ($activeTripsCount)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BluePrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${rawBookings.size} Total",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status KPI Badge Counters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusKpiBadge(title = "Booked", count = bookedCount, color = Color(0xFF1976D2), modifier = Modifier.weight(1f))
                                StatusKpiBadge(title = "Loading", count = loadingCount, color = Color(0xFFF57C00), modifier = Modifier.weight(1f))
                                StatusKpiBadge(title = "Transit", count = inTransitCount, color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f))
                                StatusKpiBadge(title = "Delivered", count = deliveredCount, color = Color(0xFF0097A7), modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (bookings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = "Empty",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No logistics trips match selected filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(bookings) { booking ->
                        BookingItemCard(
                            booking = booking,
                            onDateFormatted = { viewModel.formatDate(it) },
                            onClick = { selectedBookingForDetail = booking },
                            onUpdateStatus = { updatedBooking, newStatus ->
                                viewModel.updateBooking(updatedBooking.copy(bookingStatus = newStatus))
                            },
                            onUpdatePaymentStatus = { updatedBooking, newPaymentStatus, newAmount ->
                                val updated = updatedBooking.copy(
                                    paymentStatus = newPaymentStatus,
                                    amountReceived = newAmount,
                                    balanceAmount = (updatedBooking.freightAmount - newAmount).coerceAtLeast(0.0)
                                )
                                viewModel.updateBooking(updated)
                            },
                            onNavigateToDocuments = onNavigateToDocuments
                        )
                    }
                }
            }
        }

        // Create Booking FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_booking_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Booking")
        }

        // Add Booking Form Dialog
        if (showAddDialog) {
            BookingFormDialog(
                customers = customers,
                drivers = drivers,
                onDismiss = { showAddDialog = false },
                onSave = { newBooking ->
                    viewModel.addBooking(newBooking)
                    showAddDialog = false
                }
            )
        }

        // Booking Detail Dialog (Allows updating status/advance payments/expenses)
        selectedBookingForDetail?.let { booking ->
            val customer = customers.find { it.id == booking.customerId }
            BookingDetailDialog(
                booking = booking,
                customer = customer,
                onDismiss = { selectedBookingForDetail = null },
                onUpdate = { updated ->
                    viewModel.updateBooking(updated)
                    selectedBookingForDetail = null
                },
                onDelete = {
                    viewModel.deleteBooking(booking)
                    selectedBookingForDetail = null
                },
                onNavigateToDocuments = onNavigateToDocuments
            )
        }
    }
}

@Composable
fun StatusKpiBadge(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun TripStatusStepper(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Booked", "Loading", "In Transit", "Delivered", "Completed")
    val currentIndex = steps.indexOf(currentStatus).coerceAtLeast(0)

    val activeColor = when (currentStatus) {
        "Booked" -> Color(0xFF1976D2)
        "Loading" -> Color(0xFFF57C00)
        "In Transit" -> Color(0xFF7B1FA2)
        "Delivered" -> Color(0xFF0097A7)
        "Completed" -> Color(0xFF388E3C)
        else -> BluePrimary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { index, step ->
                val isDone = index < currentIndex
                val isCurrent = index == currentIndex

                val circleColor = when {
                    isCurrent -> activeColor
                    isDone -> activeColor.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                // Step Circle node
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 20.dp else 14.dp)
                        .clip(CircleShape)
                        .background(circleColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    } else if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Connecting Line between step nodes
                if (index < steps.size - 1) {
                    val lineColor = if (index < currentIndex) activeColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .padding(horizontal = 2.dp)
                            .background(lineColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Step Stage Label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stage ${currentIndex + 1}/5: $currentStatus",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor
            )

            val nextStatus = when (currentStatus) {
                "Booked" -> "Next: Loading"
                "Loading" -> "Next: In Transit"
                "In Transit" -> "Next: Delivered"
                "Delivered" -> "Next: Complete"
                "Completed" -> "Trip Finished"
                else -> ""
            }
            if (nextStatus.isNotBlank()) {
                Text(
                    text = nextStatus,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BookingItemCard(
    booking: Booking,
    onDateFormatted: (Long) -> String,
    onClick: () -> Unit,
    onUpdateStatus: (Booking, String) -> Unit,
    onUpdatePaymentStatus: (Booking, String, Double) -> Unit,
    onNavigateToDocuments: (Int) -> Unit
) {
    var showPaymentDropdown by remember { mutableStateOf(false) }
    var showPartialPaymentDialog by remember { mutableStateOf(false) }
    var customReceivedAmountText by remember(booking.amountReceived) {
        mutableStateOf(if (booking.amountReceived > 0) booking.amountReceived.toString() else "")
    }

    val statusColor = when (booking.bookingStatus) {
        "Booked" -> Color(0xFF1976D2)
        "Loading" -> Color(0xFFF57C00)
        "In Transit" -> Color(0xFF7B1FA2)
        "Delivered" -> Color(0xFF0097A7)
        "Completed" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    val statusIcon = when (booking.bookingStatus) {
        "Booked" -> Icons.Default.Bookmark
        "Loading" -> Icons.Default.Inventory2
        "In Transit" -> Icons.Default.LocalShipping
        "Delivered" -> Icons.Default.PinDrop
        "Completed" -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }

    if (showPartialPaymentDialog) {
        AlertDialog(
            onDismissRequest = { showPartialPaymentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Payment (${booking.bookingIdString})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Customer: ${booking.customerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Freight: ${formatCurrency(booking.freightAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customReceivedAmountText,
                        onValueChange = { customReceivedAmountText = it },
                        label = { Text("Amount Received") },
                        placeholder = { Text("e.g. 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("partial_payment_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = customReceivedAmountText.toDoubleOrNull() ?: 0.0
                        val status = if (amount >= booking.freightAmount) "Paid" else if (amount > 0) "Partial" else "Pending"
                        onUpdatePaymentStatus(booking, status, amount)
                        showPartialPaymentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPartialPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("booking_item_${booking.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Booking ID, Vehicle & Prominent Status Pill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = booking.bookingIdString,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (booking.vehicleType.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = booking.vehicleType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "Booked: ${onDateFormatted(booking.bookingDate)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = booking.bookingStatus,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = booking.bookingStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip Status Stepper Indicator
            TripStatusStepper(currentStatus = booking.bookingStatus)

            Spacer(modifier = Modifier.height(12.dp))

            // Route representation: Pickup -> Drop
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ORIGIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PinDrop, contentDescription = "Pickup", tint = BluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(booking.pickupLocation, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "To",
                        tint = statusColor,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(18.dp)
                    )

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("DESTINATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PinDrop, contentDescription = "Drop", tint = StatusPending, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(booking.dropLocation, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer & Driver/Vehicle Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Business, contentDescription = "Customer", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(booking.customerName, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Truck", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${booking.driverName} (${booking.vehicleNumber.ifBlank { "Unassigned" }})",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Click-to-Edit Payment Status Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPaymentDropdown = true }
                    .testTag("booking_payment_status_${booking.id}"),
                color = when (booking.paymentStatus) {
                    "Paid" -> Color(0xFFE8F5E9)
                    "Partial" -> Color(0xFFFFF3E0)
                    else -> Color(0xFFFFEBEE)
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    when (booking.paymentStatus) {
                        "Paid" -> Color(0xFF4CAF50)
                        "Partial" -> Color(0xFFFF9800)
                        else -> Color(0xFFEF5350)
                    }
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (booking.paymentStatus) {
                                    "Paid" -> Icons.Default.CheckCircle
                                    "Partial" -> Icons.Default.Payments
                                    else -> Icons.Default.PendingActions
                                },
                                contentDescription = "Payment Status",
                                tint = when (booking.paymentStatus) {
                                    "Paid" -> Color(0xFF2E7D32)
                                    "Partial" -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Payment: ${booking.paymentStatus}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (booking.paymentStatus) {
                                    "Paid" -> Color(0xFF2E7D32)
                                    "Partial" -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                }
                            )
                            if (booking.amountReceived > 0) {
                                Text(
                                    text = " (${formatCurrency(booking.amountReceived)} Rec'd)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Edit Payment Status",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showPaymentDropdown,
                        onDismissRequest = { showPaymentDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Paid (Full)", fontWeight = FontWeight.Bold)
                                        Text("Rec'd ${formatCurrency(booking.freightAmount)}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            },
                            onClick = {
                                showPaymentDropdown = false
                                onUpdatePaymentStatus(booking, "Paid", booking.freightAmount)
                            },
                            modifier = Modifier.testTag("set_payment_paid_${booking.id}")
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Partial Payment", fontWeight = FontWeight.Bold)
                                        Text("Enter custom amount received", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            },
                            onClick = {
                                showPaymentDropdown = false
                                showPartialPaymentDialog = true
                            },
                            modifier = Modifier.testTag("set_payment_partial_${booking.id}")
                        )

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PendingActions, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Pending (Unpaid)", fontWeight = FontWeight.Bold)
                                        Text("0 amount received", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            },
                            onClick = {
                                showPaymentDropdown = false
                                onUpdatePaymentStatus(booking, "Pending", 0.0)
                            },
                            modifier = Modifier.testTag("set_payment_pending_${booking.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Freight", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.freightAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Advance Paid", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.driverAdvance), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BlueDark)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Balance", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        formatCurrency(booking.balanceAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (booking.balanceAmount > 0) StatusPending else StatusPaid
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Actions: Quick Advance Status & Documents POD Shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val nextStatus = when (booking.bookingStatus) {
                    "Booked" -> "Loading"
                    "Loading" -> "In Transit"
                    "In Transit" -> "Delivered"
                    "Delivered" -> "Completed"
                    else -> null
                }

                if (nextStatus != null) {
                    Button(
                        onClick = { onUpdateStatus(booking, nextStatus) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DoubleArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Advance to $nextStatus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { onNavigateToDocuments(booking.id) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = "POD", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "POD / Docs", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormDialog(
    customers: List<Customer>,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onSave: (Booking) -> Unit
) {
    // Generate unique booking ID
    val autoGeneratedId = "SL-${System.currentTimeMillis() % 100000}"

    // Wizard Navigation State
    var currentStep by remember { mutableStateOf(1) }

    // Step 1 States: Client & Route
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var pickup by remember { mutableStateOf("") }
    var pickupDropdownExpanded by remember { mutableStateOf(false) }

    var drop by remember { mutableStateOf("") }
    var dropDropdownExpanded by remember { mutableStateOf(false) }

    // Step 2 States: Fleet & Crew
    var selectedDriver by remember { mutableStateOf<Driver?>(null) }
    var driverDropdownExpanded by remember { mutableStateOf(false) }

    var vehicleType by remember { mutableStateOf("") }
    var vehicleTypeDropdownExpanded by remember { mutableStateOf(false) }

    var vehicleNumber by remember { mutableStateOf("") }
    var driverMobile by remember { mutableStateOf("") }

    // Step 3 States: Billing & Payment Tracking
    var freightAmountStr by remember { mutableStateOf("") }
    var amountReceivedStr by remember { mutableStateOf("") }
    var driverAdvanceStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var paymentModeDropdownExpanded by remember { mutableStateOf(false) }
    var billingTerm by remember { mutableStateOf("Paid") }
    
    var dueDaysOffset by remember { mutableStateOf(3) } // Default 3 days
    var notes by remember { mutableStateOf("") }

    val majorHubs = listOf(
        "Delhi NCR", "Mumbai, MH", "Kolkata, WB", "Chennai, TN", "Bangalore, KA",
        "Hyderabad, TS", "Pune, MH", "Ahmedabad, GJ", "Patna, BR", "Lucknow, UP",
        "Muzaffarpur, BR", "Gaya, BR", "Varanasi, UP", "Kanpur, UP", "Noida, UP",
        "Gurugram, HR", "Ghaziabad, UP", "Ranchi, JH", "Jamshedpur, JH", "Bhagalpur, BR",
        "Darbhanga, BR", "Katihar, BR", "Purnia, BR", "Jaipur, RJ", "Indore, MP"
    )

    val standardVehicles = listOf(
        "TATA Ace (1.5 Tons)",
        "Mahindra Bolero Pickup (2 Tons)",
        "Eicher Pro 3015 (5 Tons)",
        "10 Wheeler Truck (15 Tons)",
        "12 Wheeler Truck (21 Tons)",
        "14-Wheeler Taurus (25 Tons)",
        "20ft Container (7.5 Tons)",
        "32ft Single Axle Container (15 Tons)",
        "32ft Multi Axle Container (20 Tons)"
    )

    val paymentModes = listOf("UPI", "Bank Transfer", "Cash", "Cheque")
    val billingTerms = listOf("Paid", "To Pay (Delivery)", "TBB (To Be Billed)")

    // Real-time calculations
    val freight = freightAmountStr.toDoubleOrNull() ?: 0.0
    val customerPaid = amountReceivedStr.toDoubleOrNull() ?: 0.0
    val advance = driverAdvanceStr.toDoubleOrNull() ?: 0.0
    val customerBalance = freight - customerPaid

    val calculatedDueDate = remember(dueDaysOffset) {
        System.currentTimeMillis() + (dueDaysOffset * 24 * 60 * 60 * 1000L)
    }

    // Vehicle Number Validation
    val vehicleNumberRegex = Regex("^[A-Z]{2}[- ]?\\d{2}[- ]?[A-Z]{1,2}[- ]?\\d{4}$", RegexOption.IGNORE_CASE)
    val isVehicleNumberValid = vehicleNumber.isBlank() || vehicleNumberRegex.matches(vehicleNumber.trim())

    // Simulated Route Distance
    val simulatedRoute = remember(pickup, drop) {
        if (pickup.isNotBlank() && drop.isNotBlank() && pickup != drop) {
            val hash = (pickup.hashCode() + drop.hashCode()).let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
            val distance = (hash % 1200) + 150 // 150km to 1350km
            val toll = (distance * 1.8).toInt() // ₹1.8 per km toll
            distance to toll
        } else {
            null
        }
    }

    // Step Validation
    val isStep1Valid = selectedCustomer != null && pickup.isNotBlank() && drop.isNotBlank()
    val isStep2Valid = isVehicleNumberValid
    val isStep3Valid = freight > 0.0 && customerPaid >= 0.0 && advance >= 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Title
                item {
                    Column {
                        Text(
                            text = "New Shipment Booking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = "Booking ID: $autoGeneratedId",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Step Progress Indicator
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Route", "Fleet", "Billing").forEachIndexed { index, stepName ->
                            val stepNum = index + 1
                            val isActive = currentStep >= stepNum
                            val isCurrent = currentStep == stepNum
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) BluePrimary else Color.LightGray.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stepNum.toString(),
                                        color = if (isActive) Color.White else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stepName,
                                    fontSize = 11.sp,
                                    color = if (isCurrent) BluePrimary else if (isActive) Color.Black else Color.Gray,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (index < 2) {
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .weight(0.5f)
                                        .background(if (currentStep > stepNum) BluePrimary else Color.LightGray.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                }

                // ================== STEP 1: ROUTE & CLIENT ==================
                if (currentStep == 1) {
                    item {
                        Text(
                            text = "1. Customer & Route Configuration",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueDark
                        )
                    }

                    // Customer Selection Dropdown
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { customerDropdownExpanded = true }
                                .testTag("booking_form_customer_dropdown")
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.companyName ?: "Select Customer *",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Customer Client") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false },
                                modifier = Modifier.width(280.dp)
                            ) {
                                OutlinedTextField(
                                    value = customerSearchQuery,
                                    onValueChange = { customerSearchQuery = it },
                                    placeholder = { Text("Search Customer...") },
                                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                                
                                val filteredCustomers = remember(customers, customerSearchQuery) {
                                    if (customerSearchQuery.isBlank()) {
                                        customers
                                    } else {
                                        customers.filter { 
                                            it.name.contains(customerSearchQuery, ignoreCase = true) || 
                                            it.companyName.contains(customerSearchQuery, ignoreCase = true) 
                                        }
                                    }
                                }

                                if (filteredCustomers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No customers found", color = Color.Gray, fontSize = 12.sp) },
                                        onClick = {}
                                    )
                                } else {
                                    filteredCustomers.forEach { customer ->
                                        DropdownMenuItem(
                                            text = { Text("${customer.name} - ${customer.companyName}") },
                                            onClick = {
                                                selectedCustomer = customer
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pickup Location with Auto-suggestions
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = pickup,
                                onValueChange = { 
                                    pickup = it
                                    pickupDropdownExpanded = true
                                },
                                label = { Text("Pickup Location *") },
                                leadingIcon = { Icon(Icons.Default.PinDrop, null, tint = BluePrimary) },
                                trailingIcon = {
                                    if (pickup.isNotEmpty()) {
                                        IconButton(onClick = { pickup = "" }) {
                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("booking_form_pickup")
                            )
                            
                            val filteredPickupHubs = remember(pickup) {
                                if (pickup.isBlank()) {
                                    majorHubs
                                } else {
                                    majorHubs.filter { it.contains(pickup, ignoreCase = true) }
                                }
                            }
                            
                            if (pickupDropdownExpanded && filteredPickupHubs.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = pickupDropdownExpanded,
                                    onDismissRequest = { pickupDropdownExpanded = false }
                                ) {
                                    filteredPickupHubs.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city, fontSize = 13.sp) },
                                            onClick = {
                                                pickup = city
                                                pickupDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Drop Location with Auto-suggestions
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = drop,
                                onValueChange = { 
                                    drop = it
                                    dropDropdownExpanded = true
                                },
                                label = { Text("Drop Location *") },
                                leadingIcon = { Icon(Icons.Default.PinDrop, null, tint = StatusPending) },
                                trailingIcon = {
                                    if (drop.isNotEmpty()) {
                                        IconButton(onClick = { drop = "" }) {
                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("booking_form_drop")
                            )
                            
                            val filteredDropHubs = remember(drop) {
                                if (drop.isBlank()) {
                                    majorHubs
                                } else {
                                    majorHubs.filter { it.contains(drop, ignoreCase = true) }
                                }
                            }
                            
                            if (dropDropdownExpanded && filteredDropHubs.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = dropDropdownExpanded,
                                    onDismissRequest = { dropDropdownExpanded = false }
                                ) {
                                    filteredDropHubs.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city, fontSize = 13.sp) },
                                            onClick = {
                                                drop = city
                                                dropDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Simulated Route Insights
                    item {
                        simulatedRoute?.let { (distance, toll) ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BlueAccent.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Route Info", tint = BluePrimary, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Estimated Distance: ~$distance km | Est. Road Tolls: ₹$toll",
                                        fontSize = 11.sp,
                                        color = BlueDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // ================== STEP 2: FLEET & DRIVER ==================
                if (currentStep == 2) {
                    item {
                        Text(
                            text = "2. Fleet Assignment & Driver Details",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueDark
                        )
                    }

                    // Driver Selection
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { driverDropdownExpanded = true }
                                .testTag("booking_form_driver_dropdown")
                        ) {
                            OutlinedTextField(
                                value = selectedDriver?.name ?: "Select Driver (Optional)",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Assigned Driver") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = driverDropdownExpanded,
                                onDismissRequest = { driverDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Unassigned / Spot Market Vehicle", fontWeight = FontWeight.Bold, color = Color.Gray) },
                                    onClick = {
                                        selectedDriver = null
                                        driverDropdownExpanded = false
                                    }
                                )
                                drivers.forEach { driver ->
                                    DropdownMenuItem(
                                        text = { Text("${driver.name} (${driver.vehicleNumber})") },
                                        onClick = {
                                            selectedDriver = driver
                                            vehicleNumber = driver.vehicleNumber
                                            driverMobile = driver.phone
                                            driverDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Vehicle Type Selection Dropdown
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = vehicleType,
                                onValueChange = { 
                                    vehicleType = it
                                    vehicleTypeDropdownExpanded = true
                                },
                                label = { Text("Vehicle Configuration / Type *") },
                                leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = Color.Gray) },
                                trailingIcon = {
                                    IconButton(onClick = { vehicleTypeDropdownExpanded = !vehicleTypeDropdownExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            DropdownMenu(
                                expanded = vehicleTypeDropdownExpanded,
                                onDismissRequest = { vehicleTypeDropdownExpanded = false }
                            ) {
                                standardVehicles.forEach { vehicle ->
                                    DropdownMenuItem(
                                        text = { Text(vehicle) },
                                        onClick = {
                                            vehicleType = vehicle
                                            vehicleTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Custom Vehicle Plate Input (Validated)
                    item {
                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = { vehicleNumber = it.uppercase() },
                            label = { Text("Vehicle Registration Plate Number") },
                            placeholder = { Text("e.g. MH-12-AB-1234") },
                            isError = !isVehicleNumberValid,
                            supportingText = {
                                if (!isVehicleNumberValid) {
                                    Text("Suggested format: MH-12-AB-1234 or MH12AB1234", color = StatusPending)
                                } else {
                                    Text("Official Registration Number")
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, null, tint = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Driver Mobile Number
                    item {
                        OutlinedTextField(
                            value = driverMobile,
                            onValueChange = { driverMobile = it },
                            label = { Text("Driver Mobile / Phone Number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ================== STEP 3: FINANCIALS & BILLING ==================
                if (currentStep == 3) {
                    item {
                        Text(
                            text = "3. Billing Terms & Financial Auditing",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueDark
                        )
                    }

                    // Freight Amount
                    item {
                        OutlinedTextField(
                            value = freightAmountStr,
                            onValueChange = { freightAmountStr = it },
                            label = { Text("Agreed Freight Amount * (Receivable)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = StatusPaid) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("booking_form_freight")
                        )
                    }

                    // Customer Terms Quick chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment / Contract Terms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                billingTerms.forEach { term ->
                                    val isSelected = billingTerm == term
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            billingTerm = term
                                            if (term == "Paid") {
                                                amountReceivedStr = freightAmountStr
                                            } else {
                                                amountReceivedStr = "0"
                                            }
                                        },
                                        label = { Text(term, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BluePrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Cash Received and Driver Advance Input fields
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountReceivedStr,
                                onValueChange = { amountReceivedStr = it },
                                label = { Text("Customer Paid Upfront") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_form_received")
                            )

                            OutlinedTextField(
                                value = driverAdvanceStr,
                                onValueChange = { driverAdvanceStr = it },
                                label = { Text("Driver Dispatch Advance") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_form_driver_advance")
                            )
                        }
                    }

                    // Payment Mode dropdown
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { paymentModeDropdownExpanded = true }
                        ) {
                            OutlinedTextField(
                                value = paymentMode,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Payment / Cash Mode") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = paymentModeDropdownExpanded,
                                onDismissRequest = { paymentModeDropdownExpanded = false }
                            ) {
                                paymentModes.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode) },
                                        onClick = {
                                            paymentMode = mode
                                            paymentModeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Due Date selection Terms (Slider)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Payment Due Term Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(0 to "Today", 3 to "3 Days", 7 to "7 Days", 15 to "15 Days").forEach { (days, label) ->
                                    val isSelected = dueDaysOffset == days
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { dueDaysOffset = days },
                                        label = { Text(label, fontSize = 10.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BluePrimary,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text("Terms Slider: ", fontSize = 11.sp, color = Color.Gray)
                                Slider(
                                    value = dueDaysOffset.toFloat(),
                                    onValueChange = { dueDaysOffset = it.toInt() },
                                    valueRange = 0f..60f,
                                    steps = 60,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$dueDaysOffset Days",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.width(55.dp)
                                )
                            }
                        }
                    }

                    // Dynamic Ledger Calculation Card (Audit Summary)
                    item {
                        val statusText = when {
                            customerPaid >= freight && freight > 0.0 -> "PAID"
                            customerPaid > 0.0 && freight > 0.0 -> "PARTIAL"
                            else -> "PENDING"
                        }
                        val statusColor = when (statusText) {
                            "PAID" -> StatusPaid
                            "PARTIAL" -> StatusPartial
                            else -> StatusPending
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ledger Audit Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueDark)
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Freight Bill", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(freight), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Upfront Collected", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(customerPaid), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusPaid)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Customer Outstanding", fontSize = 10.sp, color = Color.Gray)
                                        Text(formatCurrency(customerBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (customerBalance > 0) StatusPending else StatusPaid)
                                    }
                                }
                                
                                if (advance > 0.0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.LocalShipping, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                            Text("Driver Dispatch Advance:", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(formatCurrency(advance), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BlueDark)
                                    }
                                }
                            }
                        }
                    }

                    // Special Instructions / Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Special Dispatch Notes / Route Instructions") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    }
                }

                // ================== FOOTER NAVIGATION BAR ==================
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_form_back_button")
                            ) {
                                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        } else {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        if (currentStep < 3) {
                            val isNextEnabled = when (currentStep) {
                                1 -> isStep1Valid
                                2 -> isStep2Valid
                                else -> true
                            }
                            Button(
                                onClick = { currentStep++ },
                                enabled = isNextEnabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_form_next_button")
                            ) {
                                Text("Next")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            val statusText = when {
                                customerPaid >= freight && freight > 0.0 -> "Paid"
                                customerPaid > 0.0 && freight > 0.0 -> "Partial"
                                else -> "Pending"
                            }
                            Button(
                                onClick = {
                                    val customer = selectedCustomer
                                    if (customer != null && isStep3Valid && isStep1Valid && isStep2Valid) {
                                        onSave(
                                            Booking(
                                                bookingIdString = autoGeneratedId,
                                                customerId = customer.id,
                                                customerName = customer.companyName,
                                                pickupLocation = pickup,
                                                dropLocation = drop,
                                                vehicleType = vehicleType,
                                                driverId = selectedDriver?.id ?: 0,
                                                driverName = selectedDriver?.name ?: "Unassigned",
                                                driverMobile = driverMobile,
                                                vehicleNumber = vehicleNumber,
                                                freightAmount = freight,
                                                driverAdvance = advance,
                                                balanceAmount = customerBalance,
                                                paymentStatus = statusText,
                                                bookingStatus = "Booked",
                                                dueDate = calculatedDueDate,
                                                amountReceived = customerPaid,
                                                notes = notes
                                            )
                                        )
                                    }
                                },
                                enabled = isStep3Valid && isStep1Valid && isStep2Valid,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("booking_form_save_button")
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Book Shipment")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailDialog(
    booking: Booking,
    customer: Customer?,
    onDismiss: () -> Unit,
    onUpdate: (Booking) -> Unit,
    onDelete: () -> Unit,
    onNavigateToDocuments: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var status by remember { mutableStateOf(booking.bookingStatus) }
    var notes by remember { mutableStateOf(booking.notes) }

    val statusesList = listOf("Booked", "Loading", "In Transit", "Delivered", "Completed")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = booking.bookingIdString,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = booking.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Cancel Booking", tint = StatusPending)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Current Route Details
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FROM", fontSize = 10.sp, color = Color.Gray)
                        Text(booking.pickupLocation, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TO", fontSize = 10.sp, color = Color.Gray)
                        Text(booking.dropLocation, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Change status chips
                Column {
                    Text("Update Shipment Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusesList.forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Driver allocation info
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Driver & Transport Info", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueDark)
                    Text("Driver Name: ${booking.driverName}", fontSize = 13.sp)
                    Text("Vehicle: ${booking.vehicleNumber}", fontSize = 13.sp)
                    Text("Freight Bill: ${formatCurrency(booking.freightAmount)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Document Access Link
                OutlinedButton(
                    onClick = {
                        onNavigateToDocuments(booking.id)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Documents")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage Shipment Documents (LR, POD, RC)")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Professional Invoice Action Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Professional Invoice (PDF)", 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = BlueDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = com.example.util.PdfInvoiceGenerator.generateInvoicePdf(
                                    context = context,
                                    booking = booking,
                                    customer = customer
                                )
                                com.example.util.PdfInvoiceGenerator.viewPdf(context, file)
                            },
                            modifier = Modifier.weight(1f).testTag("view_invoice_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "View Invoice",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = {
                                val file = com.example.util.PdfInvoiceGenerator.generateInvoicePdf(
                                    context = context,
                                    booking = booking,
                                    customer = customer
                                )
                                com.example.util.PdfInvoiceGenerator.sharePdf(context, file)
                            },
                            modifier = Modifier.weight(1f).testTag("share_invoice_pdf_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Invoice",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onUpdate(
                                booking.copy(
                                    bookingStatus = status,
                                    notes = notes
                                )
                            )
                        }
                    ) {
                        Text("Save Status")
                    }
                }
            }
        }
    }
}
