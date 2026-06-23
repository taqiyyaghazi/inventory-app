package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
