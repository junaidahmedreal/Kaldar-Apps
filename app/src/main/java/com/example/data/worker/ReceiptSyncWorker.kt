package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.ai.GeminiApiClient
import com.example.data.local.AppDatabase
import com.example.data.security.SecurePreferencesManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.example.data.local.entity.ReceiptItem

class ReceiptSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ReceiptSyncWorker"
        const val WORK_NAME = "enexpense_pending_ai_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ReceiptSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "ReceiptSyncWorker scheduled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReceiptSyncWorker running doWork")
        val database = AppDatabase.getDatabase(applicationContext)
        val transactionDao = database.transactionDao()
        val prefs = SecurePreferencesManager(applicationContext)
        val apiKey = prefs.getGeminiApiKey()

        if (apiKey.isBlank()) {
            Log.w(TAG, "Cannot sync pending receipts: Gemini API Key is empty")
            return Result.success() // Done without error, will retry when key is set
        }

        val pendingList = transactionDao.getPendingTransactions()
        if (pendingList.isEmpty()) {
            Log.d(TAG, "No pending transactions to parse")
            return Result.success()
        }

        val geminiClient = GeminiApiClient()
        val moshi = Moshi.Builder().build()
        val listType = Types.newParameterizedType(List::class.java, ReceiptItem::class.java)
        val adapter = moshi.adapter<List<ReceiptItem>>(listType)

        var hasFailures = false

        for (tx in pendingList) {
            if (tx.rawText.isBlank()) {
                // If raw text is empty, mark as not pending to avoid infinite loop
                transactionDao.updateTransaction(tx.copy(isPendingParse = false))
                continue
            }

            val parseResult = geminiClient.parseReceiptText(tx.rawText, apiKey)
            parseResult.fold(
                onSuccess = { data ->
                    val updatedItemsJson = adapter.toJson(data.items)
                    val updatedTx = tx.copy(
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
                        itemsJson = updatedItemsJson,
                        isPendingParse = false
                    )
                    transactionDao.updateTransaction(updatedTx)
                    Log.d(TAG, "Successfully AI-parsed pending transaction ${tx.id}")
                },
                onFailure = { error ->
                    Log.w(TAG, "Failed to parse transaction ${tx.id}: ${error.message}")
                    hasFailures = true
                }
            )
        }

        return if (hasFailures) Result.retry() else Result.success()
    }
}
