package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class Location(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
