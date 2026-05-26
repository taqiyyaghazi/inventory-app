package com.example.features.manage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

// Color Utility Map helper to provide a cohesive color layout palette
fun getCategoryColors(category: String): Pair<Color, Color> {
    return when (category) {
        "Elektronik" -> Pair(Color(0xFF0F766E), Color(0xFFCCFBF1)) // Teal
        "Pakaian" -> Pair(Color(0xFFBE185D), Color(0xFFFCE7F3))    // Pink / Crimson
        "Buku & Dokumen" -> Pair(Color(0xFF1D4ED8), Color(0xFFDBEAFE)) // Royal Blue
        "Perabotan" -> Pair(Color(0xFFB45309), Color(0xFFFEF3C7))  // Amber / Gold
        "Alat Rumah Tangga" -> Pair(Color(0xFF6D28D9), Color(0xFFEDE9FE)) // Purple / Dark Orchid
        else -> Pair(Color(0xFF475569), Color(0xFFF1F5F9))         // Slate grey fallback
    }
}

// Icon Helper
@Composable
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Elektronik" -> Icons.Default.Star
        "Pakaian" -> Icons.Default.ShoppingCart
        "Buku & Dokumen" -> Icons.Default.Menu
        "Perabotan" -> Icons.Default.Home
        "Alat Rumah Tangga" -> Icons.Default.Home
        else -> Icons.Default.Info
    }
}

// Rupiah custom currency formatter
fun formatRupiah(amount: Double): String {
    val locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
    val format = NumberFormat.getCurrencyInstance(locale)
    return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
