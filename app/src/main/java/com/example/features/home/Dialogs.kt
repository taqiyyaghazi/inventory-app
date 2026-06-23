package com.example.features.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.core.model.InventoryItem
import com.example.core.model.Location
import com.example.core.viewmodel.SortOption
import com.example.features.manage.getCategoryColors
import com.example.features.manage.getCategoryIcon
import com.example.features.manage.formatRupiah

@Composable
fun SortSelectionDialog(
    currentOption: SortOption,
    onOptionSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Urutkan Barang",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SortOption.values().forEach { option ->
                    val isSelected = option == currentOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                            .clickable { onOptionSelect(option) }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Terpilih",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemFormDialog(
    title: String,
    itemToEdit: InventoryItem? = null,
    categories: List<String>,
    locations: List<Location>,
    onSubmit: (String, String, Int, String, Double, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: categories.firstOrNull() ?: "Lainnya") }
    var quantityString by remember { mutableStateOf(itemToEdit?.quantity?.toString() ?: "1") }
    var location by remember { mutableStateOf(itemToEdit?.location ?: locations.firstOrNull()?.name ?: "Tanpa Lokasi") }
    var valueString by remember { mutableStateOf(itemToEdit?.value?.toInt()?.toString() ?: "") }
    var notes by remember { mutableStateOf(itemToEdit?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(itemToEdit?.imageUrl) }
    
    var currentPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                imageUrl = currentPhotoUri?.toString()
            }
        }
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Copy the image from gallery to app's internal storage for persistence
                val copiedUri = context.copyUriToInternalStorage(uri)
                if (copiedUri != null) {
                    imageUrl = copiedUri.toString()
                } else {
                    imageUrl = uri.toString() // Fallback
                }
            }
        }
    )
    
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf("") }

    LaunchedEffect(locations) {
        if (itemToEdit == null && location == "Tanpa Lokasi" && locations.isNotEmpty()) {
            locations.firstOrNull()?.name?.let {
                location = it
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .systemBarsPadding()
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Barang") },
                        placeholder = { Text("Misal: Macbook Pro, Kemeja") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("form_name_field")
                    )
                }

                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori") },
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDropdown = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_category_field")
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCategoryDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityString,
                            onValueChange = { quantityString = it },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("form_qty_field")
                        )

                        OutlinedTextField(
                            value = valueString,
                            onValueChange = { valueString = it },
                            label = { Text("Harga/Unit (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.5f).testTag("form_value_field")
                        )
                    }
                }

                item {
                    var showLocationDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lokasi Penyimpanan") },
                            trailingIcon = {
                                IconButton(onClick = { showLocationDropdown = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_location_field")
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showLocationDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showLocationDropdown,
                            onDismissRequest = { showLocationDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            locations.forEach { loc ->
                                DropdownMenuItem(
                                    text = { Text(loc.name) },
                                    onClick = {
                                        location = loc.name
                                        showLocationDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Tambahan") },
                        placeholder = { Text("Kondisi, tanggal garansi, dll (opsional)") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("form_notes_field")
                    )
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Foto Inventaris", 
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        if (imageUrl != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Foto $name",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { imageUrl = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                                        .size(32.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Hapus foto", tint = Color.White)
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val file = context.createImageFile()
                                        val uri = context.getUriForFile(file)
                                        currentPhotoUri = uri
                                        cameraLauncher.launch(uri)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Kamera")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kamera")
                                }
                                
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Galeri")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Galeri")
                                }
                            }
                        }
                    }
                }

                if (validationError.isNotEmpty()) {
                    item {
                        Text(
                            text = validationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("form_cancel_button")
                        ) {
                            Text("Batal")
                        }
                        
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    validationError = "Nama barang wajib diisi!"
                                    return@Button
                                }
                                val qty = quantityString.toIntOrNull() ?: 0
                                if (qty <= 0) {
                                    validationError = "Jumlah barang harus lebih besar dari 0!"
                                    return@Button
                                }
                                val valDouble = valueString.toDoubleOrNull() ?: 0.0
                                val locStr = if (location.isBlank()) "Tanpa Lokasi" else location
                                
                                onSubmit(name, selectedCategory, qty, locStr, valDouble, notes, imageUrl)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("form_submit_button")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailsDialog(
    item: InventoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Topic Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labelColors = getCategoryColors(item.category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(labelColors.first.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = labelColors.first
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Tutup Detail")
                    }
                }

                // Item Name Display
                Column {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "Foto Barang",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .padding(bottom = 12.dp)
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Disimpan di: ${item.location}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Calculations
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Kuantitas",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "${item.quantity} Unit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Harga Per Unit",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = formatRupiah(item.value),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                // Accumulative worth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Estimasi Aset:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = formatRupiah(item.value * item.quantity),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Notes
                if (item.notes.isNotBlank()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Catatan Tambahan:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Actions Bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus")
                    }

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ubah")
                    }
                }
            }
        }
    }
}
