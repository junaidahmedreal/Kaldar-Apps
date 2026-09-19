package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g. "Flat 302", "Roommates", "Trip Goa"
    val adminName: String = "You (Admin)",
    val inviteCode: String = "",
    val currency: String = "$",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
