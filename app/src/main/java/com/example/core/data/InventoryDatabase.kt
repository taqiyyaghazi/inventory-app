package com.example.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.model.InventoryItem
import com.example.core.model.Location

@Database(entities = [InventoryItem::class, Location::class], version = 4, exportSchema = false)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun dao(): InventoryDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val isTestEnv = try {
                        Class.forName("org.robolectric.Robolectric")
                        true
                    } catch (e: Throwable) {
                        false
                    }

                    val builder = if (isTestEnv) {
                        Room.inMemoryDatabaseBuilder(
                            context.applicationContext,
                            InventoryDatabase::class.java
                        )
                        .allowMainThreadQueries()
                        .setQueryExecutor { it.run() }
                        .setTransactionExecutor { it.run() }
                    } else {
                        Room.databaseBuilder(
                            context.applicationContext,
                            InventoryDatabase::class.java,
                            "inventory_database"
                        )
                    }

                    val instance = builder
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                    INSTANCE = instance
                    instance
                }
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
