package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountMode: String = "PERSONAL", // "PERSONAL" or "BUSINESS"
    val merchant: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val totalAmount: Double,
    val currency: String = "$",
    val category: String = "Other",
    val paymentMethod: String = "Cash",
    val itemsJson: String = "[]",
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val invoiceNumber: String = "",
    val rawText: String = "",
    val imagePath: String? = null,
    val isPendingParse: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
