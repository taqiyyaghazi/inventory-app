package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val quantity: Int,
    val location: String,
    val value: Double,
    val notes: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
