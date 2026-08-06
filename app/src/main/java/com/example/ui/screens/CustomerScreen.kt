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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.models.Customer
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: LogisticsViewModel
) {
    val customers by viewModel.customers.collectAsState()
    val bookings by viewModel.bookings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForProfile by remember { mutableStateOf<Customer?>(null) }

    // Search query
    var searchQuery by remember { mutableStateOf("") }
    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                it.mobile.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customers by name, company, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("customer_search"),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No customers found.",
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
                    items(filteredCustomers) { customer ->
                        // Calculate Customer Metrics from Bookings
                        val customerBookings = bookings.filter { it.customerId == customer.id }
                        val totalBookings = customerBookings.size
                        val totalRevenue = customerBookings.sumOf { it.freightAmount }
                        val pendingPayments = customerBookings.sumOf { it.balanceAmount }
                        val completedDeliveries = customerBookings.filter { it.bookingStatus == "Completed" || it.bookingStatus == "Delivered" }.size

                        CustomerItemCard(
                            customer = customer,
                            totalBookings = totalBookings,
                            totalRevenue = totalRevenue,
                            pendingPayments = pendingPayments,
                            onClick = { selectedCustomerForProfile = customer }
                        )
                    }
                }
            }
        }

        // Floating Action Button to add Customer
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_customer_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }

        // Add Customer Dialog
        if (showAddDialog) {
            CustomerFormDialog(
                onDismiss = { showAddDialog = false },
                onSave = { customer ->
                    viewModel.addCustomer(customer)
                    showAddDialog = false
                }
            )
        }

        // Customer Profile Detail Dialog
        selectedCustomerForProfile?.let { customer ->
            val customerBookings = bookings.filter { it.customerId == customer.id }
            val totalBookings = customerBookings.size
            val totalRevenue = customerBookings.sumOf { it.freightAmount }
            val pendingPayments = customerBookings.sumOf { it.balanceAmount }
            val completedDeliveries = customerBookings.filter { it.bookingStatus == "Completed" || it.bookingStatus == "Delivered" }.size

            CustomerProfileDialog(
                customer = customer,
                bookings = customerBookings,
                totalBookings = totalBookings,
                totalRevenue = totalRevenue,
                pendingPayments = pendingPayments,
                completedDeliveries = completedDeliveries,
                onFormatDate = { viewModel.formatDate(it) },
                onDismiss = { selectedCustomerForProfile = null },
                onDelete = {
                    viewModel.deleteCustomer(customer)
                    selectedCustomerForProfile = null
                }
            )
        }
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    totalBookings: Int,
    totalRevenue: Double,
    pendingPayments: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_item_${customer.id}"),
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
                Column {
                    Text(
                        text = customer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = customer.companyName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BlueAccent)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalBookings Bookings",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusPaid)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatCurrency(pendingPayments), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (pendingPayments > 0) StatusPending else StatusPaid)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, contentDescription = "Phone", modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(customer.mobile, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.PinDrop, contentDescription = "Address", modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(customer.address.take(24) + if (customer.address.length > 24) "..." else "", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var whatsApp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Customer Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_name")
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_company")
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it; if (whatsApp.isEmpty()) whatsApp = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_mobile")
                )

                OutlinedTextField(
                    value = whatsApp,
                    onValueChange = { whatsApp = it },
                    label = { Text("WhatsApp Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_whatsapp")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_email")
                )

                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = { gstNumber = it },
                    label = { Text("GST Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("cust_form_gst")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("cust_form_address")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Notes") },
                    modifier = Modifier.fillMaxWidth().height(80.dp).testTag("cust_form_notes")
                )

                Spacer(modifier = Modifier.height(8.dp))
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
                            if (name.isNotBlank() && mobile.isNotBlank()) {
                                onSave(
                                    Customer(
                                        name = name,
                                        companyName = companyName.ifBlank { name },
                                        mobile = mobile,
                                        whatsApp = whatsApp.ifBlank { mobile },
                                        email = email,
                                        gstNumber = gstNumber,
                                        address = address,
                                        notes = notes
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && mobile.isNotBlank()
                    ) {
                        Text("Create Customer")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileDialog(
    customer: Customer,
    bookings: List<com.example.data.models.Booking>,
    totalBookings: Int,
    totalRevenue: Double,
    pendingPayments: Double,
    completedDeliveries: Int,
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
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Text(
                            text = customer.companyName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = StatusPending)
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
                    // 4 Grid Stats fields
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileStatCard(label = "Total Trips", value = totalBookings.toString(), color = BluePrimary, modifier = Modifier.weight(1f))
                            ProfileStatCard(label = "Completed", value = completedDeliveries.toString(), color = StatusPaid, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileStatCard(label = "Total Revenue", value = formatCurrency(totalRevenue), color = StatusPaid, modifier = Modifier.weight(1f))
                            ProfileStatCard(label = "Pending Due", value = formatCurrency(pendingPayments), color = StatusPending, modifier = Modifier.weight(1f))
                        }
                    }

                    // Profile Fields
                    Text("Contact & Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfileDetailRow(icon = Icons.Default.Phone, label = "Mobile", value = customer.mobile)
                        ProfileDetailRow(icon = Icons.Default.Chat, label = "WhatsApp", value = customer.whatsApp)
                        ProfileDetailRow(icon = Icons.Default.Email, label = "Email", value = customer.email.ifBlank { "N/A" })
                        ProfileDetailRow(icon = Icons.Default.Receipt, label = "GST Number", value = customer.gstNumber.ifBlank { "N/A" })
                        ProfileDetailRow(icon = Icons.Default.PinDrop, label = "Address", value = customer.address.ifBlank { "N/A" })
                        ProfileDetailRow(icon = Icons.Default.Notes, label = "Notes", value = customer.notes.ifBlank { "No extra notes." })
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Booking History Section
                    Text("Historical Booking Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    if (bookings.isEmpty()) {
                        Text(
                            text = "No booking records found for this client.",
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
                                        Text(
                                            text = "Freight: ${formatCurrency(booking.freightAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusPaid
                                        )
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
                                    if (booking.balanceAmount > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Pending Balance: ${formatCurrency(booking.balanceAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusPending
                                        )
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
                    Text("Close Profile")
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = BluePrimary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}
