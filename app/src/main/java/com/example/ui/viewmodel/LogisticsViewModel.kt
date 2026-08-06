package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.models.*
import com.example.data.repository.LogisticsRepository
import com.example.util.PaymentNotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReminderType {
    CUSTOMER_DUE,
    DRIVER_CASH,
    DRIVER_PENDING
}

data class ReminderAlert(
    val id: String,
    val title: String,
    val description: String,
    val type: ReminderType,
    val referenceId: Int,
    val amount: Double
)

class LogisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "shree_up_bihar_logistics_db"
    ).fallbackToDestructiveMigration().build()

    val repository: LogisticsRepository = LogisticsRepository(db.logisticsDao())

    // --- State Flows ---
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drivers: StateFlow<List<Driver>> = repository.allDrivers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<Booking>> = repository.allBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentTransactions: StateFlow<List<PaymentTransaction>> = repository.allPaymentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // --- Search & Filters ---
    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("All") // "All", "Booked", "Loading", "In Transit", "Delivered", "Completed"
    val selectedPaymentStatusFilter = MutableStateFlow("All Payments") // "All Payments", "Paid", "Pending", "Partial", "Overdue"

    // --- Broadcast Selection ---
    private val _selectedBroadcastCustomers = MutableStateFlow<Set<Int>>(emptySet())
    val selectedBroadcastCustomers: StateFlow<Set<Int>> = _selectedBroadcastCustomers.asStateFlow()

    val broadcastTemplate = MutableStateFlow(
        "🚛 Shree UP Bihar Logistics\n\n" +
                "Vehicle Available\n\n" +
                "Hyderabad → Uttar Pradesh\n" +
                "Hyderabad → Bihar\n" +
                "Hyderabad → Rajasthan\n\n" +
                "For Booking:\n" +
                "+91 98765 43210"
    )

    // --- Dynamic Notifications list ---
    private val _inAppNotifications = MutableStateFlow<List<String>>(emptyList())
    val inAppNotifications: StateFlow<List<String>> = _inAppNotifications.asStateFlow()

    // --- Theme Settings ---
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private var paymentNotificationService: PaymentNotificationService? = null

    init {
        // Seed default database values if empty & register default Admin & Staff
        viewModelScope.launch {
            seedInitialDataIfNeeded()
        }

        // Initialize Overdue Payment Monitoring Notification Service
        paymentNotificationService = PaymentNotificationService(application, repository) { alertMessage ->
            addInAppNotification(alertMessage)
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Check if users empty
        val rootUser = repository.getUserByUsername("admin")
        if (rootUser == null) {
            repository.insertUser(User(username = "admin", name = "Sanjay Sharma (Owner)", role = "Owner", pinHash = "1234"))
            repository.insertUser(User(username = "staff", name = "Rajiv Kumar (Manager)", role = "Staff", pinHash = "1111"))
        }

        // Check if customers empty
        repository.allCustomers.first().let { currentCustomers ->
            if (currentCustomers.isEmpty()) {
                val cust1Id = repository.insertCustomer(
                    Customer(
                        name = "Aditya Sharma",
                        companyName = "Aditya Agri Industries",
                        mobile = "+919876543211",
                        whatsApp = "+919876543211",
                        email = "aditya@agriind.com",
                        gstNumber = "09AAAAA1111A1Z1",
                        address = "Plot 45, Industrial Area, Hyderabad",
                        notes = "Regular customer for wheat & pulses shipment."
                    )
                )
                val cust2Id = repository.insertCustomer(
                    Customer(
                        name = "Ramesh Prasad",
                        companyName = "Bihar Food Corporation",
                        mobile = "+919876543212",
                        whatsApp = "+919876543212",
                        email = "ramesh@biharfoods.com",
                        gstNumber = "10BBBBB2222B2Z2",
                        address = "Mithapur, Patna, Bihar",
                        notes = "Bulk rice distributor."
                    )
                )
                val cust3Id = repository.insertCustomer(
                    Customer(
                        name = "Vikas Yadav",
                        companyName = "UP Steel Tubing Ltd",
                        mobile = "+919876543213",
                        whatsApp = "+919876543213",
                        email = "vikas@upsteel.com",
                        gstNumber = "09CCCCC3333C3Z3",
                        address = "Sanjay Nagar, Ghaziabad, UP",
                        notes = "Steel pipelines transport."
                    )
                )

                // Seed Drivers
                val d1Id = repository.insertDriver(
                    Driver(
                        name = "Rajesh Yadav",
                        phone = "+918877665511",
                        vehicleNumber = "UP 53 T 7789",
                        aadhaarNumber = "123456789012",
                        licenseNumber = "DL-UP532018000456",
                        bankDetails = "SBI A/C: 30445588992, IFSC: SBIN0001234",
                        paidAmount = 25000.0,
                        pendingAmount = 4500.0
                    )
                )
                val d2Id = repository.insertDriver(
                    Driver(
                        name = "Manoj Kumar",
                        phone = "+918877665522",
                        vehicleNumber = "BR 01 G 4567",
                        aadhaarNumber = "987654321098",
                        licenseNumber = "DL-BR012015000789",
                        bankDetails = "PNB A/C: 1102456789, IFSC: PUNB0121100",
                        paidAmount = 18000.0,
                        pendingAmount = 1200.0
                    )
                )

                // Seed Bookings
                val today = System.currentTimeMillis()
                val oneDay = 24 * 60 * 60 * 1000L

                val b1 = Booking(
                    bookingIdString = "SL-2026-1001",
                    bookingDate = today - (2 * oneDay),
                    customerId = cust1Id.toInt(),
                    customerName = "Aditya Agri Industries",
                    pickupLocation = "Hyderabad, TS",
                    dropLocation = "Patna, BR",
                    vehicleType = "10 Wheeler Truck",
                    driverId = d1Id.toInt(),
                    driverName = "Rajesh Yadav",
                    driverMobile = "+918877665511",
                    vehicleNumber = "UP 53 T 7789",
                    freightAmount = 45000.0,
                    driverAdvance = 15000.0,
                    balanceAmount = 10000.0,
                    paymentStatus = "Partial",
                    bookingStatus = "Delivered",
                    dueDate = today - oneDay, // Overdue
                    amountReceived = 35000.0,
                    dieselExpense = 18000.0,
                    tollExpense = 3200.0,
                    labourExpense = 1500.0,
                    foodExpense = 800.0,
                    loadingChargesExpense = 1200.0,
                    unloadingChargesExpense = 1000.0,
                    otherExpenses = 500.0,
                    driverDiesel = 10000.0,
                    driverToll = 3200.0,
                    driverFood = 800.0,
                    driverLabour = 1000.0,
                    driverRemainingBalance = 4500.0
                )
                repository.insertBooking(b1)

                val b2 = Booking(
                    bookingIdString = "SL-2026-1002",
                    bookingDate = today,
                    customerId = cust2Id.toInt(),
                    customerName = "Bihar Food Corporation",
                    pickupLocation = "Hyderabad, TS",
                    dropLocation = "Lucknow, UP",
                    vehicleType = "Container 32 Ft",
                    driverId = d2Id.toInt(),
                    driverName = "Manoj Kumar",
                    driverMobile = "+918877665522",
                    vehicleNumber = "BR 01 G 4567",
                    freightAmount = 55000.0,
                    driverAdvance = 20000.0,
                    balanceAmount = 35000.0,
                    paymentStatus = "Pending",
                    bookingStatus = "In Transit",
                    dueDate = today + (3 * oneDay),
                    amountReceived = 0.0,
                    dieselExpense = 22000.0,
                    tollExpense = 4500.0,
                    labourExpense = 2000.0,
                    foodExpense = 1200.0,
                    loadingChargesExpense = 1500.0,
                    unloadingChargesExpense = 0.0,
                    otherExpenses = 600.0,
                    driverDiesel = 15000.0,
                    driverToll = 4500.0,
                    driverFood = 1200.0,
                    driverLabour = 1000.0,
                    driverRemainingBalance = 1200.0
                )
                repository.insertBooking(b2)
                
                addInAppNotification("System seeded with initial high-quality records.")
            }
        }
    }

    // --- Dynamic Reminders & Alerts ---
    val reminders: StateFlow<List<ReminderAlert>> = bookings.map { bookingList ->
        val list = mutableListOf<ReminderAlert>()
        val currentTime = System.currentTimeMillis()

        bookingList.forEach { booking ->
            // 1. Customer Payment Overdue
            if (booking.balanceAmount > 0 && currentTime >= booking.dueDate && booking.paymentStatus != "Paid") {
                list.add(
                    ReminderAlert(
                        id = "CUST-${booking.id}",
                        title = "Overdue: Customer Payment",
                        description = "${booking.customerName} owes ₹${booking.balanceAmount} for booking ${booking.bookingIdString} (Due: ${formatDate(booking.dueDate)})",
                        type = ReminderType.CUSTOMER_DUE,
                        referenceId = booking.id,
                        amount = booking.balanceAmount
                    )
                )
            }

            // 2. Driver Unsubmitted Collected Cash
            // If delivered/completed, and payment has been marked received partially but driver has remaining balance/cash tracking
            if ((booking.bookingStatus == "Delivered" || booking.bookingStatus == "Completed") && 
                booking.driverRemainingBalance > 5000.0) {
                list.add(
                    ReminderAlert(
                        id = "DRV-CASH-${booking.id}",
                        title = "Unsubmitted Driver Cash",
                        description = "Driver ${booking.driverName} has unsubmitted cash balance of ₹${booking.driverRemainingBalance} on trip ${booking.bookingIdString}",
                        type = ReminderType.DRIVER_CASH,
                        referenceId = booking.id,
                        amount = booking.driverRemainingBalance
                    )
                )
            }

            // 3. Driver Payment Pending
            if (booking.driverRemainingBalance > 0 && booking.bookingStatus == "Completed") {
                list.add(
                    ReminderAlert(
                        id = "DRV-PEND-${booking.id}",
                        title = "Driver Payment Pending",
                        description = "Final payment of ₹${booking.driverRemainingBalance} pending for ${booking.driverName} on trip ${booking.bookingIdString}",
                        type = ReminderType.DRIVER_PENDING,
                        referenceId = booking.id,
                        amount = booking.driverRemainingBalance
                    )
                )
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filtered Bookings List ---
    val filteredBookings: StateFlow<List<Booking>> = combine(
        bookings,
        searchQuery,
        selectedStatusFilter,
        selectedPaymentStatusFilter
    ) { list, query, status, paymentStatus ->
        val currentTime = System.currentTimeMillis()
        list.filter { booking ->
            val matchesQuery = query.isBlank() ||
                    booking.bookingIdString.contains(query, ignoreCase = true) ||
                    booking.customerName.contains(query, ignoreCase = true) ||
                    booking.driverName.contains(query, ignoreCase = true) ||
                    booking.vehicleNumber.contains(query, ignoreCase = true) ||
                    booking.driverMobile.contains(query, ignoreCase = true) ||
                    booking.pickupLocation.contains(query, ignoreCase = true) ||
                    booking.dropLocation.contains(query, ignoreCase = true)

            val matchesStatus = when (status) {
                "All" -> true
                "Active", "Active Trips" -> booking.bookingStatus != "Completed"
                else -> booking.bookingStatus.equals(status, ignoreCase = true)
            }

            val matchesPaymentStatus = when (paymentStatus) {
                "All", "All Payments" -> true
                "Overdue" -> booking.balanceAmount > 0 && currentTime >= booking.dueDate && booking.paymentStatus != "Paid"
                else -> booking.paymentStatus.equals(paymentStatus, ignoreCase = true)
            }

            matchesQuery && matchesStatus && matchesPaymentStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication Actions ---
    fun login(usernameString: String, pinCode: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByUsername(usernameString.trim().lowercase())
            if (user != null && user.pinHash == pinCode) {
                _currentUser.value = user
                _loginError.value = null
                addInAppNotification("Logged in successfully as ${user.name} (${user.role})")
                onSuccess()
            } else {
                val error = "Invalid Username or PIN"
                _loginError.value = error
                onFailure(error)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        addInAppNotification("Logged out successfully")
    }

    // --- Customer Actions ---
    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.insertCustomer(customer)
            addInAppNotification("Added customer: ${customer.name}")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            addInAppNotification("Updated customer: ${customer.name}")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            addInAppNotification("Deleted customer: ${customer.name}")
        }
    }

    // --- Driver Actions ---
    fun addDriver(driver: Driver) {
        viewModelScope.launch {
            repository.insertDriver(driver)
            addInAppNotification("Added driver: ${driver.name}")
        }
    }

    fun updateDriver(driver: Driver) {
        viewModelScope.launch {
            repository.updateDriver(driver)
            addInAppNotification("Updated driver: ${driver.name}")
        }
    }

    fun deleteDriver(driver: Driver) {
        viewModelScope.launch {
            repository.deleteDriver(driver)
            addInAppNotification("Deleted driver: ${driver.name}")
        }
    }

    // --- Booking Actions ---
    fun addBooking(booking: Booking) {
        viewModelScope.launch {
            repository.insertBooking(booking)
            addInAppNotification("Booking ${booking.bookingIdString} Created!")
        }
    }

    fun updateBooking(booking: Booking) {
        viewModelScope.launch {
            repository.updateBooking(booking)
            // Trigger in-app notification for Completed
            if (booking.bookingStatus == "Completed") {
                addInAppNotification("Booking ${booking.bookingIdString} is completed.")
            } else {
                addInAppNotification("Updated booking ${booking.bookingIdString}")
            }
        }
    }

    fun deleteBooking(booking: Booking) {
        viewModelScope.launch {
            repository.deleteBooking(booking)
            addInAppNotification("Deleted booking ${booking.bookingIdString}")
        }
    }

    fun addPaymentTransaction(payment: PaymentTransaction) {
        viewModelScope.launch {
            repository.insertPaymentTransaction(payment)
            addInAppNotification("Payment of ₹${payment.amountPaid} recorded for booking ${payment.bookingIdString}")
        }
    }

    fun deletePaymentTransaction(payment: PaymentTransaction) {
        viewModelScope.launch {
            repository.deletePaymentTransaction(payment)
            addInAppNotification("Payment of ₹${payment.amountPaid} deleted for booking ${payment.bookingIdString}")
        }
    }

    // --- Document Actions ---
    fun getDocumentsForBooking(bookingId: Int): Flow<List<Document>> {
        return repository.getDocumentsForBooking(bookingId)
    }

    fun addDocument(document: Document) {
        viewModelScope.launch {
            repository.insertDocument(document)
            addInAppNotification("Document uploaded: ${document.type} - ${document.fileName}")
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            addInAppNotification("Document removed: ${document.fileName}")
        }
    }

    // --- Broadcast Management ---
    fun toggleBroadcastCustomer(customerId: Int) {
        val current = _selectedBroadcastCustomers.value.toMutableSet()
        if (current.contains(customerId)) {
            current.remove(customerId)
        } else {
            current.add(customerId)
        }
        _selectedBroadcastCustomers.value = current
    }

    fun selectAllBroadcastCustomers(allIds: List<Int>) {
        _selectedBroadcastCustomers.value = allIds.toSet()
    }

    fun clearBroadcastCustomers() {
        _selectedBroadcastCustomers.value = emptySet()
    }

    // --- Helper for Notifications ---
    fun addInAppNotification(message: String) {
        val current = _inAppNotifications.value.toMutableList()
        current.add(0, "[${formatTime(System.currentTimeMillis())}] $message")
        if (current.size > 20) current.removeAt(current.size - 1)
        _inAppNotifications.value = current
    }

    fun clearNotifications() {
        _inAppNotifications.value = emptyList()
    }

    // --- Formatting Helpers ---
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // --- Seeding 50 Leads for Multi-Broadcast trial ---
    fun seedFiftyActiveLeads() {
        viewModelScope.launch {
            val states = listOf("UP", "Bihar", "Rajasthan", "MP", "Gujarat", "Maharashtra", "Haryana")
            val cities = listOf("Lucknow", "Patna", "Jaipur", "Bhopal", "Ahmedabad", "Mumbai", "Ambala", "Kanpur", "Muzaffarpur", "Gaya", "Kota", "Indore")
            val businessTypes = listOf("Agri Traders", "Steel Logistics", "Cement Distributors", "Coal Trading Co", "Fertilizers Depot", "Cold Storage", "Textile Hub", "Mineral Suppliers", "Paper Mills", "Automobile Parts")
            val names = listOf(
                "Aakash", "Amit", "Sanjay", "Rajesh", "Vijay", "Anil", "Suresh", "Manoj", "Ramesh", "Sunil",
                "Deepak", "Pradeep", "Vikram", "Ajay", "Alok", "Pankaj", "Vikas", "Harish", "Dinesh", "Karan",
                "Rahul", "Rohan", "Sachin", "Kapil", "Ashok", "Gopal", "Shyam", "Krishna", "Arjun", "Kailash",
                "Mahendra", "Devendra", "Ravindra", "Jitendra", "Surendra", "Yogendra", "Satendra", "Narendra", "Rajendra", "Gajendra",
                "Shailendra", "Bhupendra", "Dhirendra", "Virendra", "Amarendra", "Gyanendra", "Harendra", "Lalit", "Sanjeev", "Rajiv"
            )
            val surnames = listOf(
                "Sharma", "Yadav", "Kumar", "Prasad", "Gupta", "Singh", "Maurya", "Pathak", "Sinha", "Chawla",
                "Goel", "Khandelwal", "Singhal", "Verma", "Mishra", "Tripathi", "Pandey", "Tiwari", "Dubey", "Shukla"
            )

            for (i in 1..50) {
                val name = names[i - 1] + " " + surnames[i % surnames.size]
                val state = states[i % states.size]
                val city = cities[i % cities.size]
                val business = businessTypes[i % businessTypes.size]
                val company = "${names[i - 1]} & Sons $business"
                
                // Generates Indian numbers like +91 98765 432XX
                val phoneSuffix = String.format("%02d", i)
                val phone = "+9198765432$phoneSuffix"
                
                val cust = Customer(
                    name = name,
                    companyName = company,
                    mobile = phone,
                    whatsApp = phone,
                    email = "${names[i - 1].lowercase()}@${business.replace(" ", "").lowercase()}.com",
                    gstNumber = "09" + ('A'..'Z').random() + ('A'..'Z').random() + ('A'..'Z').random() + ('A'..'Z').random() + "1234" + ('A'..'Z').random() + "1Z" + (1..9).random(),
                    address = "Sector ${i % 10 + 1}, Industrial Estate, $city, $state",
                    notes = "Automated bulk delivery lead for $state route."
                )
                repository.insertCustomer(cust)
            }
            addInAppNotification("Successfully generated and seeded 50 Customer Leads!")
        }
    }

    // --- AI Voice Assistant Pending Actions ---
    data class PendingVoiceAction(
        val actionType: String, // "create_booking", "update_booking_status", "mark_payment_received", "link_expense"
        val booking: Booking? = null,
        val bookingId: String? = null,
        val status: String? = null,
        val paymentAmount: Double? = null,
        val expenseCategory: String? = null,
        val expenseAmount: Double? = null
    )

    private val _pendingVoiceAction = MutableStateFlow<PendingVoiceAction?>(null)
    val pendingVoiceAction: StateFlow<PendingVoiceAction?> = _pendingVoiceAction.asStateFlow()

    private suspend fun resolveOrCreateCustomer(name: String): Customer {
        val existing = customers.value.find { it.companyName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing
        
        val newCustomer = Customer(
            name = name,
            companyName = name,
            mobile = "+919876543200",
            whatsApp = "+919876543200",
            email = "${name.replace(" ", "").lowercase()}@gmail.com",
            gstNumber = "09ABCDE1234F1Z5",
            address = "Registered via Voice Assistant",
            notes = "Created automatically by AI Voice Copilot"
        )
        repository.insertCustomer(newCustomer)
        kotlinx.coroutines.delay(150)
        return customers.value.find { it.companyName.equals(name, ignoreCase = true) } ?: newCustomer
    }

    private fun resolveDriver(name: String): Driver? {
        return drivers.value.find { it.name.contains(name, ignoreCase = true) }
    }

    private fun executeStatusUpdate(bookingId: String, status: String) {
        viewModelScope.launch {
            val booking = bookings.value.find { it.bookingIdString.equals(bookingId, ignoreCase = true) }
            if (booking != null) {
                val updated = booking.copy(bookingStatus = status)
                updateBooking(updated)
            } else {
                addInAppNotification("Booking $bookingId not found")
            }
        }
    }

    private fun executePaymentReceived(bookingId: String, amount: Double) {
        viewModelScope.launch {
            val booking = bookings.value.find { it.bookingIdString.equals(bookingId, ignoreCase = true) }
            if (booking != null) {
                val finalAmount = if (amount > 0.0) amount else booking.freightAmount
                val updatedReceived = booking.amountReceived + finalAmount
                val updatedStatus = if (updatedReceived >= booking.freightAmount) "Paid" else "Partial"
                val updated = booking.copy(
                    amountReceived = updatedReceived,
                    balanceAmount = (booking.freightAmount - updatedReceived).coerceAtLeast(0.0),
                    paymentStatus = updatedStatus
                )
                updateBooking(updated)
            } else {
                addInAppNotification("Booking $bookingId not found")
            }
        }
    }

    private fun executeLinkExpense(bookingId: String, category: String, amount: Double) {
        viewModelScope.launch {
            val booking = bookings.value.find { it.bookingIdString.equals(bookingId, ignoreCase = true) }
            if (booking != null) {
                val updatedNotes = if (booking.notes.isBlank()) "Linked $category: ₹$amount" else "${booking.notes}\nLinked $category: ₹$amount"
                val updated = when (category.lowercase(Locale.getDefault())) {
                    "diesel" -> booking.copy(dieselExpense = booking.dieselExpense + amount, notes = updatedNotes)
                    "toll" -> booking.copy(tollExpense = booking.tollExpense + amount, notes = updatedNotes)
                    "labour" -> booking.copy(labourExpense = booking.labourExpense + amount, notes = updatedNotes)
                    "food" -> booking.copy(foodExpense = booking.foodExpense + amount, notes = updatedNotes)
                    else -> booking.copy(otherExpenses = booking.otherExpenses + amount, notes = updatedNotes)
                }
                updateBooking(updated)
            } else {
                addInAppNotification("Booking $bookingId not found")
            }
        }
    }

    fun getDatabaseContextString(): String {
        val bList = bookings.value
        val dList = drivers.value
        val cList = customers.value
        
        val bookingsSummary = bList.joinToString("\n") { b ->
            "- ID: ${b.bookingIdString}, Cust: ${b.customerName}, Route: ${b.pickupLocation} to ${b.dropLocation}, Driver: ${b.driverName}, Vehicle: ${b.vehicleNumber}, Freight: ₹${b.freightAmount}, Advance: ₹${b.driverAdvance}, Balance: ₹${b.balanceAmount}, Status: ${b.bookingStatus}, Payment: ${b.paymentStatus}, Expenses: Diesel ₹${b.dieselExpense}, Toll ₹${b.tollExpense}, Labour ₹${b.labourExpense}, Food ₹${b.foodExpense}"
        }
        
        val driversSummary = dList.joinToString("\n") { d ->
            "- Name: ${d.name}, Phone: ${d.phone}, Vehicle: ${d.vehicleNumber}"
        }
        
        val customersSummary = cList.joinToString("\n") { c ->
            "- Name: ${c.name}, Company: ${c.companyName}, Mobile: ${c.mobile}"
        }
        
        return """
            === CURRENT DATABASE STATE ===
            ACTIVE BOOKINGS:
            $bookingsSummary
            
            REGISTERED DRIVERS:
            $driversSummary
            
            REGISTERED CUSTOMERS:
            $customersSummary
            ==============================
        """.trimIndent()
    }

    // --- Gemini Chatbot ---
    private val _chatHistory = MutableStateFlow<List<com.example.data.api.GeminiContent>>(emptyList())
    val chatHistory: StateFlow<List<com.example.data.api.GeminiContent>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isChatLoading.value = true
            
            // Add user message to history
            val userMsg = com.example.data.api.GeminiContent(
                parts = listOf(com.example.data.api.GeminiPart(text = prompt)),
                role = "user"
            )
            val updatedHistory = _chatHistory.value.toMutableList()
            updatedHistory.add(userMsg)
            _chatHistory.value = updatedHistory

            // Generate context-aware system instruction to make chatbot helpful for our Logistics app
            val databaseContext = getDatabaseContextString()
            val systemInstruction = """
                You are Shree UP Bihar Logistics Copilot, a helpful AI Transport Management Voice Assistant.
                You understand natural speech in English, Hindi (हिन्दी), and Hinglish (Hindi written in Latin script).
                You can manage the logistics business completely by voice.
                
                You have access to the live application database:
                $databaseContext
                
                Your job is to:
                1. Answer business questions using the database. (e.g., pending payments, highest pending customer, today's profit, monthly revenue, which driver has pending payment).
                   - "Show today's trips" or "Show today's bookings": Look at bookings created today (or list all active bookings) and format a concise report of today's logistics activity.
                   - "Show pending payments": calculate total freight minus amountReceived where paymentStatus is "Pending" or "Partial", and list individual bookings.
                   - "Generate today's report": Provide a consolidated report summarizing bookings made today, total freight booked, total advance received, total expenses, and estimated net profit.
                   - For highest pending customer: sum up the balanceAmount for each customer, find the one with the highest total balance, and report their details.
                   - For today's profit: sum the profit (freightAmount - totalExpenses) for bookings made today.
                   - For monthly revenue: sum the freightAmount for bookings made this month.
                   - For driver pending payments: find drivers whose bookings have pending or partial payments.
                2. Extract booking details from voice commands to add/book a truck. (e.g., "Book one truck for Gupta Traders...")
                   - Extract: Customer, Pickup, Drop, Driver, Vehicle number, Freight, Advance, and expenses if mentioned.
                   - Confirm all extracted fields to the user, and ask: "Do you want to save it?"
                   - You MUST output a structured voice action JSON inside the `<voice_action>` tag (see below).
                3. Update booking status. (e.g., "Mark booking SL-12345 as delivered")
                   - Confirm the change and ask: "Do you want to save it?" (or execute directly if clear). Set requires_confirmation to true.
                4. Mark payments. (e.g., "Mark payment received for SL-12345")
                   - Confirm and ask for confirmation.
                5. Link expenses. (e.g., "Diesel 8000 for SL-12345", "Toll 950 for SL-12345", "Labour 1200 for SL-12345", "Food 500 for SL-12345")
                   - Confirm and ask for confirmation.
                6. Send vehicle availability message to customers. (e.g., "Send vehicle availability message to customers")
                   - Draft a highly professional WhatsApp vehicle availability text based on what the user said (or standard ones like "UP Bihar vehicle available from Hyderabad").
                   - Set "action" to "update_broadcast_template", and "broadcast_template" to the drafted text. Set requires_confirmation to false.
                7. Handle confirmations:
                   - If the user says "Yes", "save it", "confirm", "haan", "correct", or similar in response to a confirmation question, output the action: "confirm_pending".
                   - If the user says "No", "cancel", "cancel it", or similar, output the action: "cancel_pending".
                
                === OUTPUT FORMAT ===
                Your response must consist of:
                1. A friendly, conversational message in the user's preferred language (English, Hindi, or Hinglish) explaining what you did or answering their question.
                2. EXACTLY ONE `<voice_action>` tag at the very end of your response containing a single-line JSON payload. Do NOT format JSON with line breaks. Keep it on a single line.
                
                === JSON SPECIFICATION ===
                The JSON inside `<voice_action>` must look like this:
                {
                  "action": "create_booking" | "update_booking_status" | "mark_payment_received" | "link_expense" | "update_broadcast_template" | "confirm_pending" | "cancel_pending" | "none",
                  "requires_confirmation": true | false,
                  "data": {
                    "customer_name": "Gupta Traders",
                    "pickup_location": "Hyderabad",
                    "drop_location": "Patna",
                    "driver_name": "Rakesh",
                    "vehicle_number": "AP39AB1234",
                    "freight_amount": 45000.0,
                    "driver_advance": 10000.0,
                    "diesel_expense": 8000.0,
                    "toll_expense": 950.0,
                    "labour_expense": 1200.0,
                    "food_expense": 500.0,
                    
                    "booking_id": "SL-12345",
                    "status": "Delivered",
                    "payment_amount": 45000.0,
                    
                    "expense_category": "Diesel" | "Toll" | "Labour" | "Food",
                    "expense_amount": 500.0,
                    
                    "broadcast_template": "🚛 Shree UP Bihar Logistics\n\nVehicle Available...\n"
                  }
                }
                
                If you are answering a general business question or drafting a message without performing a database write, set "action": "none" and "requires_confirmation": false.
                For any new write command (create, update status, payment, link expense), set "requires_confirmation": true.
                For confirming a previous action, set "action": "confirm_pending" and "requires_confirmation": false.
                For cancelling a previous action, set "action": "cancel_pending" and "requires_confirmation": false.
            """.trimIndent()

            val response = com.example.data.api.GeminiClient.getCompletion(
                prompt = prompt,
                systemInstruction = systemInstruction,
                history = updatedHistory.dropLast(1) // Send previous turns to maintain context
            )

            val voiceActionRegex = """<voice_action>(.*?)</voice_action>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matchResult = voiceActionRegex.find(response)
            if (matchResult != null) {
                val jsonStr = matchResult.groupValues[1].trim()
                try {
                    val json = org.json.JSONObject(jsonStr)
                    val action = json.optString("action", "none")
                    val requiresConfirmation = json.optBoolean("requires_confirmation", false)
                    val dataObj = json.optJSONObject("data")
                    
                    when (action) {
                        "create_booking" -> {
                            if (dataObj != null) {
                                val customerName = dataObj.optString("customer_name")
                                val pickup = dataObj.optString("pickup_location")
                                val drop = dataObj.optString("drop_location")
                                val driverName = dataObj.optString("driver_name", "Unassigned")
                                val vehicleNum = dataObj.optString("vehicle_number", "")
                                val freight = dataObj.optDouble("freight_amount", 0.0)
                                val advance = dataObj.optDouble("driver_advance", 0.0)
                                
                                val dieselExp = dataObj.optDouble("diesel_expense", 0.0)
                                val tollExp = dataObj.optDouble("toll_expense", 0.0)
                                val labourExp = dataObj.optDouble("labour_expense", 0.0)
                                val foodExp = dataObj.optDouble("food_expense", 0.0)
                                
                                val customer = resolveOrCreateCustomer(customerName)
                                val driver = resolveDriver(driverName)
                                val driverId = driver?.id ?: 0
                                val driverMobile = driver?.phone ?: "+919876543210"
                                
                                val newBooking = Booking(
                                    bookingIdString = "SL-${System.currentTimeMillis() % 100000}",
                                    customerId = customer.id,
                                    customerName = customer.companyName,
                                    pickupLocation = pickup,
                                    dropLocation = drop,
                                    vehicleType = "6 Wheeler Truck",
                                    driverId = driverId,
                                    driverName = if (driver != null) driver.name else driverName,
                                    driverMobile = driverMobile,
                                    vehicleNumber = if (driver != null && vehicleNum.isBlank()) driver.vehicleNumber else vehicleNum,
                                    freightAmount = freight,
                                    driverAdvance = advance,
                                    balanceAmount = freight - advance,
                                    paymentStatus = if (advance >= freight && freight > 0.0) "Paid" else if (advance > 0.0) "Partial" else "Pending",
                                    bookingStatus = "Booked",
                                    dieselExpense = dieselExp,
                                    tollExpense = tollExp,
                                    labourExpense = labourExp,
                                    foodExpense = foodExp,
                                    notes = "Created via Voice Assistant."
                                )
                                
                                if (requiresConfirmation) {
                                    _pendingVoiceAction.value = PendingVoiceAction(
                                        actionType = "create_booking",
                                        booking = newBooking
                                    )
                                } else {
                                    addBooking(newBooking)
                                }
                            }
                        }
                        "update_booking_status" -> {
                            if (dataObj != null) {
                                val bId = dataObj.optString("booking_id")
                                val status = dataObj.optString("status")
                                if (requiresConfirmation) {
                                    _pendingVoiceAction.value = PendingVoiceAction(
                                        actionType = "update_booking_status",
                                        bookingId = bId,
                                        status = status
                                    )
                                } else {
                                    executeStatusUpdate(bId, status)
                                }
                            }
                        }
                        "mark_payment_received" -> {
                            if (dataObj != null) {
                                val bId = dataObj.optString("booking_id")
                                val paymentAmount = dataObj.optDouble("payment_amount", 0.0)
                                if (requiresConfirmation) {
                                    _pendingVoiceAction.value = PendingVoiceAction(
                                        actionType = "mark_payment_received",
                                        bookingId = bId,
                                        paymentAmount = paymentAmount
                                    )
                                } else {
                                    executePaymentReceived(bId, paymentAmount)
                                }
                            }
                        }
                        "link_expense" -> {
                            if (dataObj != null) {
                                val bId = dataObj.optString("booking_id")
                                val category = dataObj.optString("expense_category")
                                val amount = dataObj.optDouble("expense_amount", 0.0)
                                if (requiresConfirmation) {
                                    _pendingVoiceAction.value = PendingVoiceAction(
                                        actionType = "link_expense",
                                        bookingId = bId,
                                        expenseCategory = category,
                                        expenseAmount = amount
                                    )
                                } else {
                                    executeLinkExpense(bId, category, amount)
                                }
                            }
                        }
                        "update_broadcast_template" -> {
                            if (dataObj != null) {
                                val templateText = dataObj.optString("broadcast_template")
                                if (templateText.isNotBlank()) {
                                    broadcastTemplate.value = templateText
                                    // Pre-select all customer IDs to save time and effort
                                    val allIds = customers.value.map { it.id }.toSet()
                                    _selectedBroadcastCustomers.value = allIds
                                    addInAppNotification("Broadcast Template drafted and customers auto-selected!")
                                }
                            }
                        }
                        "confirm_pending" -> {
                            val pending = _pendingVoiceAction.value
                            if (pending != null) {
                                when (pending.actionType) {
                                    "create_booking" -> {
                                        pending.booking?.let { addBooking(it) }
                                    }
                                    "update_booking_status" -> {
                                        executeStatusUpdate(pending.bookingId ?: "", pending.status ?: "")
                                    }
                                    "mark_payment_received" -> {
                                        executePaymentReceived(pending.bookingId ?: "", pending.paymentAmount ?: 0.0)
                                    }
                                    "link_expense" -> {
                                        executeLinkExpense(pending.bookingId ?: "", pending.expenseCategory ?: "", pending.expenseAmount ?: 0.0)
                                    }
                                }
                                _pendingVoiceAction.value = null
                            }
                        }
                        "cancel_pending" -> {
                            _pendingVoiceAction.value = null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val modelMsg = com.example.data.api.GeminiContent(
                parts = listOf(com.example.data.api.GeminiPart(text = response)),
                role = "model"
            )
            val finalHistory = _chatHistory.value.toMutableList()
            finalHistory.add(modelMsg)
            _chatHistory.value = finalHistory
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
        _pendingVoiceAction.value = null
    }

    fun confirmPendingVoiceAction() {
        val pending = _pendingVoiceAction.value
        if (pending != null) {
            viewModelScope.launch {
                when (pending.actionType) {
                    "create_booking" -> {
                        pending.booking?.let { addBooking(it) }
                    }
                    "update_booking_status" -> {
                        executeStatusUpdate(pending.bookingId ?: "", pending.status ?: "")
                    }
                    "mark_payment_received" -> {
                        executePaymentReceived(pending.bookingId ?: "", pending.paymentAmount ?: 0.0)
                    }
                    "link_expense" -> {
                        executeLinkExpense(pending.bookingId ?: "", pending.expenseCategory ?: "", pending.expenseAmount ?: 0.0)
                    }
                }
                _pendingVoiceAction.value = null
                addInAppNotification("Voice action confirmed and saved!")
            }
        }
    }

    fun cancelPendingVoiceAction() {
        _pendingVoiceAction.value = null
        addInAppNotification("Voice action cancelled.")
    }
}
