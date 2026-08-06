package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Customer
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BroadcastStatus {
    PENDING,
    SENT,
    SKIPPED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BroadcastScreen(
    viewModel: LogisticsViewModel
) {
    val customers by viewModel.customers.collectAsState()
    val selectedCustomers by viewModel.selectedBroadcastCustomers.collectAsState()
    val templateText by viewModel.broadcastTemplate.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Screen navigation / Campaign state
    var isCampaignActive by remember { mutableStateOf(false) }
    var campaignQueue by remember { mutableStateOf<List<Pair<Customer, BroadcastStatus>>>(emptyList()) }
    var activeQueueIdx by remember { mutableStateOf(0) }

    // Search and Route Filters
    var searchQuery by remember { mutableStateOf("") }
    var stateFilter by remember { mutableStateOf("All") }

    val filteredCustomers = customers.filter { cust ->
        val matchesQuery = cust.name.contains(searchQuery, ignoreCase = true) ||
                cust.companyName.contains(searchQuery, ignoreCase = true) ||
                cust.address.contains(searchQuery, ignoreCase = true)
        
        val matchesState = if (stateFilter == "All") true else cust.address.contains(stateFilter, ignoreCase = true)
        
        matchesQuery && matchesState
    }

    // Dynamic message compiler replacing templates
    fun compileMessage(template: String, customer: Customer): String {
        return template
            .replace("{customer_name}", customer.name, ignoreCase = true)
            .replace("{name}", customer.name, ignoreCase = true)
            .replace("{company_name}", customer.companyName, ignoreCase = true)
            .replace("{company}", customer.companyName, ignoreCase = true)
            .replace("{whats_app}", customer.whatsApp, ignoreCase = true)
            .replace("{mobile}", customer.mobile, ignoreCase = true)
            .replace("{gst}", customer.gstNumber, ignoreCase = true)
            .replace("{address}", customer.address, ignoreCase = true)
    }

    if (isCampaignActive) {
        // --- ACTIVE CAMPAIGN CONTROL CENTER ---
        val totalRecipients = campaignQueue.size
        val sentCount = campaignQueue.count { it.second == BroadcastStatus.SENT }
        val skippedCount = campaignQueue.count { it.second == BroadcastStatus.SKIPPED }
        val progressPercentage = if (totalRecipients > 0) (sentCount + skippedCount).toFloat() / totalRecipients else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LightGrayBackground)
                .padding(16.dp)
        ) {
            // Campaign Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF128C7E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "📢 Active Broadcast Campaign",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Sending bulk messages sequentially without group creation",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    OutlinedButton(
                        onClick = { isCampaignActive = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Exit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress KPI
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Campaign Progress: $sentCount Sent | $skippedCount Skipped | ${totalRecipients - sentCount - skippedCount} Pending",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "${(progressPercentage * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF128C7E)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressPercentage },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF25D366),
                        trackColor = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Customer details
            if (activeQueueIdx in campaignQueue.indices) {
                val activePair = campaignQueue[activeQueueIdx]
                val activeCustomer = activePair.first
                val activeStatus = activePair.second
                val compiledText = compileMessage(templateText, activeCustomer)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "👉 CURRENT RECIPIENT (${activeQueueIdx + 1} of $totalRecipients)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF128C7E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeCustomer.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeCustomer.companyName} • ${activeCustomer.whatsApp}",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (activeStatus) {
                                            BroadcastStatus.SENT -> StatusPaid.copy(alpha = 0.15f)
                                            BroadcastStatus.SKIPPED -> StatusPartial.copy(alpha = 0.15f)
                                            BroadcastStatus.PENDING -> Color.Gray.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = activeStatus.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (activeStatus) {
                                        BroadcastStatus.SENT -> StatusPaid
                                        BroadcastStatus.SKIPPED -> StatusPartial
                                        BroadcastStatus.PENDING -> Color.DarkGray
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Preview Dynamic Message Text",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFE5DDD5), RoundedCornerShape(10.dp)) // WhatsApp Chat Background Style
                                .padding(12.dp)
                        ) {
                            // Custom chat bubble container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCF8C6)) // WhatsApp Sent Bubble Green
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = compiledText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activeQueueIdx > 0) activeQueueIdx--
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = activeQueueIdx > 0
                            ) {
                                Icon(Icons.Default.ArrowBack, "")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val updated = campaignQueue.toMutableList()
                                    updated[activeQueueIdx] = updated[activeQueueIdx].copy(second = BroadcastStatus.SKIPPED)
                                    campaignQueue = updated
                                    viewModel.addInAppNotification("Skipped broadcasting to ${activeCustomer.name}")
                                    if (activeQueueIdx < totalRecipients - 1) activeQueueIdx++
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusPartial),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, "", tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Skip", fontSize = 12.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    // Trigger native WhatsApp link redirect
                                    launchWhatsApp(context, activeCustomer.whatsApp, compiledText)
                                    
                                    // Mark as SENT in queue
                                    val updated = campaignQueue.toMutableList()
                                    updated[activeQueueIdx] = updated[activeQueueIdx].copy(second = BroadcastStatus.SENT)
                                    campaignQueue = updated
                                    viewModel.addInAppNotification("Broadcasted WhatsApp alert to ${activeCustomer.name}")

                                    // Auto-advance
                                    if (activeQueueIdx < totalRecipients - 1) {
                                        activeQueueIdx++
                                    } else {
                                        Toast.makeText(context, "Completed last recipient in Campaign!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, "", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send & Next", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, "", tint = StatusPaid, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Campaign Completed!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("All selected customer contacts have been processed.", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable queue checklist
            Text(
                text = "Recipients Checklist Queue (${campaignQueue.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(campaignQueue) { idx, pair ->
                    val customer = pair.first
                    val status = pair.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (idx == activeQueueIdx) BlueAccent else Color.Transparent)
                            .clickable { activeQueueIdx = idx }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${idx + 1}.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (idx == activeQueueIdx) BluePrimary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(customer.companyName, fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (status) {
                                    BroadcastStatus.SENT -> Icons.Default.CheckCircle
                                    BroadcastStatus.SKIPPED -> Icons.Default.Cancel
                                    BroadcastStatus.PENDING -> Icons.Default.Schedule
                                },
                                contentDescription = "",
                                tint = when (status) {
                                    BroadcastStatus.SENT -> StatusPaid
                                    BroadcastStatus.SKIPPED -> StatusPartial
                                    BroadcastStatus.PENDING -> Color.Gray
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // --- SETUP & SELECTION SCREEN ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Informational Jumbotron banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF128C7E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💬 Shree Bulk WhatsApp Broadcast",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select 20 to 50 active transport clients to initiate a personalized, direct messaging campaign sequentially. Avoids manual group setup and protects contact privacy.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Lead Generation Seed Box
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("💡 Need Leads for Bulk Trial?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Instantly seed 50 active transport contacts in DB.", fontSize = 11.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    viewModel.seedFiftyActiveLeads()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, "", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seed 50 Leads", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Quick Selection Presets & Queue setup
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚡ Quick Preset Selectors (Direct Target)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    val top20Ids = customers.take(20).map { it.id }
                                    viewModel.selectAllBroadcastCustomers(top20Ids)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🎯 Select 20 Contacts", fontSize = 11.sp)
                            }

                            ElevatedButton(
                                onClick = {
                                    val top50Ids = customers.take(50).map { it.id }
                                    viewModel.selectAllBroadcastCustomers(top50Ids)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🎯 Select 50 Contacts", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.selectAllBroadcastCustomers(customers.map { it.id })
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Select All (${customers.size})", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearBroadcastCustomers() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusPending),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear Selection", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Search & Route Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, company, city...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, "", modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Route state filter dropdown
                    var filterExpanded by remember { mutableStateOf(false) }
                    val statesList = listOf("All", "UP", "Bihar", "Rajasthan", "MP", "Gujarat", "Haryana")
                    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                        OutlinedButton(
                            onClick = { filterExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(stateFilter, fontSize = 12.sp)
                            Icon(Icons.Default.FilterList, "")
                        }
                        DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                            statesList.forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state) },
                                    onClick = { stateFilter = state; filterExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            // Customer selector section title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Recipients (${selectedCustomers.size} selected / ${filteredCustomers.size} matching)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Customer Checklist Items
            if (filteredCustomers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No customer contacts matching criteria. Use 'Seed 50 Leads' or add clients manually.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                items(filteredCustomers) { customer ->
                    val isSelected = selectedCustomers.contains(customer.id)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleBroadcastCustomer(customer.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleBroadcastCustomer(customer.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${customer.companyName} • ${customer.address}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = customer.whatsApp,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Message Template Box
            item {
                Column {
                    Text(
                        text = "Customize Message Template",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supports placeholders: {customer_name}, {company_name}, {address}, {gst}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = templateText,
                        onValueChange = { viewModel.broadcastTemplate.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("broadcast_template_text"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Launch Action button
            item {
                Button(
                    onClick = {
                        if (selectedCustomers.isEmpty()) {
                            Toast.makeText(context, "Please select at least 1 customer recipient.", Toast.LENGTH_SHORT).show()
                        } else {
                            val selectedList = customers.filter { selectedCustomers.contains(it.id) }
                            campaignQueue = selectedList.map { Pair(it, BroadcastStatus.PENDING) }
                            activeQueueIdx = 0
                            isCampaignActive = true
                            viewModel.addInAppNotification("Configured Broadcast Campaign with ${selectedList.size} recipients.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("send_broadcast_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(14.dp),
                    enabled = selectedCustomers.isNotEmpty()
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "Launch", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Launch Broadcast Campaign (${selectedCustomers.size} Selected)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun launchWhatsApp(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=" + Uri.encode(message))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed. Opened mock transit redirect.", Toast.LENGTH_SHORT).show()
    }
}
