package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.models.PaymentTransaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel

@Composable
fun PaymentScreen(
    viewModel: LogisticsViewModel
) {
    val bookings by viewModel.bookings.collectAsState()
    val paymentTransactions by viewModel.paymentTransactions.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Customer Payments, 1 = Driver Payments, 2 = Receipts Ledger
    var selectedBookingForCustomerReceipt by remember { mutableStateOf<Booking?>(null) }
    var selectedBookingForDriverReceipt by remember { mutableStateOf<Booking?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Headers
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BluePrimary
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Customer Receipts", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Customer") }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Driver Settlements", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Driver") }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Receipts Ledger", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = "Ledger") }
            )
        }

        if (bookings.isEmpty() && activeTab != 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings available for payment tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("payments_scroll"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTab == 0) {
                    items(bookings) { booking ->
                        CustomerPaymentItem(
                            booking = booking,
                            onFormatDate = { viewModel.formatDate(it) },
                            onReceiveClick = { selectedBookingForCustomerReceipt = booking }
                        )
                    }
                } else if (activeTab == 1) {
                    items(bookings) { booking ->
                        DriverPaymentItem(
                            booking = booking,
                            onFormatDate = { viewModel.formatDate(it) },
                            onSettleClick = { selectedBookingForDriverReceipt = booking }
                        )
                    }
                } else {
                    if (paymentTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No customer receipts logged in the ledger yet.",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(paymentTransactions) { transaction ->
                            LedgerPaymentItem(
                                transaction = transaction,
                                onFormatDate = { viewModel.formatDate(it) },
                                onDeleteClick = {
                                    // Reverse payment transaction!
                                    val linkedBooking = bookings.find { it.id == transaction.bookingId }
                                    if (linkedBooking != null) {
                                        val newReceived = (linkedBooking.amountReceived - transaction.amountPaid).coerceAtLeast(0.0)
                                        val remainingBalance = linkedBooking.freightAmount - newReceived
                                        val newStatus = when {
                                            remainingBalance <= 0 -> "Paid"
                                            newReceived > 0 -> "Partial"
                                            else -> "Pending"
                                        }
                                        viewModel.updateBooking(
                                            linkedBooking.copy(
                                                amountReceived = newReceived,
                                                balanceAmount = remainingBalance,
                                                paymentStatus = newStatus
                                            )
                                        )
                                    }
                                    viewModel.deletePaymentTransaction(transaction)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Receive Customer Payment Dialog
        selectedBookingForCustomerReceipt?.let { booking ->
            CustomerPaymentReceiptDialog(
                booking = booking,
                onDismiss = { selectedBookingForCustomerReceipt = null },
                onSave = { receivedAmount, mode, refNum, notes ->
                    val totalReceived = booking.amountReceived + receivedAmount
                    val remainingBalance = booking.freightAmount - totalReceived
                    val status = when {
                        remainingBalance <= 0 -> "Paid"
                        totalReceived > 0 -> "Partial"
                        else -> "Pending"
                    }
                    viewModel.updateBooking(
                        booking.copy(
                            amountReceived = totalReceived,
                            balanceAmount = if (remainingBalance > 0) remainingBalance else 0.0,
                            paymentStatus = status
                        )
                    )
                    viewModel.addPaymentTransaction(
                        PaymentTransaction(
                            bookingId = booking.id,
                            bookingIdString = booking.bookingIdString,
                            customerId = booking.customerId,
                            customerName = booking.customerName,
                            amountPaid = receivedAmount,
                            paymentMode = mode,
                            referenceNumber = refNum,
                            notes = notes
                        )
                    )
                    selectedBookingForCustomerReceipt = null
                }
            )
        }

        // Driver Settle Payment Dialog
        selectedBookingForDriverReceipt?.let { booking ->
            DriverSettlementDialog(
                booking = booking,
                onDismiss = { selectedBookingForDriverReceipt = null },
                onSave = { diesel, toll, food, labour, finalPay ->
                    val totalDriverPaid = booking.driverAdvance + diesel + toll + food + labour + finalPay
                    val newRemaining = booking.driverRemainingBalance - finalPay
                    viewModel.updateBooking(
                        booking.copy(
                            driverDiesel = booking.driverDiesel + diesel,
                            driverToll = booking.driverToll + toll,
                            driverFood = booking.driverFood + food,
                            driverLabour = booking.driverLabour + labour,
                            driverFinalPayment = booking.driverFinalPayment + finalPay,
                            driverRemainingBalance = if (newRemaining > 0) newRemaining else 0.0
                        )
                    )
                    selectedBookingForDriverReceipt = null
                }
            )
        }
    }
}

@Composable
fun CustomerPaymentItem(
    booking: Booking,
    onFormatDate: (Long) -> String,
    onReceiveClick: () -> Unit
) {
    val statusColor = when (booking.paymentStatus) {
        "Paid" -> StatusPaid
        "Partial" -> StatusPartial
        else -> StatusPending
    }

    val statusText = when (booking.paymentStatus) {
        "Paid" -> "🟢 Paid"
        "Partial" -> "🟡 Partial"
        else -> "🔴 Pending"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.bookingIdString, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Text(booking.customerName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route & Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.pickupLocation} ➔ ${booking.dropLocation}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )

                Text(
                    text = "Due: ${onFormatDate(booking.dueDate)}",
                    fontSize = 12.sp,
                    color = if (System.currentTimeMillis() >= booking.dueDate && booking.paymentStatus != "Paid") StatusPending else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Bill", fontSize = 11.sp, color = Color.Gray)
                    Text(formatCurrency(booking.freightAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Received", fontSize = 11.sp, color = Color.Gray)
                    Text(formatCurrency(booking.amountReceived), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusPaid)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Balance", fontSize = 11.sp, color = Color.Gray)
                    Text(formatCurrency(booking.balanceAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (booking.balanceAmount > 0) StatusPending else StatusPaid)
                }
            }

            if (booking.balanceAmount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onReceiveClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = "Receive")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Customer Payment")
                }
            }
        }
    }
}

@Composable
fun DriverPaymentItem(
    booking: Booking,
    onFormatDate: (Long) -> String,
    onSettleClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.bookingIdString, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Text("Driver: ${booking.driverName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueAccent)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Truck: ${booking.vehicleNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid Layout of Driver components
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Advance Paid", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.driverAdvance), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Diesel Paid", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.driverDiesel), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Tolls Paid", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.driverToll), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Oustand Balance", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.driverRemainingBalance), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (booking.driverRemainingBalance > 0) StatusPending else StatusPaid)
                }
            }

            if (booking.driverRemainingBalance > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onSettleClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlueDark)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Settle")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Driver Payments / Settlement")
                }
            }
        }
    }
}

