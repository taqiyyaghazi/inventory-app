package com.example.core.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.InventoryDatabase
import com.example.core.data.InventoryRepository
import com.example.core.auth.AuthManager
import com.example.core.data.FirestoreSyncManager
import com.example.core.model.InventoryItem
import com.example.core.model.Location
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    DATE_DESC("Terbaru Ditambahkan"),
    DATE_ASC("Terlama Ditambahkan"),
    NAME_ASC("Nama (A-Z)"),
    NAME_DESC("Nama (Z-A)"),
    VALUE_DESC("Nilai Tertinggi"),
    VALUE_ASC("Nilai Terendah"),
    QTY_DESC("Jumlah Terbanyak"),
    QTY_ASC("Jumlah Tersedikit")
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InventoryRepository
    
    val authManager: AuthManager
    val syncManager: FirestoreSyncManager

    init {
        val database = InventoryDatabase.getDatabase(application)
        repository = InventoryRepository(database.dao(), database.locationDao(), database.categoryDao())
        
        authManager = AuthManager(application)
        syncManager = FirestoreSyncManager(repository, application)

        // Observe Auth Status and initiate Firestore Realtime replication when logged in
        viewModelScope.launch {
            authManager.userState.collect { user ->
                try {
                    if (user != null && !user.isMock) {
                        syncManager.startRealtimeSync(user.uid)
                        syncManager.triggerPushSync(user.uid)
                    } else {
                        syncManager.stopSync()
                    }
                } catch (t: Throwable) {
                    println("[ERROR VM] Auth state synchronization failed: ${t.message}")
                    t.printStackTrace()
                }
            }
        }

        // Initial sequential loading and auto-seeding to prevent thread contention during onCreate
        viewModelScope.launch {
            try {
                val currentLocs = repository.getAllLocationsDirectly()
                if (currentLocs.isEmpty()) {
                    val initialLocations = listOf(
                        "Meja Kerja Atas",
                        "Kabinet Tengah",
                        "Ruang Studio Kerja",
                        "Dry Box Kamar",
                        "Laci Samping",
                        "Lemari Pakaian",
                        "Rak Belajar",
                        "Tanpa Lokasi"
                    )
                    initialLocations.forEach { locName ->
                        repository.insertLocation(Location(name = locName))
                    }
                }
            } catch (t: Throwable) {
                println("[ERROR VM] Failed during sequential Room database initialization: ${t.message}")
                t.printStackTrace()
            }
        }
    }

    val rawItems: StateFlow<List<InventoryItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val locationsState: StateFlow<List<Location>> = repository.allLocations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Search and filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.DATE_DESC)
    val sortBy = _sortBy.asStateFlow()

    // Real categories directly from database
    val categoriesState: StateFlow<List<com.example.core.model.Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Core list with filters and search applied
    val itemsState: StateFlow<List<InventoryItem>> = combine(
        rawItems,
        _searchQuery,
        _selectedCategory,
        _sortBy
    ) { rawList, query, category, sort ->
        var filtered = rawList

        // Apply category filter
        if (category != "Semua") {
            filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Apply search query
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true) ||
                it.notes.contains(query, ignoreCase = true)
            }
        }

        // Apply sorting
        when (sort) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            SortOption.DATE_ASC -> filtered.sortedBy { it.timestamp }
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            SortOption.VALUE_DESC -> filtered.sortedByDescending { it.value * it.quantity }
            SortOption.VALUE_ASC -> filtered.sortedBy { it.value * it.quantity }
            SortOption.QTY_DESC -> filtered.sortedByDescending { it.quantity }
            SortOption.QTY_ASC -> filtered.sortedBy { it.quantity }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary Statistics
    val statsState: StateFlow<InventoryStats> = rawItems.map { list ->
        val totalItems = list.sumOf { it.quantity }
        val totalValue = list.sumOf { it.value * it.quantity }
        val uniqueItems = list.size
        
        InventoryStats(
            totalItems = totalItems,
            totalValue = totalValue,
            uniqueItemsCount = uniqueItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryStats(0, 0.0, 0)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSortBy(option: SortOption) {
        _sortBy.value = option
    }

    // CRUD Actions
    fun addItem(name: String, category: String, quantity: Int, location: String, value: Double, notes: String, imageUrl: String? = null) {
        viewModelScope.launch {
            try {
                val item = InventoryItem(
                    name = name,
                    category = category,
                    quantity = quantity,
                    location = location,
                    value = value,
                    notes = notes,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertItem(item)
                
                val user = authManager.userState.value
                if (user != null && !user.isMock) {
                    syncManager.pushItem(user.uid, item)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addItems(items: List<InventoryItem>) {
        viewModelScope.launch {
            repository.insertItems(items)
            
            val user = authManager.userState.value
            if (user != null && !user.isMock) {
                items.forEach { syncManager.pushItem(user.uid, it) }
            }
        }
    }

    fun updateItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                val updated = item.copy(timestamp = System.currentTimeMillis())
                repository.updateItem(updated)
                
                val user = authManager.userState.value
                if (user != null && !user.isMock) {
                    syncManager.pushItem(user.uid, updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateItemQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return
        viewModelScope.launch {
            try {
                val updated = item.copy(quantity = newQuantity, timestamp = System.currentTimeMillis())
                repository.updateItem(updated)
                
                val user = authManager.userState.value
                if (user != null && !user.isMock) {
                    syncManager.pushItem(user.uid, updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                
                val user = authManager.userState.value
                if (user != null && !user.isMock) {
                    syncManager.deleteItem(user.uid, item.uuid)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllItems() {
        viewModelScope.launch {
            val currentItems = repository.getAllItemsDirectly()
            repository.deleteAllItems()
            
            val user = authManager.userState.value
            if (user != null && !user.isMock) {
                currentItems.forEach { syncManager.deleteItem(user.uid, it.uuid) }
            }
        }
    }

    fun addLocation(name: String) {
        viewModelScope.launch {
            val location = Location(name = name, timestamp = System.currentTimeMillis())
            repository.insertLocation(location)
            
            val user = authManager.userState.value
            if (user != null && !user.isMock) {
                syncManager.pushLocation(user.uid, location)
            }
        }
    }

    fun updateLocation(location: Location, newName: String) {
        viewModelScope.launch {
            val updated = location.copy(name = newName, timestamp = System.currentTimeMillis())
            repository.updateLocation(updated)
            
            val user = authManager.userState.value
            if (user != null && !user.isMock) {
                syncManager.pushLocation(user.uid, updated)
            }
        }
    }

    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            try {
                repository.deleteLocation(location)
                
                val user = authManager.userState.value
                if (user != null && !user.isMock) {
                    syncManager.deleteLocation(user.uid, location.uuid)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshItems() {
        // No-op, managed by Room Flow
    }

    fun refreshLocations() {
        // No-op, managed by Room Flow
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            val category = com.example.core.model.Category(name = name, timestamp = System.currentTimeMillis())
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: com.example.core.model.Category, newName: String) {
        viewModelScope.launch {
            val updated = category.copy(name = newName, timestamp = System.currentTimeMillis())
            repository.updateCategory(updated)
        }
    }

    fun deleteCategory(category: com.example.core.model.Category) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class InventoryStats(
    val totalItems: Int,
    val totalValue: Double,
    val uniqueItemsCount: Int
)
