package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.theme.MyApplicationTheme
import com.example.core.viewmodel.InventoryViewModel
import com.example.features.dashboard.InventoryDashboard
import com.example.features.dashboard.DashboardTab

class MainActivity : ComponentActivity() {
    val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(DashboardTab.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background,
                            tonalElevation = 4.dp,
                            modifier = Modifier.navigationBarsPadding().height(72.dp)
                        ) {
                            NavigationBarItem(
                                modifier = Modifier.testTag("tab_home"),
                                selected = currentTab == DashboardTab.HOME,
                                onClick = { currentTab = DashboardTab.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                                label = { Text("Beranda", fontSize = 11.sp, fontWeight = if (currentTab == DashboardTab.HOME) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == DashboardTab.STATS,
                                onClick = { currentTab = DashboardTab.STATS },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Statistik") },
                                label = { Text("Statistik", fontSize = 11.sp, fontWeight = if (currentTab == DashboardTab.STATS) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTab == DashboardTab.MANAGE,
                                onClick = { currentTab = DashboardTab.MANAGE },
                                icon = { Icon(Icons.Default.Menu, contentDescription = "Kelola") },
                                label = { Text("Kelola", fontSize = 11.sp, fontWeight = if (currentTab == DashboardTab.MANAGE) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            )
                            NavigationBarItem(
                                modifier = Modifier.testTag("tab_settings"),
                                selected = currentTab == DashboardTab.SETTINGS,
                                onClick = { currentTab = DashboardTab.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Setelan") },
                                label = { Text("Setelan", fontSize = 11.sp, fontWeight = if (currentTab == DashboardTab.SETTINGS) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    InventoryDashboard(
                        viewModel = viewModel,
                        currentTab = currentTab,
                        onTabChange = { currentTab = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
