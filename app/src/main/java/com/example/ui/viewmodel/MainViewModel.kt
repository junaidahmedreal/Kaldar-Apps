package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.data.security.SecurePreferencesManager
import com.example.ui.components.CategorySlice
import com.example.ui.components.getCategoryColor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ExpenseRepository(application)
    private val prefs = SecurePreferencesManager(application)

    private val _accountMode = MutableStateFlow(prefs.getAccountMode())
    val accountMode: StateFlow<String> = _accountMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getAppLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    val pendingCount: StateFlow<Int> = repository.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = _accountMode
        .flatMapLatest { mode -> repository.getTransactions(mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTransactions: StateFlow<List<TransactionEntity>> = _accountMode
        .flatMapLatest { mode -> repository.getTransactionsForMonth(mode, currentMonthPrefix) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpentThisMonth: StateFlow<Double> = monthlyTransactions
        .combine(_accountMode) { list, _ ->
            list.sumOf { it.totalAmount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySlices: StateFlow<List<CategorySlice>> = monthlyTransactions
        .combine(totalSpentThisMonth) { list, total ->
            if (total == 0.0) return@combine emptyList<CategorySlice>()

            val grouped = list.groupBy { it.category }
            grouped.map { (category, txList) ->
                val catTotal = txList.sumOf { it.totalAmount }
                val pct = ((catTotal / total) * 100).toFloat()
                CategorySlice(
                    category = category,
                    amount = catTotal,
                    color = getCategoryColor(category),
                    percentage = pct
                )
            }.sortedByDescending { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureCategoriesPopulated()
        }
    }

    fun setAccountMode(mode: String) {
        _accountMode.value = mode
        prefs.setAccountMode(mode)
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.setAppLanguage(lang)
    }

    fun reparseAllPending() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.reparsePendingTransactionsNow()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
