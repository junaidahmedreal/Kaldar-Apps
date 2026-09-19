package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.ParsedReceiptData
import com.example.data.ai.RuleBasedCategorizer
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ReceiptItem
import com.example.data.local.entity.TransactionEntity
import com.example.data.security.SecurePreferencesManager
import com.example.data.worker.ReceiptSyncWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val prefs: SecurePreferencesManager = SecurePreferencesManager(context),
    private val geminiClient: GeminiApiClient = GeminiApiClient()
) {
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val categoryDao = database.categoryDao()

    private val moshi = Moshi.Builder().build()
    private val itemsAdapter = moshi.adapter<List<ReceiptItem>>(
        Types.newParameterizedType(List::class.java, ReceiptItem::class.java)
    )

    fun getTransactions(mode: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByMode(mode)
    }

    fun getTransactionsForMonth(mode: String, monthPrefix: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsForMonth(mode, monthPrefix)
    }

    fun getPendingCount(): Flow<Int> {
        return transactionDao.getPendingCount()
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionByIdDirect(id)
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        // Delete image file if exists
        transaction.imagePath?.let { path ->
            deleteReceiptImage(path)
        }
        transactionDao.deleteTransaction(transaction)
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun processAndSaveReceipt(
        rawOcrText: String,
        accountMode: String,
        imageBitmap: Bitmap?,
        deleteImageAfterOcr: Boolean
    ): Long = withContext(Dispatchers.IO) {
        var imagePath: String? = null
        if (!deleteImageAfterOcr && imageBitmap != null) {
            imagePath = saveReceiptImageToPrivateStorage(imageBitmap)
        }

        val apiKey = prefs.getGeminiApiKey()
        val defaultCurrency = prefs.getDefaultCurrency()

        var parsedData: ParsedReceiptData
        var isPending = false

        if (isOnline() && apiKey.isNotBlank()) {
            val result = geminiClient.parseReceiptText(rawOcrText, apiKey)
            if (result.isSuccess) {
                parsedData = result.getOrNull() ?: RuleBasedCategorizer.parseOfflineFallback(rawOcrText, defaultCurrency)
                isPending = false
            } else {
                Log.w("ExpenseRepo", "Online parse failed: ${result.exceptionOrNull()?.message}. Using offline fallback")
                parsedData = RuleBasedCategorizer.parseOfflineFallback(rawOcrText, defaultCurrency)
                isPending = true
                ReceiptSyncWorker.schedule(context)
            }
        } else {
            // Offline or no API key
            parsedData = RuleBasedCategorizer.parseOfflineFallback(rawOcrText, defaultCurrency)
            isPending = apiKey.isNotBlank() // Only pending retry if user has an API key configured
            if (isPending) {
                ReceiptSyncWorker.schedule(context)
            }
        }

        val itemsJson = itemsAdapter.toJson(parsedData.items)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nowTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        val entity = TransactionEntity(
            accountMode = accountMode,
            merchant = if (parsedData.merchant.isNotBlank()) parsedData.merchant else "Receipt Expense",
            date = if (parsedData.date.isNotBlank()) parsedData.date else todayDate,
            time = if (parsedData.time.isNotBlank()) parsedData.time else nowTime,
            totalAmount = parsedData.totalAmount,
            currency = parsedData.currency,
            category = parsedData.category,
            paymentMethod = parsedData.paymentMethod,
            itemsJson = itemsJson,
            tax = parsedData.tax,
            discount = parsedData.discount,
            invoiceNumber = parsedData.invoiceNumber,
            rawText = rawOcrText,
            imagePath = imagePath,
            isPendingParse = isPending
        )

        transactionDao.insertTransaction(entity)
    }

    suspend fun reparsePendingTransactionsNow(): Int = withContext(Dispatchers.IO) {
        val apiKey = prefs.getGeminiApiKey()
        if (apiKey.isBlank() || !isOnline()) return@withContext 0

        val pending = transactionDao.getPendingTransactions()
        var count = 0
        for (tx in pending) {
            if (tx.rawText.isNotBlank()) {
                val result = geminiClient.parseReceiptText(tx.rawText, apiKey)
                result.getOrNull()?.let { data ->
                    val updatedItems = itemsAdapter.toJson(data.items)
                    val updated = tx.copy(
                        merchant = if (data.merchant.isNotBlank() && data.merchant != "Unknown Merchant") data.merchant else tx.merchant,
                        date = if (data.date.isNotBlank()) data.date else tx.date,
                        time = if (data.time.isNotBlank()) data.time else tx.time,
                        totalAmount = if (data.totalAmount > 0) data.totalAmount else tx.totalAmount,
                        currency = if (data.currency.isNotBlank()) data.currency else tx.currency,
                        category = if (data.category.isNotBlank() && data.category != "Other") data.category else tx.category,
                        paymentMethod = if (data.paymentMethod.isNotBlank()) data.paymentMethod else tx.paymentMethod,
                        tax = if (data.tax > 0) data.tax else tx.tax,
                        discount = if (data.discount > 0) data.discount else tx.discount,
                        invoiceNumber = if (data.invoiceNumber.isNotBlank()) data.invoiceNumber else tx.invoiceNumber,
                        itemsJson = updatedItems,
                        isPendingParse = false
                    )
                    transactionDao.updateTransaction(updated)
                    count++
                }
            }
        }
        count
    }

    // Budget Methods
    fun getBudgetsForMonth(mode: String, month: Int, year: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(mode, month, year)
    }

    suspend fun saveBudget(budget: BudgetEntity): Long {
        val existing = budgetDao.getBudget(budget.accountMode, budget.category, budget.month, budget.year)
        return if (existing != null) {
            val updated = existing.copy(monthlyLimit = budget.monthlyLimit)
            budgetDao.updateBudget(updated)
            existing.id
        } else {
            budgetDao.insertBudget(budget)
        }
    }

    suspend fun deleteBudget(id: Long) {
        budgetDao.deleteBudgetById(id)
    }

    // Category Methods
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    suspend fun ensureCategoriesPopulated() = withContext(Dispatchers.IO) {
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertAll(AppDatabase.DEFAULT_CATEGORIES)
        }
    }

    // Private image storage
    fun saveReceiptImageToPrivateStorage(bitmap: Bitmap): String {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val file = File(receiptsDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return file.absolutePath
    }

    fun deleteReceiptImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ExpenseRepo", "Failed to delete image: ${e.message}")
        }
    }

    fun parseItems(itemsJson: String): List<ReceiptItem> {
        return try {
            itemsAdapter.fromJson(itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
