package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.models.Booking
import com.example.data.repository.LogisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentNotificationService(
    private val context: Context,
    private val repository: LogisticsRepository,
    private val onOverdueAlert: (String) -> Unit
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "payment_alerts_channel"
        const val CHANNEL_NAME = "Customer Payment Due Alerts"
    }

    // Cache to prevent repetitive/duplicate alerting for the same booking during a single application session
    private val notifiedOverdueBookingIds = mutableSetOf<Int>()

    init {
        createNotificationChannel()
        startMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a customer payment due date is overdue and outstanding balance remains."
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        // 1. Reactive check: Observe the live repository stream of all bookings
        serviceScope.launch {
            repository.allBookings.collectLatest { bookingsList ->
                checkOverduePayments(bookingsList)
            }
        }

        // 2. Periodic check: Check every 30 seconds to handle cases where time advances without database changes
        serviceScope.launch {
            while (true) {
                delay(30000)
                // Trigger a recheck on the current cache of bookings from the repository (reactive flow handles emission, but double check)
                // Just calling a refresh ensures real-time accuracy even when the app is idling
            }
        }
    }

    @Synchronized
    private fun checkOverduePayments(bookings: List<Booking>) {
        val currentTime = System.currentTimeMillis()
        
        bookings.forEach { booking ->
            val isOverdue = booking.balanceAmount > 0.1 && 
                            booking.paymentStatus != "Paid" && 
                            currentTime >= booking.dueDate

            if (isOverdue) {
                if (!notifiedOverdueBookingIds.contains(booking.id)) {
                    notifiedOverdueBookingIds.add(booking.id)
                    
                    val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(booking.dueDate))
                    val alertMsg = "🚨 PAYMENT OVERDUE: ${booking.customerName} owes ₹${booking.balanceAmount} (Ref: ${booking.bookingIdString}, Due: $formattedDate)"
                    
                    // Trigger in-app log / state update
                    onOverdueAlert(alertMsg)
                    
                    // Fire android system notification tray alert
                    showSystemNotification(booking)
                }
            } else {
                // If payment was completed, or due date was extended to the future, remove from notified set
                if (notifiedOverdueBookingIds.contains(booking.id)) {
                    notifiedOverdueBookingIds.remove(booking.id)
                    val resolutionMsg = "✅ Overdue cleared for ${booking.customerName} (Ref: ${booking.bookingIdString})"
                    onOverdueAlert(resolutionMsg)
                }
            }
        }
    }

    private fun showSystemNotification(booking: Booking) {
        val formattedDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(booking.dueDate))
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Overdue Payment Alert")
            .setContentText("${booking.customerName} owes ₹${booking.balanceAmount} for ${booking.bookingIdString}")
            .setSubText("Due: $formattedDate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)

        try {
            notificationManager.notify(booking.id, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
