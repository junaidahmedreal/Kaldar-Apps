package com.example.data.local.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReceiptItem(
    val name: String,
    val quantity: Double = 1.0,
    val price: Double = 0.0
)
