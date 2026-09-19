package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.repository.ExpenseRepository
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategoryBudgetStatus(
    val id: Long = 0,
    val category: String,
    val limit: Double,
    val spent: Double,
    val percentage: Float,
    val isWarning80: Boolean,
    val isExceeded100: Boolean
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExpenseRepository(application)

    private val cal = Calendar.getInstance()
    private val currentMonth = cal.get(Calendar.MONTH) + 1
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _accountMode = MutableStateFlow("PERSONAL")
    val accountMode: StateFlow<String> = _accountMode.asStateFlow()

    fun setAccountMode(mode: String) {
        _accountMode.value = mode
    }

    val categories: StateFlow<List<CategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val budgetsFlow = _accountMode.flatMapLatest { mode ->
        repository.getBudgetsForMonth(mode, currentMonth, currentYear)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthlyTransactionsFlow = _accountMode.flatMapLatest { mode ->
        repository.getTransactionsForMonth(mode, currentMonthPrefix)
    }

    val budgetStatuses: StateFlow<List<CategoryBudgetStatus>> = combine(
        budgetsFlow,
        monthlyTransactionsFlow
    ) { budgets, transactions ->
        val spentMap = transactions.groupBy { it.category }
            .mapValues { (_, txList) -> txList.sumOf { it.totalAmount } }

        budgets.map { budget ->
            val spent = spentMap[budget.category] ?: 0.0
            val pct = if (budget.monthlyLimit > 0) ((spent / budget.monthlyLimit) * 100).toFloat() else 0f
            CategoryBudgetStatus(
                id = budget.id,
                category = budget.category,
                limit = budget.monthlyLimit,
                spent = spent,
                percentage = pct,
                isWarning80 = pct in 80f..99.99f,
                isExceeded100 = pct >= 100f
            )
        }.sortedByDescending { it.percentage }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val entity = BudgetEntity(
                accountMode = _accountMode.value,
                category = category,
                monthlyLimit = limit,
                month = currentMonth,
                year = currentYear
            )
            repository.saveBudget(entity)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
        }
    }
}
