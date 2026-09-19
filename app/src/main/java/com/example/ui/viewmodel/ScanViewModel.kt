package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.ParsedReceiptData
import com.example.data.ai.RuleBasedCategorizer
import com.example.data.local.entity.ReceiptItem
import com.example.data.local.entity.TransactionEntity
import com.example.data.ocr.ReceiptOcrHelper
import com.example.data.repository.ExpenseRepository
import com.example.data.security.SecurePreferencesManager
import com.example.data.worker.ReceiptSyncWorker
import com.example.ui.components.applyRedactionsToBitmap
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScanStep {
    SELECT_SOURCE,
    EXTRACTING_OCR,
    OCR_PREVIEW,
    ANALYZING_AI,
    EDIT_DETAILS,
    COMPLETED
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository(application)
    private val ocrHelper = ReceiptOcrHelper()
    private val geminiClient = GeminiApiClient()
    private val prefs = SecurePreferencesManager(application)

    private val moshi = Moshi.Builder().build()
    private val itemsAdapter = moshi.adapter<List<ReceiptItem>>(
        Types.newParameterizedType(List::class.java, ReceiptItem::class.java)
    )

    private val _currentStep = MutableStateFlow(ScanStep.SELECT_SOURCE)
    val currentStep: StateFlow<ScanStep> = _currentStep.asStateFlow()

    private val _rawOcrText = MutableStateFlow("")
    val rawOcrText: StateFlow<String> = _rawOcrText.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _redactionBoxes = MutableStateFlow<List<Rect>>(emptyList())
    val redactionBoxes: StateFlow<List<Rect>> = _redactionBoxes.asStateFlow()

    private val _deleteImageAfterOcr = MutableStateFlow(prefs.isDeleteImageAfterOcrEnabled())
    val deleteImageAfterOcr: StateFlow<Boolean> = _deleteImageAfterOcr.asStateFlow()

    // Editable form state
    val merchant = MutableStateFlow("")
    val totalAmount = MutableStateFlow("")
    val currency = MutableStateFlow(prefs.getDefaultCurrency())
    val category = MutableStateFlow("Food")
    val paymentMethod = MutableStateFlow("Cash")
    val date = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val time = MutableStateFlow(SimpleDateFormat("HH:mm", Locale.US).format(Date()))
    val invoiceNumber = MutableStateFlow("")
    val tax = MutableStateFlow("0.0")
    val discount = MutableStateFlow("0.0")
    val items = MutableStateFlow<List<ReceiptItem>>(emptyList())

    private val _isAiParsed = MutableStateFlow(false)
    val isAiParsed: StateFlow<Boolean> = _isAiParsed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setRedactionBoxes(boxes: List<Rect>) {
        _redactionBoxes.value = boxes
    }

    fun setDeleteImageAfterOcr(delete: Boolean) {
        _deleteImageAfterOcr.value = delete
    }

    fun onImagePicked(context: Context, uri: Uri) {
        viewModelScope.launch {
            _currentStep.value = ScanStep.EXTRACTING_OCR
            _errorMessage.value = null

            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap == null) {
                _errorMessage.value = "Failed to load image from gallery"
                _currentStep.value = ScanStep.SELECT_SOURCE
                return@launch
            }

            _capturedBitmap.value = bitmap
            runOcr(bitmap)
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            _currentStep.value = ScanStep.EXTRACTING_OCR
            _errorMessage.value = null
            _capturedBitmap.value = bitmap
            runOcr(bitmap)
        }
    }

    private suspend fun runOcr(bitmap: Bitmap) {
        val result = ocrHelper.extractTextFromBitmap(bitmap)
        result.fold(
            onSuccess = { extracted ->
                val cleanedText = if (extracted.isBlank()) "NO TEXT DETECTED ON RECEIPT" else extracted
                _rawOcrText.value = cleanedText
                _currentStep.value = ScanStep.OCR_PREVIEW
            },
            onFailure = { error ->
                _errorMessage.value = "OCR failed: ${error.message}"
                _currentStep.value = ScanStep.SELECT_SOURCE
            }
        )
    }

    fun proceedToSmartAiParse() {
        val text = _rawOcrText.value
        val apiKey = prefs.getGeminiApiKey()

        viewModelScope.launch {
            _currentStep.value = ScanStep.ANALYZING_AI

            if (repository.isOnline() && apiKey.isNotBlank()) {
                val result = geminiClient.parseReceiptText(text, apiKey)
                result.fold(
                    onSuccess = { parsed ->
                        applyParsedDataToForm(parsed, isAi = true)
                        _currentStep.value = ScanStep.EDIT_DETAILS
                    },
                    onFailure = {
                        // Fallback to offline rule-based
                        val fallback = RuleBasedCategorizer.parseOfflineFallback(text, prefs.getDefaultCurrency())
                        applyParsedDataToForm(fallback, isAi = false)
                        _currentStep.value = ScanStep.EDIT_DETAILS
                    }
                )
            } else {
                // Offline fallback
                val fallback = RuleBasedCategorizer.parseOfflineFallback(text, prefs.getDefaultCurrency())
                applyParsedDataToForm(fallback, isAi = false)
                _currentStep.value = ScanStep.EDIT_DETAILS
            }
        }
    }

    fun skipAiAndUseRuleBased() {
        val text = _rawOcrText.value
        val fallback = RuleBasedCategorizer.parseOfflineFallback(text, prefs.getDefaultCurrency())
        applyParsedDataToForm(fallback, isAi = false)
        _currentStep.value = ScanStep.EDIT_DETAILS
    }

    private fun applyParsedDataToForm(data: ParsedReceiptData, isAi: Boolean) {
        merchant.value = data.merchant
        totalAmount.value = if (data.totalAmount > 0) String.format(Locale.US, "%.2f", data.totalAmount) else ""
        currency.value = data.currency
        category.value = data.category
        paymentMethod.value = data.paymentMethod
        if (data.date.isNotBlank()) date.value = data.date
        if (data.time.isNotBlank()) time.value = data.time
        invoiceNumber.value = data.invoiceNumber
        tax.value = if (data.tax > 0) String.format(Locale.US, "%.2f", data.tax) else "0.0"
        discount.value = if (data.discount > 0) String.format(Locale.US, "%.2f", data.discount) else "0.0"
        items.value = data.items
        _isAiParsed.value = isAi
    }

    fun saveTransaction(accountMode: String, displayedCanvasWidth: Float = 0f, displayedCanvasHeight: Float = 0f) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalBitmap = _capturedBitmap.value

            // Apply redactions if any
            if (finalBitmap != null && _redactionBoxes.value.isNotEmpty() && displayedCanvasWidth > 0 && displayedCanvasHeight > 0) {
                finalBitmap = applyRedactionsToBitmap(
                    finalBitmap,
                    _redactionBoxes.value,
                    displayedCanvasWidth,
                    displayedCanvasHeight
                )
            }

            var imagePath: String? = null
            if (!_deleteImageAfterOcr.value && finalBitmap != null) {
                imagePath = repository.saveReceiptImageToPrivateStorage(finalBitmap)
            }

            val amt = totalAmount.value.toDoubleOrNull() ?: 0.0
            val txTax = tax.value.toDoubleOrNull() ?: 0.0
            val txDisc = discount.value.toDoubleOrNull() ?: 0.0
            val itemsJson = itemsAdapter.toJson(items.value)

            val isPending = !_isAiParsed.value && prefs.getGeminiApiKey().isNotBlank()

            val entity = TransactionEntity(
                accountMode = accountMode,
                merchant = merchant.value.ifBlank { "Receipt Expense" },
                date = date.value.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                time = time.value.ifBlank { SimpleDateFormat("HH:mm", Locale.US).format(Date()) },
                totalAmount = amt,
                currency = currency.value.ifBlank { "$" },
                category = category.value.ifBlank { "Other" },
                paymentMethod = paymentMethod.value.ifBlank { "Cash" },
                itemsJson = itemsJson,
                tax = txTax,
                discount = txDisc,
                invoiceNumber = invoiceNumber.value,
                rawText = _rawOcrText.value,
                imagePath = imagePath,
                isPendingParse = isPending
            )

            repository.saveTransaction(entity)

            if (isPending) {
                ReceiptSyncWorker.schedule(getApplication())
            }

            _currentStep.value = ScanStep.COMPLETED
        }
    }

    fun reset() {
        _currentStep.value = ScanStep.SELECT_SOURCE
        _rawOcrText.value = ""
        _capturedBitmap.value = null
        _redactionBoxes.value = emptyList()
        _errorMessage.value = null
        _isAiParsed.value = false
        merchant.value = ""
        totalAmount.value = ""
        invoiceNumber.value = ""
        tax.value = "0.0"
        discount.value = "0.0"
        items.value = emptyList()
    }
}
