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
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel

@Composable
fun ExpenseScreen(
    viewModel: LogisticsViewModel
) {
    val bookings by viewModel.bookings.collectAsState()

    var selectedBookingForExpenseEdit by remember { mutableStateOf<Booking?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = BluePrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💸 Direct Trip Expense Tracker",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Track trip fuel, labour, tolls, and loading costs to calculate true net margins.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        if (bookings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings available for expense tracking.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("expense_scroll"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    var selectedBookingForLink by remember { mutableStateOf<Booking?>(null) }
                    var selectedCategoryForLink by remember { mutableStateOf("Diesel Fuel") }
                    var linkAmountStr by remember { mutableStateOf("") }
                    var linkNotes by remember { mutableStateOf("") }
                    var isBookingDropdownExpanded by remember { mutableStateOf(false) }
                    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔗 Link Cost to specific Booking ID",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Booking Selection Dropdown
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isBookingDropdownExpanded = true }
                            ) {
                                OutlinedTextField(
                                    value = selectedBookingForLink?.let { "${it.bookingIdString} (${it.customerName})" } ?: "Select Booking ID...",
                                    onValueChange = {},
                                    enabled = false,
                                    label = { Text("Select Booking") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown"
                                        )
                                    },
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
                                    expanded = isBookingDropdownExpanded,
                                    onDismissRequest = { isBookingDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    bookings.forEach { b ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${b.bookingIdString} - ${b.customerName} (${b.pickupLocation} ➔ ${b.dropLocation})",
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                selectedBookingForLink = b
                                                isBookingDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Category Selection Dropdown
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { isCategoryDropdownExpanded = true }
                                ) {
                                    OutlinedTextField(
                                        value = selectedCategoryForLink,
                                        onValueChange = {},
                                        enabled = false,
                                        label = { Text("Cost Category") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown"
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val categories = listOf("Diesel Fuel", "Tolls Paid", "Labour Costs", "Food & Catering", "Other Expenses")
                                    DropdownMenu(
                                        expanded = isCategoryDropdownExpanded,
                                        onDismissRequest = { isCategoryDropdownExpanded = false }
                                    ) {
                                        categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat, fontSize = 13.sp) },
                                                onClick = {
                                                    selectedCategoryForLink = cat
                                                    isCategoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Amount Input Field
                                OutlinedTextField(
                                    value = linkAmountStr,
                                    onValueChange = { linkAmountStr = it },
                                    label = { Text("Amount (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Notes Input Field
                            OutlinedTextField(
                                value = linkNotes,
                                onValueChange = { linkNotes = it },
                                label = { Text("Expense Notes (optional)") },
                                placeholder = { Text("e.g. Fuel receipt ref, Toll plaza, driver meal") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Save Link Button
                            Button(
                                onClick = {
                                    val booking = selectedBookingForLink
                                    if (booking == null) {
                                        return@Button
                                    }
                                    val amount = linkAmountStr.toDoubleOrNull() ?: 0.0
                                    if (amount <= 0.0) {
                                        return@Button
                                    }

                                    val updatedNotes = if (linkNotes.isNotBlank()) {
                                        if (booking.notes.isBlank()) "Linked $selectedCategoryForLink: ₹$amount ($linkNotes)" else "${booking.notes}\nLinked $selectedCategoryForLink: ₹$amount ($linkNotes)"
                                    } else {
                                        booking.notes
                                    }

                                    val updatedBooking = when (selectedCategoryForLink) {
                                        "Diesel Fuel" -> booking.copy(dieselExpense = booking.dieselExpense + amount, notes = updatedNotes)
                                        "Tolls Paid" -> booking.copy(tollExpense = booking.tollExpense + amount, notes = updatedNotes)
                                        "Labour Costs" -> booking.copy(labourExpense = booking.labourExpense + amount, notes = updatedNotes)
                                        "Food & Catering" -> booking.copy(foodExpense = booking.foodExpense + amount, notes = updatedNotes)
                                        else -> booking.copy(otherExpenses = booking.otherExpenses + amount, notes = updatedNotes)
                                    }

                                    viewModel.updateBooking(updatedBooking)
                                    viewModel.addInAppNotification("Linked ₹${amount.toInt()} $selectedCategoryForLink to ${booking.bookingIdString}")
                                    
                                    // Reset Form
                                    selectedBookingForLink = null
                                    linkAmountStr = ""
                                    linkNotes = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("link_expense_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Link")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Link Expense to Booking ID", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(bookings) { booking ->
                    ExpenseItemCard(
                        booking = booking,
                        onClick = { selectedBookingForExpenseEdit = booking }
                    )
                }
            }
        }

        // Edit Expenses Dialog
        selectedBookingForExpenseEdit?.let { booking ->
            ExpenseEditDialog(
                booking = booking,
                onDismiss = { selectedBookingForExpenseEdit = null },
                onSave = { updatedBooking ->
                    viewModel.updateBooking(updatedBooking)
                    selectedBookingForExpenseEdit = null
                }
            )
        }
    }
}

@Composable
fun ExpenseItemCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val totalExpenses = booking.getTotalExpenses()
    val profit = booking.getProfit()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    Text("Customer: ${booking.customerName}", fontSize = 13.sp, color = Color.Gray)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueAccent)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${booking.pickupLocation.take(12)} ➔ ${booking.dropLocation.take(12)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-breakdown details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Diesel Fuel", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.dieselExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Tolls Paid", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.tollExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Labour Costs", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.labourExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Loading/Unload", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(booking.loadingChargesExpense + booking.unloadingChargesExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Calculated Outputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Expenses", fontSize = 10.sp, color = Color.Gray)
                    Text(formatCurrency(totalExpenses), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusPending)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Trip Profit", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = formatCurrency(profit),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (profit >= 0) StatusPaid else StatusPending
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseEditDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onSave: (Booking) -> Unit
) {
    var dieselStr by remember { mutableStateOf(booking.dieselExpense.toString()) }
    var tollStr by remember { mutableStateOf(booking.tollExpense.toString()) }
    var labourStr by remember { mutableStateOf(booking.labourExpense.toString()) }
    var foodStr by remember { mutableStateOf(booking.foodExpense.toString()) }
    var loadingChargesStr by remember { mutableStateOf(booking.loadingChargesExpense.toString()) }
    var unloadingChargesStr by remember { mutableStateOf(booking.unloadingChargesExpense.toString()) }
    var otherStr by remember { mutableStateOf(booking.otherExpenses.toString()) }

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
                        text = "Edit Direct Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Booking ID: ${booking.bookingIdString}\nFreight Revenue: ${formatCurrency(booking.freightAmount)}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                item {
                    OutlinedTextField(
                        value = dieselStr,
                        onValueChange = { dieselStr = it },
                        label = { Text("Diesel Fuel (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = tollStr,
                        onValueChange = { tollStr = it },
                        label = { Text("Toll Road Charges (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = labourStr,
                        onValueChange = { labourStr = it },
                        label = { Text("Driver / Crew Labour (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = foodStr,
                        onValueChange = { foodStr = it },
                        label = { Text("Food & Catering Expenses (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = loadingChargesStr,
                            onValueChange = { loadingChargesStr = it },
                            label = { Text("Loading Fees") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = unloadingChargesStr,
                            onValueChange = { unloadingChargesStr = it },
                            label = { Text("Unload Fees") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = otherStr,
                        onValueChange = { otherStr = it },
                        label = { Text("Other Expenses (₹)") },
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
                                onSave(
                                    booking.copy(
                                        dieselExpense = dieselStr.toDoubleOrNull() ?: 0.0,
                                        tollExpense = tollStr.toDoubleOrNull() ?: 0.0,
                                        labourExpense = labourStr.toDoubleOrNull() ?: 0.0,
                                        foodExpense = foodStr.toDoubleOrNull() ?: 0.0,
                                        loadingChargesExpense = loadingChargesStr.toDoubleOrNull() ?: 0.0,
                                        unloadingChargesExpense = unloadingChargesStr.toDoubleOrNull() ?: 0.0,
                                        otherExpenses = otherStr.toDoubleOrNull() ?: 0.0
                                    )
                                )
                            }
                        ) {
                            Text("Save Expenses")
                        }
                    }
                }
            }
        }
    }
}
