package com.example.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.auth.UserProfile
import com.example.core.data.SyncState

@Composable
fun SettingsScreen(
    totalAset: Int,
    totalValue: Double,
    userProfile: UserProfile?,
    syncState: SyncState,
    onSignInFake: (name: String, email: String) -> Unit,
    onSignInReal: () -> Unit,
    onSignOut: () -> Unit,
    onTriggerSync: () -> Unit,
    onSeedData: () -> Unit,
    onClearData: () -> Unit
) {
    var showSandboxPane by remember { mutableStateOf(false) }
    var sandboxName by remember { mutableStateOf("") }
    var sandboxEmail by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_lazy_column")
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Cloud Authentication & Firestore Sync Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (userProfile != null) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (userProfile != null) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userProfile != null) Icons.Default.CheckCircle else Icons.Default.AccountCircle,
                                contentDescription = "Sembunyi Cloud",
                                tint = if (userProfile != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (userProfile != null) "Cloud Backup Terkoneksi" else "Google Cloud Backup & Sync",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                if (userProfile != null) "Tersinkronisasi secara real-time" else "Spark Plan (100% Selamanya Free)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (userProfile != null) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (userProfile == null) {
                        // User not authenticated UI
                        Text(
                            "Secara otomatis mencadangkan data barang inventaris Anda ke Google Firebase Firestore (Rencana Gratis / Spark Plan). Data tetap aman meskipun perangkat hilang.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Real integration Google Sign-In Button
                        Button(
                            onClick = onSignInReal,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("google_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share, 
                                contentDescription = "Google Icon"
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Hubungkan Akun Google Real")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sandbox simulation button for preview
                        OutlinedButton(
                            onClick = { showSandboxPane = !showSandboxPane },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = "Sandbox")
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (showSandboxPane) "Sembunyikan Sandbox Panel" else "Coba Sandbox Simulasi (Free)")
                        }

                        AnimatedVisibility(visible = showSandboxPane) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp)
                            ) {
                                Text(
                                    "Simulasi Cloud Sandbox",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Masukkan identitas untuk mencoba simulasi cloud sync dan profil visual.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = sandboxName,
                                    onValueChange = { sandboxName = it },
                                    label = { Text("Nama") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = sandboxEmail,
                                    onValueChange = { sandboxEmail = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (sandboxName.isNotBlank() && sandboxEmail.isNotBlank()) {
                                            onSignInFake(sandboxName, sandboxEmail)
                                        }
                                    },
                                    enabled = sandboxName.isNotBlank() && sandboxEmail.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Aktifkan Sandbox Sync")
                                }
                            }
                        }
                    } else {
                        // User Authenticated UI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        userProfile.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        userProfile.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        userProfile.email,
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Sync state visualizer
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (syncState) {
                                        SyncState.IDLE -> Icons.Default.Info
                                        SyncState.SYNCING -> Icons.Default.Refresh
                                        SyncState.SUCCESS -> Icons.Default.CheckCircle
                                        SyncState.ERROR -> Icons.Default.Warning
                                    },
                                    contentDescription = "Status",
                                    tint = when (syncState) {
                                        SyncState.IDLE -> MaterialTheme.colorScheme.outline
                                        SyncState.SYNCING -> MaterialTheme.colorScheme.primary
                                        SyncState.SUCCESS -> Color(0xFF2E7D32)
                                        SyncState.ERROR -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (syncState) {
                                        SyncState.IDLE -> "Belum mencadangkan data terbaru."
                                        SyncState.SYNCING -> "Sedang menyinkronkan ke Cloud..."
                                        SyncState.SUCCESS -> "Berhasil dicadangkan! Database Real-time Aktif."
                                        SyncState.ERROR -> "Gagal sinkron. Memasuki cloud sandbox mode."
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = when (syncState) {
                                            SyncState.IDLE -> MaterialTheme.colorScheme.outline
                                            SyncState.SYNCING -> MaterialTheme.colorScheme.primary
                                            SyncState.SUCCESS -> Color(0xFF2E7D32)
                                            SyncState.ERROR -> MaterialTheme.colorScheme.error
                                        }
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions Row: Sync Now & Sign Out
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onTriggerSync,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "SyncNow", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cadangkan", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = onSignOut,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                @Suppress("DEPRECATION")
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "LogOut", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keluar Akun", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. App Info ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Inventaris Pribadi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                "Versi Pro 1.0.0",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Kelola aset, barang berharga, dan kelengkapan dokumen pribadi Anda secara cepat dan offline-first menggunakan sandboxing database lokal.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }

        // --- 3. Generator Data Percobaan ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Generator Data Percobaan",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Muat data dummy berkualitas tinggi (MacBook, Headset Sony, Herman Miller Chair dll) sesuai dengan contoh desain HTML.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            println("[DEBUG UI] Muat Data Demo Sekarang button clicked inside SettingsScreen.")
                            onSeedData()
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("seed_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Seed")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Muat Data Demo Sekarang")
                    }
                }
            }
        }

        // --- 4. Danger Zone ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "Zona Bahaya",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Penghapusan seluruh data inventaris bersifat permanen dan tidak dapat dipulihkan.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onClearData,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Erase")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kosongkan Semua Data")
                    }
                }
            }
        }
    }
}
