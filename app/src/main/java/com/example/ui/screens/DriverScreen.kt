package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.models.Driver
import com.example.data.models.Booking
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel

@Composable
fun DriverScreen(
    viewModel: LogisticsViewModel
) {
    val drivers by viewModel.drivers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDriverForDetail by remember { mutableStateOf<Driver?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredDrivers = drivers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.vehicleNumber.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search drivers by name, truck, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("driver_search"),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredDrivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No drivers found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDrivers) { driver ->
                        val driverBookings = bookings.filter { it.driverId == driver.id }
                        val totalTrips = driverBookings.size
                        val totalPaid = driverBookings.sumOf { it.driverAdvance + it.driverFinalPayment }
                        val totalPending = driverBookings.sumOf { it.driverRemainingBalance }

                        DriverItemCard(
                            driver = driver,
                            totalTrips = totalTrips,
                            totalPaid = totalPaid,
                            totalPending = totalPending,
                            onClick = { selectedDriverForDetail = driver }
                        )
                    }
                }
            }
        }

        // Add Driver FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_driver_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Driver")
        }

        // Add Driver Dialog
        if (showAddDialog) {
            DriverFormDialog(
                onDismiss = { showAddDialog = false },
                onSave = { driver ->
                    viewModel.addDriver(driver)
                    showAddDialog = false
                }
            )
        }

        // Driver Detail Dialog
        selectedDriverForDetail?.let { driver ->
            val driverBookings = bookings.filter { it.driverId == driver.id }
            val totalTrips = driverBookings.size
            val totalPaid = driverBookings.sumOf { it.driverAdvance + it.driverFinalPayment }
            val totalPending = driverBookings.sumOf { it.driverRemainingBalance }

            DriverDetailDialog(
                driver = driver,
                bookings = driverBookings,
                totalTrips = totalTrips,
                totalPaid = totalPaid,
                totalPending = totalPending,
                onFormatDate = { viewModel.formatDate(it) },
                onDismiss = { selectedDriverForDetail = null },
                onDelete = {
                    viewModel.deleteDriver(driver)
                    selectedDriverForDetail = null
                }
            )
        }
    }
}

