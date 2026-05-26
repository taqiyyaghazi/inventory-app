package com.example.core.data

import com.example.core.model.InventoryItem
import com.example.core.model.Location
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val dao: InventoryDao,
    private val locationDao: LocationDao
) {
    val allItems: Flow<List<InventoryItem>> = dao.getAllItems()
    val allLocations: Flow<List<Location>> = locationDao.getAllLocations()

    suspend fun insertItem(item: InventoryItem) {
        dao.insertItem(item)
    }

    suspend fun insertItems(items: List<InventoryItem>) {
        dao.insertItems(items)
    }

    suspend fun updateItem(item: InventoryItem) {
        dao.updateItem(item)
    }

    suspend fun deleteItem(item: InventoryItem) {
        dao.deleteItem(item)
    }

    suspend fun deleteAllItems() {
        dao.deleteAllItems()
    }

    suspend fun getItemById(id: Int): InventoryItem? {
        return dao.getItemById(id)
    }

    suspend fun getAllItemsDirectly(): List<InventoryItem> {
        return dao.getAllItemsDirectly()
    }

    suspend fun getAllLocationsDirectly(): List<Location> {
        return locationDao.getAllLocationsDirectly()
    }

    suspend fun insertLocation(location: Location) {
        locationDao.insertLocation(location)
    }

    suspend fun updateLocation(location: Location) {
        locationDao.updateLocation(location)
    }

    suspend fun deleteLocation(location: Location) {
        locationDao.deleteLocation(location)
    }
}
