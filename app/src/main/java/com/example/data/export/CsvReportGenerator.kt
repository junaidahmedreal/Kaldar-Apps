package com.example.data.export

import android.content.Context
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvReportGenerator(private val context: Context) {

    suspend fun generateCsv(
        transactions: List<TransactionEntity>,
        accountMode: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "EnExpense_${accountMode}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            val file = File(reportsDir, fileName)

            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,AccountMode,Date,Time,Merchant,Category,PaymentMethod,TotalAmount,Currency,Tax,Discount,InvoiceNumber,Status\n")

                for (tx in transactions) {
                    val escapedMerchant = escapeCsv(tx.merchant)
                    val escapedCategory = escapeCsv(tx.category)
                    val escapedPayment = escapeCsv(tx.paymentMethod)
                    val escapedInvoice = escapeCsv(tx.invoiceNumber)
                    val status = if (tx.isPendingParse) "Pending AI Parse" else "Parsed"

                    writer.append("${tx.id},")
                    writer.append("${tx.accountMode},")
                    writer.append("${tx.date},")
                    writer.append("${tx.time},")
                    writer.append("$escapedMerchant,")
                    writer.append("$escapedCategory,")
                    writer.append("$escapedPayment,")
                    writer.append("${String.format(Locale.US, "%.2f", tx.totalAmount)},")
                    writer.append("${tx.currency},")
                    writer.append("${String.format(Locale.US, "%.2f", tx.tax)},")
                    writer.append("${String.format(Locale.US, "%.2f", tx.discount)},")
                    writer.append("$escapedInvoice,")
                    writer.append("$status\n")
                }
            }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        var result = value.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }
}