@Composable
fun DriverItemCard(
    driver: Driver,
    totalTrips: Int,
    totalPaid: Double,
    totalPending: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("driver_item_${driver.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driver.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Truck No: ${driver.vehicleNumber}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (driver.vehicleStatus) {
                        "Available" -> StatusPaid
                        "In Transit" -> BluePrimary
                        "Maintenance" -> StatusPending
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = driver.vehicleStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueAccent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalTrips Trips",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Paid Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(totalPaid), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusPaid)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(totalPending), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (totalPending > 0) StatusPending else StatusPaid)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.1f)) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    val phoneText = if (driver.alternatePhone.isNotBlank()) {
                        "${driver.phone} / ${driver.alternatePhone}"
                    } else {
                        driver.phone
                    }
                    Text(
                        text = phoneText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.9f), horizontalArrangement = Arrangement.End) {
                    Icon(Icons.Default.Badge, contentDescription = "License", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    val licenseText = if (driver.licenseType.isNotBlank()) {
                        "${driver.licenseNumber.take(10)} (${driver.licenseType})"
                    } else {
                        driver.licenseNumber.take(12)
                    }
                    Text(
                        text = licenseText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DriverFormDialog(
    onDismiss: () -> Unit,
    onSave: (Driver) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var vehicleStatus by remember { mutableStateOf("Available") }
    var aadhaarNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var licenseType by remember { mutableStateOf("") }
    var licenseExpiryDate by remember { mutableStateOf("") }
    var bankDetails by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Register New Driver",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Personal Information
                    Text(
                        text = "Contact & Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Driver Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_name")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Primary Phone Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_phone")
                    )

                    OutlinedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        label = { Text("Alternate / Emergency Phone (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_alt_phone")
                    )

                    OutlinedTextField(
                        value = aadhaarNumber,
                        onValueChange = { aadhaarNumber = it },
                        label = { Text("Aadhaar UID Number (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_aadhaar")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // License Information
                    Text(
                        text = "Driving License Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it },
                        label = { Text("Driving License Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_license")
                    )

                    Column {
                        OutlinedTextField(
                            value = licenseType,
                            onValueChange = { licenseType = it },
                            label = { Text("License Class / Type (e.g. HMV, LMV)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("driver_form_lic_type")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HMV", "LMV", "MCWG").forEach { type ->
                                SuggestionChip(
                                    onClick = { licenseType = type },
                                    label = { Text(type, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = licenseExpiryDate,
                        onValueChange = { licenseExpiryDate = it },
                        label = { Text("License Expiry Date (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2030-12-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_lic_expiry")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Vehicle Assignment Status
                    Text(
                        text = "Assigned Vehicle & Duty Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    OutlinedTextField(
                        value = vehicleNumber,
                        onValueChange = { vehicleNumber = it },
                        label = { Text("Vehicle Plate Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("driver_form_vehicle")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Assigned Vehicle Status *",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Available", "In Transit", "Maintenance", "Off Duty").forEach { status ->
                                val isSelected = vehicleStatus == status
                                val statusColor = when (status) {
                                    "Available" -> StatusPaid
                                    "In Transit" -> BluePrimary
                                    "Maintenance" -> StatusPending
                                    else -> Color.Gray
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { vehicleStatus = status },
                                    label = { Text(status, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = statusColor.copy(alpha = 0.15f),
                                        selectedLabelColor = statusColor,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Bank Account details
                    Text(
                        text = "Payout Bank Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    OutlinedTextField(
                        value = bankDetails,
                        onValueChange = { bankDetails = it },
                        label = { Text("Bank Payment Details (A/C No, IFSC Code)") },
                        modifier = Modifier.fillMaxWidth().height(80.dp).testTag("driver_form_bank")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank() && vehicleNumber.isNotBlank() && licenseNumber.isNotBlank()) {
                                onSave(
                                    Driver(
                                        name = name,
                                        phone = phone,
                                        alternatePhone = alternatePhone,
                                        vehicleNumber = vehicleNumber.uppercase(),
                                        aadhaarNumber = aadhaarNumber,
                                        licenseNumber = licenseNumber.uppercase(),
                                        licenseType = licenseType,
                                        licenseExpiryDate = licenseExpiryDate,
                                        vehicleStatus = vehicleStatus,
                                        bankDetails = bankDetails
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank() && vehicleNumber.isNotBlank() && licenseNumber.isNotBlank()
                    ) {
                        Text("Register Driver")
                    }
                }
            }
        }
    }
}

@Composable
fun DriverDetailDialog(
    driver: Driver,
    bookings: List<Booking>,
    totalTrips: Int,
    totalPaid: Double,
    totalPending: Double,
    onFormatDate: (Long) -> String,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = driver.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Truck Plate: ${driver.vehicleNumber}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )

                            val statusColor = when (driver.vehicleStatus) {
                                "Available" -> StatusPaid
                                "In Transit" -> BluePrimary
                                "Maintenance" -> StatusPending
                                else -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = driver.vehicleStatus,
                                    color = statusColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Driver", tint = StatusPending)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Grid Stats
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileStatCard(label = "Total Trips", value = totalTrips.toString(), color = BluePrimary, modifier = Modifier.weight(1f))
                        ProfileStatCard(label = "Paid Advance/Final", value = formatCurrency(totalPaid), color = StatusPaid, modifier = Modifier.weight(1f))
                        ProfileStatCard(label = "Remaining Balance", value = formatCurrency(totalPending), color = StatusPending, modifier = Modifier.weight(1f))
                    }

                    // Contact & License Details
                    Text("Contact & Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileDetailRow(icon = Icons.Default.Phone, label = "Mobile", value = driver.phone)
                        if (driver.alternatePhone.isNotBlank()) {
                            ProfileDetailRow(icon = Icons.Default.Phone, label = "Alt Phone", value = driver.alternatePhone)
                        }
                        ProfileDetailRow(icon = Icons.Default.Badge, label = "License No", value = driver.licenseNumber)
                        if (driver.licenseType.isNotBlank()) {
                            ProfileDetailRow(icon = Icons.Default.Assignment, label = "License Type", value = driver.licenseType)
                        }
                        if (driver.licenseExpiryDate.isNotBlank()) {
                            ProfileDetailRow(icon = Icons.Default.Schedule, label = "License Expiry", value = driver.licenseExpiryDate)
                        }
                        ProfileDetailRow(icon = Icons.Default.CreditCard, label = "Aadhaar UID", value = driver.aadhaarNumber.ifBlank { "N/A" })
                        ProfileDetailRow(icon = Icons.Default.AccountBalance, label = "Bank Payout Account", value = driver.bankDetails.ifBlank { "No payment accounts stored." })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Historical Booking Records Section
                    Text("Assigned Trip Log History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    if (bookings.isEmpty()) {
                        Text(
                            text = "No assigned booking records found for this driver.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        bookings.forEach { booking ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = booking.bookingIdString,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = onFormatDate(booking.bookingDate),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PinDrop, contentDescription = "Route", tint = BluePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${booking.pickupLocation} to ${booking.dropLocation}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Total Driver Pay: ${formatCurrency(booking.getDriverTotalPayment())}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                            if (booking.driverRemainingBalance > 0) {
                                                Text(
                                                    text = "Remaining Due: ${formatCurrency(booking.driverRemainingBalance)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusPending
                                                )
                                            } else {
                                                Text(
                                                    text = "Driver Cleared",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusPaid
                                                )
                                            }
                                        }

                                        val statusColor = when (booking.bookingStatus) {
                                            "Completed", "Delivered" -> StatusPaid
                                            "In Transit" -> BluePrimary
                                            else -> StatusPending
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = booking.bookingStatus,
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Details")
                }
            }
        }
    }
}
