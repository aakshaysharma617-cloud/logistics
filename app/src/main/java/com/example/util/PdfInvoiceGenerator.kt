package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.models.Booking
import com.example.data.models.Customer
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfInvoiceGenerator {

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun generateInvoicePdf(
        context: Context,
        booking: Booking,
        customer: Customer?
    ): File {
        val pdfDocument = PdfDocument()
        
        // standard A4 size is 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint definitions
        val paint = Paint().apply { isAntiAlias = true }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 12f
            isFakeBoldText = true
        }
        val normalPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 10f
        }

        // Draw top aesthetic background banner strip
        paint.color = Color.parseColor("#0B3C5D") // Shree Royal Deep Blue
        canvas.drawRect(40f, 40f, 555f, 48f, paint)

        // 1. Company Information / Branding Header
        paint.color = Color.parseColor("#0B3C5D")
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("SHREE UP BIHAR LOGISTICS CO.", 40f, 78f, paint)

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Fleet Operations & National Highway Transport Operators", 40f, 92f, paint)
        canvas.drawText("Lucknow • Patna • Kanpur • Indore • Delhi-NCR", 40f, 104f, paint)

        // Large right-aligned "INVOICE" watermark title
        paint.color = Color.parseColor("#0B3C5D")
        paint.textSize = 26f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", 555f, 85f, paint)

        // Reset text alignment
        paint.textAlign = Paint.Align.LEFT

        // Simple divider
        paint.color = Color.parseColor("#CCCCCC")
        canvas.drawLine(40f, 115f, 555f, 115f, paint)

        // 2. Invoice Details (Left and Right Column)
        // Y starting position
        val detailsY = 138f

        // Column 1 (Left) - Billed To Client
        boldPaint.color = Color.parseColor("#0B3C5D")
        boldPaint.textSize = 10f
        canvas.drawText("BILLED TO / CUSTOMER:", 40f, detailsY, boldPaint)

        normalPaint.color = Color.BLACK
        normalPaint.textSize = 11f
        normalPaint.isFakeBoldText = true
        val clientName = customer?.name ?: booking.customerName
        canvas.drawText(clientName, 40f, detailsY + 18f, normalPaint)
        normalPaint.isFakeBoldText = false

        val companyName = customer?.companyName ?: "Freight Direct Client"
        canvas.drawText(companyName, 40f, detailsY + 32f, normalPaint)

        val clientAddress = customer?.address ?: "Consignee Route Address"
        // Draw limited address line
        val addressLine = if (clientAddress.length > 40) clientAddress.take(38) + "..." else clientAddress
        canvas.drawText(addressLine, 40f, detailsY + 46f, normalPaint)

        val gstNo = if (!customer?.gstNumber.isNullOrBlank()) "GSTIN: ${customer?.gstNumber}" else "GSTIN: Not Provided"
        canvas.drawText(gstNo, 40f, detailsY + 60f, normalPaint)

        val contactNo = if (!customer?.whatsApp.isNullOrBlank()) "WhatsApp: ${customer?.whatsApp}" else "Mobile: ${booking.driverMobile}"
        canvas.drawText(contactNo, 40f, detailsY + 74f, normalPaint)


        // Column 2 (Right) - Booking Details
        val col2X = 340f
        boldPaint.color = Color.parseColor("#0B3C5D")
        boldPaint.textSize = 10f
        canvas.drawText("INVOICE INFORMATION:", col2X, detailsY, boldPaint)

        normalPaint.color = Color.BLACK
        normalPaint.textSize = 10f
        canvas.drawText("Invoice Number:", col2X, detailsY + 18f, normalPaint)
        boldPaint.color = Color.BLACK
        boldPaint.textSize = 10f
        canvas.drawText(booking.bookingIdString, col2X + 100f, detailsY + 18f, boldPaint)

        canvas.drawText("Booking Date:", col2X, detailsY + 32f, normalPaint)
        canvas.drawText(formatDate(booking.bookingDate), col2X + 100f, detailsY + 32f, normalPaint)

        canvas.drawText("Due Date:", col2X, detailsY + 46f, normalPaint)
        canvas.drawText(formatDate(booking.dueDate), col2X + 100f, detailsY + 46f, normalPaint)

        canvas.drawText("Transit Route:", col2X, detailsY + 60f, normalPaint)
        boldPaint.color = Color.parseColor("#0B3C5D")
        boldPaint.textSize = 10f
        canvas.drawText("${booking.pickupLocation} ➔ ${booking.dropLocation}", col2X + 100f, detailsY + 60f, boldPaint)

        canvas.drawText("Vehicle Type:", col2X, detailsY + 74f, normalPaint)
        canvas.drawText(booking.vehicleType, col2X + 100f, detailsY + 74f, normalPaint)


        // 3. Status Badging (PAID / PENDING / PARTIAL)
        val statusY = detailsY + 94f
        paint.color = Color.parseColor("#EEEEEE")
        // Draw light grey background for shipment details
        val rectBackground = RectF(40f, statusY, 555f, statusY + 30f)
        canvas.drawRoundRect(rectBackground, 6f, 6f, paint)

        // Draw internal status label
        normalPaint.color = Color.DKGRAY
        normalPaint.textSize = 10f
        canvas.drawText("Shipment Status: ", 50f, statusY + 18f, normalPaint)
        
        boldPaint.textSize = 10f
        boldPaint.color = when (booking.bookingStatus) {
            "Completed" -> Color.parseColor("#4CAF50")
            "Delivered" -> Color.parseColor("#00ACC1")
            "In Transit" -> Color.parseColor("#8E24AA")
            else -> Color.parseColor("#FF9800")
        }
        canvas.drawText(booking.bookingStatus.uppercase(Locale.ROOT), 150f, statusY + 18f, boldPaint)

        // Payment status badge right aligned
        normalPaint.color = Color.DKGRAY
        canvas.drawText("Payment Account Status: ", col2X, statusY + 18f, normalPaint)
        
        val payStatusColor = when (booking.paymentStatus) {
            "Paid" -> Color.parseColor("#4CAF50")
            "Partial" -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#F44336")
        }
        boldPaint.color = payStatusColor
        canvas.drawText(booking.paymentStatus.uppercase(Locale.ROOT), col2X + 130f, statusY + 18f, boldPaint)


        // 4. Driver & Transport Information card
        val transportY = statusY + 42f
        paint.color = Color.parseColor("#F6F8FA")
        val transportRect = RectF(40f, transportY, 555f, transportY + 48f)
        canvas.drawRoundRect(transportRect, 6f, 6f, paint)

        boldPaint.color = Color.parseColor("#333333")
        boldPaint.textSize = 9f
        canvas.drawText("ALLOCATED VEHICLE & DISPATCH DRIVER DETAILS", 50f, transportY + 15f, boldPaint)

        normalPaint.color = Color.BLACK
        normalPaint.textSize = 9.5f
        val driverDetails = "Driver Name: ${booking.driverName}  |  Mobile: ${booking.driverMobile}"
        canvas.drawText(driverDetails, 50f, transportY + 28f, normalPaint)
        
        val vehicleDetails = "Truck Register No: ${booking.vehicleNumber}  |  Booking Notes: ${booking.notes.ifBlank { "N/A" }}"
        canvas.drawText(vehicleDetails, 50f, transportY + 40f, normalPaint)


        // 5. Bill of Lading Line Items Table
        val tableY = transportY + 65f
        
        // Table Header
        paint.color = Color.parseColor("#0B3C5D")
        canvas.drawRect(40f, tableY, 555f, tableY + 24f, paint)

        boldPaint.color = Color.WHITE
        boldPaint.textSize = 10f
        canvas.drawText("S.NO", 50f, tableY + 16f, boldPaint)
        canvas.drawText("BILLABLE SERVICE DESCRIPTION", 90f, tableY + 16f, boldPaint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL AMOUNT", 545f, tableY + 16f, boldPaint)
        paint.textAlign = Paint.Align.LEFT

        // Draw line items rows
        var currentY = tableY + 24f
        val itemHeight = 28f
        paint.color = Color.parseColor("#E0E0E0")

        // Item 1: Primary Freight Charges
        currentY += itemHeight
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        normalPaint.textSize = 10f
        normalPaint.color = Color.BLACK
        canvas.drawText("1", 52f, currentY - 8f, normalPaint)
        
        val desc = "Primary Freight Truck Dispatch (${booking.pickupLocation} to ${booking.dropLocation})"
        canvas.drawText(desc, 90f, currentY - 8f, normalPaint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatCurrency(booking.freightAmount), 545f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.LEFT

        // Item 2: Loading & Unloading Add-ons (If exists, else print 0.0)
        currentY += itemHeight
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        canvas.drawText("2", 52f, currentY - 8f, normalPaint)
        canvas.drawText("Loading & Unloading Labour Surcharge", 90f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.RIGHT
        val loadingUnloading = booking.loadingChargesExpense + booking.unloadingChargesExpense
        canvas.drawText(formatCurrency(loadingUnloading), 545f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.LEFT

        // Item 3: Direct Highway Tolls & State Fees (If exists)
        currentY += itemHeight
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        canvas.drawText("3", 52f, currentY - 8f, normalPaint)
        canvas.drawText("National Highway Tolls & Border Clearances", 90f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatCurrency(booking.tollExpense), 545f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.LEFT

        // Item 4: Ancillary Charges / Fuel Surcharges (If exists)
        currentY += itemHeight
        canvas.drawLine(40f, currentY, 555f, currentY, paint)
        canvas.drawText("4", 52f, currentY - 8f, normalPaint)
        canvas.drawText("Ancillary Fuel Surcharges & Food/Labour Allowance", 90f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.RIGHT
        val fuelAllowance = booking.dieselExpense + booking.foodExpense + booking.labourExpense + booking.otherExpenses
        canvas.drawText(formatCurrency(fuelAllowance), 545f, currentY - 8f, normalPaint)
        paint.textAlign = Paint.Align.LEFT


        // 6. Summary Totals Box (Right Aligned Bottom Box)
        val summaryY = currentY + 20f
        val sumBoxLeft = 320f
        
        paint.color = Color.parseColor("#F6F8FA")
        val sumBoxRect = RectF(sumBoxLeft, summaryY, 555f, summaryY + 85f)
        canvas.drawRoundRect(sumBoxRect, 6f, 6f, paint)

        val sumLabelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.DKGRAY
        }
        val sumValPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9.5f
            color = Color.BLACK
            textAlign = Paint.Align.RIGHT
        }

        val totalFreightBill = booking.freightAmount + loadingUnloading + booking.tollExpense + fuelAllowance
        
        canvas.drawText("Subtotal Gross Amt:", sumBoxLeft + 15f, summaryY + 18f, sumLabelPaint)
        canvas.drawText(formatCurrency(totalFreightBill), 540f, summaryY + 18f, sumValPaint)

        canvas.drawText("Paid to Date / Recd:", sumBoxLeft + 15f, summaryY + 36f, sumLabelPaint)
        sumValPaint.color = Color.parseColor("#4CAF50")
        canvas.drawText(formatCurrency(booking.amountReceived), 540f, summaryY + 36f, sumValPaint)

        // Bold Grand Outstanding Total Balance
        canvas.drawLine(sumBoxLeft + 10f, summaryY + 48f, 545f, summaryY + 48f, paint)

        val finalBalance = totalFreightBill - booking.amountReceived
        sumLabelPaint.isFakeBoldText = true
        sumLabelPaint.color = Color.BLACK
        sumLabelPaint.textSize = 11f
        canvas.drawText("OUTSTANDING DUE:", sumBoxLeft + 15f, summaryY + 68f, sumLabelPaint)

        sumValPaint.color = if (finalBalance > 0) Color.parseColor("#F44336") else Color.parseColor("#4CAF50")
        sumValPaint.isFakeBoldText = true
        sumValPaint.textSize = 11f
        canvas.drawText(formatCurrency(finalBalance), 540f, summaryY + 68f, sumValPaint)


        // 7. Invoice Declaration & Terms (Bottom Left)
        val termsY = summaryY + 105f
        boldPaint.color = Color.parseColor("#0B3C5D")
        boldPaint.textSize = 9.5f
        canvas.drawText("TERMS & CONDITIONS OF CARRIAGE", 40f, termsY, boldPaint)

        normalPaint.color = Color.GRAY
        normalPaint.textSize = 8f
        canvas.drawText("1. Freight billing calculations are based on actual highway distance and loaded freight weighment.", 40f, termsY + 14f, normalPaint)
        canvas.drawText("2. Interest @ 18% p.a. will be levied if the outstanding balance is not cleared as per due date.", 40f, termsY + 24f, normalPaint)
        canvas.drawText("3. All transshipment losses or shortfalls must be recorded on the POD copy upon receipt.", 40f, termsY + 34f, normalPaint)
        canvas.drawText("4. Subject to local regional transport authority (RTA) regulations and Lucknow jurisdiction.", 40f, termsY + 44f, normalPaint)


        // Authorized Signatory Line
        val signY = termsY + 55f
        paint.color = Color.parseColor("#CCCCCC")
        canvas.drawLine(380f, signY, 540f, signY, paint)
        
        normalPaint.color = Color.BLACK
        normalPaint.textSize = 8.5f
        normalPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Authorized Signatory For", 460f, signY + 12f, normalPaint)
        canvas.drawText("Shree UP Bihar Logistics Co.", 460f, signY + 22f, normalPaint)
        normalPaint.textAlign = Paint.Align.LEFT


        // Bottom Footer watermark
        paint.color = Color.parseColor("#0B3C5D")
        canvas.drawRect(40f, 792f, 555f, 796f, paint)

        normalPaint.color = Color.GRAY
        normalPaint.textSize = 8f
        normalPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Thank you for your business! For queries or bulk contract bookings, contact support@shreeupbiharlogistics.com", 297f, 810f, normalPaint)
        normalPaint.textAlign = Paint.Align.LEFT

        // Finish Page
        pdfDocument.finishPage(page)

        // Write the PDF file to cache directory
        val fileName = "Invoice_${booking.bookingIdString}.pdf"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun viewPdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "No PDF viewer found. Saved in storage.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Cargo Invoice - ${file.nameWithoutExtension}")
                putExtra(android.content.Intent.EXTRA_TEXT, "Greetings! Please find attached the transport bill / logistics cargo invoice for Booking reference: ${file.nameWithoutExtension.substringAfter("_")}")
                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Invoice PDF"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not share file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
