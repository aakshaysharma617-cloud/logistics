package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val companyName: String,
    val mobile: String,
    val whatsApp: String,
    val email: String,
    val gstNumber: String,
    val address: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "drivers")
data class Driver(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val alternatePhone: String = "",
    val vehicleNumber: String,
    val aadhaarNumber: String,
    val licenseNumber: String,
    val licenseType: String = "",
    val licenseExpiryDate: String = "",
    val vehicleStatus: String = "Available",
    val bankDetails: String,
    val paidAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingIdString: String, // Auto-generated e.g., "SUB-10001"
    val bookingDate: Long = System.currentTimeMillis(),
    val customerId: Int,
    val customerName: String,
    val pickupLocation: String,
    val dropLocation: String,
    val vehicleType: String,
    val driverId: Int, // 0 if unassigned
    val driverName: String,
    val driverMobile: String,
    val vehicleNumber: String,
    val freightAmount: Double = 0.0,
    val driverAdvance: Double = 0.0,
    val balanceAmount: Double = 0.0,
    val paymentStatus: String = "Pending", // "Paid", "Partial", "Pending"
    val bookingStatus: String = "Booked", // "Booked", "Loading", "In Transit", "Delivered", "Completed"
    val dueDate: Long = System.currentTimeMillis(),
    val amountReceived: Double = 0.0, // Amount received from customer
    
    // Driver Payment Details
    val driverDiesel: Double = 0.0,
    val driverToll: Double = 0.0,
    val driverFood: Double = 0.0,
    val driverLabour: Double = 0.0,
    val driverFinalPayment: Double = 0.0,
    val driverRemainingBalance: Double = 0.0,
    
    // Direct Booking Expenses
    val dieselExpense: Double = 0.0,
    val tollExpense: Double = 0.0,
    val labourExpense: Double = 0.0,
    val foodExpense: Double = 0.0,
    val loadingChargesExpense: Double = 0.0,
    val unloadingChargesExpense: Double = 0.0,
    val otherExpenses: Double = 0.0,
    
    val notes: String = ""
) {
    // Utility functions to calculate expenses and profits
    fun getTotalExpenses(): Double {
        return dieselExpense + tollExpense + labourExpense + foodExpense + loadingChargesExpense + unloadingChargesExpense + otherExpenses
    }
    
    fun getProfit(): Double {
        return freightAmount - getTotalExpenses()
    }
    
    fun getDriverTotalPayment(): Double {
        return driverAdvance + driverDiesel + driverToll + driverFood + driverLabour + driverFinalPayment
    }
}

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingId: Int,
    val type: String, // "LR Copy", "POD", "Invoice", "Driver License", "RC Book", "Insurance", "Customer Documents"
    val fileName: String,
    val fileUri: String,
    val uploadDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val role: String, // "Owner", "Staff"
    val pinHash: String, // Direct PIN/Password verification
    val name: String
)

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookingId: Int,
    val bookingIdString: String,
    val customerId: Int,
    val customerName: String,
    val amountPaid: Double,
    val paymentMode: String, // "Cash", "UPI", "Bank Transfer", "Cheque"
    val referenceNumber: String = "",
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

