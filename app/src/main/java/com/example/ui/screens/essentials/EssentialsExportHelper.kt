package com.example.ui.screens.essentials

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.UsageCycleItem
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object EssentialsExportHelper {

    fun generateTrackersCsv(
        trackers: List<TrackerItem>,
        cycles: List<UsageCycleItem>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== LIFEOS SMART TRACKERS SUMMARY ===")
        sb.appendLine("ID,Title,Description,EnabledMeasurements,DistanceUnit,QuantityUnit,InventoryPolicy,CurrentStock,EmptyStock,ReminderIntervalDays")
        trackers.forEach { t ->
            val measures = buildList {
                if (t.measurements.trackDuration) add("Duration")
                if (t.measurements.trackDistance) add("Distance")
                if (t.measurements.trackQuantity) add("Quantity")
            }.joinToString(";")
            sb.appendLine("${t.id},\"${escapeCsv(t.title)}\",\"${escapeCsv(t.description)}\",\"$measures\",${t.distanceUnit.name},${t.quantityUnit.name},${t.inventoryPolicy.name},${t.currentStock},${t.emptyStock},${t.reminderDaysInterval}")
        }

        sb.appendLine()
        sb.appendLine("=== TRACKER CYCLES LOG ===")
        sb.appendLine("CycleID,TrackerID,CycleName,Status,StartDate,FinishDate,StartOdometer,CurrentOdometer,FinishOdometer,InitialQty,RemainingQty,TotalDistance,QuantityConsumed")
        cycles.forEach { c ->
            sb.appendLine("${c.id},${c.trackerId},\"${escapeCsv(c.cycleName)}\",${c.status.name},${c.startDateIso},${c.finishDateIso ?: "ACTIVE"},${c.startOdometer},${c.currentOdometer},${c.finishOdometer ?: ""},${c.initialQuantity},${c.remainingQuantity},${c.totalDistance},${c.quantityConsumed}")
        }

        return sb.toString()
    }

    fun generateSubscriptionsCsv(
        subscriptions: List<SubscriptionItem>,
        bills: List<BillingOccurrenceItem>,
        payments: List<SubscriptionPaymentItem> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== LIFEOS SUBSCRIPTIONS SUMMARY ===")
        sb.appendLine("ID,Title,Description,Status,Amount,Currency,Frequency,AnchorDay,NextRenewalDate,AutoRenew,IsTrial")
        subscriptions.forEach { s ->
            sb.appendLine("${s.id},\"${escapeCsv(s.title)}\",\"${escapeCsv(s.description)}\",${s.status.name},${s.amount},${s.currency},${s.frequency.name},${s.anchorDayOfMonth},${s.nextRenewalDateIso},${s.autoRenew},${s.isTrial}")
        }

        sb.appendLine()
        sb.appendLine("=== BILLING OCCURRENCES ===")
        sb.appendLine("BillID,SubscriptionID,DueDate,ExpectedAmount,Currency,Status,Notes")
        bills.forEach { b ->
            sb.appendLine("${b.id},${b.subscriptionId},${b.dueDateIso},${b.expectedAmount},${b.currency},${b.status.name},\"${escapeCsv(b.notes)}\"")
        }

        sb.appendLine()
        sb.appendLine("=== PAYMENT AUDIT LOG ===")
        sb.appendLine("PaymentID,BillID,SubscriptionID,AmountPaid,PaymentDate,PaymentMethod,TransactionRef,Notes")
        payments.forEach { p ->
            sb.appendLine("${p.id},${p.billId},${p.subscriptionId},${p.amountPaid},${p.paymentDateIso},\"${escapeCsv(p.paymentMethod)}\",\"${escapeCsv(p.transactionRef)}\",\"${escapeCsv(p.notes)}\"")
        }

        return sb.toString()
    }

    private fun escapeCsv(text: String): String {
        return text.replace("\"", "\"\"")
    }

    fun shareCsvContent(context: Context, filename: String, content: String) {
        val file = File(context.cacheDir, filename)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $filename"))
    }

    fun generateAndShareTrackersPdf(
        context: Context,
        trackers: List<TrackerItem>,
        cycles: List<UsageCycleItem>
    ) {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 30, 45)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = Color.rgb(100, 110, 120)
            textSize = 11f
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("LifeOS — Smart Trackers Report", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Generated on ${LocalDate.now()} • Privacy-First Offline Architecture", 40f, y, subPaint)
        y += 24f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        canvas.drawText("ACTIVE TRACKERS (${trackers.size})", 40f, y, boldPaint)
        y += 18f

        trackers.take(15).forEach { tracker ->
            val trackerCycles = cycles.filter { it.trackerId == tracker.id }
            val active = trackerCycles.find { it.status == CycleStatus.ACTIVE || it.status == CycleStatus.PAUSED }
            canvas.drawText("${tracker.emoji} ${tracker.title}", 40f, y, boldPaint)
            val info = "Stock: ${tracker.currentStock} ${tracker.quantityUnit.symbol} | Active Cycle: ${active?.cycleName ?: "None"}"
            canvas.drawText(info, 300f, y, textPaint)
            y += 16f
            if (y > 780f) return@forEach
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        canvas.drawText("TOTAL RECORDED CYCLES: ${cycles.size}", 40f, y, boldPaint)

        pdfDoc.finishPage(page)

        val file = File(context.cacheDir, "LifeOS_Trackers_Report.pdf")
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LifeOS Trackers Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Trackers PDF Report"))
    }

    fun generateAndShareSubscriptionsPdf(
        context: Context,
        subscriptions: List<SubscriptionItem>,
        bills: List<BillingOccurrenceItem>,
        payments: List<SubscriptionPaymentItem> = emptyList()
    ) {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 30, 45)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = Color.rgb(100, 110, 120)
            textSize = 11f
        }
        val textPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            strokeWidth = 1f
        }

        var y = 40f
        canvas.drawText("LifeOS — Subscriptions & Recurring Bills Report", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("Generated on ${LocalDate.now()} • Privacy-First Offline Architecture", 40f, y, subPaint)
        y += 24f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        val totalMonthly = subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }.sumOf { s ->
            when (s.frequency) {
                BillingFrequency.YEARLY -> s.amount / 12.0
                BillingFrequency.WEEKLY -> s.amount * 52.0 / 12.0
                BillingFrequency.QUARTERLY -> s.amount / 3.0
                else -> s.amount
            }
        }

        canvas.drawText("ACTIVE SUBSCRIPTIONS (${subscriptions.size})", 40f, y, boldPaint)
        canvas.drawText("Monthly Commitment: ~${String.format("%.2f", totalMonthly)}", 340f, y, boldPaint)
        y += 18f

        subscriptions.take(15).forEach { sub ->
            val pendingBill = bills.find { it.subscriptionId == sub.id && it.status != BillStatus.PAID }
            canvas.drawText("${sub.emoji} ${sub.title} (${sub.status.name})", 40f, y, boldPaint)
            val info = "${sub.currency} ${sub.amount} / ${sub.frequency.displayName} | Next: ${sub.nextRenewalDateIso}"
            canvas.drawText(info, 300f, y, textPaint)
            y += 16f
            if (y > 780f) return@forEach
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 20f

        val totalPaid = payments.sumOf { it.amountPaid }
        canvas.drawText("TOTAL PAID HISTORICAL: ${String.format("%.2f", totalPaid)}", 40f, y, boldPaint)

        pdfDoc.finishPage(page)

        val file = File(context.cacheDir, "LifeOS_Subscriptions_Report.pdf")
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LifeOS Subscriptions Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Subscriptions PDF Report"))
    }
}