@Composable
fun CustomerPaymentReceiptDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var refNum by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Record Customer Payment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Customer: ${booking.customerName}\n" +
                            "Booking ID: ${booking.bookingIdString}\n" +
                            "Remaining Balance: ${formatCurrency(booking.balanceAmount)}",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Payment Received Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                Text(
                    text = "Payment Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "Bank Transfer", "Cash", "Cheque").forEach { mode ->
                        val isSelected = paymentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { paymentMode = mode }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = refNum,
                    onValueChange = { refNum = it },
                    label = { Text("Reference / Trans ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_ref_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Accounting Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_notes_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                onSave(amount, paymentMode, refNum, notes)
                            }
                        },
                        enabled = amountStr.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Record Payment")
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerPaymentItem(
    transaction: PaymentTransaction,
    onFormatDate: (Long) -> String,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Linked to: ${transaction.bookingIdString}",
                        fontSize = 12.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(transaction.amountPaid),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StatusPaid
                    )
                    Text(
                        text = onFormatDate(transaction.paymentDate),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlueAccent.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = transaction.paymentMode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }

                    if (transaction.referenceNumber.isNotBlank()) {
                        Text(
                            text = "Ref: ${transaction.referenceNumber}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Reverse/Delete Transaction",
                        tint = StatusPending,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${transaction.notes}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DriverSettlementDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double) -> Unit
) {
    var dieselStr by remember { mutableStateOf("") }
    var tollStr by remember { mutableStateOf("") }
    var foodStr by remember { mutableStateOf("") }
    var labourStr by remember { mutableStateOf("") }
    var finalPayStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Driver Payment Tracker",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Driver: ${booking.driverName} (${booking.vehicleNumber})\n" +
                                "Pending Settlements: ${formatCurrency(booking.driverRemainingBalance)}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                item {
                    OutlinedTextField(
                        value = dieselStr,
                        onValueChange = { dieselStr = it },
                        label = { Text("Diesel Expense (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = tollStr,
                        onValueChange = { tollStr = it },
                        label = { Text("Toll Charges (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = foodStr,
                        onValueChange = { foodStr = it },
                        label = { Text("Food Expense (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = labourStr,
                        onValueChange = { labourStr = it },
                        label = { Text("Labour Charges (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finalPayStr,
                        onValueChange = { finalPayStr = it },
                        label = { Text("Final Payout / Cash Record (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val diesel = dieselStr.toDoubleOrNull() ?: 0.0
                                val toll = tollStr.toDoubleOrNull() ?: 0.0
                                val food = foodStr.toDoubleOrNull() ?: 0.0
                                val labour = labourStr.toDoubleOrNull() ?: 0.0
                                val finalPay = finalPayStr.toDoubleOrNull() ?: 0.0

                                onSave(diesel, toll, food, labour, finalPay)
                            }
                        ) {
                            Text("Save Payments")
                        }
                    }
                }
            }
        }
    }
}
