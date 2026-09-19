package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room_expenses",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roomId")]
)
data class RoomExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roomId: Long,
    val title: String,
    val amount: Double,
    val paidByMemberName: String,
    val category: String = "Groceries",
    val date: String, // YYYY-MM-DD
    val monthYear: String, // YYYY-MM
    val currency: String = "$",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
