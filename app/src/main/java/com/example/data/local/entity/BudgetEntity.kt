package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountMode: String = "PERSONAL", // "PERSONAL" or "BUSINESS"
    val category: String,
    val monthlyLimit: Double,
    val month: Int, // 1 - 12
    val year: Int
)
