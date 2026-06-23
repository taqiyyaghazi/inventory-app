package com.example.core.data

import android.content.Context
import android.util.Log
import com.example.core.model.InventoryItem
import com.example.core.model.Location
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncState {
    IDLE, SYNCING, SUCCESS, ERROR
}

class FirestoreSyncManager(
    private val repository: InventoryRepository,
    private val context: Context
) {
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState = _syncState.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private var itemsListenerRegistration: ListenerRegistration? = null
    private var locationsListenerRegistration: ListenerRegistration? = null

    init {
        try {
            firestore = FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e("FirestoreSyncManager", "Firestore is not available: ${e.message}")
        }
    }

    fun isFirestoreAvailable(): Boolean {
        return firestore != null
    }

    // Trigger full upload sync (Local -> Firestore)
    fun triggerPushSync(userId: String) {
        val db = firestore ?: return
        _syncState.value = SyncState.SYNCING
        coroutineScope.launch {
            try {
                // 1. Sync Items
                val localItems = repository.getAllItemsDirectly()
                localItems.forEach { item ->
                    db.collection("users")
                        .document(userId)
                        .collection("items")
                        .document(item.uuid)
                        .set(serializeItem(item))
                }

                // 2. Sync Locations
                val localLocations = repository.getAllLocationsDirectly()
                localLocations.forEach { loc ->
                    db.collection("users")
                        .document(userId)
                        .collection("locations")
                        .document(loc.uuid)
                        .set(serializeLocation(loc))
                }

                _syncState.value = SyncState.SUCCESS
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Push sync failed", e)
                _syncState.value = SyncState.ERROR
            }
        }
    }

    // Push individual item on addition/update
    fun pushItem(userId: String, item: InventoryItem) {
        val db = firestore ?: return
        coroutineScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("items")
                    .document(item.uuid)
                    .set(serializeItem(item))
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Failed to push item to Firestore: ${item.name}", e)
            }
        }
    }

    // Delete individual item from Firestore
    fun deleteItem(userId: String, itemUuid: String) {
        val db = firestore ?: return
        coroutineScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("items")
                    .document(itemUuid)
                    .delete()
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Failed to delete item from Firestore", e)
            }
        }
    }

    // Push individual location on deletion/update
    fun pushLocation(userId: String, location: Location) {
        val db = firestore ?: return
        coroutineScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("locations")
                    .document(location.uuid)
                    .set(serializeLocation(location))
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Failed to push location to Firestore: ${location.name}", e)
            }
        }
    }

    // Delete individual location
    fun deleteLocation(userId: String, locUuid: String) {
        val db = firestore ?: return
        coroutineScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("locations")
                    .document(locUuid)
                    .delete()
            } catch (e: Exception) {
                Log.e("FirestoreSyncManager", "Failed to delete location from Firestore", e)
            }
        }
    }

    // Start listening to live cloud updates (Firestore -> Local Room)
    fun startRealtimeSync(userId: String) {
        try {
            val db = firestore ?: return
            stopSync()

            Log.d("FirestoreSyncManager", "Starting Realtime Sync for user: $userId")

            // 1. Listen for items
            itemsListenerRegistration = db.collection("users")
                .document(userId)
                .collection("items")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirestoreSyncManager", "Listen for items failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        coroutineScope.launch {
                            try {
                                val currentItems = repository.getAllItemsDirectly()
                                for (docChange in snapshots.documentChanges) {
                                    val doc = docChange.document
                                    val uuid = doc.id
                                    when (docChange.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            val remoteItem = deserializeItem(doc) ?: continue
                                            val existing = currentItems.firstOrNull { it.uuid == uuid }
                                            if (existing == null || remoteItem.timestamp > existing.timestamp) {
                                                val idToUse = existing?.id ?: 0
                                                repository.insertItem(remoteItem.copy(id = idToUse))
                                            }
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            val existing = currentItems.firstOrNull { it.uuid == uuid }
                                            if (existing != null) {
                                                repository.deleteItem(existing)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirestoreSyncManager", "Error processing items realtime snapshot", e)
                            }
                        }
                    }
                }

            // 2. Listen for locations
            locationsListenerRegistration = db.collection("users")
                .document(userId)
                .collection("locations")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("FirestoreSyncManager", "Listen for locations failed.", error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        coroutineScope.launch {
                            try {
                                val currentLocs = repository.getAllLocationsDirectly()
                                for (docChange in snapshots.documentChanges) {
                                    val doc = docChange.document
                                    val uuid = doc.id
                                    when (docChange.type) {
                                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                            val remoteLoc = deserializeLocation(doc) ?: continue
                                            val existing = currentLocs.firstOrNull { it.uuid == uuid }
                                            if (existing == null || remoteLoc.timestamp > existing.timestamp) {
                                                val idToUse = existing?.id ?: 0
                                                repository.insertLocation(remoteLoc.copy(id = idToUse))
                                            }
                                        }
                                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                            val existing = currentLocs.firstOrNull { it.uuid == uuid }
                                            if (existing != null) {
                                                repository.deleteLocation(existing)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("FirestoreSyncManager", "Error processing locations realtime snapshot", e)
                            }
                        }
                    }
                }
        } catch (t: Throwable) {
            Log.e("FirestoreSyncManager", "startRealtimeSync failed", t)
        }
    }

    fun stopSync() {
        try {
            itemsListenerRegistration?.remove()
            itemsListenerRegistration = null
            locationsListenerRegistration?.remove()
            locationsListenerRegistration = null
            _syncState.value = SyncState.IDLE
        } catch (t: Throwable) {
            Log.e("FirestoreSyncManager", "stopSync failed", t)
        }
    }

    // Helper serialization / deserialization methods
    private fun serializeItem(item: InventoryItem): Map<String, Any> {
        return mapOf(
            "uuid" to item.uuid,
            "name" to item.name,
            "category" to item.category,
            "quantity" to item.quantity,
            "location" to item.location,
            "value" to item.value,
            "notes" to item.notes,
            "imageUrl" to (item.imageUrl ?: ""),
            "timestamp" to item.timestamp
        )
    }

    private fun deserializeItem(doc: DocumentSnapshot): InventoryItem? {
        return try {
            val uuid = doc.getString("uuid") ?: doc.id
            val name = doc.getString("name") ?: return null
            val category = doc.getString("category") ?: "Lainnya"
            val quantity = doc.getLong("quantity")?.toInt() ?: 1
            val location = doc.getString("location") ?: "Tanpa Lokasi"
            val value = doc.getDouble("value") ?: 0.0
            val notes = doc.getString("notes") ?: ""
            val imageUrlStr = doc.getString("imageUrl")
            val imageUrl = if (imageUrlStr.isNullOrBlank()) null else imageUrlStr
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

            InventoryItem(
                uuid = uuid,
                name = name,
                category = category,
                quantity = quantity,
                location = location,
                value = value,
                notes = notes,
                imageUrl = imageUrl,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to deserialize item doc: ${doc.id}", e)
            null
        }
    }

    private fun serializeLocation(loc: Location): Map<String, Any> {
        return mapOf(
            "uuid" to loc.uuid,
            "name" to loc.name,
            "timestamp" to loc.timestamp
        )
    }

    private fun deserializeLocation(doc: DocumentSnapshot): Location? {
        return try {
            val uuid = doc.getString("uuid") ?: doc.id
            val name = doc.getString("name") ?: return null
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

            Location(
                uuid = uuid,
                name = name,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed to deserialize location doc: ${doc.id}", e)
            null
        }
    }
}
