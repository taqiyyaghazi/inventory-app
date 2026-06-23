package com.example.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.dashboard.DashboardTab
import com.example.core.model.InventoryItem
import com.example.features.home.ItemDetailsDialog
import com.example.features.home.ItemFormDialog
import com.example.features.home.SortSelectionDialog
import com.example.core.viewmodel.InventoryViewModel
import com.example.features.home.*
import com.example.features.manage.CategoriesScreen
import com.example.features.settings.SettingsScreen
import com.example.features.stats.StatsAnalyticsScreen

@Composable
fun InventoryDashboard(
    viewModel: InventoryViewModel,
    currentTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val items by viewModel.itemsState.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val locations by viewModel.locationsState.collectAsStateWithLifecycle()
    val categoriesEntities by viewModel.categoriesState.collectAsStateWithLifecycle()

    val categoriesList = listOf("Semua") + categoriesEntities.map { it.name }
    val categoriesOnlyList = categoriesEntities.map { it.name }

    val authManager = viewModel.authManager
    val userProfile by authManager.userState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncManager.syncState.collectAsStateWithLifecycle()

    val webClientId = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId) else "missing"
    } catch (e: Exception) {
        "missing"
    }
    
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                authManager.signInWithGoogle(
                    account,
                    onSuccess = {
                        android.util.Log.d("InventoryDashboard", "Successfully linked with Firebase!")
                        android.widget.Toast.makeText(context.applicationContext, "Berhasil masuk dengan Google!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        android.util.Log.e("InventoryDashboard", "Firebase integration failed", e)
                        android.widget.Toast.makeText(
                            context.applicationContext,
                            "Gagal masuk Firebase: ${e.message}\nPastikan SHA-1 terdaftar di Firebase Console & Provider Google Login aktif.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        
                        // Fallback mode if Firebase isn't fully configured
                        authManager.signInMockUser(account.displayName ?: "User", account.email ?: "unknown")
                        android.widget.Toast.makeText(context.applicationContext, "Masuk mode Fallback Sandbox karena Firebase error.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } catch (e: Exception) {
            val statusCode = if (e is com.google.android.gms.common.api.ApiException) e.statusCode else -1
            val errMessage = e.message ?: "Unknown Error"
            android.util.Log.e("InventoryDashboard", "Google Sign-In failed or cancelled. Status Code: $statusCode", e)
            
            try {
                android.widget.Toast.makeText(
                    context.applicationContext, 
                    "Login Google gagal/dibatalkan (Code: $statusCode).\nAlasan: $errMessage\nPastikan SHA-1 terdaftar di Firebase Console & Provider Google Login aktif.", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (t: Throwable) {
                android.util.Log.e("InventoryDashboard", "Failed to show toast", t)
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var itemToShowDetails by remember { mutableStateOf<InventoryItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar - Header Section
            HeaderSection(
                selectedTab = currentTab,
                onTabSelect = onTabChange
            )

            // Dynamic Tab Views
            when (currentTab) {
                DashboardTab.HOME -> {
                    // Search & Sort bar
                    SearchAndSortBar(
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onSortClick = { showSortMenu = true },
                        currentSort = sortBy
                    )

                    // Category scrollable filter chips
                    CategorySelectorRow(
                        categories = categoriesList,
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.setSelectedCategory(it) }
                    )

                    // Compact Polish value statistics tray
                    StatsCompactTray(
                        totalItems = stats.totalItems,
                        totalValue = stats.totalValue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Title header inside list
                    Text(
                        text = "DAFTAR INVENTARIS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    // Inventory listing
                    if (items.isEmpty()) {
                        EmptyStateSection(
                            isFilterActive = searchQuery.isNotEmpty() || selectedCategory != "Semua",
                            onClearFilters = {
                                viewModel.setSearchQuery("")
                                viewModel.setSelectedCategory("Semua")
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("inventory_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items, key = { it.id }) { item ->
                                InventoryItemCard(
                                    item = item,
                                    onIncrement = { viewModel.updateItemQuantity(item, item.quantity + 1) },
                                    onDecrement = { viewModel.updateItemQuantity(item, item.quantity - 1) },
                                    onTap = { itemToShowDetails = item },
                                    onEdit = { itemToEdit = item },
                                    onDelete = { viewModel.removeItem(item) }
                                )
                            }
                        }
                    }
                }

                DashboardTab.STATS -> {
                    StatsAnalyticsScreen(
                        stats = stats,
                        items = items
                    )
                }

                DashboardTab.MANAGE -> {
                    CategoriesScreen(
                        categories = categoriesEntities,
                        items = items,
                        locations = locations,
                        onSelectCategory = { cat ->
                            viewModel.setSelectedCategory(cat)
                            onTabChange(DashboardTab.HOME)
                        },
                        onAddLocation = { name ->
                            viewModel.addLocation(name)
                        },
                        onEditLocation = { loc, newName ->
                            viewModel.updateLocation(loc, newName)
                        },
                        onDeleteLocation = { loc ->
                            viewModel.deleteLocation(loc)
                        },
                        onAddCategory = { name ->
                            viewModel.addCategory(name)
                        },
                        onEditCategory = { cat, newName ->
                            viewModel.updateCategory(cat, newName)
                        },
                        onDeleteCategory = { cat ->
                            viewModel.deleteCategory(cat)
                        }
                    )
                }

                DashboardTab.SETTINGS -> {
                    SettingsScreen(
                        totalAset = stats.totalItems,
                        totalValue = stats.totalValue,
                        userProfile = userProfile,
                        syncState = syncState,
                        onSignInFake = { name, email ->
                            authManager.signInMockUser(name, email)
                        },
                        onSignInReal = {
                            if (webClientId == "missing" || webClientId.contains("dummy")) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Kredensial Oauth Google belum diatur. Gunakan mode Sandbox atau isi google-services.json",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                try {
                                    val gmsAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                                    if (gmsAvailable == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                                        val gsc = authManager.getGoogleSignInClient(webClientId)
                                        val intent = gsc.signInIntent
                                        if (intent.resolveActivity(context.packageManager) != null) {
                                            googleSignInLauncher.launch(intent)
                                        } else {
                                            android.widget.Toast.makeText(context.applicationContext, "Tidak ada aplikasi untuk menangani Google Sign-In.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context.applicationContext,
                                            "Layanan Google Play tidak tersedia/ter-update. Menggunakan mode Sandbox.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        authManager.signInMockUser("Akun Emulator (GMS Missing)", "emulator@sandbox.local")
                                    }
                                } catch (e: Throwable) {
                                    android.util.Log.e("InventoryDashboard", "Launch Google Sign In failed", e)
                                    android.widget.Toast.makeText(
                                        context.applicationContext,
                                        "Kesalahan saat mencoba Google Sign-In.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onSignOut = {
                            authManager.signOut()
                        },
                        onTriggerSync = {
                            userProfile?.let { viewModel.syncManager.triggerPushSync(it.uid) }
                        },
                        onSeedData = {
                            println("[DEBUG MAIN] onSeedData callback invoked in InventoryDashboard!")
                            viewModel.addItems(
                                listOf(
                                    InventoryItem(name = "MacBook Pro M2 Studio", category = "Elektronik", quantity = 1, location = "Meja Kerja Atas", value = 28500000.0, notes = "Unit laptop m2 utama kelengkapan charger.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Sony WH-1000XM5 ANC", category = "Elektronik", quantity = 1, location = "Kabinet Tengah", value = 5190000.0, notes = "Headphones noise-cancelling hitam.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Herman Miller Aeron Chair", category = "Perabotan", quantity = 2, location = "Ruang Studio Kerja", value = 16800000.0, notes = "Kursi ergonomis mesh gray.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Fujifilm X-T5 Mirrorless", category = "Elektronik", quantity = 1, location = "Dry Box Kamar", value = 26900000.0, notes = "Include lens prime f/1.4.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Smart Watch Active", category = "Elektronik", quantity = 1, location = "Laci Samping", value = 3200000.0, notes = "Kondisi sangat mulus terpasang tempered glass.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Kemeja Kasual Corduroy", category = "Pakaian", quantity = 4, location = "Lemari Pakaian", value = 380000.0, notes = "Warna navy & cokelat.", timestamp = System.currentTimeMillis()),
                                    InventoryItem(name = "Buku Panduan Kotlin Core", category = "Buku & Dokumen", quantity = 2, location = "Rak Belajar", value = 185000.0, notes = "Edisi hardcover revisi.", timestamp = System.currentTimeMillis())
                                )
                            )
                        },
                        onClearData = {
                            viewModel.clearAllItems()
                        }
                    )
                }
            }
        }

        // Add Button FAB floating nicely
        if (currentTab == DashboardTab.HOME) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("add_item_fab"),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Barang",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Sorting menu popup dialog
        if (showSortMenu) {
            SortSelectionDialog(
                currentOption = sortBy,
                onOptionSelect = {
                    viewModel.setSortBy(it)
                    showSortMenu = false
                },
                onDismiss = { showSortMenu = false }
            )
        }

        // Add Item Dialog
        if (showAddDialog) {
            ItemFormDialog(
                title = "Tambah Barang Baru",
                categories = categoriesOnlyList,
                locations = locations,
                onSubmit = { name, category, qty, loc, valDouble, notes, imageUrl ->
                    viewModel.addItem(name, category, qty, loc, valDouble, notes, imageUrl)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        // Edit Item dialog
        itemToEdit?.let { item ->
            ItemFormDialog(
                title = "Ubah Informasi Barang",
                itemToEdit = item,
                categories = categoriesOnlyList,
                locations = locations,
                onSubmit = { name, category, qty, loc, valDouble, notes, imageUrl ->
                    viewModel.updateItem(
                        item.copy(
                            name = name,
                            category = category,
                            quantity = qty,
                            location = loc,
                            value = valDouble,
                            notes = notes,
                            imageUrl = imageUrl,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    itemToEdit = null
                },
                onDismiss = { itemToEdit = null }
            )
        }

        // Detailed overlay dialog sheet
        itemToShowDetails?.let { item ->
            ItemDetailsDialog(
                item = item,
                onEdit = {
                    itemToEdit = item
                    itemToShowDetails = null
                },
                onDelete = {
                    viewModel.removeItem(item)
                    itemToShowDetails = null
                },
                onDismiss = { itemToShowDetails = null }
            )
        }
    }
}
