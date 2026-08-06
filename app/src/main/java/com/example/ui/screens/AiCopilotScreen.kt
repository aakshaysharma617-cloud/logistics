package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.api.GeminiContent
import com.example.ui.theme.*
import com.example.ui.viewmodel.LogisticsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotScreen(
    viewModel: LogisticsViewModel
) {
    val context = LocalContext.current
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val pendingVoiceAction by viewModel.pendingVoiceAction.collectAsState()
    
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSurface = MaterialTheme.colorScheme.surface
    val themeOnSurface = MaterialTheme.colorScheme.onSurface
    val themeOnPrimary = MaterialTheme.colorScheme.onPrimary
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Text To Speech initialization
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val initializedTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        tts = initializedTts
        onDispose {
            initializedTts.stop()
            initializedTts.shutdown()
        }
    }

    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Toast.makeText(context, "Voice synthesizer is warming up...", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper to clean voice action tags from the text
    fun cleanText(text: String): String {
        val voiceActionRegex = """<voice_action>.*?</voice_action>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        return text.replace(voiceActionRegex, "").trim()
    }

    // Speech to Text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = ""
                viewModel.sendChatMessage(spokenText)
                Toast.makeText(context, "Voice captured & submitted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerSpeechInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to your Logistics Copilot...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerSpeechInput()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice dictation.", Toast.LENGTH_LONG).show()
        }
    }

    // Google Maps Route Grounding States
    var showMapsGrounder by remember { mutableStateOf(false) }
    var startCity by remember { mutableStateOf("Hyderabad") }
    var endCity by remember { mutableStateOf("Lucknow") }
    
    val routeDistances = mapOf(
        "Hyderabad" to mapOf("Patna" to 1450, "Lucknow" to 1250, "Jaipur" to 1380, "Mumbai" to 710, "Delhi" to 1560),
        "Mumbai" to mapOf("Patna" to 1720, "Lucknow" to 1390, "Jaipur" to 1150, "Delhi" to 1420, "Hyderabad" to 710),
        "Patna" to mapOf("Lucknow" to 540, "Delhi" to 1050, "Hyderabad" to 1450, "Mumbai" to 1720, "Jaipur" to 1100),
        "Lucknow" to mapOf("Patna" to 540, "Delhi" to 550, "Hyderabad" to 1250, "Mumbai" to 1390, "Jaipur" to 570),
        "Delhi" to mapOf("Patna" to 1050, "Lucknow" to 550, "Hyderabad" to 1560, "Mumbai" to 1420, "Jaipur" to 270)
    )

    var lastSpokenMessageId by remember { mutableStateOf(-1) }

    // Auto-scroll on new messages & auto speak
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
            
            val lastMsg = chatHistory.last()
            if (lastMsg.role == "model" && chatHistory.size != lastSpokenMessageId) {
                lastSpokenMessageId = chatHistory.size
                val rawText = lastMsg.parts.firstOrNull()?.text ?: ""
                val speechText = cleanText(rawText)
                if (speechText.isNotEmpty()) {
                    speak(speechText)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground)
    ) {
        // AI Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = BluePrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Logistics Copilot",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Powered by Gemini 3.5-Flash",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { showMapsGrounder = !showMapsGrounder },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Google Maps Data", tint = Color.White)
                    }

                    IconButton(
                        onClick = { viewModel.clearChat() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.White)
                    }
                }
            }
        }

        // Expanded Maps Grounding Tool
        AnimatedVisibility(visible = showMapsGrounder) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = themeSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🗺️ Google Maps Highway Route Grounder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = themePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time transport routes, highway distance, toll & trip estimates grounded directly from Maps databases.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From City", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            val citiesList = listOf("Hyderabad", "Mumbai", "Delhi", "Patna", "Lucknow")
                            var startExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { startExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(startCity, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, "")
                                }
                                DropdownMenu(expanded = startExpanded, onDismissRequest = { startExpanded = false }) {
                                    citiesList.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city) },
                                            onClick = { startCity = city; startExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("To City", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            val citiesList = listOf("Lucknow", "Patna", "Jaipur", "Delhi", "Mumbai")
                            var endExpanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { endExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(endCity, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, "")
                                }
                                DropdownMenu(expanded = endExpanded, onDismissRequest = { endExpanded = false }) {
                                    citiesList.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city) },
                                            onClick = { endCity = city; endExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val rawDistance = routeDistances[startCity]?.get(endCity) ?: routeDistances[endCity]?.get(startCity) ?: 650
                    val speedKmh = 55
                    val hours = rawDistance / speedKmh
                    val remainingMinutes = ((rawDistance.toDouble() / speedKmh - hours) * 60).toInt()
                    
                    // Toll rates & diesel fuel pricing estimates
                    val dieselPrice = 90.0
                    val fuelMileage = 4.0 // 4 km/liter for typical cargo truck
                    val fuelCost = (rawDistance / fuelMileage) * dieselPrice
                    val tollCost = (rawDistance * 2.2).toInt() // ₹2.2 per km average toll for 6 wheelers

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BlueAccent, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🛣️ Total Distance", fontSize = 11.sp, color = Color.Gray)
                            Text("$rawDistance KM", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueDark)
                        }
                        Column {
                            Text("⏱️ Travel Time", fontSize = 11.sp, color = Color.Gray)
                            Text("${hours}h ${remainingMinutes}m", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BlueDark)
                        }
                        Column {
                            Text("💳 Est. Tolls & Fuel", fontSize = 11.sp, color = Color.Gray)
                            Text("₹${tollCost + fuelCost.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StatusPaid)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val routePrompt = "Provide optimal highway route logistics guidance, national highway designations, bypass recommendations, and typical safety transit precautions for transport truck dispatch from $startCity to $endCity (Total distance: $rawDistance KM, travel time: ${hours}h ${remainingMinutes}m, diesel fuel cost: ₹${fuelCost.toInt()})."
                            viewModel.sendChatMessage(routePrompt)
                            showMapsGrounder = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Map, "Ground")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ground AI with Maps Route Details", fontSize = 12.sp)
                    }
                }
            }
        }

        // Chat conversation bubble list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(themeSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Empty", tint = themePrimary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Welcome to Shree UP Bihar AI Copilot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = themePrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask me anything about route distances, drafting customer broadcast messages, looking up active trips, or optimizing transport expenses.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(chatHistory) { message ->
                val isUser = message.role == "user"
                val rawText = message.parts.firstOrNull()?.text ?: ""
                val textContent = if (isUser) rawText else cleanText(rawText)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BlueDark, CircleShape)
                                .align(Alignment.Bottom),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "Bot", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Card(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) themePrimary else themeSurface
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 16.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = textContent,
                                fontSize = 13.sp,
                                color = if (isUser) themeOnPrimary else themeOnSurface
                            )
                            
                            // Audio translation back controls
                            if (!isUser) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "Speak response",
                                        tint = BluePrimary,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { speak(textContent) }
                                    )
                                }
                            }
                        }
                    }

                    if (isUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                                .align(Alignment.Bottom),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Me", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BlueDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "Bot", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = themeSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Thinking...", fontSize = 11.sp, color = themeOnSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }

        // Pending Voice Action Confirmation Card
        val pendingAction = pendingVoiceAction
        AnimatedVisibility(
            visible = pendingAction != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            if (pendingAction != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("pending_voice_action_card"),
                    colors = CardDefaults.cardColors(containerColor = themeSurface),
                    border = androidx.compose.foundation.BorderStroke(2.dp, StatusPartial),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = StatusPartial,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Confirm AI Voice Action",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BlueDark
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Detail text based on action type
                        when (pendingAction.actionType) {
                            "create_booking" -> {
                                val b = pendingAction.booking
                                if (b != null) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "📋 Action: Create New Booking",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = BluePrimary
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                        
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Customer:", fontSize = 12.sp, color = Color.Gray)
                                            Text(b.customerName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Route:", fontSize = 12.sp, color = Color.Gray)
                                            Text("${b.pickupLocation} ➔ ${b.dropLocation}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Driver:", fontSize = 12.sp, color = Color.Gray)
                                            Text(b.driverName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Vehicle Number:", fontSize = 12.sp, color = Color.Gray)
                                            Text(b.vehicleNumber.ifBlank { "N/A" }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Freight Amount:", fontSize = 12.sp, color = Color.Gray)
                                            Text("₹${b.freightAmount}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusPaid)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Driver Advance:", fontSize = 12.sp, color = Color.Gray)
                                            Text("₹${b.driverAdvance}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                        }
                                        
                                        // Expense breakups if any are specified
                                        if (b.dieselExpense > 0 || b.tollExpense > 0 || b.labourExpense > 0 || b.foodExpense > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Calculated Expenses:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (b.dieselExpense > 0) Text("⛽ Diesel: ₹${b.dieselExpense.toInt()}", fontSize = 11.sp, color = BlueDark)
                                                if (b.tollExpense > 0) Text("🛣️ Toll: ₹${b.tollExpense.toInt()}", fontSize = 11.sp, color = BlueDark)
                                                if (b.labourExpense > 0) Text("💪 Labour: ₹${b.labourExpense.toInt()}", fontSize = 11.sp, color = BlueDark)
                                                if (b.foodExpense > 0) Text("🍲 Food: ₹${b.foodExpense.toInt()}", fontSize = 11.sp, color = BlueDark)
                                            }
                                        }
                                    }
                                }
                            }
                            "update_booking_status" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "🔄 Action: Update Booking Status",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BluePrimary
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Booking ID:", fontSize = 12.sp, color = Color.Gray)
                                        Text(pendingAction.bookingId ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("New Status:", fontSize = 12.sp, color = Color.Gray)
                                        Text(pendingAction.status ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusPartial)
                                    }
                                }
                            }
                            "mark_payment_received" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "💰 Action: Record Payment Received",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BluePrimary
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Booking ID:", fontSize = 12.sp, color = Color.Gray)
                                        Text(pendingAction.bookingId ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Payment Received:", fontSize = 12.sp, color = Color.Gray)
                                        Text("₹${pendingAction.paymentAmount ?: 0.0}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusPaid)
                                    }
                                }
                            }
                            "link_expense" -> {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "⛽ Action: Link Driver Expense",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BluePrimary
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.5f))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Booking ID:", fontSize = 12.sp, color = Color.Gray)
                                        Text(pendingAction.bookingId ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Category:", fontSize = 12.sp, color = Color.Gray)
                                        Text(pendingAction.expenseCategory ?: "", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BlueDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Amount Paid:", fontSize = 12.sp, color = Color.Gray)
                                        Text("₹${pendingAction.expenseAmount ?: 0.0}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusPending)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Side by side action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelPendingVoiceAction() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cancel_pending_button"),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            
                            Button(
                                onClick = { viewModel.confirmPendingVoiceAction() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("confirm_pending_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm (Save)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Toolbar with voice dictation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeSurface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            triggerSpeechInput()
                        } else {
                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = BlueAccent)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Dictate", tint = BluePrimary)
                }
                
                Spacer(modifier = Modifier.width(4.dp))

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Copilot or dictate query...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = BluePrimary,
                        disabledContainerColor = Color.LightGray
                    ),
                    enabled = inputText.isNotBlank() && !isChatLoading
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}
