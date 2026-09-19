package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.export.CsvReportGenerator
import com.example.data.export.PdfReportGenerator
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository(application)
    private val pdfGenerator = PdfReportGenerator(application)
    private val csvGenerator = CsvReportGenerator(application)

    private val _accountMode = MutableStateFlow("PERSONAL")
    val accountMode: StateFlow<String> = _accountMode.asStateFlow()

    fun setAccountMode(mode: String) {
        _accountMode.value = mode
    }

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _accountMode
        .flatMapLatest { mode -> repository.getTransactions(mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportAndSharePdf(context: Context) {
        val txList = transactions.value
        if (txList.isEmpty()) {
            _exportStatusMessage.value = "No transactions available to export"
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            _exportStatusMessage.value = null

            val result = pdfGenerator.generatePdf(
                transactions = txList,
                accountMode = _accountMode.value,
                monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
            )

            _isExporting.value = false

            result.fold(
                onSuccess = { file ->
                    shareFile(context, file, "application/pdf", "EnExpense PDF Report")
                    _exportStatusMessage.value = "PDF Exported Successfully"
                },
                onFailure = { error ->
                    _exportStatusMessage.value = "Failed to export PDF: ${error.message}"
                }
            )
        }
    }

    fun exportAndShareCsv(context: Context) {
        val txList = transactions.value
        if (txList.isEmpty()) {
            _exportStatusMessage.value = "No transactions available to export"
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            _exportStatusMessage.value = null

            val result = csvGenerator.generateCsv(
                transactions = txList,
                accountMode = _accountMode.value
            )

            _isExporting.value = false

            result.fold(
                onSuccess = { file ->
                    shareFile(context, file, "text/csv", "EnExpense CSV Spreadsheet")
                    _exportStatusMessage.value = "CSV Exported Successfully"
                },
                onFailure = { error ->
                    _exportStatusMessage.value = "Failed to export CSV: ${error.message}"
                }
            )
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Report via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun clearStatusMessage() {
        _exportStatusMessage.value = null
    }
}
