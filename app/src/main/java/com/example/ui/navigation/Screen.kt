package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val stringKey: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "dashboard", Icons.Default.Dashboard)
    object Scan : Screen("scan", "scan_receipt", Icons.Default.DocumentScanner)
    object Transactions : Screen("transactions", "transactions", Icons.Default.ReceiptLong)
    object Rooms : Screen("rooms", "rooms", Icons.Default.Group)
    object Budget : Screen("budget", "budgets", Icons.Default.PieChart)
    object Reports : Screen("reports", "reports", Icons.Default.Assessment)
    object Settings : Screen("settings", "settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Scan, Transactions, Rooms, Budget, Reports, Settings)
    }
}
