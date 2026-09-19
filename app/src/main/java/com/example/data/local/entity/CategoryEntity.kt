package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String, // e.g. "restaurant", "local_gas_station", "shopping_cart", etc.
    val color: Long, // Color ARGB long
    val isDefault: Boolean = false
)
