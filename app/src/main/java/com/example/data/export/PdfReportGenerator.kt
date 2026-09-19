package com.example.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    suspend fun generatePdf(
        transactions: List<TransactionEntity>,
        accountMode: String,
        monthName: String = "All Time"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width at 72dpi
            val pageHeight = 842 // A4 standard height at 72dpi

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 11f
            }
            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 9f
            }
            val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            // Draw Header
            var y = 40f

            // Emerald banner stripe
            paint.color = Color.parseColor("#10B981")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 8f, paint)

            canvas.drawText("EnExpense Expense Report", 30f, y, titlePaint)
            y += 18f
            canvas.drawText("Account: $accountMode Mode | Period: $monthName", 30f, y, subtitlePaint)
            y += 14f
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date())
            canvas.drawText("Generated: $dateStr (100% Offline)", 30f, y, subtitlePaint)
            y += 24f

            // Summary Card
            val totalExpense = transactions.sumOf { it.totalAmount }
            val currencySymbol = transactions.firstOrNull()?.currency ?: "$"

            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(30f, y, pageWidth - 30f, y + 50f, 8f, 8f, paint)

            canvas.drawText("Total Transactions: ${transactions.size}", 45f, y + 22f, headerPaint)
            canvas.drawText("Total Spent:", 45f, y + 40f, textPaint)

            val totalText = "$currencySymbol ${String.format(Locale.US, "%.2f", totalExpense)}"
            amountPaint.textSize = 14f
            amountPaint.color = Color.parseColor("#059669")
            canvas.drawText(totalText, pageWidth - 45f, y + 36f, amountPaint)
            amountPaint.textSize = 9f
            amountPaint.color = Color.parseColor("#0F172A")

            y += 70f

            // Table Header
            paint.color = Color.parseColor("#F8FAFC")
            canvas.drawRect(30f, y - 14f, pageWidth - 30f, y + 6f, paint)
            canvas.drawLine(30f, y + 6f, pageWidth - 30f, y + 6f, linePaint)

            canvas.drawText("Date", 35f, y, headerPaint)
            canvas.drawText("Merchant / Details", 110f, y, headerPaint)
            canvas.drawText("Category", 280f, y, headerPaint)
            canvas.drawText("Payment", 380f, y, headerPaint)
            canvas.drawText("Amount", pageWidth - 35f, y, headerPaint)
            y += 18f

            val itemsPerPage = 32
            var itemCountOnPage = 0

            for (tx in transactions) {
                if (itemCountOnPage >= itemsPerPage) {
                    // Start new page
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 40f
                    itemCountOnPage = 0

                    // Repeat Table Header
                    canvas.drawText("Date", 35f, y, headerPaint)
                    canvas.drawText("Merchant / Details", 110f, y, headerPaint)
                    canvas.drawText("Category", 280f, y, headerPaint)
                    canvas.drawText("Payment", 380f, y, headerPaint)
                    canvas.drawText("Amount", pageWidth - 35f, y, headerPaint)
                    y += 18f
                }

                // Row
                canvas.drawText(tx.date, 35f, y, textPaint)
                val merchantDisplay = if (tx.merchant.length > 28) tx.merchant.take(25) + "..." else tx.merchant
                canvas.drawText(merchantDisplay, 110f, y, textPaint)
                canvas.drawText(tx.category, 280f, y, textPaint)
                canvas.drawText(tx.paymentMethod, 380f, y, textPaint)

                val amtStr = "${tx.currency} ${String.format(Locale.US, "%.2f", tx.totalAmount)}"
                canvas.drawText(amtStr, pageWidth - 35f, y, amountPaint)

                canvas.drawLine(30f, y + 4f, pageWidth - 30f, y + 4f, linePaint)
                y += 16f
                itemCountOnPage++
            }

            // Footer
            canvas.drawText(
                "EnExpense — 100% Offline AI Expense Tracker",
                30f,
                pageHeight - 20f,
                subtitlePaint
            )

            pdfDocument.finishPage(page)

            // Save to cache dir for sharing
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "EnExpense_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
            val file = File(reportsDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
